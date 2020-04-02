package com.android.internal.custom.longshot.injector;

import android.content.Context;
import android.util.Log;

public class ViewInjector {
    public static final String LONGSHOTDE_TAG = "Longshot.View";
    private static boolean LONGSHOT_DEBUG = false;

    public static void processActionDownLongshot(Context context, View view) {
        boolean isInScrollingContainer = view.isInScrollingContainer();
        if (!OpViewInjector.View.isInjection) {
            return;
        }
        if (view.isScrollContainer()) {
            int[] position = new int[2];
            view.getLocationInWindow(position);
            OpViewInjector.View.setScrolledViewTop(context, position[1]);
            if (LONGSHOT_DEBUG) {
                Log.m170i(LONGSHOTDE_TAG, "touch view isScrollContainer");
            }
        } else if (isInScrollingContainer && OpViewInjector.View.isInjection) {
            ViewParent targetView = view.getParent();
            while (targetView != null && (targetView instanceof ViewGroup)) {
                if (((ViewGroup) targetView).shouldDelayChildPressedState()) {
                    int[] position2 = new int[2];
                    ((ViewGroup) targetView).getLocationInWindow(position2);
                    OpViewInjector.View.setScrolledViewTop(context, position2[1]);
                    if (LONGSHOT_DEBUG) {
                        Log.m170i(LONGSHOTDE_TAG, "touch view isInScrollingContainer");
                        return;
                    }
                    return;
                }
                targetView = targetView.getParent();
            }
        }
    }
}
