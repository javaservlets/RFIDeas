/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */

package com.rfideas.forgerock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

// httpPost to a user's FR profile the hex value of badge (and store it in the sunIdentityMSISDNNumber attribute)
// (see 'to do' comments given some of the hard-coded values here

public class EnrollUser {
    private static String serveraddress = "http://robbie.freng.org:8080";

    public static String getToken() { //start by 'simply' getting an access toke(n) / tokenID
        String cook = "";
        try {
            HttpClient httpclient = HttpClients.createDefault();
            //httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
            HttpPost http = new HttpPost(serveraddress + "/openam/json/realms/root/authenticate");
            http.setHeader("X-OpenAM-Username", "amadmin");
            http.setHeader("X-OpenAM-Password", "password");
            http.setHeader("Content-Type", "application/json");
            http.setHeader("Accept-API-Version", "resource=2.0, protocol=1.0");
            http.setHeader("cache-control", "no-cache");
            HttpResponse response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                cook = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stripNoise(cook);
    }

    private static String stripNoise(String str) {
        String toke = "";
        JSONObject jobj = new JSONObject(str);
        Object idtkn = jobj.get("tokenId");
        toke = idtkn.toString();
        return toke;
    }

    // now that we have a tokenID, update the usr's record with an attribute for the badge ID/hex value
    public static String updateAttribute(String usr, String badgeId, String toke) {
        String str = "";
        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPut http = new HttpPut(serveraddress + "/openam/json/realms/root/users/" + usr); //todo pass in server instance
            http.setHeader("X-Requested-With", "XMLHttpRequest");
            http.setHeader("Connection", "keep-alive");
            http.setHeader("Content-Type", "application/json");
            http.setHeader("Accept-API-Version", "resource=2.0, protocol=1.0");
            http.setHeader("cache-control", "no-cache");
            http.setHeader("Cookie", "amlbcookie=01; iPlanetDirectoryPro=" + toke);

            // getBody will place the hex value in the attr defined there within
            http.setEntity(new StringEntity(getBody(badgeId), "utf-8"));
            HttpResponse response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                str = EntityUtils.toString(entity);
                log(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String getBody(String badgeId) {
        JSONObject jobj = new JSONObject();
        jobj.put("sunIdentityMSISDNNumber", badgeId);//todo pass in attr fld name at RT
        return jobj.toString();
    }

    public static void log(String str) {
        System.out.println(str);
    }
}