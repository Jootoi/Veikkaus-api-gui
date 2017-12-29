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
import java.util.ArrayList;

public class RunnerInfo extends Updatable {
    int runnerId;
    int raceId;
    String horseName;
    int startNumber;
    int startTrack;
    int distance;
    boolean scratched;
    int prize;
    String frontShoes;
    String rearShoes;
    boolean frontShoesChanged;
    boolean rearShoesChanged;
    String sire;
    String dam;
    String damSire;
    int horseAge;
    String birthDate;
    String gender;
    Color color;
    String driverName;
    String driverNameInitials;
    String driverLicenseClass;
    String driverOutfitColors;
    String driverRacingColors;
    String driverHelmetColors;
    String driverStats;
    String coachName;
    String coachNameInitials;
    String ownerName;
    String ownerHomeTown;
    boolean specialCart;
    Stats stats;
    ArrayList<PrevStarts> prevStarts;
    
    
    
    
    
    public RunnerInfo() {}
	public RunnerInfo(int startNum, int id) {
		startNumber = startNum;
		raceId = id;
		super.updateTime = System.currentTimeMillis();
	}
    
    
    
    
    //Error block
    int code;
    String description;
}
