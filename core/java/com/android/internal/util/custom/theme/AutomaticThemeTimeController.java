/*
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

package com.android.internal.util.custom.theme;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.R;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AutomaticThemeTimeController {

    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mIsOnPeriod = false;
    private Observer mObserver = null;

    private BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isOnPeriod = isOnPeriod();
            if (mIsOnPeriod != isOnPeriod){
                mIsOnPeriod = isOnPeriod;
                if (mObserver != null){
                    mObserver.onDarkThemePeriodChanged(mIsOnPeriod);
                }
            }
        }
    };

    public AutomaticThemeTimeController(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    public void registerListener(Observer observer){
        if (mObserver != null){
            return;
        }
        mObserver = observer;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        mContext.registerReceiver(mTimeChangedReceiver, filter);
        mObserver.onDarkThemePeriodChanged(mIsOnPeriod);
    }

    public void unregisterListener(){
        if (mObserver == null){
            return;
        }
        mObserver = null;
        mContext.unregisterReceiver(mTimeChangedReceiver);
    }

    public boolean isOnPeriod() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = getDateTimeBefore(getCustomStartTime(), now);
        LocalDateTime end = getDateTimeAfter(getCustomEndTime(), start);
        return now.isBefore(end);
    }

    public LocalDateTime getDateTimeBefore(LocalTime localTime, LocalDateTime compareTime) {
        final LocalDateTime ldt = LocalDateTime.of(compareTime.getYear(), compareTime.getMonth(),
                compareTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute());

        // Check if the local time has passed, if so return the same time yesterday.
        return ldt.isAfter(compareTime) ? ldt.minusDays(1) : ldt;
    }

    public LocalDateTime getDateTimeAfter(LocalTime localTime, LocalDateTime compareTime) {
        final LocalDateTime ldt = LocalDateTime.of(compareTime.getYear(), compareTime.getMonth(),
                compareTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute());

        // Check if the local time has passed, if so return the same time tomorrow.
        return ldt.isBefore(compareTime) ? ldt.plusDays(1) : ldt;
    }

    public LocalTime getCustomStartTime() {
        int startTimeValue = Settings.System.getIntForUser(mContentResolver,
                Settings.System.THEME_MODE_AUTOMATIC_START_TIME, -1, UserHandle.USER_CURRENT);
        if (startTimeValue == -1) {
            startTimeValue = mContext.getResources().getInteger(
                    R.integer.config_defaultAutomaticThemeStartTime);
        }
        return LocalTime.ofSecondOfDay(startTimeValue / 1000);
    }

    public void setCustomStartTime(LocalTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("startTime cannot be null");
        }
        Settings.System.putIntForUser(mContentResolver, Settings.System.THEME_MODE_AUTOMATIC_START_TIME, startTime.toSecondOfDay() * 1000, UserHandle.USER_CURRENT);
    }

    public LocalTime getCustomEndTime() {
        int endTimeValue = Settings.System.getIntForUser(mContentResolver,
                Settings.System.THEME_MODE_AUTOMATIC_END_TIME, -1, UserHandle.USER_CURRENT);
        if (endTimeValue == -1) {
            endTimeValue = mContext.getResources().getInteger(
                    R.integer.config_defaultAutomaticThemeEndTime);
        }
        return LocalTime.ofSecondOfDay(endTimeValue / 1000);
    }

    public void setCustomEndTime(LocalTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("endTime cannot be null");
        }
        Settings.System.putIntForUser(mContentResolver, Settings.System.THEME_MODE_AUTOMATIC_END_TIME, endTime.toSecondOfDay() * 1000, UserHandle.USER_CURRENT);
    }

    public long getAlarmTimeMillis() {
        if (!isOnPeriod()) {
            return 0;
        }
        LocalDateTime next = getDateTimeAfter(getCustomEndTime(), LocalDateTime.now());
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public interface Observer {
        void onDarkThemePeriodChanged(boolean isDarkTheme);
    }

}
