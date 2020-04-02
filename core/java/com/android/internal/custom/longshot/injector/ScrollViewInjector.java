package com.android.internal.custom.longshot.injector;

import android.content.Context;
import android.os.ServiceManager;

import com.android.internal.custom.longshot.OpLongScreenshotManagerService;

public class ScrollViewInjector {

    public static class ScrollView {
        private static final String TAG = "ScrollViewInjector";
        public static boolean isInjection = false;

        public static void onOverScrolled(Context context, boolean isOverScroll) {
            OpLongScreenshotManagerService sm = (OpLongScreenshotManagerService) context.getSystemService(Context.LONGSCREENSHOT_SERVICE);
            if (isInjection && sm != null && sm.isLongshotMoveState()) {
                sm.notifyLongshotScroll(isOverScroll);
            }
        }
    }
}
