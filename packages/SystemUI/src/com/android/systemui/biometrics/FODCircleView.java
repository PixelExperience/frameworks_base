/**
 * Copyright (C) 2019-2020 The LineageOS Project
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

package com.android.systemui.biometrics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.pocket.IPocketCallback;
import android.pocket.PocketManager;
import android.provider.Settings;
import android.util.Spline;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.utils.ThreadUtils;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreen;
import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreenCallback;

import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class FODCircleView extends ImageView implements ConfigurationListener {
    private static final String DOZE_INTENT = "com.android.systemui.doze.pulse";
    private static final String SCREEN_BRIGHTNESS = Settings.System.SCREEN_BRIGHTNESS;
    private final int mPositionX;
    private final int mPositionY;
    private final int mSize;
    private final int mDreamingMaxOffset;
    private final int mNavigationBarSize;
    private final boolean mShouldBoostBrightness;
    private final boolean mTargetUsesInKernelDimming;
    private final Paint mPaintFingerprintBackground = new Paint();
    private final Paint mPaintFingerprint = new Paint();
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
    private final WindowManager.LayoutParams mPressedParams = new WindowManager.LayoutParams();
    private final WindowManager mWindowManager;

    private IFingerprintInscreen mFingerprintInscreenDaemon;
    private final Context mContext;

    private int mCurrentBrightness;
    private int mDreamingOffsetY;

    private boolean mIsBouncer;
    private boolean mIsDreaming;
    private boolean mIsCircleShowing;
    private boolean mIsScreenTurnedOn;
    private boolean mIsAnimating = false;

    private boolean mDozeEnabled;
    private boolean mFodGestureEnable;
    private boolean mPressPending;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    private final Handler mHandler;

    private final ImageView mPressedView;

    private final LockPatternUtils mLockPatternUtils;

    private Timer mBurnInProtectionTimer;

    private final Spline mFODiconBrightnessToDimAmountSpline;

    private final IFingerprintInscreenCallback mFingerprintInscreenCallback =
            new IFingerprintInscreenCallback.Stub() {
        @Override
        public void onFingerDown() {
            if (mUpdateMonitor.userNeedsStrongAuth()) {
                // Keyguard requires strong authentication (not biometrics)
                return;
            }
            if (mFodGestureEnable && !mIsScreenTurnedOn) {
                if (mDozeEnabled) {
                    mHandler.post(() -> mContext.sendBroadcast(new Intent(DOZE_INTENT)));
                } else {
                    mWakeLock.acquire(3000);
                    mHandler.post(() -> mPowerManager.wakeUp(SystemClock.uptimeMillis(),
                        PowerManager.WAKE_REASON_GESTURE, FODCircleView.class.getSimpleName()));
                }
                mPressPending = true;
            } else {
                mHandler.post(() -> showCircle());
            }
        }

        @Override
        public void onFingerUp() {
            mHandler.post(() -> hideCircle());
            if (mFodGestureEnable && mPressPending) {
                mPressPending = false;
            }
        }
    };

    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onDreamingStateChanged(boolean dreaming) {
            mIsDreaming = dreaming;
            updateIconDim(false);

            if (dreaming) {
                mBurnInProtectionTimer = new Timer();
                mBurnInProtectionTimer.schedule(new BurnInProtectionTask(), 0, 60 * 1000);
            } else if (mBurnInProtectionTimer != null) {
                mBurnInProtectionTimer.cancel();
                mBurnInProtectionTimer = null;
                updatePosition();
            }
        }

        @Override
        public void onKeyguardBouncerChanged(boolean isBouncer) {
            mIsBouncer = isBouncer;
            if (mUpdateMonitor.isFingerprintDetectionRunning() && !mUpdateMonitor.userNeedsStrongAuth()) {
                if (isPinOrPattern(KeyguardUpdateMonitor.getCurrentUser()) || !isBouncer) {
                    show();
                }
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (!showing && !mIsDreaming) hide();
            handlePocketManagerCallback(showing);
        }
    };

    private final ScreenLifecycle mScreenMonitor;
    private final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() {
        @Override
        public void onScreenTurnedOn() {
            mIsScreenTurnedOn = true;
            if (!mFodGestureEnable && mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            }
            if (mFodGestureEnable && mPressPending) {
                mHandler.post(() -> showCircle());
                mPressPending = false;
            }
        }

        @Override
        public void onScreenTurningOff() {
            if (mFodGestureEnable) {
                hideCircle();
            }
            else {
                hide();
            }
        }

        @Override
        public void onScreenTurnedOff() {
            mIsScreenTurnedOn = false;
        }
    };

    private class FodGestureSettingsObserver extends ContentObserver {
        FodGestureSettingsObserver(Context context, Handler handler) {
            super(handler);
        }

        void registerListener() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(
                    Settings.Secure.DOZE_ENABLED),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(
                    Settings.System.FOD_GESTURE),
                    false, this, UserHandle.USER_ALL);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateSettings();
        }

        public void updateSettings() {
            mDozeEnabled = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    Settings.Secure.DOZE_ENABLED, 1,
                    UserHandle.USER_CURRENT) == 1;
            mFodGestureEnable = Settings.System.getIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.FOD_GESTURE, 1,
                    UserHandle.USER_CURRENT) == 1;
        }
    }

    private final WakefulnessLifecycle mWakefulnessMonitor;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        @Override
        public void onStartedWakingUp() {
            if (!mIsScreenTurnedOn &&
                    mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            }
        }

        @Override
        public void onFinishedGoingToSleep() {
            updateIconDim(true);
        }
    };

    private void handlePocketManagerCallback(boolean keyguardShowing){
        if (!keyguardShowing){
            if (mPocketCallbackAdded){
                mPocketCallbackAdded = false;
                mPocketManager.removeCallback(mPocketCallback);
            }
        }else{
            if (!mPocketCallbackAdded){
                mPocketCallbackAdded = true;
                mPocketManager.addCallback(mPocketCallback);
            }
        }
    }

    private boolean mCutoutMasked;
    private int mStatusbarHeight;
    private FodGestureSettingsObserver mFodGestureSettingsObserver;
    private class CustomSettingsObserver extends ContentObserver {
        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SCREEN_BRIGHTNESS), false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            // if (uri.equals(Settings.System.getUriFor(SCREEN_BRIGHTNESS))) {
            update();
            // }
        }

        void update() {
            int brightness = Settings.System.getInt(
                    mContext.getContentResolver(), SCREEN_BRIGHTNESS, 100);
            if (mCurrentBrightness != brightness) {
                mCurrentBrightness = brightness;
                updateIconDim(false);
            }
        }
    }

    private final CustomSettingsObserver mCustomSettingsObserver;

    private PocketManager mPocketManager;
    private boolean mIsDeviceInPocket;
    private boolean mPocketCallbackAdded = false;
    private final IPocketCallback mPocketCallback = new IPocketCallback.Stub() {

        @Override
        public void onStateChanged(boolean isDeviceInPocket, int reason) {
            boolean wasDeviceInPocket = mIsDeviceInPocket;
            if (reason == PocketManager.REASON_SENSOR) {
                mIsDeviceInPocket = isDeviceInPocket;
            } else {
                mIsDeviceInPocket = false;
            }
        }

    };

    public FODCircleView(Context context) {
        super(context);
        mContext = context;

        setScaleType(ScaleType.CENTER);

        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        if (daemon == null) {
            throw new RuntimeException("Unable to get IFingerprintInscreen");
        }

        try {
            mShouldBoostBrightness = daemon.shouldBoostBrightness();
            mPositionX = daemon.getPositionX();
            mPositionY = daemon.getPositionY();
            mSize = daemon.getSize();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to retrieve FOD circle position or size");
        }

        Resources res = context.getResources();
        mPaintFingerprint.setColor(res.getColor(R.color.config_fodColor));
        mPaintFingerprint.setAntiAlias(true);
        mPaintFingerprintBackground.setColor(res.getColor(R.color.config_fodColorBackground));
        mPaintFingerprintBackground.setAntiAlias(true);


        mPowerManager = context.getSystemService(PowerManager.class);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                 FODCircleView.class.getSimpleName());

        float[] icon_dim_amount =
                getFloatArray(res.obtainTypedArray(R.array.config_FODiconDimAmount));
        float[] display_brightness =
                getFloatArray(res.obtainTypedArray(R.array.config_FODiconDisplayBrightness));
        mFODiconBrightnessToDimAmountSpline =
                Spline.createSpline(display_brightness, icon_dim_amount);

        mTargetUsesInKernelDimming = res.getBoolean(com.android.internal.R.bool.config_targetUsesInKernelDimming);

        mWindowManager = context.getSystemService(WindowManager.class);

        mNavigationBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);

        mDreamingMaxOffset = (int) (mSize * 0.1f);

        mHandler = new Handler(Looper.getMainLooper());

        mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
        mCustomSettingsObserver.update();

        mParams.height = mSize;
        mParams.width = mSize;
        mParams.format = PixelFormat.TRANSLUCENT;

        mParams.packageName = "android";
        mParams.type = WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;

        mPressedParams.copyFrom(mParams);
        mPressedParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        mParams.setTitle("Fingerprint on display");
        mPressedParams.setTitle("Fingerprint on display.touched");

        mPressedView = new ImageView(context)  {
            @Override
            protected void onDraw(Canvas canvas) {
                if (mIsCircleShowing) {
                    float half = mSize / 2.0f;
                    canvas.drawCircle(half, half, half, mPaintFingerprint);
                }
                super.onDraw(canvas);
            }
        };
        mPressedView.setImageResource(R.drawable.fod_icon_pressed);

        mWindowManager.addView(this, mParams);

        updatePosition();
        hide();

        mLockPatternUtils = new LockPatternUtils(mContext);

        mWakefulnessMonitor = Dependency.get(WakefulnessLifecycle.class);
        mScreenMonitor = Dependency.get(ScreenLifecycle.class);
        mUpdateMonitor = Dependency.get(KeyguardUpdateMonitor.class);

        if (context.getResources().getBoolean(
            com.android.internal.R.bool.config_supportsScreenOffInDisplayFingerprint)){
            mFodGestureSettingsObserver = new FodGestureSettingsObserver(context, mHandler);
            mFodGestureSettingsObserver.registerListener();
        }

        updateCutoutFlags();

        // Pocket
        mPocketManager = (PocketManager) context.getSystemService(Context.POCKET_SERVICE);
    }


    private int getDimAlpha() {
        return Math.round(mFODiconBrightnessToDimAmountSpline.interpolate(mCurrentBrightness));
    }

    public void updateIconDim(boolean animate) {
        if (!mIsCircleShowing && mTargetUsesInKernelDimming) {
            if (animate && !mIsAnimating) {
                ValueAnimator anim = new ValueAnimator();
                anim.setIntValues(0, getDimAlpha());
                anim.addUpdateListener(valueAnimator -> {
                    int progress = (Integer) valueAnimator.getAnimatedValue();
                    setColorFilter(Color.argb(progress, 0, 0, 0),
                            PorterDuff.Mode.SRC_ATOP);
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsAnimating = false;
                    }
                });
                anim.setDuration(250);
                mIsAnimating = true;
                mHandler.post(anim::start);
            } else if (!mIsAnimating) {
                mHandler.post(() ->
                        setColorFilter(Color.argb(getDimAlpha(), 0, 0, 0),
                        PorterDuff.Mode.SRC_ATOP));
            }
        } else {
            mHandler.post(() -> setColorFilter(Color.argb(0, 0, 0, 0),
                    PorterDuff.Mode.SRC_ATOP));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        mWakefulnessMonitor.addObserver(mWakefulnessObserver);
        mScreenMonitor.addObserver(mScreenObserver);
        mUpdateMonitor.registerCallback(mMonitorCallback);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWakefulnessMonitor.removeObserver(mWakefulnessObserver);
        mScreenMonitor.removeObserver(mScreenObserver);
        mUpdateMonitor.removeCallback(mMonitorCallback);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mIsCircleShowing) {
            float half = mSize / 2.0f;
            canvas.drawCircle(half, half, half, mPaintFingerprintBackground);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);

        boolean newIsInside = (x > 0 && x < mSize) && (y > 0 && y < mSize);

        if (event.getAction() == MotionEvent.ACTION_DOWN && newIsInside) {
            showCircle();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            hideCircle();
            return true;
        } else return event.getAction() == MotionEvent.ACTION_MOVE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updatePosition();
    }

    public IFingerprintInscreen getFingerprintInScreenDaemon() {
        if (mFingerprintInscreenDaemon == null) {
            try {
                mFingerprintInscreenDaemon = IFingerprintInscreen.getService();
                if (mFingerprintInscreenDaemon != null) {
                    mFingerprintInscreenDaemon.setCallback(mFingerprintInscreenCallback);
                    mFingerprintInscreenDaemon.asBinder().linkToDeath((cookie) -> mFingerprintInscreenDaemon = null, 0);
                }
            } catch (NoSuchElementException | RemoteException e) {
                // do nothing
            }
        }
        return mFingerprintInscreenDaemon;
    }

    public void dispatchPress() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onPress();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchRelease() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onRelease();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchShow() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onShowFODView();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchHide() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onHideFODView();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void showCircle() {
        if (mIsDeviceInPocket) return;
        mIsCircleShowing = true;

        setKeepScreenOn(true);

        setDim(true);
        ThreadUtils.postOnBackgroundThread(this::dispatchPress);
        setImageDrawable(null);
        updateIconDim(false);
        updatePosition();
        invalidate();
    }

    public void hideCircle() {
        mIsCircleShowing = false;

        setImageResource(R.drawable.fod_icon_default);
        invalidate();

        ThreadUtils.postOnBackgroundThread(this::dispatchRelease);
        setDim(false);

        setKeepScreenOn(false);
    }

    public void show() {
        if (mUpdateMonitor.userNeedsStrongAuth()) {
            // Keyguard requires strong authentication (not biometrics)
            return;
        }

        if (!mFodGestureEnable && !mUpdateMonitor.isScreenOn()) {
            // Keyguard is shown just after screen turning off
            return;
        }

        if (mIsBouncer && !isPinOrPattern(KeyguardUpdateMonitor.getCurrentUser())) {
            // Ignore show calls when Keyguard password screen is being shown
            return;
        }

        updatePosition();
        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();

        ThreadUtils.postOnBackgroundThread(this::dispatchShow);
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
        mCustomSettingsObserver.unobserve();
        hideCircle();
        ThreadUtils.postOnBackgroundThread(this::dispatchHide);
    }

    private void updatePosition() {
        Display defaultDisplay = mContext.getDisplay();

        Point size = new Point();
        defaultDisplay.getRealSize(size);

        int rotation = defaultDisplay.getRotation();
        int cutoutMaskedExtra = mCutoutMasked ? mStatusbarHeight : 0;
        int x, y;
        switch (rotation) {
            case Surface.ROTATION_0:
                x = mPositionX;
                y = mPositionY - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_90:
                x = mPositionY;
                y = mPositionX - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_180:
                x = mPositionX;
                y = size.y - mPositionY - mSize - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_270:
                x = size.x - mPositionY - mSize - mNavigationBarSize - cutoutMaskedExtra;
                y = mPositionX;
                break;
            default:
                throw new IllegalArgumentException("Unknown rotation: " + rotation);
        }

        mPressedParams.x = mParams.x = x;
        mPressedParams.y = mParams.y = y;

        if (mIsDreaming && !mIsCircleShowing) {
            mParams.y += mDreamingOffsetY;
        }

        mWindowManager.updateViewLayout(this, mParams);

        if (mPressedView.getParent() != null) {
            mWindowManager.updateViewLayout(mPressedView, mPressedParams);
        }
    }

    private void setDim(boolean dim) {
        if (dim) {
            int curBrightness = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 100);
            int dimAmount = 0;

            IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
            try {
                dimAmount = daemon.getDimAmount(curBrightness);
            } catch (RemoteException e) {
                // do nothing
            }

            if (mShouldBoostBrightness) {
                mPressedParams.screenBrightness = 1.0f;
            }

            mPressedParams.dimAmount = dimAmount / 255.0f;
            if (mPressedView.getParent() == null) {
                mWindowManager.addView(mPressedView, mPressedParams);
            } else {
                mWindowManager.updateViewLayout(mPressedView, mPressedParams);
            }
        } else {
            if (mShouldBoostBrightness) {
                mPressedParams.screenBrightness = 0.0f;
            }
            mPressedParams.dimAmount = 0.0f;
            if (mPressedView.getParent() != null) {
                mWindowManager.removeView(mPressedView);
            }
            updateIconDim(true);
        }
    }

    private boolean isPinOrPattern(int userId) {
        int passwordQuality = mLockPatternUtils.getActivePasswordQuality(userId);
        switch (passwordQuality) {
            // PIN
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            // Pattern
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                return true;
        }

        return false;
    }

    private class BurnInProtectionTask extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis() / 1000 / 60;

            // Let y to be not synchronized with x, so that we get maximum movement
            mDreamingOffsetY = (int) ((now + mDreamingMaxOffset / 3) % (mDreamingMaxOffset * 2));
            mDreamingOffsetY -= mDreamingMaxOffset;

            mHandler.post(FODCircleView.this::updatePosition);
        }
    }

    @Override
    public void onOverlayChanged() {
        updateCutoutFlags();
    }

    private void updateCutoutFlags() {
        mStatusbarHeight = getContext().getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height_portrait);
        boolean cutoutMasked = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_maskMainBuiltInDisplayCutout);
        if (mCutoutMasked != cutoutMasked){
            mCutoutMasked = cutoutMasked;
            updatePosition();
        }
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }
}
