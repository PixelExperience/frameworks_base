package com.android.internal.custom.longshot.injector;

import android.content.Context;
import android.os.ServiceManager;

import com.android.internal.custom.longshot.ILongScreenshotManager;

public class ScrollViewInjector {

    public static class ScrollView {
        private static final String TAG = "ScrollViewInjector";
        public static boolean isInjection = false;

        public static void onOverScrolled(Context context, boolean isOverScroll) {
            ILongScreenshotManager sm = ILongScreenshotManager.Stub.asInterface(ServiceManager.getService(Context.LONGSCREENSHOT_SERVICE));
            if (isInjection && sm != null && sm.isLongshotMoveState()) {
                sm.notifyLongshotScroll(isOverScroll);
            }
        }
    }
}
