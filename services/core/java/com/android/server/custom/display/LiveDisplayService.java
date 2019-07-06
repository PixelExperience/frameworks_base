/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2019 The LineageOS Project
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager.ServiceType;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.UserHandle;
import android.view.Display;

import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;

import com.android.server.custom.common.UserContentObserver;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.android.internal.custom.app.LineageContextConstants;
import com.android.internal.custom.hardware.HSIC;
import com.android.internal.custom.hardware.ILiveDisplayService;
import com.android.internal.custom.hardware.LiveDisplayConfig;
import android.provider.Settings;

import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_FIRST;
import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_LAST;
import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_OFF;

/**
 * LiveDisplay is an advanced set of features for improving
 * display quality under various ambient conditions.
 *
 * The service is constructed with a set of LiveDisplayFeatures
 * which provide capabilities such as outdoor mode, night mode,
 * and calibration. It interacts with LineageHardwareService to relay
 * changes down to the lower layers.
 */
public class LiveDisplayService extends SystemService {

    private static final String TAG = "LiveDisplay";

    private final Context mContext;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;

    private DisplayManager mDisplayManager;
    private ModeObserver mModeObserver;

    private final List<LiveDisplayFeature> mFeatures = new ArrayList<LiveDisplayFeature>();

    private ColorTemperatureController mCTC;
    private DisplayHardwareController mDHC;
    private OutdoorModeController mOMC;
    private PictureAdjustmentController mPAC;

    private LiveDisplayConfig mConfig;

    static int MODE_CHANGED = 1;
    static int DISPLAY_CHANGED = 2;
    static int ALL_CHANGED = 255;

    // PowerManager ServiceType to use when we're only
    // interested in gleaning global battery saver state.
    private static final int SERVICE_TYPE_DUMMY = ServiceType.GPS;

    static class State {
        public boolean mLowPowerMode = false;
        public boolean mScreenOn = false;
        public int mMode = -1;

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "[mLowPowerMode=%b, mScreenOn=%b, mMode=%d",
                    mLowPowerMode, mScreenOn, mMode);
        }
    }

    private final State mState = new State();

    public LiveDisplayService(Context context) {
        super(context);

        mContext = context;

        mHandlerThread = new ServiceThread(TAG,
                Process.THREAD_PRIORITY_DEFAULT, false /*allowIo*/);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public void onStart() {
        publishBinderService(LineageContextConstants.LINEAGE_LIVEDISPLAY_SERVICE, mBinder);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            final boolean isNightDisplayAvailable = 
                mContext.getResources().getBoolean(com.android.internal.R.bool.config_nightDisplayAvailable);

            mDHC = new DisplayHardwareController(mContext, mHandler);
            mFeatures.add(mDHC);

            mCTC = new ColorTemperatureController(mContext, mHandler, mDHC);
            if (!isNightDisplayAvailable) {
                mFeatures.add(mCTC);
            }

            mOMC = new OutdoorModeController(mContext, mHandler);
            mFeatures.add(mOMC);

            mPAC = new PictureAdjustmentController(mContext, mHandler);
            mFeatures.add(mPAC);

            // Get capabilities, throw out any unused features
            final BitSet capabilities = new BitSet();
            for (Iterator<LiveDisplayFeature> it = mFeatures.iterator(); it.hasNext();) {
                final LiveDisplayFeature feature = it.next();
                if (!feature.getCapabilities(capabilities)) {
                    it.remove();
                }
            }

            mConfig = new LiveDisplayConfig(capabilities,
                    mOMC.getDefaultAutoOutdoorMode(), mDHC.getDefaultAutoContrast(),
                    mDHC.getDefaultCABC(), mDHC.getDefaultColorEnhancement(),
                    mPAC.getHueRange(), mPAC.getSaturationRange(),
                    mPAC.getIntensityRange(), mPAC.getContrastRange(),
                    mPAC.getSaturationThresholdRange());

            // listeners
            mDisplayManager = (DisplayManager) getContext().getSystemService(
                    Context.DISPLAY_SERVICE);
            mDisplayManager.registerDisplayListener(mDisplayListener, null);
            mState.mScreenOn = mDisplayManager.getDisplay(
                    Display.DEFAULT_DISPLAY).getState() == Display.STATE_ON;

            PowerManagerInternal pmi = LocalServices.getService(PowerManagerInternal.class);
            pmi.registerLowPowerModeObserver(mLowPowerModeListener);
            // ServiceType does not matter when retrieving global saver mode.
            mState.mLowPowerMode =
                    pmi.getLowPowerState(SERVICE_TYPE_DUMMY).globalBatterySaverEnabled;

            if (mConfig.hasModeSupport()) {
                mModeObserver = new ModeObserver(mHandler);
                mState.mMode = mModeObserver.getMode();
            }

            // start and update all features
            for (int i = 0; i < mFeatures.size(); i++) {
                mFeatures.get(i).start();
            }

            updateFeatures(ALL_CHANGED);
        }
    }

    private void updateFeatures(final int flags) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mFeatures.size(); i++) {
                    mFeatures.get(i).update(flags, mState);
                }
            }
        });
    }

    private final IBinder mBinder = new ILiveDisplayService.Stub() {

        @Override
        public LiveDisplayConfig getConfig() {
            return mConfig;
        }

        @Override
        public int getMode() {
            if (mConfig != null && mConfig.hasModeSupport()) {
                return mModeObserver.getMode();
            } else {
                return MODE_OFF;
            }
        }

        @Override
        public float[] getColorAdjustment() {
            return mDHC.getColorAdjustment();
        }

        @Override
        public boolean setColorAdjustment(float[] adj) {
            mContext.enforceCallingOrSelfPermission(
                    "lineageos.permission.MANAGE_LIVEDISPLAY", null);
            return mDHC.setColorAdjustment(adj);
        }

        @Override
        public boolean isAutoContrastEnabled() {
            return mDHC.isAutoContrastEnabled();
        }

        @Override
        public  boolean setAutoContrastEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    "lineageos.permission.MANAGE_LIVEDISPLAY", null);
            return mDHC.setAutoContrastEnabled(enabled);
        }

        @Override
        public boolean isCABCEnabled() {
            return mDHC.isCABCEnabled();
        }

        @Override
        public boolean setCABCEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    "lineageos.permission.MANAGE_LIVEDISPLAY", null);
            return mDHC.setCABCEnabled(enabled);
        }

        @Override
        public boolean isColorEnhancementEnabled() {
            return mDHC.isColorEnhancementEnabled();
        }

        @Override
        public boolean setColorEnhancementEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    "lineageos.permission.MANAGE_LIVEDISPLAY", null);
            return mDHC.setColorEnhancementEnabled(enabled);
        }

        @Override
        public boolean isAutomaticOutdoorModeEnabled() {
            return mOMC.isAutomaticOutdoorModeEnabled();
        }

        @Override
        public boolean setAutomaticOutdoorModeEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission(
                    "lineageos.permission.MANAGE_LIVEDISPLAY", null);
            return mOMC.setAutomaticOutdoorModeEnabled(enabled);
        }

        @Override
        public boolean setDisplayTemperature(int temperature) {
            mContext.enforceCallingOrSelfPermission(
                    "lineageos.permission.MANAGE_LIVEDISPLAY", null);
            mCTC.setDisplayTemperature(temperature);
            return true;
        }

        @Override
        public HSIC getPictureAdjustment() { return mPAC.getPictureAdjustment(); }

        @Override
        public boolean setPictureAdjustment(final HSIC hsic) { return mPAC.setPictureAdjustment(hsic); }

        @Override
        public HSIC getDefaultPictureAdjustment() { return mPAC.getDefaultPictureAdjustment(); }

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);

            pw.println();
            pw.println("LiveDisplay Service State:");
            pw.println("  mState=" + mState.toString());
            pw.println("  mConfig=" + mConfig.toString());

            for (int i = 0; i < mFeatures.size(); i++) {
                mFeatures.get(i).dump(pw);
            }
        }
    };

    // Listener for screen on/off events
    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                boolean screenOn = isScreenOn();
                if (screenOn != mState.mScreenOn) {
                    mState.mScreenOn = screenOn;
                    updateFeatures(DISPLAY_CHANGED);
                }
            }
        }
    };


    // Display postprocessing can have power impact.
    private PowerManagerInternal.LowPowerModeListener mLowPowerModeListener =
            new PowerManagerInternal.LowPowerModeListener() {
        @Override
        public void onLowPowerModeChanged(PowerSaveState state) {
            final boolean lowPowerMode = state.globalBatterySaverEnabled;
            if (lowPowerMode != mState.mLowPowerMode) {
                mState.mLowPowerMode = lowPowerMode;
                updateFeatures(MODE_CHANGED);
            }
         }

         @Override
         public int getServiceType() {
             return SERVICE_TYPE_DUMMY;
         }
    };

    // Watch for mode changes
    private final class ModeObserver extends UserContentObserver {

        private final Uri MODE_SETTING =
                Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_ACTIVATED);

        ModeObserver(Handler handler) {
            super(handler);

            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(MODE_SETTING, false, this, UserHandle.USER_ALL);

            observe();
        }

        @Override
        protected void update() {
            int mode = getMode();
            if (mode != mState.mMode) {
                mState.mMode = mode;

                updateFeatures(MODE_CHANGED);
            }
        }

        int getMode() {
            return getInt(Settings.Secure.NIGHT_DISPLAY_ACTIVATED, 0);
        }
    }

    private boolean isScreenOn() {
        return mDisplayManager.getDisplay(
                Display.DEFAULT_DISPLAY).getState() == Display.STATE_ON;
    }

    private int getInt(String setting, int defValue) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                setting, defValue, UserHandle.USER_CURRENT);
    }

    private void putInt(String setting, int value) {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                setting, value, UserHandle.USER_CURRENT);
    }
}
