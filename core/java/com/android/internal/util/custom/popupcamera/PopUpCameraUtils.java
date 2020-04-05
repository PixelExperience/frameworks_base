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
package com.android.internal.util.custom.popupcamera;

import android.content.Context;
import android.provider.Settings;
import android.os.UserHandle;

public class PopUpCameraUtils {
    public static String MANAGE_POPUP_CAMERA_SERVICE_PERMISSION =
            "org.pixelexperience.device.MANAGE_POPUP_CAMERA_SERVICE";

    public static boolean supportsLed(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_popUpNotificationLight);
    }

    public static boolean isLedEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.POPUP_CAMERA_LED_ENABLED, 1,
                UserHandle.USER_CURRENT) != 0;
    }

    public static void setLedEnabled(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(),
                Settings.System.POPUP_CAMERA_LED_ENABLED, enabled ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    public static void blockBatteryLed(Context context, boolean blocked) {
        Settings.System.putIntForUser(context.getContentResolver(),
                Settings.System.POPUP_CAMERA_BATTERY_LED_BLOCKED, blocked ? 1 : 0,
                UserHandle.USER_CURRENT);
    }
}
