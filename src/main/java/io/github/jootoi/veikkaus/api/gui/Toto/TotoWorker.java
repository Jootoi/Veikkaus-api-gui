package io.github.jootoi.veikkaus.api.gui.Toto;

/* 
 * Copyright (c) 2017, Joonas Toimela
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import io.github.jootoi.veikkaus.api.gui.APITools.APIparser;
import io.github.jootoi.veikkaus.api.gui.StaticTools.SearchCollection;
import io.github.jootoi.veikkaus.api.gui.UserInterface.GUI;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;

//Sisältää varsinaisen työluupin sekä joitain avustavia metodeja.
public class TotoWorker extends Thread {

    private final GUI UI;
    boolean wasRestarted;
    private boolean stop = false;
    private final APIparser CON = new APIparser();
    private static DelayQueue<Updatable> updateQue;
    Eventcollection events;
    ArrayList<Pools> allPools = new ArrayList<>(); 
    Headers commonHeaders = new Headers.Builder()
            .add("Accept", "application/json")
            .add("Content-Type", "application/json")
            .add("X-ESA-API.Key", "ROBOT")
            .build();
    //Automaattisen päivittämisen ajastus
    int dailyUpdateTime = 8; //tuntia keskiyöstä
    boolean enableAutoUpdate = true; //ei tee mitään (vielä)
    int waitBetweenUpdates = 10; //päivitysten välinen aika minuutteina
    int startAutoUpdate = 60; //kuinka monta minuuttia ennen lähtöä automaattinen päivittäminen alkaa.
    
    //Alustaa joitakin arvoja ennen säikeen käynnistämistä.
    public TotoWorker(GUI gui, DelayQueue uq, boolean restart) {
        UI = gui;
        updateQue = uq;
        wasRestarted = restart;
    }


    @Override
    public void run() {
        if (!wasRestarted) {
            dailyUpdate();
        }

        //Mainloop
        while (!stop) {

            Updatable u = null;
            Updatable pu = updateQue.peek();

            long delay = pu != null ? pu.getDelay(TimeUnit.SECONDS) : 60; //Jos jonossa ei ole työtehtäviä odotetaan minuutti ennen uudelleen tarkistusta (ei kuuluisi tapahtua).
            if (delay > 0) {
                UI.writetoEvents("Seuraavan päivityksen oletettu odotusaika: " + delay + " sekuntia");
            }
            try {
                u = updateQue.take();
            }
            catch (InterruptedException ex) {
                UI.writetoWarnings("Odottaminen keskeytyi: " + ex);
            }
            if (u == null) {
                continue;
            }
            //Alle 2 minuuttia vanhat päivitykset ajetaan, jos päivityksen määräaika umpeutui yli 2min sitten päivitystä ei ajeta.
            if (u.getDelay(TimeUnit.MILLISECONDS) < (-120 * 1000)) {
                UI.writetoWarnings("Poistettiin kohde päivitysjonosta, koska päivityshetki oli " + u.getDelay(TimeUnit.SECONDS) + " sekuntia sitten.");
                continue;
            }
            switch (u.getType()) {
                case ODDS: {
                    int poolID = (int) u.getParameters().get("poolID");
                    Pool poolNew = null;
                    Pools p = null;
                    try {
                        p = (Pools) SearchCollection.search(allPools, "poolId", poolID);
                        poolNew = getKertoimet(poolID);
                    }
                    catch (IOException | NullPointerException ex) {
                        UI.writetoWarnings(ex.getMessage());
                    }
                    catch (IllegalAccessException ex) {
                        Logger.getLogger(TotoWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (poolNew != null && p != null) {
                        p.setPool(poolNew);
                        long newUpdateTime = u.getUpdateTime() + waitBetweenUpdates * 60 * 1000;
                        if (newUpdateTime < p.firstRaceStartTime) {
                            updateQue.add(new Updatable(Updatable.UpdateType.ODDS, u.getParameters(), newUpdateTime));
                        }
                        updateOddsbyId(p);
                    }
                }
                break;
                case RACE: {
                    int raceID = (int) u.getParameters().get("raceID");
                    Tulokset result = null;
                    try {
                        result = getResult(raceID);
                    }
                    catch (IOException ex) {
                        UI.writetoWarnings(ex.getMessage());
                    }
                    if (result != null) {

                        showResults(raceID, result);
                    }
                    else {
                        u.setUpdatesTried(u.getUpdatesTried() + 1);
                        if (u.getUpdatesTried() < 5) {
                            UI.writetoWarnings("Tulosten haku epäonnistui. raceID: " + raceID + ". Yritetään uudetaan 5 minuutin päästä.");
                            u.setUpdateTime(u.getUpdateTime() + 5 * 60 * 1000);
                            updateQue.add(u);
                        }
                        else {
                            UI.writetoWarnings("Tulosten haku epäonnistui viidennen kerran. raceID: " + raceID + ". Ei yritetä uudelleen.");
                        }

                    }
                }
                break;
                case DAILY:
                    dailyUpdate();
                    break;
                case INFO: {
                    int raceID = (int) u.getParameters().get("raceID");
                    int number = (int) u.getParameters().get("number");
                    RunnerInfoCollection res = null;
                    try {
                        res = getRunnerInfo(raceID, "pool");
                    }
                    catch (IOException ex) {
                        UI.writetoWarnings(ex.getMessage());
                    }
                    if (res != null) {
                        try {
                            RunnerInfo r = res.collection.get(number);
                            showInfo(r);
                        }
                        catch (IndexOutOfBoundsException e) {
                            UI.writetoWarnings("Jokin meni vikaan etsittäessä juoksijan tietoja: " + e);
                        }
                    }
                }
                break;
                default:
                    UI.writetoWarnings("Unhandled update type: " + u.getType());
            }
        }
    }

    @Override
    public void destroy() {
        stop = true;
    }

    

    //Hakee tiedot kaikista päivän tapahtumista.
    public Eventcollection getKohteet() throws IOException {
        UI.writetoEvents("Haetaan päivän tapahtumia...");
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/cards/today")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        Eventcollection returnable = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Päivän kohteiden haku epäonnistui: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parseEventsJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get events, error: " + ex + "\ntext returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
        }
        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }

    //Hakee kaikki lähdöt yksittäisestä tapahtumasta.
    public Racescollection getLahdot(int eventID) throws IOException {
        UI.writetoEvents("Haetaan lähtöjä eventID: " + eventID);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/card/" + Integer.toString(eventID) + "/races")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        Racescollection returnable = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Lähtöjen haku epäonnistui tapahtumalle: " + eventID + " koska: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parseRacesJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get races, error: " + ex + "\ntext returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
        }
        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }

        //Hakee kaikki pelikohteet, jotka liittyvät yksittäiseen lähtöön.
    public Poolscollection getPools(int raceID) throws IOException {
        UI.writetoEvents("Haetaan pelikohteita raceID " + raceID);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/race/" + Integer.toString(raceID) + "/pools")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        Poolscollection returnable = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Pelikohteiden haku epäonnistui lähdölle: " + raceID + " koska: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parsePoolsJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get pools, error: " + ex + "\ntext returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
            if(returnable != null) {
                for (Pools p : returnable.collection) {
                    int cur = p.poolId;
                    p.setPool(getKertoimet(cur));
                }
            }
        }

        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }

    //Hakee kaikki pelikohteet, jotka liittyvät yksittäiseen tapahtumaan.
    public Poolscollection getPoolsforEvent(int eventID) throws IOException {
        UI.writetoEvents("Haetaan pelikohteita eventID " + eventID);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/card/" + Integer.toString(eventID) + "/pools")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        Poolscollection returnable = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Pelikohteiden haku epäonnistui tapahtumalle: " + eventID + " koska: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parsePoolsJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get pools, error: " + ex + "\ntext returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
            if(returnable != null) {
                for (Pools p : returnable.collection) {
                    int cur = p.poolId;
                    p.setPool(getKertoimet(cur));
                }
            }
        }

        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }

    //Hakee pelikertoimet yksittäiselle pelikohteelle.
    public Pool getKertoimet(int poolID) throws IOException {
        UI.writetoEvents("Haetaan kertoimia poolID " + poolID);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/pool/" + Integer.toString(poolID) + "/odds")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        Pool returnable = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Kertoimien haku epäonnistui kohteelle: " + poolID + " koska: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parseOddsJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get odds, error: " + ex + "text returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
        }
        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }
    
    //Hakee yksittäisen lähdön tulokset.
    public Tulokset getResult(int raceID) throws IOException {
        Tulokset returnable = null;
        UI.writetoEvents("Haetaan tuloksia raceID " + raceID);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/race/" + Integer.toString(raceID) + "/results")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Tulosten haku epäonnistui lähdölle: " + raceID + " koska: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parseTuloksetJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get results, error: " + ex + "text returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
        }
        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }

    /*
    Metodi on suunniteltu ajettavaksi päivittäin, näinollen se tyhjentää (edellisen päivän) monia tietorakenteita ja luo ne uudestaan.
    Lisäksi metodi lisää automaattisten päivitysten ensimmäisen päivityskerran ajankohdan muuttujiin.
    
    */
    public void dailyUpdate() {
        allPools.clear();
        boolean somethingNotRight = false;
        try {
            events = getKohteet();
        }
        catch (IOException ex) {
            UI.writetoWarnings(ex.getMessage());
        }

        try {
            publishEventInfo(events.collection);
            UI.setTotoLoadProgress(events.collection.size());
            int i = 0;
            for (Events e : events.collection) {
                int id = e.cardId;
                try {
                    e.setPools(getPoolsforEvent(id));
                    allPools.addAll(e.getPools().collection);
                    e.setRaces(getLahdot(id));
                }
                catch (IOException ex) {
                    UI.writetoWarnings(ex.getMessage());
                    continue;
                }
                for (Races r : e.getRaces().collection) {
                    int raceID = r.raceId;
                    ArrayList<Pools> pools = SearchCollection.searchAll(allPools, "firstRaceId", raceID);
                    
                    r.setPools(pools);
                    for(Pools p:pools) {
                        HashMap<String,Object> params = new HashMap<>();
                        params.put("poolID", p.poolId);
                        long updateTime = p.firstRaceStartTime - startAutoUpdate*60*1000;
                        updateQue.add(new Updatable(Updatable.UpdateType.ODDS, params, updateTime));
                    }
                }
                i++;
                UI.changeTotoLoadProgress(i);
            }
            publishEventInfo(events.collection);
            fillPoolsTab(events.collection);
            
        }
    
        catch (Exception e) {
            UI.writetoWarnings("Jokin meni vikaan: " + e);
            somethingNotRight = true;
        }

        //Lisätään päivittäinen päivitys itsessään prioriteetti jonoon oikeaan ajanhetkeen.
        //Alunperin päivitys oli kovakoodattu tapahtumaan klo 8, tämän vuoksi hämäävä atribuutin nimi. Refactorointia odotellessa...
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Europe/Helsinki");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext8;
        zonedNext8 = zonedNow.withHour(dailyUpdateTime).withMinute(0).withSecond(0);
        if (zonedNow.compareTo(zonedNext8) > 0) {
            zonedNext8 = zonedNext8.plusDays(1);
        }
        if(somethingNotRight) {
            UI.writetoWarnings("Jokin meni vikaan, estetään tulevat päivitykset. Voit sammutta ohjelman");
        }
        else {
            updateQue.add(new Updatable(Updatable.UpdateType.DAILY, dailyUpdateTime));
        }
    }


    
    //Hakee pelikohteen tulokset
    private Tulokset getResultPool(int poolId) throws IOException {
        Tulokset returnable = null;
        UI.writetoEvents("Haetaan tuloksia raceID " + poolId);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/pool/" + Integer.toString(poolId) + "/results")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Tulosten haku epäonnistui kohteelle: " + poolId + " koska: " + ex);
        }
        if (res != null) {
            try {
                returnable = CON.parseTuloksetJson(res);
            }
            catch (Exception ex) {
                UI.writetoWarnings("Failed to get results, error: " + ex + "text returned by API: " + res);
                throw new IOException("Data returned by API could not be deserialized");
            }
        }
        return returnable;
    }
    
    
    //Hakee tarkemmat tiedot kisaajasta.
    public RunnerInfoCollection getRunnerInfo(int raceOrPoolId, String idType) throws IOException {
       RunnerInfoCollection returnable = null;
        UI.writetoEvents("Haetaan juoksijan tietoja " + idType + "ID: " + raceOrPoolId);
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.veikkaus.fi")
                .addPathSegments("/api/toto-info/v1/"+ idType + "/" + Integer.toString(raceOrPoolId) + "/runners")
                .build();
        Request req = new Request.Builder()
                .get()
                .headers(commonHeaders)
                .url(url)
                .build();
        String res = null;
        try {
            res = CON.con(req);
        }
        catch (IOException ex) {
            UI.writetoWarnings("Juoksijan tietojen haku epäonnistui. " +idType + "ID: " + raceOrPoolId +  " koska: " + ex);
        }
        if (res != null) {
           try {
               returnable = CON.parseRunnerInfoJson(res);
           }
           catch (Exception ex) {
               UI.writetoWarnings("Failed to get runner info, error: " + ex + "\ntext returned by API: " + res);
               throw new IOException("Data returned by API could not be deserialized");
           }
        }
        if(returnable != null && returnable.description == null) { 
            return returnable;
        }
        else if(returnable == null) return null;
        else throw new IOException("API returned error code: " + returnable.code + "with description: " + returnable.description);
    }
    
    
    
    private void fillPoolsTab(ArrayList<Events> data) {
        //Uloin luuppi luo tapahtumien välilehdet
        UI.resetTotoUI();
        for (Events e : data) {
            if (e == null) {
                continue;
            }
            UI.createTabbedPane(e.trackName, e.cardId, 4);
            if (e.getRaces() == null) {
                continue;
            }
            //Sitten luodaan lähtöjen välilehdet
            for (Races r : e.getRaces().collection) {
                String tabname = "Lähtö " + Integer.toString(r.number);
                UI.createTabbedPane(tabname, r.raceId, r.cardId);
                //Ja lopuksi pelikohteiden välilehdet ja taulukot.
                for (Pools p : r.getPools()) {
                    if (p.getPool() == null) {
                        continue;
                    }
                    //Aloitetaan taulukoiden luonti.
                    String tabname3 = p.poolName;
                    int total = p.getPool().netSales;

                    //Taulukon muoto riippuu pelikohteen tyypistä.
                    Object[][] table = null;
                    String[] labels = null;
                    switch (tabname3) {
                        case "Voittaja": {
                            table = new Object[p.getPool().odds.size()][3];
                            labels = new String[]{"Numero", "Kerroin", "Peliprosentti"};
                            int i = 0;
                            for (Odds o : p.getPool().odds) {
                                table[i][0] = o.runnerNumber;
                                table[i][1] = (double) (o.probable) / 100;
                                if (total != 0) {
                                    table[i][2] = (o.amount * 100 / total);
                                }
                                else {
                                    table[i][2] = 0;
                                }
                                i++;
                            }
                            break;
                        }
                        case "Sija": {
                            table = new Object[p.getPool().odds.size()][4];
                            labels = new String[]{"Numero", "Min.", "Max.", "Peliprosentti"};
                            int i = 0;
                            for (Odds o : p.getPool().odds) {
                                table[i][0] = o.runnerNumber;
                                table[i][1] = (double) (o.minProbable) / 100;
                                table[i][2] = (double) (o.maxProbable) / 100;
                                if (total != 0) {
                                    table[i][3] = (o.amount * 100 / total);
                                }
                                else {
                                    table[i][3] = 0;
                                }
                                i++;
                            }
                            break;
                        }
                        case "Toto5":
                        case "Toto54":
                        case "Toto75":
                        case "Toto76":
                        case "Toto8":
                        case "Toto87":
                        case "Toto86":
                        case "Toto65":
                        case "Toto6":
                        case "Toto64":
                        case "Toto4": {
                            int amount = p.getPool().odds.size();
                            int lahtoja = p.getPool().odds.get(amount - 1).legNumber;
                            table = new Object[16][lahtoja + 1];
                            labels = new String[lahtoja + 1];
                            labels[0] = "#";
                            int j = 1;
                            while (j < lahtoja + 1) {
                                labels[j] = "Lähtö " + Integer.toString(j);
                                j++;
                            }
                            int i = 0;
                            for (Odds o : p.getPool().odds) {
                                table[o.runnerNumber - 1][0] = o.runnerNumber;
                                table[o.runnerNumber - 1][o.legNumber] = o.percentage / 100;
                                i++;
                            }
                            break;
                        }
                        case "Eksakta":
                        case "Päivän Duo":
                        case "Kaksari": {
                            table = new Object[p.getPool().odds.size()][3];
                            labels = new String[]{"Juoksija 1", "Juoksija 2", "Kerroin"};
                            int i = 0;
                            for (Odds o : p.getPool().odds) {
                                table[i][0] = o.runnerNumber1;
                                table[i][1] = o.runnerNumber2;
                                table[i][2] = (double) (o.probable) / 100;
                                i++;
                            }
                            break;
                        }
                        case "Troikka": {
                            table = new Object[p.getPool().odds.size()][4];
                            labels = new String[]{"Juoksija 1", "Juoksija 2", "Juoksija 3", "Kerroin"};
                            int i = 0;
                            for (Odds o : p.getPool().odds) {
                                table[i][0] = o.runnerNumber1;
                                table[i][1] = o.runnerNumber2;
                                table[i][2] = o.runnerNumber3;
                                table[i][3] = (double) (o.probable) / 100;
                                i++;
                            }
                            break;
                        }
                        case "Sijapari": {
                            table = new Object[p.getPool().odds.size()][4];
                            labels = new String[]{"Juoksija 1", "Juoksija 2", "Min.", "Max."};
                            int i = 0;
                            for (Odds o : p.getPool().odds) {
                                table[i][0] = o.runnerNumber1;
                                table[i][1] = o.runnerNumber2;
                                table[i][2] = (double) o.minProbable / 100;
                                table[i][3] = (double) o.maxProbable / 100;
                                i++;
                            }
                            break;
                        }
                        default:
                            UI.writetoWarnings("Pelikohteen nimen tunnistamisessa epäonnistuttiin: " + tabname3);
                            break;
                    }
                    boolean addListener = ("Voittaja".equalsIgnoreCase(tabname3) || "Sija".equalsIgnoreCase(tabname3));
                    UI.addOddsContent(r.raceId, tabname3, table, labels, p.getPool().updated, p.poolId, addListener);
                }
            }
        }
    }
    
    private void updateOddsbyId(Pools data) {
        Object[][] table = null;
        String[] labels = null;
        int total = data.getPool().netSales;
        switch (data.poolName) {
            case "Voittaja": {
                table = new Object[data.getPool().odds.size()][3];
                labels = new String[]{"Numero", "Kerroin", "Peliprosentti"};
                int i = 0;
                for (Odds o : data.getPool().odds) {
                    table[i][0] = o.runnerNumber;
                    table[i][1] = (double) (o.probable) / 100;
                    if (total != 0) {
                        table[i][2] = (o.amount * 100 / total);
                    }
                    else {
                        table[i][2] = 0;
                    }
                    i++;
                }
                break;
            }
            case "Sija": {
                table = new Object[data.getPool().odds.size()][4];
                labels = new String[]{"Numero", "Min.", "Max.", "Peliprosentti"};
                int i = 0;
                for (Odds o : data.getPool().odds) {
                    table[i][0] = o.runnerNumber;
                    table[i][1] = (double) (o.minProbable) / 100;
                    table[i][2] = (double) (o.maxProbable) / 100;
                    if (total != 0) {
                        table[i][3] = (o.amount * 100 / total);
                    }
                    else {
                        table[i][3] = 0;
                    }
                    i++;
                }
                break;
            }
            case "Toto5":
            case "Toto54":
            case "Toto75":
            case "Toto76":
            case "Toto8":
            case "Toto87":
            case "Toto86":
            case "Toto65":
            case "Toto6":
            case "Toto64":
            case "Toto4": {
                int amount = data.getPool().odds.size();
                int lahtoja = data.getPool().odds.get(amount - 1).legNumber;
                table = new Object[16][lahtoja + 1];
                labels = new String[lahtoja + 1];
                labels[0] = "#";
                int j = 1;
                while (j < lahtoja + 1) {
                    labels[j] = "Lähtö " + Integer.toString(j);
                    j++;
                }
                for (Odds o : data.getPool().odds) {
                    table[o.runnerNumber - 1][0] = o.runnerNumber;
                    table[o.runnerNumber - 1][o.legNumber] = o.percentage / 100;
                }
                break;
            }
            case "Eksakta":
            case "Päivän Duo":
            case "Kaksari": {
                table = new Object[data.getPool().odds.size()][3];
                labels = new String[]{"Juoksija 1", "Juoksija 2", "Kerroin"};
                int i = 0;
                for (Odds o : data.getPool().odds) {
                    table[i][0] = o.runnerNumber1;
                    table[i][1] = o.runnerNumber2;
                    table[i][2] = (double) (o.probable) / 100;
                    i++;
                }
                break;
            }
            case "Troikka": {
                table = new Object[data.getPool().odds.size()][4];
                labels = new String[]{"Juoksija 1", "Juoksija 2", "Juoksija 3", "Kerroin"};
                int i = 0;
                for (Odds o : data.getPool().odds) {
                    table[i][0] = o.runnerNumber1;
                    table[i][1] = o.runnerNumber2;
                    table[i][2] = o.runnerNumber3;
                    table[i][3] = (double) (o.probable) / 100;
                    i++;
                }
                break;
            }
            case "Sijapari": {
                table = new Object[data.getPool().odds.size()][4];
                labels = new String[]{"Juoksija 1", "Juoksija 2", "Min.", "Max."};
                int i = 0;
                for (Odds o : data.getPool().odds) {
                    table[i][0] = o.runnerNumber1;
                    table[i][1] = o.runnerNumber2;
                    table[i][2] = (double) o.minProbable / 100;
                    table[i][3] = (double) o.maxProbable / 100;
                    i++;
                }
                break;
            }
            default:
                UI.writetoWarnings("Pelikohteen nimen tunnistamisessa epäonnistuttiin: " + data.poolName);
                break;
        }
        boolean addListener = ("Voittaja".equalsIgnoreCase(data.poolName) || "Sija".equalsIgnoreCase(data.poolName));
        UI.replaceOddsContent(data.poolId, data.poolName, table, labels, data.getPool().updated, addListener);
    }
    
    private void showResults(int raceID, Tulokset results) {
        if (results.toteResult == null) {
            return;
        }
        ArrayList<ArrayList<String>> resultArray = new ArrayList<>();
        ArrayList<String> generalResults = new ArrayList<>();
        generalResults.add("Kokonaistulokset:");
        generalResults.add("Tulojärjestys: " + results.toteResult);
        String poisj = "";
        for (int n : results.scratched) {
            poisj += " " + n;
        }
        generalResults.add("Poisjääneet:" + poisj);
        resultArray.add(generalResults);
        ArrayList<String> sij = new ArrayList<>();
        sij.add("Sijapeli");
        for (Results res : results.results) {
            ArrayList<String> p = new ArrayList<>();
            switch (res.poolType) {
                case "VOI": {
                    p.add(res.poolName + " peli");
                    p.add("Numero: " + res.combination);
                    p.add("Kerroin: " + (double) res.probable / 100);
                    p.add("Hevosen nimi: " + res.horseName);
                    p.add("Ohjastajan nimi: " + res.driverFirstName + " " + res.driverLastName);
                    break;
                }
                case "SIJ": {
                    sij.add(" ");
                    sij.add("Numero: " + res.combination);
                    sij.add("Kerroin: " + (double) res.probable / 100);
                    sij.add("Hevosen nimi: " + res.horseName);
                    sij.add("Ohjastajan nimi: " + res.driverFirstName + " " + res.driverLastName);
                    break;
                }
                case "TRO":
                case "DUO":
                case "KAK": {
                    p.add(res.poolName + " peli");
                    p.add("Oikearivi: " + res.combination);
                    p.add("Kerroin: " + (double) res.probable / 100);
                    break;
                }
            }
            resultArray.add(p);
        }
        resultArray.add(sij);
        UI.addResultsTab(resultArray, raceID);
    }
    
    private void showInfo(RunnerInfo data) {
         String[] messages = new String[12];
            messages[0]="Hevosen nimi: " + data.horseName;
            messages[1]="Lähtö numero: " + data.startNumber;
            messages[2]="Etukengät: " + data.frontShoes;
            messages[3]="Takakengät: " + data.rearShoes;
            messages[4]="Ikä: " + data.horseAge;
            messages[5]="Isä: " + data.sire;
            messages[6]="Emä: " + data.dam;
            messages[7]="Sukupuoli: " + data.gender;
            messages[8]="";
            messages[9]="Ohjastajan nimi: " + data.driverName;
            messages[10]="Valmentajan nimi: " + data.coachName;
            messages[11]="Omistajan nimi: " + data.ownerName;
            UI.showInfoDialog(messages);

    }
    
    private void publishEventInfo(ArrayList<Events> data) {
        ArrayList<Object[][]> dataTables = new ArrayList<>();
        ArrayList<Object[]> dataLabels = new ArrayList<>();
        ArrayList<String> tabNames = new ArrayList<>();
        
        //First the general info tab
        Object[][] generalData = new Object[data.size()][4];
        Object[] generalLabels = new Object[]{"Tapahtuman tunniste (EventID)", "Maa", "Paikkakunta", "Tapahtuma alkaa"};
        
        int i = 0;
        while (i < data.size()) {
            Events cur = data.get(i);
            generalData[i][0] = cur.cardId;
            generalData[i][1] = cur.country;
            generalData[i][2] = cur.trackName;
            generalData[i][3] = LocalDateTime.ofInstant(Instant.ofEpochMilli(cur.firstRaceStart), ZoneId.systemDefault()).toString();
            i++;
        }
        dataTables.add(generalData);dataLabels.add(generalLabels);tabNames.add("Päivän tapahtumat");
        
        //Then a generic table for each event
        for(Events event:data) {
            if (event.getRaces() == null) {continue;}
            String[] labels = new String[]{"Lähtö #", "Matka", "Lähtö aika", "Monte?"};
            Object[][] table = new Object[event.getRaces().collection.size()][4];
            int j = 0;
            for (Races r : event.getRaces().collection) {
                if (r == null) {continue;}
                table[j][0] = r.number;
                table[j][1] = r.distance;
                table[j][2] = LocalDateTime.ofInstant(Instant.ofEpochMilli(r.startTime), ZoneId.systemDefault());;
                table[j][3] = r.monte;
                j++;
            }
            dataTables.add(table); dataLabels.add(labels); tabNames.add(event.trackName);
        }
        UI.addEventInfo(dataTables, dataLabels, tabNames);
        
    }
}
