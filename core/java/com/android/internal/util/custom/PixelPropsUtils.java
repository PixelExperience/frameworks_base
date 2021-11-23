/*
 * Copyright (C) 2020 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.custom;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PixelPropsUtils {

    public static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Map<String, Object> propsToChangePixel6;

    private static final Map<String, Object> propsToChangePixel5;
    private static final String[] packagesToChangePixel5 = {
            "com.google.android.tts",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.recorder"
    };

    private static final Map<String, ArrayList<String>> propsToKeep;
    private static final String[] extraPackagesToChange = {
            "com.android.vending",
            "com.breel.wallpapers20"
    };

    private static ArrayList<String> allProps = new ArrayList<>(Arrays.asList("BRAND", "MANUFACTURER", "DEVICE", "PRODUCT", "MODEL", "FINGERPRINT"));

    private static volatile boolean sIsGms = false;

    static {
        propsToKeep = new HashMap<>();
        propsToKeep.put("com.google.android.settings.intelligence", new ArrayList<>(Collections.singletonList("FINGERPRINT")));
        propsToKeep.put("com.google.android.GoogleCamera", allProps);
        propsToChangePixel6 = new HashMap<>();
        propsToChangePixel6.put("BRAND", "google");
        propsToChangePixel6.put("MANUFACTURER", "Google");
        propsToChangePixel6.put("DEVICE", "raven");
        propsToChangePixel6.put("PRODUCT", "raven");
        propsToChangePixel6.put("MODEL", "Pixel 6 Pro");
        propsToChangePixel6.put("FINGERPRINT", "google/raven/raven:12/SD1A.210817.036/7805805:user/release-keys");
        propsToChangePixel5 = new HashMap<>();
        propsToChangePixel5.put("BRAND", "google");
        propsToChangePixel5.put("MANUFACTURER", "Google");
        propsToChangePixel5.put("DEVICE", "redfin");
        propsToChangePixel5.put("PRODUCT", "redfin");
        propsToChangePixel5.put("MODEL", "Pixel 5");
        propsToChangePixel5.put("FINGERPRINT", "google/redfin/redfin:12/SP1A.210812.015/7679548:user/release-keys");
    }

    public static void setProps(String packageName) {
        if (packageName == null) {
            return;
        }
        if (packageName.equals(PACKAGE_GMS)) {
            sIsGms = true;
        }
        if (packageName.startsWith("com.google.") || Arrays.asList(extraPackagesToChange).contains(packageName)) {
            Map<String, Object> propsToChange = propsToChangePixel6;

            if (Arrays.asList(packagesToChangePixel5).contains(packageName)) {
                propsToChange = propsToChangePixel5;
            }

            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (propsToKeep.containsKey(packageName) && propsToKeep.get(packageName).contains(key)) {
                    if (DEBUG) Log.d(TAG, "Not defining " + key + " prop for: " + packageName);
                    continue;
                }
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
        }
        // Set proper indexing fingerprint
        if (packageName.equals("com.google.android.settings.intelligence")) {
            setPropValue("FINGERPRINT", Build.VERSION.INCREMENTAL);
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (sIsGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }
    }
}