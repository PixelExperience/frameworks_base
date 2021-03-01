package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.android.internal.util.custom.cutout.CutoutUtils;

import com.android.systemui.statusbar.CustomStatusBarItem;

public class StatusBarNetworkTraffic extends NetworkTraffic {

    private CustomStatusBarItem.DarkReceiver mDarkReceiver =
            new CustomStatusBarItem.DarkReceiver() {
        public void onDarkChanged(Rect area, float darkIntensity, int tint) {
            StatusBarNetworkTraffic.this.onDarkChanged(area, darkIntensity, tint);
        }

        public void setFillColors(int darkColor, int lightColor) {
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
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached) {
            CustomStatusBarItem.Manager manager =
                    CustomStatusBarItem.findManager((View) this);
            manager.addDarkReceiver(mDarkReceiver);
        }
    }

    @Override
    protected int getMyMode(){
        if (CutoutUtils.hasCutout(mContext)){
            return MODE_DISABLED;
        }
        return MODE_STATUS_BAR;
    }
}
