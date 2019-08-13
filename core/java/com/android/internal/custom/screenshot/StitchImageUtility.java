package com.android.internal.custom.screenshot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class StitchImageUtility {
    public static final String STITCHIMAGE_APP_PACKAGE_NAME = "com.asus.stitchimage";
    public static final String STITCHIMAGE_FILEPROVIDER_CLASS = "com.asus.stitchimage.fileprovider";
    private static final String STITCHIMAGE_OVERLAY_SERVICE_CLASS = "com.asus.stitchimage.OverlayService";
    private static final String STITCHIMAGE_SERVICE_PACKAGE_NAME = "com.asus.stitchimage.service";
    private static final String EXTRA_KEY_STITCHIMAGE_SETTINGS_CALLFROM = "callfrom";
    private static final String EXTRA_VALUE_STITCHIMAGE_SETTINGS_CALLFROM_ASUSSETTINGS = "AsusSettings";
    private static String TAG = "StitchImageUtility";
    private boolean mSupportLongScreenshot = false;
    private Context mContext;

    StitchImageUtility(Context context) {
        mContext = context;
        mSupportLongScreenshot = isAvailable(mContext);
    }

    public boolean takeScreenShot() {
        if (mSupportLongScreenshot) {
            try {
                Log.i(TAG, "Take long screenshot.");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(STITCHIMAGE_APP_PACKAGE_NAME, STITCHIMAGE_OVERLAY_SERVICE_CLASS));
                intent.putExtra(EXTRA_KEY_STITCHIMAGE_SETTINGS_CALLFROM, EXTRA_VALUE_STITCHIMAGE_SETTINGS_CALLFROM_ASUSSETTINGS);
                mContext.startService(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "trigger stitchimage failed, Exception :" + e);
            }
        }
        return false;
    }

    public static boolean isAvailable(Context context) {
        try {
            context.getPackageManager().getPackageInfo(STITCHIMAGE_APP_PACKAGE_NAME, 0);
            context.getPackageManager().getPackageInfo(STITCHIMAGE_SERVICE_PACKAGE_NAME, 0);
            return true;
        } catch (NameNotFoundException unused) {
        }
        return false;
    }
}
