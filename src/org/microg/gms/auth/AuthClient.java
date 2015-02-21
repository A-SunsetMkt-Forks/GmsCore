/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.auth;

import android.net.Uri;
import android.util.Log;

import org.microg.gms.common.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class AuthClient {
    private static final String TAG = "GmsAuthClient";
    private static final String SERVICE_URL = "https://android.clients.google.com/auth";

    public static AuthResponse request(AuthRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SERVICE_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        Map<String, String> httpHeaders = request.getHttpHeaders();
        for (String key : httpHeaders.keySet()) {
            connection.setRequestProperty(key, httpHeaders.get(key));
        }
        StringBuilder content = new StringBuilder();
        Map<String, String> formContent = request.getFormContent();
        for (String key : formContent.keySet()) {
            if (content.length() > 0)
                content.append("&");
            content.append(Uri.encode(key)).append("=").append(Uri.encode(formContent.get(key)));
        }

        Log.d(TAG, "-- Request --\n" + content);
        OutputStream os = connection.getOutputStream();
        os.write(content.toString().getBytes());
        os.close();

        if (connection.getResponseCode() != 200) {
            throw new IOException(connection.getResponseMessage());
        }

        String result = new String(Utils.readStreamToEnd(connection.getInputStream()));
        Log.d(TAG, "-- Response --\n" + result);
        return AuthResponse.parse(result);
    }

    public static void request(final AuthRequest request, final GmsAuthCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onResponse(request(request));
                } catch (Exception e) {
                    callback.onException(e);
                }
            }
        }).start();
    }

    public static interface GmsAuthCallback {
        void onResponse(AuthResponse response);

        void onException(Exception exception);
    }
}