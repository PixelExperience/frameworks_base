/*
 * Copyright (C) 2018 CypherOS
 * Copyright (C) 2018 PixelExperience
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>.
 */
package com.android.systemui.ambient.play;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.util.custom.ambient.play.AmbientPlayHistoryManager;
import com.android.internal.util.custom.ambient.play.AmbientPlayProvider.Observable;
import com.android.internal.util.custom.ambient.play.AmbientPlayQuietPeriod;
import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;

public class AmbientIndicationManager {

    private static final String TAG = "AmbientIndicationManager";
    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mIsRecognitionEnabled;
    private boolean mIsRecognitionEnabledOnKeyguard;
    private boolean mIsRecognitionNotificationEnabled;
    private boolean mLowBatteryRestrictionEnabled;
    private boolean mMobileDataRestrictionEnabled;
    private RecognitionObserver mRecognitionObserver;
    private String ACTION_UPDATE_AMBIENT_INDICATION = "update_ambient_indication";
    private AlarmManager mAlarmManager;
    private int lastAlarmInterval = 0;
    private long lastUpdated = 0;
    private boolean isRecognitionObserverBusy = false;
    private boolean mIsBatteryLow = false;
    private int mCurrentNetworkStatus = -1;
    private AmbientPlayQuietPeriod mAmbientPlayQuietPeriod;
    public boolean DEBUG = false;

    private List<AmbientIndicationManagerCallback> mCallbacks;

    public boolean isRecognitionEnabled() {
        if (!mIsRecognitionEnabled) {
            updateAmbientPlayAlarm(true);
            return false;
        }
        if (mCurrentNetworkStatus == -1) {
            updateAmbientPlayAlarm(true);
            if (DEBUG) Log.d(TAG, "Disabling recognition due to no network available");
            return false;
        }
        if (mLowBatteryRestrictionEnabled && mIsBatteryLow) {
            updateAmbientPlayAlarm(true);
            if (DEBUG) Log.d(TAG, "Disabling recognition due to low battery restriction");
            return false;
        }
        if (mMobileDataRestrictionEnabled && mCurrentNetworkStatus == 1) {
            updateAmbientPlayAlarm(true);
            if (DEBUG) Log.d(TAG, "Disabling recognition due to mobile data restriction");
            return false;
        }
        if (mAmbientPlayQuietPeriod.isOnPeriod()) {
            updateAmbientPlayAlarm(true);
            if (DEBUG) Log.d(TAG, "Disabling recognition due to quiet period");
            return false;
        }
        return true;
    }

    private boolean needsUpdate() {
        return System.currentTimeMillis() - lastUpdated > lastAlarmInterval;
    }

    private void updateAmbientPlayAlarm(boolean cancelOnly) {
        int UPDATE_AMBIENT_INDICATION_PENDING_INTENT_CODE = 96545687;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, UPDATE_AMBIENT_INDICATION_PENDING_INTENT_CODE, new Intent(ACTION_UPDATE_AMBIENT_INDICATION), 0);
        mAlarmManager.cancel(pendingIntent);
        if (cancelOnly) {
            if (DEBUG) Log.d(TAG, "updateAmbientPlayAlarm: Cancelling alarm");
            return;
        }
        lastAlarmInterval = 0;
        if (!isRecognitionEnabled()) return;
        int duration = 90000; // 1 minute and 30 seconds by default
        lastAlarmInterval = duration;
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + duration, pendingIntent);
        if (DEBUG) Log.d(TAG, "updateAmbientPlayAlarm: Alarm scheduled");
    }

    public int getRecordingMaxTime() {
        return 10000; // 10 seconds
    }

    public int getAmbientClearViewInterval() {
        return 60000; // Interval to clean the view after song is detected. (Default 1 minute)
    }

    private void updateNetworkStatus() {
        final ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        final Network network = connectivityManager.getActiveNetwork();
        final NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        /*
         * Return -1 if We don't have any network connectivity
         * Return 0 if we are on WiFi  (desired)
         * Return 1 if we are on MobileData (Little less desired)
         * Return 2 if not sure which connection is user on but has network connectivity
         */
        // NetworkInfo object will return null in case device is in flight mode.
        if (activeNetworkInfo == null || capabilities == null) {
            mCurrentNetworkStatus = -1;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            mCurrentNetworkStatus = 0;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            mCurrentNetworkStatus = 1;
        } else if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            mCurrentNetworkStatus = 2;
        } else {
            mCurrentNetworkStatus = -1;
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (DEBUG) Log.d(TAG, "Received intent: " + intent.getAction());
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) || Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                startRecordingIfNecessary(true);
            } else if (ACTION_UPDATE_AMBIENT_INDICATION.equals(intent.getAction())) {
                startRecordingIfNecessary(false);
            } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                lastUpdated = 0;
                lastAlarmInterval = 0;
                updateAmbientPlayAlarm(false);
            } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
                boolean mIsBatteryLow_ = mIsBatteryLow;
                mIsBatteryLow = false;
                startRecordingIfNecessary(true);
            } else if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                boolean mIsBatteryLow_ = mIsBatteryLow;
                mIsBatteryLow = true;
                startRecordingIfNecessary(true);
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                int mCurrentNetworkStatus_ = mCurrentNetworkStatus;
                updateNetworkStatus();
                if (mCurrentNetworkStatus_ != mCurrentNetworkStatus) {
                    startRecordingIfNecessary(true);
                    if (mCurrentNetworkStatus_ == -1){
                        updateAmbientPlayAlarm(false);
                    }
                }
            }
        }
    };

    public AmbientIndicationManager(Context context) {
        mContext = context;
        updateNetworkStatus();
        mIsBatteryLow = isBatteryLevelLow();
        mAmbientPlayQuietPeriod = new AmbientPlayQuietPeriod(context);
        mCallbacks = new ArrayList<>();
        mContentResolver = context.getContentResolver();
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();
        mSettingsObserver.update();
        mRecognitionObserver = new RecognitionObserver(context, this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(ACTION_UPDATE_AMBIENT_INDICATION);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        context.registerReceiver(broadcastReceiver, filter);
    }

    private boolean isBatteryLevelLow() {
        Intent batteryStatus = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryStatus != null && batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
    }

    private void startRecordingIfNecessary(boolean check) {
        if (!isRecognitionEnabled()) {
            if (DEBUG) Log.d(TAG, "startRecordingIfNecessary: Recognition disabled");
            return;
        }
        if (check && !needsUpdate()){
            if (DEBUG) Log.d(TAG, "startRecordingIfNecessary: needsUpdate false");
            return;
        }
        if (!isRecognitionObserverBusy) {
            if (DEBUG) Log.d(TAG, "startRecordingIfNecessary: Recording");
            isRecognitionObserverBusy = true;
            updateAmbientPlayAlarm(true);
            mRecognitionObserver.startRecording();
        }
    }

    public void unregister() {
        mCallbacks = new ArrayList<>();
        mSettingsObserver.unregister();
        mContext.unregisterReceiver(broadcastReceiver);
    }

    private SettingsObserver mSettingsObserver;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.AMBIENT_RECOGNITION),
                    false, this, UserHandle.USER_ALL);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.AMBIENT_RECOGNITION_KEYGUARD),
                    false, this, UserHandle.USER_ALL);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.AMBIENT_RECOGNITION_NOTIFICATION),
                    false, this, UserHandle.USER_ALL);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.AMBIENT_RECOGNITION_SAVING_OPTIONS_LOW_BATTERY),
                    false, this, UserHandle.USER_ALL);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.AMBIENT_RECOGNITION_SAVING_OPTIONS_MOBILE_DATA),
                    false, this, UserHandle.USER_ALL);
        }

        void unregister() {
            mContentResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            update();
            if (uri.equals(Settings.System.getUriFor(Settings.System.AMBIENT_RECOGNITION))) {
                lastUpdated = 0;
                lastAlarmInterval = 0;
                dispatchSettingsChanged(Settings.System.AMBIENT_RECOGNITION, mIsRecognitionEnabled);
                updateAmbientPlayAlarm(false);
            } else if (uri.equals(Settings.System.getUriFor(Settings.System.AMBIENT_RECOGNITION_KEYGUARD))) {
                dispatchSettingsChanged(Settings.System.AMBIENT_RECOGNITION_KEYGUARD, mIsRecognitionEnabledOnKeyguard);
            } else if (uri.equals(Settings.System.getUriFor(Settings.System.AMBIENT_RECOGNITION_NOTIFICATION))) {
                dispatchSettingsChanged(Settings.System.AMBIENT_RECOGNITION_NOTIFICATION, mIsRecognitionNotificationEnabled);
            }
        }

        public void update() {
            mIsRecognitionEnabled = Settings.System.getIntForUser(mContentResolver,
                    Settings.System.AMBIENT_RECOGNITION, 0, UserHandle.USER_CURRENT) != 0;
            mIsRecognitionEnabledOnKeyguard = Settings.System.getIntForUser(mContentResolver,
                    Settings.System.AMBIENT_RECOGNITION_KEYGUARD, 1, UserHandle.USER_CURRENT) != 0;
            mIsRecognitionNotificationEnabled = Settings.System.getIntForUser(mContentResolver,
                    Settings.System.AMBIENT_RECOGNITION_NOTIFICATION, 1, UserHandle.USER_CURRENT) != 0;
            mLowBatteryRestrictionEnabled = Settings.System.getIntForUser(mContentResolver,
                    Settings.System.AMBIENT_RECOGNITION_SAVING_OPTIONS_LOW_BATTERY, 1, UserHandle.USER_CURRENT) != 0;
            mMobileDataRestrictionEnabled = Settings.System.getIntForUser(mContentResolver,
                    Settings.System.AMBIENT_RECOGNITION_SAVING_OPTIONS_MOBILE_DATA, 0, UserHandle.USER_CURRENT) != 0;
        }
    }

    public void unRegisterCallback(AmbientIndicationManagerCallback callback) {
        mCallbacks.remove(callback);
    }

    public void registerCallback(AmbientIndicationManagerCallback callback) {
        mCallbacks.add(callback);
        callback.onSettingsChanged(Settings.System.AMBIENT_RECOGNITION, mIsRecognitionEnabled);
        callback.onSettingsChanged(Settings.System.AMBIENT_RECOGNITION_KEYGUARD, mIsRecognitionEnabledOnKeyguard);
        callback.onSettingsChanged(Settings.System.AMBIENT_RECOGNITION_NOTIFICATION, mIsRecognitionNotificationEnabled);
    }

    public void dispatchRecognitionResult(Observable observed) {
        isRecognitionObserverBusy = false;
        lastUpdated = System.currentTimeMillis();
        if (!isRecognitionEnabled()) {
            dispatchRecognitionNoResult();
            return;
        }
        if (mIsRecognitionNotificationEnabled) {
            showNotification(observed.Song, observed.Artist);
        }
        AmbientPlayHistoryManager.addSong(observed.Song, observed.Artist, mContext);
        AmbientPlayHistoryManager.sendMatchBroadcast(mContext);
        for (AmbientIndicationManagerCallback cb : mCallbacks) {
            try {
                cb.onRecognitionResult(observed);
            } catch (Exception ignored) {
            }
        }
        updateAmbientPlayAlarm(false);
    }

    public void dispatchRecognitionNoResult() {
        isRecognitionObserverBusy = false;
        lastUpdated = System.currentTimeMillis();
        for (AmbientIndicationManagerCallback cb : mCallbacks) {
            try {
                cb.onRecognitionNoResult();
            } catch (Exception ignored) {
            }
        }
        updateAmbientPlayAlarm(false);
    }

    public void dispatchRecognitionError() {
        isRecognitionObserverBusy = false;
        lastUpdated = System.currentTimeMillis();
        for (AmbientIndicationManagerCallback cb : mCallbacks) {
            try {
                cb.onRecognitionError();
            } catch (Exception ignored) {
            }
        }
        updateAmbientPlayAlarm(false);
    }

    private void dispatchSettingsChanged(String key, boolean newValue) {
        for (AmbientIndicationManagerCallback cb : mCallbacks) {
            try {
                cb.onSettingsChanged(key, newValue);
            } catch (Exception ignored) {
            }
        }
    }

    private void showNotification(String song, String artist) {
        Notification.Builder mBuilder =
                new Notification.Builder(mContext, "music_recognized_channel");
        final Bundle extras = Bundle.forPair(Notification.EXTRA_SUBSTITUTE_APP_NAME,
                mContext.getResources().getString(com.android.internal.R.string.ambient_recognition_notification));
        mBuilder.setSmallIcon(R.drawable.ic_music_note_24dp);
        mBuilder.setContentText(String.format(mContext.getResources().getString(
                com.android.internal.R.string.ambient_recognition_information), song, artist));
        mBuilder.setColor(mContext.getResources().getColor(com.android.internal.R.color.system_notification_accent_color));
        mBuilder.setAutoCancel(false);
        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        mBuilder.setLocalOnly(true);
        mBuilder.setShowWhen(true);
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setTicker(String.format(mContext.getResources().getString(
                com.android.internal.R.string.ambient_recognition_information), song, artist));
        mBuilder.setExtras(extras);

        Intent historyIntent = new Intent();
        historyIntent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$AmbientPlayHistoryActivity"));
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, historyIntent, 0);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel("music_recognized_channel",
                mContext.getResources().getString(com.android.internal.R.string.ambient_recognition_notification),
                NotificationManager.IMPORTANCE_MIN);
        mNotificationManager.createNotificationChannel(channel);
        mNotificationManager.notify(122306791, mBuilder.build());
    }
}
