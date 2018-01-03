package io.github.jootoi.veikkaus.api.gui;

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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;

//Sisältää varsinaisen työluupin sekä joitain avustavia metodeja.
public class mainloop extends Thread {

    private final GUI UI;
    boolean wasRestarted;
    private boolean stop = false;
    private final APIparser CON = new APIparser();
    private static DelayQueue<Updatable> updateQue;
    private HashMap<Integer, HashMap<Integer, HashMap<String, Pools>>> reorderedPools = new HashMap<>();
    Eventcollection kohteet;
    ConcurrentSkipListMap<Integer, ArrayList<Pools>> allPools = new ConcurrentSkipListMap<>(); //Avaimena lähdön ID, datana lista kaikista lähdön kohteista
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
    public mainloop(GUI gui, DelayQueue uq, boolean restart) {
        UI = gui;
        updateQue = uq;
        wasRestarted = restart;
    }


    @Override
    public void run() {
        if(!wasRestarted) {
            dailyUpdate();
        }

        //Mainloop
        while (!stop) {
            
            Updatable u = null;
            Updatable pu = updateQue.peek();
            
            long delay = pu != null ? pu.getDelay(TimeUnit.SECONDS):60; //Jos jonossa ei ole työtehtäviä odotetaan minuutti ennen uudelleen tarkistusta (ei kuuluisi tapahtua).
            if (delay > 0) {
                UI.writetoEvents("Seuraavan päivityksen oletettu odotusaika: " + delay + " sekuntia");
            }
            try {
                u = updateQue.take();
            }
            catch (InterruptedException ex) {
                UI.writetoWarnings("Odottaminen keskeytyi: " + ex);
            }
            if (u != null) {
                //Alle 2 minuuttia vanhat päivitykset ajetaan, jos päivityksen määräaika umpeutui yli 2min sitten päivitystä ei ajeta.
                if (u.getDelay(TimeUnit.MILLISECONDS) < (-120 * 1000)) {
                    UI.writetoWarnings("Poistettiin kohde päivitysjonosta, koska päivityshetki oli " + u.getDelay(TimeUnit.SECONDS) + " sekuntia sitten.");
                }
                else if (u instanceof Pools) {
                    Pools p = (Pools) u;
                    Pool poolNew = null;
                    try {
                        poolNew = getKertoimet(p.poolId);
                    }
                    catch (IOException ex) {
                        UI.writetoWarnings(ex.getMessage());
                    }
                    if(poolNew != null) {
                        p.setPool(poolNew);
                    }
                    long newUpdateTime =  p.updateTime+waitBetweenUpdates*60*1000;
                    if(newUpdateTime < p.firstRaceStartTime) {
                        p.updateTime = newUpdateTime;
                        updateQue.add(p);
                    }

                    boolean succes = UI.updateOddsbyId(p.poolId, p);
                    if (!succes) {
                        UI.writetoWarnings("Epäonnistuttiin kohteen " + p.poolId + " päivittämisessä");
                    }
                }
                else if (u instanceof Races) {
                    Races r = (Races) u;
                    Tulokset result = null;
                    try {
                        result = getResult(r.raceId);
                    }
                    catch (IOException ex) {
                        UI.writetoWarnings(ex.getMessage());
                    }
                    if (result != null) {
                        
                        UI.showResults(r.raceId, result);
                     }
                    else {
                        r.updatesTried++;
                        if(r.updatesTried < 5) {
                            UI.writetoWarnings("Tulosten haku epäonnistui. raceID: " + r.raceId + ". Yritetään uudetaan 5 minuutin päästä.");
                            r.updateTime += 5*60*1000;
                            updateQue.add(r);
                        }
                        else {
                            UI.writetoWarnings("Tulosten haku epäonnistui viidennen kerran. raceID: " + r.raceId + ". Ei yritetä uudelleen.");
                        }
                        
                    }
                }
                else if (u instanceof Eventcollection) {
                    dailyUpdate();
                }
                else if (u instanceof RunnerInfo) {
                    RunnerInfo r = (RunnerInfo) u;
                    RunnerInfoCollection res = null;
                    try {
                        res = getRunnerInfo(r.raceId, "pool");
                    }
                    catch (IOException ex) {
                        UI.writetoWarnings(ex.getMessage());
                    }
                    if(res != null) {
                        try {
                            r = res.collection.get(r.startNumber);
                            UI.showInfoDialog(r);
                        }
                        catch(IndexOutOfBoundsException e) {
                            UI.writetoWarnings("Jokin meni vikaan etsittäessä juoksijan tietoja: " + e);
                        }
                    }
                    else {
                        UI.writetoWarnings("Jokin meni vikaan etsittäessä juoksijan tietoja: " + res.description);
                    }
                }
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

    //Apumetodi prioriteettijonon luomiseksi päivittäisen päivityksen yhteydessä.
    public void constructUpdateQueue(Eventcollection e) {
        updateQue.clear();
        updateQue.add(e);
        for (Events eve : e.collection) {
            for (Races r : eve.getRaces().collection) {
                //Mahdollistaa tulosten automaattisen haun, käyttöliittymä ei kuitenkaan toistaseksi näytä tuloksia,
                //joten käytänössä tuloksia ei koskaan haeta.
                if(r.updateTime != -1) {
                    updateQue.add(r);
                }
                for (Pools p : r.getPools()) {
                    if(p.updateTime != -1) {
                        updateQue.add(p);
                    }
                }
            }
        }
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
    Laittaa pelikohteet karttoihin niin, että pelikohteita on mahdollista hakea tietämättä pelikohteen IDtä.
    Lopullisesta kartassa tapahtuman IDllä saadaan kartta, jossa kaikki tapahtumaan liittyvät pelikohteet löytyvät (ensimmäisen)
    lähdön IDllä ja tämän jälkeen lähdössä olevat löytyvät pelikohteen nimellä. Samaa ilmaisutapaa käytetään käyttöliittymän kertoimet
    sivulla.
    Lopullisesta kartasta tulee hieman monimutkainen (Map<int, Map<int, Map<String, Pools>>>).
    */
    private void poolstoMap(Poolscollection pools) {
        
        for (Pools p : pools.collection) {
            HashMap<Integer, HashMap<String, Pools>> poolsById = new HashMap<>();
            HashMap<String, Pools> poolByName = new HashMap<>();
            int key = p.firstRaceId;
            if (this.allPools.containsKey(key)) {
                ArrayList tmp = this.allPools.get(key);
                tmp.add(p);
            }
            else {
                ArrayList<Pools> tmp = new ArrayList<>();
                tmp.add(p);
                this.allPools.put(key, tmp);
            }
            poolByName.put(p.poolType, p);
            poolsById.put(p.firstRaceId, poolByName);
            reorderedPools.put(p.cardId, poolsById);
        }
        
    }

    //Hakee kartasta IDtä vastaavan listan tai tekee uuden listan (eli ei palauta nullia)
    private ArrayList<Pools> getPoolsfromMap(int raceID) {
        return (this.allPools.get(raceID) != null) ? this.allPools.get(raceID):new ArrayList<>();
    }

    /*
    Metodi on suunniteltu ajettavaksi päivittäin, näinollen se tyhjentää (edellisen päivän) monia tietorakenteita ja luo ne uudestaan.
    Lisäksi metodi lisää automaattisten päivitysten ensimmäisen päivityskerran ajankohdan muuttujiin.
    
    */
    public void dailyUpdate() {
        allPools.clear();
        reorderedPools.clear();
        reorderedPools = new HashMap<>();
        boolean somethingNotRight = false;
        try {
            kohteet = getKohteet();
        }
        catch (IOException ex) {
            UI.writetoWarnings(ex.getMessage());
        }

        try {
            UI.updateEventsTable(kohteet.collection);
            UI.setTotoLoadProgress(kohteet.collection.size());
            int i = 0;
            for (Events e : kohteet.collection) {
                int id = e.cardId;
                try {
                    e.setPools(getPoolsforEvent(id));
                    poolstoMap(e.getPools());
                    e.setRaces(getLahdot(id));
                }
                catch (IOException ex) {
                    UI.writetoWarnings(ex.getMessage());
                    continue;
                }
                for (Races r : e.getRaces().collection) {
                    int raceID = r.raceId;
                    ArrayList<Pools> pools = getPoolsfromMap(raceID);
                    
                    r.setPools(pools);
                    for(Pools p:pools) {
                        p.updateTime = p.firstRaceStartTime - startAutoUpdate*60*1000;
                    }
                }
                i++;
                UI.changeTotoLoadProgress(i);
            }
            UI.racestoEvents(kohteet.collection);
            UI.fillPoolsTab(kohteet.collection);
            
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
            kohteet.updateTime = zonedNext8.toEpochSecond() * 1000;
            constructUpdateQueue(kohteet);
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
}
