/*
 * Copyright (C) 2021 The LineageOS Project
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

package com.android.server.custom.globalactions;

import static android.provider.Settings.Secure.POWER_MENU_ACTIONS;
import static android.provider.Settings.Secure.getStringForUser;
import static android.provider.Settings.Secure.putStringForUser;

import static com.android.internal.util.custom.globalactions.PowerMenuConstants.GLOBAL_ACTION_KEY_LOCKDOWN;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.custom.app.ContextConstants;
import com.android.internal.util.custom.globalactions.ICustomGlobalActions;
import com.android.internal.util.custom.globalactions.PowerMenuConstants;

import com.android.server.LocalServices;
import com.android.server.SystemService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @hide
 */
public class CustomGlobalActionsService extends SystemService {

    private static final String TAG = "CustomGlobalActions";

    private final Context mContext;
    private final ContentResolver mContentResolver;

    private final List<String> mLocalUserConfig = new ArrayList<String>();

    // Observes user-controlled settings
    private GlobalActionsSettingsObserver mObserver;

    public CustomGlobalActionsService(Context context) {
        super(context);

        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    private class GlobalActionsSettingsObserver extends ContentObserver {

        public GlobalActionsSettingsObserver(Context context, Handler handler) {
            super(handler);
        }

        public void observe(boolean enabled) {
        }

        @Override
        public void onChange(boolean selfChange) {
        }
    };

    private void populateUserConfig() {
        mLocalUserConfig.clear();
        mLocalUserConfig.addAll(Arrays.asList(getUserConfig()));
    }

    private String[] getUserConfig() {
        String savedActions = getStringForUser(mContentResolver,
                POWER_MENU_ACTIONS, UserHandle.USER_CURRENT);

        if (savedActions == null) {
            return mContext.getResources().getStringArray(
                    com.android.internal.R.array.config_globalActionsList);
        } else {
            return savedActions.split("\\|");
        }
    }

    private void updateUserConfigInternal(boolean enabled, String action) {
        if (enabled) {
            if (!mLocalUserConfig.contains(action)) {
                mLocalUserConfig.add(action);
            }
        } else {
            if (mLocalUserConfig.contains(action)) {
                mLocalUserConfig.remove(action);
            }
        }
        saveUserConfig();
    }

    private void saveUserConfig() {
        List<String> actions = new ArrayList<String>();
        for (String action : PowerMenuConstants.getAllActions()) {
            if (mLocalUserConfig.contains(action)) {
                actions.add(action);
            }
        }

        String s = String.join("|", actions);
        putStringForUser(mContentResolver, POWER_MENU_ACTIONS, s, UserHandle.USER_CURRENT);
    }

    @Override
    public void onStart() {
        publishBinderService(ContextConstants.CUSTOM_GLOBAL_ACTIONS_SERVICE, mBinder);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            populateUserConfig();

            mObserver = new GlobalActionsSettingsObserver(mContext, null);
            mObserver.observe(true);
        }
    }

    private final IBinder mBinder = new ICustomGlobalActions.Stub() {

        @Override
        public void updateUserConfig(boolean enabled, String action) {
            updateUserConfigInternal(enabled, action);
        }

        @Override
        public List<String> getLocalUserConfig() {
            populateUserConfig();
            return mLocalUserConfig;
        }

        @Override
        public String[] getUserActionsArray() {
            return getUserConfig();
        }

        @Override
        public boolean userConfigContains(String preference) {
            return getLocalUserConfig().contains(preference);
        }
    };
}