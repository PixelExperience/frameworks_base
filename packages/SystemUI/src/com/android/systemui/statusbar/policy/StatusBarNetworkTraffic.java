package com.android.systemui.statusbar.policy;

import static com.android.systemui.statusbar.StatusBarIconView.STATE_DOT;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_HIDDEN;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_ICON;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.StatusIconDisplayable;

public class StatusBarNetworkTraffic extends NetworkTraffic implements StatusIconDisplayable {

    private static final String TAG = "StatusBarNetworkTraffic";

    private static final boolean DEBUG = false;

    public static final String SLOT = "networktraffic";

    protected static final int MY_MODE = MODE_STATUS_BAR;

    private int mVisibleState = -1;
    private boolean mSystemIconVisible = true;
    private boolean mColorIsStatic = false;

    public StatusBarNetworkTraffic(Context context) {
        this(context, null);
    }

    public StatusBarNetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarNetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //init();
        mTxtFont = "sans-serif-condensed";
    }

    @Override
    protected void onAttachedToWindow() {
        if (!mAttached) {
            Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
        }
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAttached) {
            Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        if (mColorIsStatic) {
            return;
        }
        mTintColor = DarkIconDispatcher.getTint(area, this, tint);
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }

    @Override
    public String getSlot() {
        return SLOT;
    }

    @Override
    public boolean isIconVisible() {
        return (mMode == MODE_STATUS_BAR);
    }

    @Override
    public int getVisibleState() {
        return mVisibleState;
    }

    @Override
    public void setVisibleState(int state, boolean animate) {
        if (state == mVisibleState) {
            return;
        }
        mVisibleState = state;

        switch (state) {
            case STATE_ICON:
                mSystemIconVisible = true;
                break;
            case STATE_DOT:
            case STATE_HIDDEN:
            default:
                mSystemIconVisible = false;
                break;
        }
        updateVisibility();
    }

    @Override
    protected void updateVisibility() {
        if (!mSystemIconVisible) {
            setVisibility(View.GONE);
        }else{
            super.updateVisibility();
        }
    }

    @Override
    public void setStaticDrawableColor(int color) {
        mColorIsStatic = true;
        mTintColor = color;
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }

    @Override
    public void setDecorColor(int color) {
    }
}
