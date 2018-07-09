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
import java.util.HashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


public class Updatable implements Delayed {
    private long updateTime = -1;
    private int updatesTried = 0;
    
    private UpdateType type;
    private HashMap<String, Object> parameters;

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public int getUpdatesTried() {
        return updatesTried;
    }

    public void setUpdatesTried(int updatesTried) {
        this.updatesTried = updatesTried;
    }

    public UpdateType getType() {
        return type;
    }

    public void setType(UpdateType type) {
        this.type = type;
    }

    public HashMap<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(HashMap<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public enum UpdateType {
    ODDS, RACE, DAILY, INFO
    }
    public Updatable(UpdateType type, HashMap<String, Object> parameters, long updateTime) {
        this.type = type;
        this.parameters = parameters;
        this.updateTime = updateTime;
    }
    public Updatable(UpdateType type, long updateTime) {
        this.type = type;
        this.updateTime = updateTime;

    }
    public Updatable(){}
    
    @Override
    public int compareTo(Delayed o) throws NullPointerException {
        if(o != null) {
            if(this.getUpdateTime()<((Updatable)o).getUpdateTime()) {
                return -1;
            }
            else if(this.getUpdateTime() >((Updatable)o).getUpdateTime()) {
                return 1;
            }
            else {
                return 0;
            }
        }
        else { 
            throw new NullPointerException();
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long dif = getUpdateTime()-System.currentTimeMillis();
        return unit.convert(dif, TimeUnit.MILLISECONDS);
    }
}
