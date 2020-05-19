package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.util.custom.cutout.CutoutUtils;

import com.android.systemui.statusbar.CustomStatusBarItem;

public class StatusBarNetworkTraffic extends NetworkTraffic {

    private boolean mNetworkTrafficIsVisible;
    private int mDarkModeFillColor;
    private int mLightModeFillColor;

    private CustomStatusBarItem.DarkReceiver mDarkReceiver =
            new CustomStatusBarItem.DarkReceiver() {
        public void onDarkChanged(Rect area, float darkIntensity, int tint) {
            mTintColor = tint;
            setTextColor(mTintColor);
            updateTrafficDrawable();
        }

        public void setFillColors(int darkColor, int lightColor) {
            mDarkModeFillColor = darkColor;
            mLightModeFillColor = lightColor;
        }
    };

    private CustomStatusBarItem.VisibilityReceiver mVisibilityReceiver =
            new CustomStatusBarItem.VisibilityReceiver() {
        public void onVisibilityChanged(boolean isVisible) {
            if (mNetworkTrafficIsVisible != isVisible) {
                mNetworkTrafficIsVisible = isVisible;
                updateViewState();
            }
        }
    };

    public StatusBarNetworkTraffic(Context context) {
        this(context, null);
    }

    public StatusBarNetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarNetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTxtFont = "sans-serif-condensed";
    }

    @Override
    protected void onAttachedToWindow() {
        if (!mAttached) {
            CustomStatusBarItem.Manager manager =
                    CustomStatusBarItem.findManager((View) this);
            manager.addDarkReceiver(mDarkReceiver);
            manager.addVisibilityReceiver(mVisibilityReceiver);
        }
        super.onAttachedToWindow();
    }

    @Override
    protected int getMyMode(){
        if (!mNetworkTrafficIsVisible ||
                CutoutUtils.hasCutout(mContext, true /* ignoreCutoutMasked*/)){
            return MODE_DISABLED;
        }
        return MODE_STATUS_BAR;
    }
}
