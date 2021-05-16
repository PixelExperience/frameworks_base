package com.google.android.systemui.ambientmusic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.R;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.doze.util.BurnInHelperKt;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;

public class AmbientIndicationContainer extends AutoReinflateContainer implements DozeReceiver, View.OnClickListener, StatusBarStateController.StateListener, NotificationMediaManager.MediaListener {
    private final static String TAG = "AmbientIndicationContainer";
    private final static String WAKELOCK_TAG = "AmbientIndication";
    private int mAmbientIndicationIconSize;
    private Drawable mAmbientMusicAnimation;
    private PendingIntent mAmbientMusicIntent;
    private CharSequence mAmbientMusicText;
    private boolean mAmbientSkipUnlock;
    private int mBurnInPreventionOffset;
    private float mDozeAmount;
    private boolean mDozing;
    private int mDrawablePadding;
    private final Handler mHandler;
    private final Rect mIconBounds = new Rect();
    private int mIndicationTextMode;
    private int mMediaPlaybackState;
    private boolean mNotificationsHidden;
    private StatusBar mStatusBar;
    private TextView mText;
    private int mTextColor;
    private ValueAnimator mTextColorAnimator;
    private final WakeLock mWakeLock;

    private int AMBIENT_MUSIC_MODE = 1;

    @Override
    public void onStateChanged(int i) {
    }

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mHandler = new Handler(Looper.getMainLooper());
        mWakeLock = createWakeLock(mContext, mHandler);
    }

    @VisibleForTesting
    private WakeLock createWakeLock(Context context, Handler handler) {
        return new DelayedWakeLock(handler, WakeLock.createPartial(context, WAKELOCK_TAG));
    }

    public void initializeView(StatusBar statusBar) {
        mStatusBar = statusBar;
        addInflateListener(new AutoReinflateContainer.InflateListener() {
            @Override
            public final void onInflated(View view) {
                mText = (TextView) findViewById(R.id.ambient_indication_text);
                mAmbientMusicAnimation = getResources().getDrawable(R.drawable.audioanim_animation, mContext.getTheme());
                mTextColor = mText.getCurrentTextColor();
                mAmbientIndicationIconSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_icon_size);
                mBurnInPreventionOffset = getResources().getDimensionPixelSize(R.dimen.default_burn_in_prevention_offset);
                mDrawablePadding = mText.getCompoundDrawablePadding();
                updateColors();
                updatePill();
                mText.setOnClickListener(AmbientIndicationContainer.this);
            }
        });
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                updateBottomPadding();
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
        ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).addCallback(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).removeCallback(this);
        ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).removeCallback(this);
        mMediaPlaybackState = 0;
    }

    public void setAmbientMusic(CharSequence charSequence, PendingIntent pendingIntent, boolean z) {
        mAmbientMusicText = charSequence;
        mAmbientMusicIntent = pendingIntent;
        mAmbientSkipUnlock = z;
        updatePill();
    }

    private void updatePill() {
        int oldIndicationMode = mIndicationTextMode;
        mIndicationTextMode = AMBIENT_MUSIC_MODE;
        Drawable drawable = mAmbientMusicAnimation;
        boolean isDrawableAnimated = mIndicationTextMode == AMBIENT_MUSIC_MODE;
        boolean hasEmptyText = mAmbientMusicText != null && mAmbientMusicText.length() == 0;
        mText.setClickable(mAmbientMusicIntent != null);
        mText.setText(mAmbientMusicText);
        mText.setContentDescription(mAmbientMusicText);
        if (drawable != null) {
            mIconBounds.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            MathUtils.fitRect(mIconBounds, mAmbientIndicationIconSize);
            drawable.setBounds(mIconBounds);
        }
        mText.setCompoundDrawables(isLayoutRtl() ? null : drawable, null, isLayoutRtl() ? drawable : null, null);
        mText.setCompoundDrawablePadding((hasEmptyText || drawable == null) ? 0 : mDrawablePadding);
        boolean shouldShowText = (!TextUtils.isEmpty(mAmbientMusicText) || hasEmptyText) && !mNotificationsHidden;
        boolean oldTextVisible = mText.getVisibility() == View.VISIBLE;
        mText.setVisibility(shouldShowText ? View.VISIBLE : View.GONE);
        if (!shouldShowText) {
            mText.animate().cancel();
            if (drawable != null && (drawable instanceof AnimatedVectorDrawable)) {
                ((AnimatedVectorDrawable) drawable).reset();
            }
            mHandler.post(mWakeLock.wrap(() -> {}));
        } else if (!oldTextVisible) {
            mWakeLock.acquire(WAKELOCK_TAG);
            if (drawable != null && (drawable instanceof AnimatedVectorDrawable)) {
                ((AnimatedVectorDrawable) drawable).start();
            }
            mText.setTranslationY((float) (mText.getHeight() / 2));
            mText.setAlpha(0.0f);
            mText.animate().alpha(1.0f).translationY(0.0f).setStartDelay(150).setDuration(100).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    mWakeLock.release(WAKELOCK_TAG);
                }
            }).setInterpolator(Interpolators.DECELERATE_QUINT).start();
        } else if (mIndicationTextMode == oldIndicationMode) {
            mHandler.post(mWakeLock.wrap(() -> {}));
        } else if (drawable != null && (drawable instanceof AnimatedVectorDrawable)) {
            mWakeLock.acquire(WAKELOCK_TAG);
            ((AnimatedVectorDrawable) drawable).start();
            mWakeLock.release(WAKELOCK_TAG);
        }
        updateBottomPadding();
    }

    private void updateBottomPadding() {
        mStatusBar.getPanelController().setAmbientIndicationBottomPadding(mText.getVisibility() == View.VISIBLE ? mStatusBar.getNotificationScrollLayout().getBottom() - getTop() : 0);
    }

    public void hideAmbientMusic() {
        setAmbientMusic(null, null, false);
    }

    @Override
    public void onClick(View view) {
        if (mAmbientMusicIntent != null) {
            mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), mText, "AMBIENT_MUSIC_CLICK");
            if (mAmbientSkipUnlock) {
                sendBroadcastWithoutDismissingKeyguard(mAmbientMusicIntent);
            } else {
                mStatusBar.postStartActivityDismissingKeyguard(mAmbientMusicIntent);
            }
        }
    }

    @Override
    public void onDozingChanged(boolean z) {
        mDozing = z;
        mText.setEnabled(!z);
        updateColors();
        updateBurnInOffsets();
    }

    @Override
    public void dozeTimeTick() {
        updatePill();
        updateBurnInOffsets();
    }

    private void updateBurnInOffsets() {
        int burnInOffset = BurnInHelperKt.getBurnInOffset(mBurnInPreventionOffset * 2, true);
        int i = mBurnInPreventionOffset;
        setTranslationX(((float) (burnInOffset - i)) * mDozeAmount);
        setTranslationY(((float) (BurnInHelperKt.getBurnInOffset(i * 2, false) - mBurnInPreventionOffset)) * mDozeAmount);
    }

    private void updateColors() {
        if (mTextColorAnimator != null && mTextColorAnimator.isRunning()) {
            mTextColorAnimator.cancel();
        }
        int defaultColor = mText.getTextColors().getDefaultColor();
        int i = mDozing ? -1 : mTextColor;
        if (defaultColor != i) {
            mTextColorAnimator = ValueAnimator.ofArgb(defaultColor, i);
            mTextColorAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            mTextColorAnimator.setDuration(500L);
            mTextColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                    mText.setTextColor(intValue);
                    mText.setCompoundDrawableTintList(ColorStateList.valueOf(intValue));
                }
            });
            mTextColorAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    mTextColorAnimator = null;
                }
            });
            mTextColorAnimator.start();
        }
    }

    @Override
    public void onDozeAmountChanged(float linear, float eased) {
        mDozeAmount = eased;
        updateBurnInOffsets();
    }

    private void sendBroadcastWithoutDismissingKeyguard(PendingIntent pendingIntent) {
        if (!pendingIntent.isActivity()) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Sending intent failed: " + e);
            }
        }
    }

    @Override
    public void onPrimaryMetadataOrStateChanged(MediaMetadata metadata, int state) {
        if (mMediaPlaybackState != state) {
            mMediaPlaybackState = state;
            if (isMediaPlaying()) {
                hideAmbientMusic();
            }
        }
    }

    protected boolean isMediaPlaying() {
        return NotificationMediaManager.isPlayingState(mMediaPlaybackState);
    }
}
