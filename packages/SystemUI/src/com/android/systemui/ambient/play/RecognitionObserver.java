/*
 * Copyright (C) 2014 Fastboot Mobile, LLC.
 * Copyright (C) 2018 CypherOS
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

package com.android.systemui.ambient.play;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import com.android.internal.util.custom.ambient.play.AmbientPlayProvider;
import com.android.internal.util.custom.ambient.play.AmbientPlayProvider.Observable;

/**
 * Class helping audio fingerprinting for recognition
 */
public class RecognitionObserver implements AmbientIndicationManagerCallback {

    private static final String TAG = "RecognitionObserver";

    private AmbientIndicationManager mManager;
    private AmbientPlayProvider mProvider;
    private RecorderThread mRecorderThread;
    private boolean mIsRecording = false;
    private Context mContext;

    RecognitionObserver(Context context, AmbientIndicationManager manager) {
        this.mManager = manager;
        this.mContext = context;
        manager.registerCallback(this);
        mProvider = new AmbientPlayProvider(context);
    }

    @Override
    public void onRecognitionResult(Observable observed) {

    }

    @Override
    public void onRecognitionNoResult() {

    }

    @Override
    public void onRecognitionError() {

    }

    @Override
    public void onSettingsChanged(String key, boolean newValue) {
        if (key.equals(Settings.System.AMBIENT_RECOGNITION)) {
            if (!mManager.isRecognitionEnabled()) {
                if (mManager.DEBUG)
                    Log.d(TAG, "Recognition disabled, stopping all and triggering dispatchRecognitionNoResult");
                stopRecording();
                mManager.dispatchRecognitionNoResult();
            }
        }
    }

    private boolean isNullResult(Observable observed) {
        return observed == null || observed.Artist == null || observed.Song == null;
    }

    private void reportResult(Observable observed) {
        stopRecording();
        // If the recording is still active and we have no match, don't do anything. Otherwise,
        // report the result.
        if (!mManager.isRecognitionEnabled() || isNullResult(observed)) {
            if (mManager.DEBUG) Log.d(TAG, "Reporting onNoMatch");
            mManager.dispatchRecognitionNoResult();
        } else {
            if (mManager.DEBUG) Log.d(TAG, "Reporting result");
            mManager.dispatchRecognitionResult(observed);
        }
    }

    private class RecorderThread extends Thread {
        public void run() {
            reportResult(mProvider.recognize());
        }
    }

    void startRecording() {
        if (!mManager.isRecognitionEnabled()) {
            stopRecording();
            mManager.dispatchRecognitionError();
            return;
        }
        if (mIsRecording) {
            return;
        }
        mIsRecording = true;
        if (mRecorderThread == null){
            mRecorderThread = new RecorderThread();
        }
        try{
            mRecorderThread.start();
        }catch(Exception e){
            stopRecording();
            mManager.dispatchRecognitionError();
        }
    }

    private void stopRecording() {
        if (!mIsRecording){
            return;
        }
        mProvider.stopRecognition();
        mIsRecording = false;
        mRecorderThread = null;
    }
}
