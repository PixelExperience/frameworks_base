/*
* Copyright (C) 2019 The Pixel Experience Project
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
import android.content.Intent;
import android.os.UserHandle;

public class PopUpCameraUtils {
    public static String MANAGE_POPUP_CAMERA_SERVICE_PERMISSION =
            "org.pixelexperience.device.MANAGE_POPUP_CAMERA_SERVICE";

    public static String ACTION_BATTERY_LED_STATE_OVERRIDE =
            "android.intent.action.BATTERY_LED_STATE_OVERRIDE";

    public static String EXTRA_OVERRIDE_BATTERY_LED_STATE =
            "android.intent.extra.OVERRIDE_BATTERY_LED_STATE";

    public static void blockBatteryLed(Context context, boolean blocked) {
        Intent intent = new Intent(ACTION_BATTERY_LED_STATE_OVERRIDE);
        intent.putExtra(EXTRA_OVERRIDE_BATTERY_LED_STATE, blocked);
        context.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }
}
