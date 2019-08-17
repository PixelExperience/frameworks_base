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

public class AmbientPlayHistoryEntry {
    private int id;
    private long ts;
    private String song;
    private String artist;

    public AmbientPlayHistoryEntry(int id, long ts, String song, String artist) {
        this.id = id;
        this.ts = ts;
        this.song = song;
        this.artist = artist;
    }

    public long geMatchTimestamp() {
        return this.ts;
    }

    public int getSongID() {
        return this.id;
    }

    public String getArtistTitle() {
        return this.artist;
    }

    public String getSongTitle() {
        return this.song;
    }
}