/*
 * Copyright (C) 2018 PixelExperience
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>.
 */

package com.android.internal.util.custom.ambient.play;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

public class AmbientPlayProvider {

    public static final String SERVICE_PACKAGE = "org.pixelexperience.ambient.play.provider";
    private static final String TAG = "AmbientPlayProvider";
    private static final boolean DEBUG = false;
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_SONG = "song";
    private static final String[] PROJECTION_DEFAULT = new String[]{
            COLUMN_ARTIST,
            COLUMN_SONG
    };

    private static boolean isAvailable(Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(SERVICE_PACKAGE, PackageManager.GET_ACTIVITIES);
            int enabled = pm.getApplicationEnabledSetting(SERVICE_PACKAGE);
            return enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                    enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private Context mContext;
    private ActivityManager mActivityManager;

    public AmbientPlayProvider(Context context){
        mContext = context;
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void stopRecognition() {
        if (!isAvailable(mContext)) {
            return;
        }
        try{
            mActivityManager.forceStopPackage("org.pixelexperience.ambient.play.provider");
        }catch(Exception ignored){
        }
    }

    public Observable recognize() {
        Observable observed = new Observable();
        if (!isAvailable(mContext)) {
            return observed;
        }
        Cursor c = mContext.getContentResolver().query(Uri.parse("content://org.pixelexperience.ambient.play.provider/query/recognize"), PROJECTION_DEFAULT,
                null, null, null);
        if (c != null) {
            try {
                int count = c.getCount();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        c.moveToPosition(i);
                        if (i == 0) {
                            if (c.getString(0) != null && !c.getString(0).equals("") && c.getString(1) != null && !c.getString(1).equals("")) {
                                observed.Artist = c.getString(0);
                                observed.Song = c.getString(1);
                            }
                        }
                    }
                }
            } finally {
                c.close();
            }
        }
        return observed;
    }

    /**
     * Class storing fingerprinting results
     */
    public static class Observable {

        public String Artist;
        public String Song;

        @Override
        public String toString() {
            return Song + " by " + Artist;
        }
    }

}