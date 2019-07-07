/*
 * Copyright (C) 2019 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.custom;

import android.content.ComponentName;
import android.content.Context;
import com.android.systemui.qs.external.CustomTile;

import com.android.internal.custom.hardware.LineageHardwareManager;
import static com.android.internal.custom.hardware.LineageHardwareManager.FEATURE_READING_ENHANCEMENT;

public class WellbeingHelper {
    private static final String mWellbeingPackage =
         "com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.screen.ui.GrayscaleTileService";

    public static boolean shouldAddTile(Context context, ComponentName componentName) {
        if (componentName.flattenToString().equals(mWellbeingPackage)) {
            try {
                LineageHardwareManager manager = LineageHardwareManager.getInstance(context);
                return !manager.isSupported(FEATURE_READING_ENHANCEMENT);
            } catch (Exception e) {
            }
        }
        return true;
    }

    public static boolean shouldAddTile(Context context, String spec) {
        if (spec.startsWith(CustomTile.PREFIX)){
            return shouldAddTile(context, CustomTile.getComponentFromSpec(spec));
        }
        return true;
    }
}
