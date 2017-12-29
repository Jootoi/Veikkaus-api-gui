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
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import okhttp3.*;


public class APIparser {
    private final OkHttpClient CLIENT;
    private final CookieJar cookieJar;
    private final Gson PARSER;
    
    public APIparser() {
        cookieJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<>();
            }
        };
        PARSER = new Gson(); 
        
        CLIENT = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
    }
    public String con(Request req) throws IOException {
        String result = null;
        try(Response res = CLIENT.newCall(req).execute()) {
            if(!res.isSuccessful()) {
                throw new IOException("Error code:" + res);
            }
            else {
                if(res != null && res.body() != null) {
                    result = res.body().string();
                }
            }
        }
        catch(Exception e) {
            throw e;
        }
        return result;
    }
    public Eventcollection parseEventsJson(String jsonString) throws Exception {
        Eventcollection returnable;
        returnable = PARSER.fromJson(jsonString, Eventcollection.class);
        return returnable;
    }

    public Racescollection parseRacesJson(String jsonString) throws Exception {
        Racescollection returnable;
        returnable = PARSER.fromJson(jsonString, Racescollection.class);
        return returnable;
    }

    public Poolscollection parsePoolsJson(String jsonString) throws Exception {
        Poolscollection returnable;
        returnable = PARSER.fromJson(jsonString, Poolscollection.class);
        return returnable;
    }

    public Pool parseOddsJson(String jsonString) throws Exception {
        Pool returnable;
        returnable = PARSER.fromJson(jsonString, Pool.class);
        return returnable;
    }
    public Tulokset parseTuloksetJson(String JsonString) throws Exception {
        Tulokset returnable;
        returnable = PARSER.fromJson(JsonString, Tulokset.class);
        return returnable;
    }
    public RunnerInfoCollection parseRunnerInfoJson(String JsonString) throws Exception {
        RunnerInfoCollection returnable;
        returnable = PARSER.fromJson(JsonString, RunnerInfoCollection.class);
        return returnable;
    }
    
}
