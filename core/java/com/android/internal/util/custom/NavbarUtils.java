/*
* Copyright (C) 2017 The Pixel Experience Project
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

import android.content.Context;

import android.os.SystemProperties;
import android.content.ContentResolver;
import android.provider.Settings;
import android.os.UserHandle;
import android.os.Handler;
import com.android.internal.util.hwkeys.ActionUtils;

public class NavbarUtils {

    public static boolean hasNavbarByDefault(Context context) {
        boolean needsNav = context.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            needsNav = false;
        } else if ("0".equals(navBarOverride)) {
            needsNav = true;
        }
        return needsNav;
    }

    public static boolean isNavigationBarEnabled(Context context){
        boolean mHasNavigationBar = false;
        boolean mNavBarOverride = false;

        // Allow a system property to override this. Used by the emulator.
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            mNavBarOverride = true;
        } else if ("0".equals(navBarOverride)) {
            mNavBarOverride = false;
        }
        mHasNavigationBar = !mNavBarOverride && Settings.Secure.getIntForUser(
                context.getContentResolver(), Settings.Secure.NAVIGATION_BAR_ENABLED,
                context.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar) ? 1 : 0,
                UserHandle.USER_CURRENT) == 1;

        return mHasNavigationBar;
    }

    public static void setNavigationBarEnabled(Context context, Boolean enabled){
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_BAR_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    public static boolean canDisableNavigationBar(Context context){
        return context.getResources().getBoolean(com.android.internal.R.bool.config_canDisableNavigationBar) || ActionUtils.isHWKeysSupported(context);
    }

    public static void reloadNavigationBar(Context context){
        setNavigationBarEnabled(context, false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setNavigationBarEnabled(context, true);
            }
        }, 1000);
    }
}
