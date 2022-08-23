/*
 * Copyright (C) 2022 The Pixel Experience Project
 *               2021-2022 crDroid Android Project
 * Copyright (C) 2022 Paranoid Android
 * Copyright (C) 2022 StatiXOS
 * Copyright (C) 2023 the RisingOS Android Project
 *           (C) 2023 ArrowOS
 *           (C) 2023 The LibreMobileOS Foundation
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

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PixelPropsUtils {

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String[] sCertifiedProps =
            Resources.getSystem().getStringArray(R.array.config_certifiedBuildProperties);
    
    private static final Boolean sGPhotosSpoofPixelXL =
            Resources.getSystem().getBoolean(R.bool.config_GPhotosSpoofPixelXL);

    private static final Map<String, Object> propsToChangeGeneric;

    private static final Map<String, Object> propsToChangeRecentPixel =
            createGoogleSpoofProps("Pixel 8 Pro",
                    "google/husky/husky:14/UQ1A.240205.004/11269751:user/release-keys");

    private static final Map<String, Object> propsToChangePixelTablet =
            createGoogleSpoofProps("Pixel Tablet",
                    "google/tangorpro/tangorpro:14/UQ1A.240205.002/11224170:user/release-keys");

    private static final Map<String, Object> propsToChangePixel5a =
            createGoogleSpoofProps("Pixel 5a",
                    "google/barbet/barbet:14/UQ1A.240205.002/11224170:user/release-keys");

    private static final Map<String, Object> propsToChangePixelXL =
            createGoogleSpoofProps("Pixel XL",
                    "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");

    private static final Map<String, ArrayList<String>> propsToKeep;

    static {
        propsToKeep = new HashMap<>();
        propsToKeep.put("com.google.android.settings.intelligence", new ArrayList<>(Collections.singletonList("FINGERPRINT")));
        propsToChangeGeneric = new HashMap<>();
        propsToChangeGeneric.put("TYPE", "user");
        propsToChangeGeneric.put("TAGS", "release-keys");
    }

    private static final ArrayList<String> packagesToChangeRecentPixel = 
        new ArrayList<String> (
            Arrays.asList(
                "com.google.android.apps.emojiwallpaper",
                "com.google.android.wallpaper.effects",
                "com.google.pixel.livewallpaper",
                "com.google.android.apps.wallpaper.pixel",
                "com.google.android.apps.wallpaper",
                "com.google.android.apps.bard",
                "com.google.android.apps.customization.pixel",
                "com.google.android.apps.privacy.wildlife",
                "com.google.android.apps.subscriptions.red",
                "com.google.android.apps.photos",
                "com.google.android.gms",
                "com.google.android.googlequicksearchbox"
        ));

    private static final ArrayList<String> extraPackagesToChange = 
        new ArrayList<String> (
            Arrays.asList(
                "com.android.chrome",
                "com.breel.wallpapers20",
                "com.microsoft.android.smsorganizer",
                "com.nothing.smartcenter",
                "com.nhs.online.nhsonline",
                "com.amazon.avod.thirdpartyclient",
                "com.disney.disneyplus",
                "com.netflix.mediaclient",
                "in.startv.hotstar",
                "jp.id_credit_sp2.android"
        ));

    private static final ArrayList<String> customGoogleCameraPackages = 
        new ArrayList<String> (
            Arrays.asList(
                "com.google.android.MTCL83",
                "com.google.android.UltraCVM",
                "com.google.android.apps.cameralite"
        ));

    private static final ArrayList<String> packagesToKeep = 
        new ArrayList<String> (
            Arrays.asList(
                "com.google.android.as",
                "com.google.android.apps.motionsense.bridge",
                "com.google.android.euicc",
                "com.google.ar.core",
                "com.google.android.youtube",
                "com.google.android.apps.youtube.kids",
                "com.google.android.apps.youtube.music",
                "com.google.android.apps.wearables.maestro.companion",
                "com.google.android.apps.subscriptions.red",
                "com.google.android.apps.tachyon",
                "com.google.android.apps.tycho",
                "com.google.android.apps.restore",
                "com.google.oslo",
                "it.ingdirect.app"
        ));

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static volatile boolean sIsGms, sIsFinsky, sIsSetupWizard, sIsGoogle, sIsSamsung;

    private static String getBuildID(String fingerprint) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9]+\\.\\d+\\.\\d+\\.\\w+)");
        Matcher matcher = pattern.matcher(fingerprint);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String getDeviceName(String fingerprint) {
        String[] parts = fingerprint.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "";
    }

    private static Map<String, Object> createGoogleSpoofProps(String model, String fingerprint) {
        Map<String, Object> props = new HashMap<>();
        props.put("BRAND", "google");
        props.put("MANUFACTURER", "Google");
        props.put("ID", getBuildID(fingerprint));
        props.put("DEVICE", getDeviceName(fingerprint));
        props.put("PRODUCT", getDeviceName(fingerprint));
        props.put("MODEL", model);
        props.put("FINGERPRINT", fingerprint);
        props.put("TYPE", "user");
        props.put("TAGS", "release-keys");
        return props;
    }

    private static boolean isGoogleCameraPackage(String packageName){
        return packageName.startsWith("com.google.android.GoogleCamera") ||
            customGoogleCameraPackages.contains(packageName);
    }

    private static boolean shouldTryToCertifyDevice() {
        if (!sIsGms) return false;

        final String processName = Application.getProcessName();
        if (!processName.toLowerCase().contains("unstable")
                && !processName.toLowerCase().contains("chimera")
                && !processName.toLowerCase().contains("pixelmigrate")
                && !processName.toLowerCase().contains("instrumentation")) {
            return false;
        }

        final boolean[] shouldCertify = {true};

        setPropValue("TIME", System.currentTimeMillis());

        final boolean was = isGmsAddAccountActivityOnTop();
        final String reason = "GmsAddAccountActivityOnTop";
        if (!was) {
            spoofBuildGms();
        }
        dlog("Skip spoofing build for GMS, because " + reason + "!");
        TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean isNow = isGmsAddAccountActivityOnTop();
                if (isNow ^ was) {
                    dlog(String.format("%s changed: isNow=%b, was=%b, killing myself!", reason, isNow, was));
                    shouldCertify[0] = false;
                }
            }
        };
        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener!", e);
            spoofBuildGms();
        }
        if (shouldCertify[0]) {
            try {
                ActivityTaskManager.getService().unregisterTaskStackListener(taskStackListener); // this will be registered on next query
            } catch (Exception e) {}
        }
        return shouldCertify[0];
    }

    private static void spoofBuildGms() {
        if (sCertifiedProps == null || sCertifiedProps.length == 0) return;
        // Alter model name and fingerprint to avoid hardware attestation enforcement
        setPropValue("PRODUCT", sCertifiedProps[0].isEmpty() ? getDeviceName(sCertifiedProps[4]) : sCertifiedProps[0]);
        setPropValue("DEVICE", sCertifiedProps[1].isEmpty() ? getDeviceName(sCertifiedProps[4]) : sCertifiedProps[1]);
        setPropValue("MANUFACTURER", sCertifiedProps[2]);
        setPropValue("BRAND", sCertifiedProps[3]);
        setPropValue("MODEL", sCertifiedProps[4]);
        setPropValue("FINGERPRINT", sCertifiedProps[5]);
        if (!sCertifiedProps[6].isEmpty()) {
            setVersionFieldString("SECURITY_PATCH", sCertifiedProps[6]);
        }
        if (!sCertifiedProps[7].isEmpty() && sCertifiedProps[7].matches("\\d+")) {
            setVersionFieldInt("DEVICE_INITIAL_SDK_INT", Integer.parseInt(sCertifiedProps[7]));
        }
        setPropValue("ID", sCertifiedProps[8].isEmpty() ? getBuildID(sCertifiedProps[4]) : sCertifiedProps[8]);
        setPropValue("TYPE", sCertifiedProps[9].isEmpty() ? "user" : sCertifiedProps[9]);
        setPropValue("TAGS", sCertifiedProps[10].isEmpty() ? "release-keys" : sCertifiedProps[10]);
    }

    public static void setProps(Context context) {
        propsToChangeGeneric.forEach((k, v) -> setPropValue(k, v));

        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();
        if (packageName == null || processName == null || packageName.isEmpty()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        final boolean sIsTablet = isDeviceTablet(appContext);
        sIsGoogle = packageName.toLowerCase().contains("google") || processName.toLowerCase().contains("google");
        sIsSamsung = packageName.toLowerCase().contains("samsung") || processName.toLowerCase().contains("samsung");
        sIsGms = packageName.equals("com.google.android.gms");
        sIsFinsky = packageName.equals("com.android.vending");
        sIsSetupWizard = packageName.equals("com.google.android.setupwizard");

        if (shouldTryToCertifyDevice()) {
            return;
        }
        if (packagesToKeep.contains(packageName)
            || packagesToKeep.contains(processName)) {
            return;
        }

        Map<String, Object> propsToChange = new HashMap<>();
        if (sIsGoogle || sIsSamsung
            || extraPackagesToChange.contains(packageName)
            || extraPackagesToChange.contains(processName)) {

            if (packagesToChangeRecentPixel.contains(packageName)
                || packagesToChangeRecentPixel.contains(processName)) {
                propsToChange = propsToChangeRecentPixel;
            } else if (sIsTablet) {
                propsToChange = propsToChangePixelTablet;
            } else {
                propsToChange = propsToChangePixel5a;
            }
            if (packageName.equals("com.google.android.apps.photos")) {
                if (sGPhotosSpoofPixelXL) {
                    propsToChange = propsToChangePixelXL;
                }
                else {
                    propsToChange = propsToChangeRecentPixel;
                }
            }
            if (sIsGms
                && (processName.toLowerCase().contains("ui")
                || processName.toLowerCase().contains("gservice")
                || processName.toLowerCase().contains("gapps")
                || processName.toLowerCase().contains("learning")
                || processName.toLowerCase().contains("search")
                || processName.toLowerCase().contains("persistent"))) {
                propsToChange = propsToChangePixel5a;
            }
            // Allow process spoofing for GoogleCamera packages
            if (isGoogleCameraPackage(packageName) && (propsToChange == null || propsToChange.isEmpty())) {
                return;
            }
        }
        if (propsToChange == null || propsToChange.isEmpty()) return;
        dlog("Defining props for: " + packageName);
        for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            if (propsToKeep.containsKey(packageName) && propsToKeep.get(packageName).contains(key)) {
                dlog("Not defining " + key + " prop for: " + packageName);
                continue;
            }
            dlog("Defining " + key + " prop for: " + packageName);
            setPropValue(key, value);
        }
        // Set proper indexing fingerprint
        if (packageName.equals("com.google.android.settings.intelligence")) {
            setPropValue("FINGERPRINT", Build.VERSION.INCREMENTAL);
            return;
        }
    }

    private static boolean isDeviceTablet(Context context) {
        if (context == null) {
            return false;
        }
        Configuration configuration = context.getResources().getConfiguration();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH;
    }

    private static void setPropValue(String key, Object value) {
        try {
            dlog("Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionField(String key, Object value) {
        try {
            dlog("Defining version field " + key + " to " + value.toString());
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set version field " + key, e);
        }
    }

    private static void setVersionFieldInt(String key, int value) {
        try {
            dlog("Defining version field " + key + " to " + value);
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionFieldString(String key, String value) {
        try {
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = context.getPackageManager().getNameForUid(callingUid);
        dlog("shouldBypassTaskPermission: callingPackage:" + callingPackage);
        return callingPackage != null && callingPackage.toLowerCase().contains("google");
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                            .anyMatch(elem -> elem.getClassName().toLowerCase()
                                .contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (sIsSetupWizard || !shouldTryToCertifyDevice()) {
            Process.killProcess(Process.myPid());
            return;
        }
        if (isCallerSafetyNet() || sIsFinsky) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
