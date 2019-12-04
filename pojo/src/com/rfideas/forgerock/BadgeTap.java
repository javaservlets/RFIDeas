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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

// if you pass 'mfa (username)' to ReaderComm, this class will write the hex value of a badgetap
// and a timestamp to http://firebaseDB/(username.json)
// 06 04 06 04 09 07 09 05 08 07 01 03 02 00 01 01
public class BadgeTap {
    private static final String DATABASE_URL = "https://forgerock-51592.firebaseio.com"; //todo put this in a props file
    private FirebaseDatabase firebaseDatabase;

    public BadgeTap() {
        try { //std firebase access
            FileInputStream serviceAccount = new FileInputStream("account-services.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            FirebaseApp.initializeApp(options);
            firebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL);

        } catch (IOException ioe) {
            System.out.println("+++ ERROR: do not proceed until your firebase credentials work (" + ioe.getMessage());
            return;
        }
    }

    public void update(String key, String value) {
        try { //std firebase update
            if (firebaseDatabase == null) {
                System.out.println("+++ ERROR: check firebase credentials please");
                return;
            }

            DatabaseReference ref = firebaseDatabase.getReference(key);
            final CountDownLatch latch = new CountDownLatch(1);

            ref.setValue(value, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        System.out.println("Data could not be written " + databaseError.getMessage());
                        latch.countDown();
                    } else {
                        System.out.println(" saved " + key + " / " + value);
                        latch.countDown();
                    }
                }
            });
            System.out.println("checking firebase credentials...");
            latch.await();
        } catch (Exception e) {
            System.out.println("+++ ERROR2: check firebase credentials" + e.getMessage());
        }
    }

    public void close() {
        System.out.println("closed...");

        firebaseDatabase.getApp().delete();
    }


}
