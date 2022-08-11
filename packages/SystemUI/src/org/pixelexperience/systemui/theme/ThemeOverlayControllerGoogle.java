/*
 * Copyright (C) 2022 The Pixel Experience Project
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

package org.pixelexperience.systemui.theme;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.UserManager;
import android.util.Log;

import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.flags.SystemPropertiesHelper;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.theme.ThemeOverlayApplier;
import com.android.systemui.theme.ThemeOverlayController;
import com.android.systemui.util.settings.SecureSettings;
import com.android.systemui.util.settings.SystemSettings;

import java.util.concurrent.Executor;

import javax.inject.Inject;

@SysUISingleton
public class ThemeOverlayControllerGoogle extends ThemeOverlayController {
    private final Resources resources;
    private final SystemPropertiesHelper systemProperties;

    @Inject
    public ThemeOverlayControllerGoogle(Context context, BroadcastDispatcher broadcastDispatcher,
                                  @Background Handler bgHandler, @Main Executor mainExecutor,
                                  @Background Executor bgExecutor, ThemeOverlayApplier themeOverlayApplier,
                                  SecureSettings secureSettings, WallpaperManager wallpaperManager,
                                  UserManager userManager, DeviceProvisionedController deviceProvisionedController,
                                  UserTracker userTracker, DumpManager dumpManager, FeatureFlags featureFlags,
                                  WakefulnessLifecycle wakefulnessLifecycle, SystemSettings systemSettings,
                                  SystemPropertiesHelper systemPropertiesHelper, @Main Resources resources,
                                  ConfigurationController configurationController) {
        super(context, broadcastDispatcher, bgHandler, mainExecutor, bgExecutor,
                themeOverlayApplier, secureSettings, wallpaperManager, userManager,
                deviceProvisionedController, userTracker, dumpManager, featureFlags,
                wakefulnessLifecycle, systemSettings);
        this.systemProperties = systemPropertiesHelper;
        this.resources = resources;
        configurationController.addCallback(new ConfigurationController.ConfigurationListener() {
            @Override
            public void onThemeChanged() {
                setBootColorSystemProps();
            }
        });
        int[] bootColors = getBootColors();
        int length = bootColors.length;
        int i = 0;
        while (i < length) {
            int i2 = bootColors[i];
            i++;
            Log.d("ThemeOverlayController", "Boot animation colors " + i + ": " + i2);
        }
    }

    public final void setBootColorSystemProps() {
        try {
            int[] bootColors = getBootColors();
            int i = 0;
            int length = bootColors.length;
            while (i < length) {
                int i2 = bootColors[i];
                i++;
                systemProperties.set("persist.bootanim.color" + i, i2);
                Log.d("ThemeOverlayController", "Writing boot animation colors " + i + ": " + i2);
            }
        } catch (RuntimeException unused) {
            Log.w("ThemeOverlayController", "Cannot set sysprop. Look for 'init' and 'dmesg' logs for more info.");
        }
    }

    private final int[] getBootColors() {
        return new int[]{
                this.resources.getColor(android.R.color.system_accent3_100),
                this.resources.getColor(android.R.color.system_accent1_300),
                this.resources.getColor(android.R.color.system_accent2_500),
                this.resources.getColor(android.R.color.system_accent1_100)
        };
    }
}

