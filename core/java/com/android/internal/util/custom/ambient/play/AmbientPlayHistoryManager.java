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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

public class AmbientPlayHistoryManager {
    private static final String AUTHORITY = "org.pixelexperience.ambient.play.history.provider";
    private static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/songs");
    private static final String KEY_ID = "_id";
    private static final String KEY_TIMESTAMP = "ts";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_SONG = "song";
    private static final String[] PROJECTION = {KEY_ID, KEY_TIMESTAMP, KEY_SONG, KEY_ARTIST};
    private static String ACTION_SONG_MATCH = "com.android.internal.util.custom.ambient.play.AMBIENT_PLAY_SONG_MATCH";
    public static Intent INTENT_SONG_MATCH = new Intent(ACTION_SONG_MATCH);
    public static final String SERVICE_PACKAGE = "org.pixelexperience.ambient.play.history";

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

    public static void addSong(String song, String artist, Context context) {
        if (!isAvailable(context)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        values.put(KEY_SONG, song);
        values.put(KEY_ARTIST, artist);
        context.getContentResolver().insert(CONTENT_URI, values);
    }

    public static List<AmbientPlayHistoryEntry> getSongs(Context context) {
        List<AmbientPlayHistoryEntry> result = new ArrayList<>();
        if (!isAvailable(context)) {
            return result;
        }
        try (Cursor cursor = context.getContentResolver().query(CONTENT_URI, PROJECTION, null, null,
                null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    result.add(new AmbientPlayHistoryEntry(cursor.getInt(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3)));
                }
            }
        }
        return result;
    }

    public static void deleteSong(int id, Context context) {
        if (!isAvailable(context)) {
            return;
        }
        context.getContentResolver().delete(Uri.parse(CONTENT_URI + "/" + id), null, null);
    }

    public static void deleteAll(Context context) {
        if (!isAvailable(context)) {
            return;
        }
        context.getContentResolver().delete(CONTENT_URI, null, null);
    }

    public static void sendMatchBroadcast(Context context) {
        if (!isAvailable(context)) {
            return;
        }
        context.sendBroadcastAsUser(INTENT_SONG_MATCH, UserHandle.CURRENT);
    }
}
