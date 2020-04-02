package com.android.server.wm;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import com.android.internal.util.ScreenshotHelper;
import com.android.internal.custom.longshot.injector.ScreenshotHelperInjector;

public class OpDisplayPolicyInjector {
    private static final String EXTRA_FOCUS_WINDOW = "focusWindow";
    private static final String EXTRA_LONGSHOT = "longshot";
    private static final String TAG = "DisplayPolicyInjector";

    public static void stopLongshotConnection(ScreenshotHelper screenshotHelper, WindowState focusedWindow) {
        ScreenshotHelperInjector screenshotHelperInjector;
        if (screenshotHelper != null && (screenshotHelperInjector = screenshotHelper.getScreenshotHelperInjector()) != null) {
            screenshotHelperInjector.stopLongshotConnection();
            if (focusedWindow != null) {
                focusedWindow.longshotStop();
            }
        }
    }

    public static void takeScreenshot(WindowState focusedWindow, Context context, boolean isDocked, ScreenshotHelper screenshotHelper, boolean isUserSetupComplete, int displayRotation, boolean statusBarVisible, boolean navigationBarVisible, Handler handler, int screenshotType) {
        boolean longshot;
        boolean inMultiWindow = focusedWindow != null ? focusedWindow.inMultiWindowMode() : false;
        boolean dockMinimized = isDocked;
        if (screenshotType == 2 || !isUserSetupComplete || !isDeviceProvisioned(context) || ((inMultiWindow && !dockMinimized) || displayRotation != 0)) {
            longshot = false;
        } else {
            longshot = true;
        }
        Bundle screenshotBundle = new Bundle();
        screenshotBundle.putBoolean(EXTRA_LONGSHOT, longshot);
        if (focusedWindow != null) {
            screenshotBundle.putString(EXTRA_FOCUS_WINDOW, focusedWindow.getAttrs().packageName);
        }
        ScreenshotHelperInjector screenshotHelperInjector = screenshotHelper.getScreenshotHelperInjector();
        if (screenshotHelperInjector != null) {
            screenshotHelperInjector.takeScreenshot(screenshotType, statusBarVisible, navigationBarVisible, handler, longshot, screenshotBundle);
        }
    }

    static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
    }
}
