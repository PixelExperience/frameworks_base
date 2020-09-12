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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.biometrics.BiometricSourceType;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.pocket.IPocketCallback;
import android.pocket.PocketManager;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.utils.ThreadUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreen;
import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreenCallback;

import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class FODCircleView extends ImageView implements ConfigurationListener {
    private static final int FADE_ANIM_DURATION = 125;
    private final String SCREEN_BRIGHTNESS = Settings.System.SCREEN_BRIGHTNESS;
    private final int[][] BRIGHTNESS_ALPHA_ARRAY = {
        new int[]{0, 255},
        new int[]{1, 224},
        new int[]{2, 213},
        new int[]{3, 211},
        new int[]{4, 208},
        new int[]{5, 206},
        new int[]{6, 203},
        new int[]{8, 200},
        new int[]{10, 196},
        new int[]{15, 186},
        new int[]{20, 176},
        new int[]{30, 160},
        new int[]{45, 139},
        new int[]{70, 114},
        new int[]{100, 90},
        new int[]{150, 56},
        new int[]{227, 14},
        new int[]{255, 0}
    };
    private final int mPositionX;
    private final int mPositionY;
    private final int mSize;
    private final int mDreamingMaxOffset;
    private final int mNavigationBarSize;
    private final boolean mHideFodCircleGoingToSleep;
    private final boolean mShouldBoostBrightness;
    private final boolean mTargetUsesInKernelDimming;
    private final Paint mPaintFingerprintBackground = new Paint();
    private final Paint mPaintFingerprint = new Paint();
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
    private final WindowManager.LayoutParams mPressedParams = new WindowManager.LayoutParams();
    private final WindowManager mWindowManager;

    private IFingerprintInscreen mFingerprintInscreenDaemon;
    private Context mContext;

    private int mCurrentBrightness;
    private int mDreamingOffsetY;

    private boolean mFading;
    private boolean mIsBouncer;
    private boolean mIsBiometricRunning;
    private boolean mIsCircleShowing;
    private boolean mIsDreaming;
    private boolean mIsKeyguard;
    private boolean mTouchedOutside;
    private boolean mIsAnimating = false;

    private Handler mHandler;

    private final ImageView mPressedView;

    private LockPatternUtils mLockPatternUtils;

    private Timer mBurnInProtectionTimer;

    private IFingerprintInscreenCallback mFingerprintInscreenCallback =
            new IFingerprintInscreenCallback.Stub() {
        @Override
        public void onFingerDown() {
            if (mUpdateMonitor.userNeedsStrongAuth()) {
                // Keyguard requires strong authentication (not biometrics)
                return;
            }

            mHandler.post(() -> showCircle());
        }

        @Override
        public void onFingerUp() {
            mHandler.post(() -> hideCircle());
        }
    };

    private KeyguardUpdateMonitor mUpdateMonitor;

    private KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onBiometricAuthenticated(int userId, BiometricSourceType biometricSourceType,
                boolean isStrongBiometric) {
            // We assume that if biometricSourceType matches Fingerprint it will be
            // handled here, so we hide only when other biometric types authenticate
            if (biometricSourceType != BiometricSourceType.FINGERPRINT) {
                hide();
            }
        }

        @Override
        public void onBiometricRunningStateChanged(boolean running,
                BiometricSourceType biometricSourceType) {
            if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
                mIsBiometricRunning = running;
            }
        }

        @Override
        public void onDreamingStateChanged(boolean dreaming) {
            mIsDreaming = dreaming;
            updateIconDim(false);

            if (mIsKeyguard && mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
                updateIconDim(false);
            } else {
                hide();
            }

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
        public void onKeyguardVisibilityChanged(boolean showing) {
            mIsKeyguard = showing;
            if (!showing) {
                hide();
            } else {
                updateIconDim(false);
            }
            handlePocketManagerCallback(showing);
        }

        @Override
        public void onKeyguardBouncerChanged(boolean isBouncer) {
            mIsBouncer = isBouncer;
            if (mUpdateMonitor.isFingerprintDetectionRunning() && !mUpdateMonitor.userNeedsStrongAuth()) {
                if (isPinOrPattern(mUpdateMonitor.getCurrentUser()) || !isBouncer) {
                    show();
                } else {
                    hide();
                }
            } else {
                hide();
            }
        }

        @Override
        public void onStartedGoingToSleep(int why) {
            if (mHideFodCircleGoingToSleep) {
                hide();
            }
        }

        @Override
        public void onScreenTurnedOff() {
            if (!mHideFodCircleGoingToSleep) {
                hide();
            }
        }

        @Override
        public void onStartedWakingUp() {
            if (mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            }
        }

        @Override
        public void onScreenTurnedOn() {
            if (mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            }
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

    private CustomSettingsObserver mCustomSettingsObserver;

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

        mTargetUsesInKernelDimming = res.getBoolean(com.android.internal.R.bool.config_targetUsesInKernelDimming);
        
        mHideFodCircleGoingToSleep = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_hideFodCircleGoingToSleep);

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
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;

        mPressedParams.copyFrom(mParams);
        mPressedParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        mParams.setTitle("Fingerprint on display");
        mPressedParams.setTitle("Fingerprint on display.touched");

        mPressedView = new ImageView(context)  {
            @Override
            protected void onDraw(Canvas canvas) {
                if (mIsCircleShowing) {
                    canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2.0f, mPaintFingerprint);
                }
                super.onDraw(canvas);
            }
        };
        mPressedView.setImageResource(R.drawable.fod_icon_pressed);

        mWindowManager.addView(this, mParams);

        updatePosition();
        hide();

        mLockPatternUtils = new LockPatternUtils(mContext);

        mUpdateMonitor = Dependency.get(KeyguardUpdateMonitor.class);
        mUpdateMonitor.registerCallback(mMonitorCallback);
    
        updateCutoutFlags();
        Dependency.get(ConfigurationController.class).addCallback(this);

        // Pocket
        mPocketManager = (PocketManager) context.getSystemService(Context.POCKET_SERVICE);
    }

    private int interpolate(int i, int i2, int i3, int i4, int i5) {
        int i6 = i5 - i4;
        int i7 = i - i2;
        int i8 = ((i6 * 2) * i7) / (i3 - i2);
        int i9 = i8 / 2;
        int i10 = i2 - i3;
        return i4 + i9 + (i8 % 2) + ((i10 == 0 || i6 == 0) ? 0 : (((i7 * 2) * (i - i3)) / i6) / i10);
    }

    private int getDimAlpha() {
        int length = BRIGHTNESS_ALPHA_ARRAY.length;
        int i = 0;
        while (i < length && BRIGHTNESS_ALPHA_ARRAY[i][0] < mCurrentBrightness) {
            i++;
        }
        if (i == 0) {
            return BRIGHTNESS_ALPHA_ARRAY[0][1];
        }
        if (i == length) {
            return BRIGHTNESS_ALPHA_ARRAY[length - 1][1];
        }
        int[][] iArr = BRIGHTNESS_ALPHA_ARRAY;
        int i2 = i - 1;
        return interpolate(mCurrentBrightness, iArr[i2][0], iArr[i][0], iArr[i2][1], iArr[i][1]);
    }

    public void updateIconDim(boolean animate) {
        if (!mIsCircleShowing && mTargetUsesInKernelDimming) {
            if (animate && !mIsAnimating) {
                ValueAnimator anim = new ValueAnimator();
                anim.setIntValues(0, getDimAlpha());
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int progress = (Integer) valueAnimator.getAnimatedValue();
                        setColorFilter(Color.argb(progress, 0, 0, 0),
                                PorterDuff.Mode.SRC_ATOP);
                    }
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsAnimating = false;
                    }
                });
                anim.setDuration(500);
                mIsAnimating = true;
                mHandler.post(() -> anim.start());
            } else if (!mIsAnimating) {
                mHandler.post(() ->
                        setColorFilter(Color.argb(getDimAlpha(), 0, 0, 0),
                        PorterDuff.Mode.SRC_ATOP));
            }
        } else {
            mHandler.post(() -> setColorFilter(Color.argb(0, 0, 0, 0), PorterDuff.Mode.SRC_ATOP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mIsCircleShowing) {
            canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2.0f, mPaintFingerprintBackground);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);

        boolean newIsInside = (x > 0 && x < mSize) && (y > 0 && y < mSize);
        mTouchedOutside = false;

        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mTouchedOutside = true;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && newIsInside) {
            showCircle();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            hideCircle();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        }

        return false;
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
                    mFingerprintInscreenDaemon.asBinder().linkToDeath((cookie) -> {
                        mFingerprintInscreenDaemon = null;
                    }, 0);
                }
            } catch (NoSuchElementException | RemoteException e) {
                // do nothing
            }
        }
        return mFingerprintInscreenDaemon;
    }

    public void dispatchPress() {
        if (mFading) return;
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
        if (mFading || mTouchedOutside || (mIsKeyguard && mIsDeviceInPocket)) return;
        mIsCircleShowing = true;

        setKeepScreenOn(true);

        setDim(true);
        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchPress();
        });

        setImageDrawable(null);
        updateIconDim(false);
        updatePosition();
        invalidate();
    }

    public void hideCircle() {
        mIsCircleShowing = false;

        setImageResource(R.drawable.fod_icon_default);
        invalidate();

        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchRelease();
        });
        setDim(false);

        setKeepScreenOn(false);
    }

    public void show() {
        if (mUpdateMonitor.userNeedsStrongAuth()) {
            // Keyguard requires strong authentication (not biometrics)
            return;
        }

        if (!mUpdateMonitor.isScreenOn()) {
            // Keyguard is shown just after screen turning off
            return;
        }

        if (mIsBouncer && !isPinOrPattern(mUpdateMonitor.getCurrentUser())) {
            // Ignore show calls when Keyguard password screen is being shown
            return;
        }

        if (mIsKeyguard && mUpdateMonitor.getUserCanSkipBouncer(mUpdateMonitor.getCurrentUser())) {
            // Ignore show calls if user can skip bouncer
            return;
        }

        if (mIsKeyguard && !mIsBiometricRunning) {
            return;
        }

        updatePosition();
        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();

        setVisibility(View.VISIBLE);
        animate().withStartAction(() -> mFading = true)
                .alpha(mIsDreaming ? 0.5f : 1.0f)
                .setDuration(FADE_ANIM_DURATION)
                .withEndAction(() -> mFading = false)
                .start();
        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchShow();
        });
    }

    public void hide() {
        animate().withStartAction(() -> mFading = true)
                .alpha(0)
                .setDuration(FADE_ANIM_DURATION)
                .withEndAction(() -> {
                    setVisibility(View.GONE);
                    mFading = false;
                })
                .start();
        mCustomSettingsObserver.unobserve();
        hideCircle();
        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchHide();
        });
    }

    private void updatePosition() {
        Display defaultDisplay = mWindowManager.getDefaultDisplay();

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

            mHandler.post(() -> updatePosition());
        }
    };

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
}
