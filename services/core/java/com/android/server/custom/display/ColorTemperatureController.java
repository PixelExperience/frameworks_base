/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2018-2019 The LineageOS Project
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
package com.android.server.custom.display;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.MathUtils;
import android.util.Range;
import android.util.Slog;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.io.PrintWriter;
import java.util.BitSet;

import com.android.internal.custom.hardware.LineageHardwareManager;
import com.android.internal.custom.hardware.LiveDisplayManager;
import com.android.internal.util.custom.ColorUtils;

import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_NIGHT;
import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_OFF;

public class ColorTemperatureController extends LiveDisplayFeature {

    private final DisplayHardwareController mDisplayHardware;

    private final boolean mUseTemperatureAdjustment;
    private final boolean mUseColorBalance;
    private final Range<Integer> mColorBalanceRange;
    private final Range<Integer> mColorTemperatureRange;
    private final double[] mColorBalanceCurve;

    private int mColorTemperature = -1;

    private AccelerateDecelerateInterpolator mInterpolator;
    private ValueAnimator mAnimator;

    private final LineageHardwareManager mHardware;

    public ColorTemperatureController(Context context,
            Handler handler, DisplayHardwareController displayHardware) {
        super(context, handler);
        mDisplayHardware = displayHardware;
        mHardware = LineageHardwareManager.getInstance(mContext);

        mUseColorBalance = mHardware
                .isSupported(LineageHardwareManager.FEATURE_COLOR_BALANCE);
        mColorBalanceRange = mHardware.getColorBalanceRange();

        mUseTemperatureAdjustment = mUseColorBalance ||
                mDisplayHardware.hasColorAdjustment();

        mColorTemperatureRange = Range.create(
                mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_nightDisplayColorTemperatureMin),
                mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_nightDisplayColorTemperatureMax));

        mColorBalanceCurve = com.android.internal.util.custom.MathUtils.powerCurve(
                mColorTemperatureRange.getLower(),
                0,
                mColorTemperatureRange.getUpper());

        mInterpolator = new AccelerateDecelerateInterpolator();
    }

    @Override
    public boolean getCapabilities(final BitSet caps) {
        if (mUseTemperatureAdjustment) {
            caps.set(MODE_NIGHT);
            if (mUseColorBalance) {
                caps.set(LiveDisplayManager.FEATURE_COLOR_BALANCE);
            }
        }
        return mUseTemperatureAdjustment;
    }

    @Override
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("  ColorTemperatureController State:");
        pw.println("    mColorTemperature=" + mColorTemperature);
    }

    /**
     * Smoothly animate the current display color balance
     */
    private synchronized void animateColorBalance(int balance) {

        // always start with the current values in the hardware
        int current = mHardware.getColorBalance();

        if (current == balance) {
            return;
        }

        long duration = (long)(5 * Math.abs(current - balance));


        if (DEBUG) {
            Slog.d(TAG, "animateDisplayColor current=" + current +
                    " target=" + balance + " duration=" + duration);
        }

        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator.removeAllUpdateListeners();
        }

        mAnimator = ValueAnimator.ofInt(current, balance);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(mInterpolator);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                synchronized (ColorTemperatureController.this) {
                    if (isScreenOn()) {
                        int value = (int) animation.getAnimatedValue();
                        mHardware.setColorBalance(value);
                    }
                }
            }
        });
        mAnimator.start();
    }

    /*
     * Map the color temperature to a color balance value using a power curve. This assumes the
     * correct configuration at the device level!
     */
    private int mapColorTemperatureToBalance(int temperature) {
        double z = com.android.internal.util.custom.MathUtils.powerCurveToLinear(mColorBalanceCurve, temperature);
        return Math.round(MathUtils.lerp((float)mColorBalanceRange.getLower(),
                (float)mColorBalanceRange.getUpper(), (float)z));
    }

    synchronized void setDisplayTemperature(int temperature) {
        if (!mColorTemperatureRange.contains(temperature)) {
            Slog.e(TAG, "Color temperature out of range: " + temperature);
            return;
        }

        mColorTemperature = temperature;

        if (mUseColorBalance) {
            int balance = mapColorTemperatureToBalance(temperature);
            Slog.d(TAG, "Set color balance = " + balance + " (temperature=" + temperature + ")");
            animateColorBalance(balance);
            return;
        }

        final float[] rgb = ColorUtils.temperatureToRGB(temperature);
        if (mDisplayHardware.setAdditionalAdjustment(rgb)) {
            if (DEBUG) {
                Slog.d(TAG, "Adjust display temperature to " + temperature + "K");
            }
        }
    }

    @Override
    public void onStart() {
    }

    @Override
    protected void onUpdate() {
    }

    @Override
    protected synchronized void onSettingsChanged(Uri uri) {
    }
}
