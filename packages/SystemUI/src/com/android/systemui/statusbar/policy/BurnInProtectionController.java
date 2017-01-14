/*
 * Copyright 2017-2018 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.navigationbar.NavigationBarView;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;
import javax.inject.Singleton;

@SysUISingleton
public class BurnInProtectionController {
    private static final String TAG = "BurnInProtectionController";
    private static final boolean DEBUG = false;
    private static final int TOTAL_SHIFTS_IN_ONE_DIRECTION = 3;

    private final Context mContext;
    private final Handler mUiHandler;
    private final Object mLock = new Object();
    private final boolean mShiftEnabled;
    private final int mShiftInterval;

    private StatusBar mStatusBar;
    private PhoneStatusBarView mPhoneStatusBarView;
    private Timer mTimer;
    // Shift amount in pixels
    private int mHorizontalShift, mVerticalShift;
    private int mHorizontalMaxShift, mVerticalMaxShift;
    // Increment / Decrement (based on sign) for each tick
    private int mHorizontalShiftStep, mVerticalShiftStep;

    @Inject
    public BurnInProtectionController(Context context,
            ConfigurationController configurationController) {
        mContext = context;
        mUiHandler = new Handler(Looper.getMainLooper());

        final Resources res = mContext.getResources();
        mShiftEnabled = res.getBoolean(R.bool.config_statusBarBurnInProtection);
        mShiftInterval = res.getInteger(R.integer.config_shift_interval) * 1000;
        logD("mShiftEnabled = " + mShiftEnabled + ", mShiftInterval = " + mShiftInterval);
        loadResources(res);

        // Reload resources on configuration change
        configurationController.addCallback(new ConfigurationListener() {
            @Override
            public void onDensityOrFontScaleChanged() {
                logD("onDensityOrFontScaleChanged");
                loadResources(mContext.getResources());
            }
        });
    }

    public void setStatusBar(StatusBar statusBar) {
        mStatusBar = statusBar;
    }

    public void setPhoneStatusBarView(PhoneStatusBarView phoneStatusBarView) {
        mPhoneStatusBarView = phoneStatusBarView;
    }

    public void startShiftTimer() {
        if (!mShiftEnabled) return;
        if (mTimer == null) {
            logD("mTimer is set");
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                shiftItems();
            }
        }, 0, mShiftInterval);
        logD("Started swift timer");
    }

    public void stopShiftTimer() {
        if (!mShiftEnabled || mTimer == null) return;
        mHorizontalShift = mVerticalShift = 0;
        mTimer.cancel();
        mTimer.purge();
        mTimer = null;
        logD("Cancelled swift timer");
    }

    private void loadResources(Resources res)  {
        synchronized (mLock) {
            mHorizontalShift = mVerticalShift = 0;
            mHorizontalMaxShift = res.getDimensionPixelSize(R.dimen.horizontal_max_shift);
            mHorizontalShiftStep = mHorizontalMaxShift / TOTAL_SHIFTS_IN_ONE_DIRECTION;
            mVerticalMaxShift = res.getDimensionPixelSize(R.dimen.vertical_max_shift);
            mVerticalShiftStep = mVerticalMaxShift / TOTAL_SHIFTS_IN_ONE_DIRECTION;
            logD("mHorizontalMaxShift = " + mHorizontalMaxShift +
                ", mHorizontalShiftStep = " + mHorizontalShiftStep +
                ", mVerticalMaxShift = " + mVerticalMaxShift +
                ", mVerticalShiftStep = " + mVerticalShiftStep);
        }
    }

    private void shiftItems() {
        synchronized (mLock) {
            mHorizontalShift += mHorizontalShiftStep;
            if ((mHorizontalShift >=  mHorizontalMaxShift) ||
                    (mHorizontalShift <= -mHorizontalMaxShift)) {
                logD("shifting horizontal direction");
                mHorizontalShiftStep *= -1;
            }

            mVerticalShift += mVerticalShiftStep;
            if ((mVerticalShift >=  mVerticalMaxShift) ||
                    (mVerticalShift <= -mVerticalMaxShift)) {
                logD("shifting vertical direction");
                mVerticalShiftStep *= -1;
            }
        }

        logD("Shifting items, mHorizontalShift = " + mHorizontalShift +
            ", mVerticalShift = " + mVerticalShift);

        mUiHandler.post(() -> {
            if (mPhoneStatusBarView != null) {
                mPhoneStatusBarView.shiftStatusBarItems(mHorizontalShift, mVerticalShift);
            }
            if (mStatusBar != null) {
                final NavigationBarView mNavigationBarView = mStatusBar.getNavigationBarView();
                if (mNavigationBarView != null) {
                    mNavigationBarView.shiftNavigationBarItems(mHorizontalShift, mVerticalShift);
                }
            }
        });
    }

    private static void logD(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
