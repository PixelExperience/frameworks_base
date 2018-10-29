/*
 * Copyright (C) 2018 The OmniROM Project
 *                    The PixelExperience Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.internal.util.custom.weather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.android.internal.R;

public class WeatherClient {

    public static final String SERVICE_PACKAGE = "org.pixelexperience.weather.client";
    private static final String SERVICE_PACKAGE_PERMISSION = SERVICE_PACKAGE + ".READ_WEATHER";
    public static final Uri WEATHER_URI = Uri.parse("content://org.pixelexperience.weather.client.provider/weather");
    public static final int WEATHER_UPDATE_SUCCESS = 0; // Success
    public static final int WEATHER_UPDATE_RUNNING = 1; // Update running
    public static final int WEATHER_UPDATE_ERROR = 2; // Error
    private static final String TAG = "WeatherClient";
    private static final boolean DEBUG = false;
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_CONDITIONS = "conditions";
    private static final String COLUMN_TEMPERATURE_METRIC = "temperatureMetric";
    private static final String COLUMN_TEMPERATURE_IMPERIAL = "temperatureImperial";
    private static final String[] PROJECTION_DEFAULT_WEATHER = new String[]{
            COLUMN_STATUS,
            COLUMN_CONDITIONS,
            COLUMN_TEMPERATURE_METRIC,
            COLUMN_TEMPERATURE_IMPERIAL
    };

    private static final int WEATHER_UPDATE_INTERVAL = 60 * 10 * 1000; // 10 minutes
    private String updateIntentAction;
    private PendingIntent pendingWeatherUpdate;
    private WeatherInfo mWeatherInfo = new WeatherInfo();
    private Context mContext;
    private List<WeatherObserver> mObserver;
    private boolean isRunning;
    private boolean isScreenOn = true;
    private long lastUpdated;
    private long scheduledAlarmTime = 0;
    private AlarmManager alarmManager;
    private BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (DEBUG) Log.d(TAG, "Received intent: " + intent.getAction());
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                onScreenOff();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                onScreenOn();
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || updateIntentAction.equals(intent.getAction())) {
                updateWeatherAndNotify();
            } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                resetScheduledAlarm();
                updateWeatherAndNotify();
            }
        }
    };

    public WeatherClient(Context context) {
        mContext = context;
        mContext.enforceCallingOrSelfPermission(SERVICE_PACKAGE_PERMISSION, "Missing or invalid weather permission: " + SERVICE_PACKAGE_PERMISSION);
        updateIntentAction = "updateIntentAction_" + Integer.toString(getRandomInt());
        pendingWeatherUpdate = PendingIntent.getBroadcast(mContext, getRandomInt(), new Intent(updateIntentAction), 0);
        mObserver = new ArrayList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(updateIntentAction);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        mContext.registerReceiver(weatherReceiver, filter);
    }

    public static boolean isAvailable(Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(SERVICE_PACKAGE, PackageManager.GET_ACTIVITIES);
            int enabled = pm.getApplicationEnabledSetting(SERVICE_PACKAGE);
            return enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                    enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private int getRandomInt() {
        Random r = new Random();
        return r.nextInt((20000000 - 10000000) + 1) + 10000000;
    }

    private void updateWeatherAndNotify() {
        if (isRunning){
            return;
        }
        isRunning = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeatherData();
                for (WeatherObserver observer : mObserver) {
                    try {
                        observer.onWeatherUpdated(mWeatherInfo);
                    } catch (Exception ignored) {
                    }
                }
                lastUpdated = System.currentTimeMillis();
                resetScheduledAlarm();
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    private boolean needsUpdate() {
        boolean lastUpdatedExpired = System.currentTimeMillis() - lastUpdated > WEATHER_UPDATE_INTERVAL;
        return mWeatherInfo.getStatus() != WEATHER_UPDATE_SUCCESS || lastUpdatedExpired;
    }

    private void onScreenOn() {
        if (isScreenOn){
            return;
        }
        if (DEBUG) Log.d(TAG, "onScreenOn");
        isScreenOn = true;
        if (!isRunning) {
            if (needsUpdate()) {
                if (DEBUG) Log.d(TAG, "Needs update, triggering updateWeatherAndNotify");
                updateWeatherAndNotify();
            } else {
                if (DEBUG) Log.d(TAG, "Scheduling update");
                scheduleWeatherUpdateAlarm();
            }
        }
    }

    private void onScreenOff() {
        if (DEBUG) Log.d(TAG, "onScreenOff");
        isScreenOn = false;
        cancelWeatherUpdateAlarm();
    }

    private void resetScheduledAlarm(){
        scheduledAlarmTime = 0;
        scheduleWeatherUpdateAlarm();
    }

    private void scheduleWeatherUpdateAlarm() {
        if (!isScreenOn) {
            return;
        }
        if (System.currentTimeMillis() >= scheduledAlarmTime){
            scheduledAlarmTime = System.currentTimeMillis() + WEATHER_UPDATE_INTERVAL;
        }
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingWeatherUpdate);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledAlarmTime, pendingWeatherUpdate);
        if (DEBUG) Log.d(TAG, "Update scheduled");
    }

    private void cancelWeatherUpdateAlarm() {
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingWeatherUpdate);
        if (DEBUG) Log.d(TAG, "Update scheduling canceled");
    }

    private void updateWeatherData() {
        if (!isAvailable(mContext)) {
            isRunning = false;
            return;
        }
        isRunning = true;
        Cursor c = mContext.getContentResolver().query(WEATHER_URI, PROJECTION_DEFAULT_WEATHER,
                null, null, null);
        if (c != null) {
            try {
                int count = c.getCount();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        c.moveToPosition(i);
                        if (i == 0) {
                            mWeatherInfo.status = c.getInt(0);
                            mWeatherInfo.conditions = c.getString(1);
                            mWeatherInfo.temperatureMetric = c.getInt(2);
                            mWeatherInfo.temperatureImperial = c.getInt(3);
                        }
                    }
                }
            } finally {
                c.close();
            }
        } else {
            mWeatherInfo.status = WEATHER_UPDATE_ERROR;
        }
        if (DEBUG) Log.d(TAG, mWeatherInfo.toString());
        isRunning = false;
    }

    public void addObserver(final WeatherObserver observer) {
        mObserver.add(observer);
        if (isRunning) {
            return;
        }
        isRunning = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeatherData();
                try {
                    observer.onWeatherUpdated(mWeatherInfo);
                } catch (Exception ignored) {
                }
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    public void removeObserver(WeatherObserver observer) {
        mObserver.remove(observer);
    }

    public void destroy(){
        mContext.unregisterReceiver(weatherReceiver);
        mObserver = new ArrayList<>();
    }

    public interface WeatherObserver {
        void onWeatherUpdated(WeatherInfo info);
    }

    public class WeatherInfo {

        int status = WEATHER_UPDATE_ERROR;
        String conditions = "";
        int temperatureMetric = 0;
        int temperatureImperial = 0;

        public WeatherInfo() {
        }

        public int getTemperature(boolean metric) {
            return metric ? this.temperatureMetric : this.temperatureImperial;
        }

        public int getStatus() {
            return this.status;
        }

        public String getConditions() {
            return this.conditions;
        }

        public int getWeatherConditionImage() {
            boolean isDay = getConditions().contains("d");
            if (getConditions().contains(WeatherClient.Conditions.CONDITION_STORMY)){
                return R.drawable.weather_11;
            }else if (getConditions().contains(WeatherClient.Conditions.CONDITION_SNOWY) && conditions.contains(WeatherClient.Conditions.CONDITION_ICY)){
                return R.drawable.weather_13;
            }else if (getConditions().contains(WeatherClient.Conditions.CONDITION_RAINY)){
                return R.drawable.weather_09;
            }else if (getConditions().contains(WeatherClient.Conditions.CONDITION_FOGGY) && conditions.contains(WeatherClient.Conditions.CONDITION_HAZY)){
                return R.drawable.weather_50;
            }else if (getConditions().contains(WeatherClient.Conditions.CONDITION_CLOUDY) && conditions.contains(WeatherClient.Conditions.CONDITION_CLEAR)){
                return isDay ? R.drawable.weather_02 : R.drawable.weather_02n;
            }else if (getConditions().contains(WeatherClient.Conditions.CONDITION_CLOUDY)){
                return isDay ? R.drawable.weather_03 : R.drawable.weather_03n;
            }else if (getConditions().contains(WeatherClient.Conditions.CONDITION_CLEAR)){
                return isDay ? R.drawable.weather_01 : R.drawable.weather_01n;
            }else{
                return isDay ? R.drawable.weather_04 : R.drawable.weather_04n; // Default
            }
        }

        @Override
        public String toString() {
            return "WeatherInfo: " +
                    "status=" + getStatus() + "," +
                    "conditions=" + getConditions() + "," +
                    "temperatureMetric=" + getTemperature(true) + "," +
                    "temperatureImperial=" + getTemperature(false);
        }
    }

    public class Conditions {
        public static final String CONDITION_UNKNOWN = "0";
        public static final String CONDITION_CLEAR = "1";
        public static final String CONDITION_CLOUDY = "2";
        public static final String CONDITION_FOGGY = "3";
        public static final String CONDITION_HAZY = "4";
        public static final String CONDITION_ICY = "5";
        public static final String CONDITION_RAINY = "6";
        public static final String CONDITION_SNOWY = "7";
        public static final String CONDITION_STORMY = "8";
        public static final String CONDITION_WINDY = "9";
    }
}
