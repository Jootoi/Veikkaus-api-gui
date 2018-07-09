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
import java.util.ArrayList;




public class Events {
    boolean cancelled;
    int cardId;
    String country;
    int currentRaceNumber;
    String currentRaceStatus;
    long firstRaceStart;
    boolean future;
    long lastRaceOfficial;
    boolean lunchRaces;
    String meetDate;
    int minutesToPost;
    int priority;
    String raceType;
    String trackAbreviation;
    String trackName;
    int trackNumber;
    boolean mainPerformance;
    String[] totoPools;
    long epgStartTime;
    long epgStopTime;
    int epgChannel;
    ArrayList<SpecialPools> jackpotPools;
    ArrayList<SpecialPools> bonusPools;
    
    private Racescollection races;
    private Poolscollection pools;
    public Racescollection getRaces() {
        return races;
    }
    public void setRaces(Racescollection r) {
        races = r;
    }
    
    
    public Poolscollection getPools() {
        return pools;
    }
    public void setPools(Poolscollection p) {
        pools = p;
    }
    
    public Events() {};
    

}
