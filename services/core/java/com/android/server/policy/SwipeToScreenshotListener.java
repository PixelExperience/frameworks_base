/*
 * Copyright (C) 2019 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.policy;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants.PointerEventListener;

public class SwipeToScreenshotListener implements PointerEventListener {
    private static final String TAG = "SwipeToScreenshotListener";
    private static final int THREE_GESTURE_STATE_NONE = 0;
    private static final int THREE_GESTURE_STATE_DETECTING = 1;
    private static final int THREE_GESTURE_STATE_DETECTED_FALSE = 2;
    private static final int THREE_GESTURE_STATE_DETECTED_TRUE = 3;
    private static final int THREE_GESTURE_STATE_NO_DETECT = 4;
    private boolean mBootCompleted;
    private Context mContext;
    private boolean mDeviceProvisioned = false;
    private float[] mInitMotionY;
    private int[] mPointerIds;
    private int mThreeGestureState;
    private int mThreeGestureThreshold;
    private int mThreshold;
    private final Callbacks mCallbacks;

    public SwipeToScreenshotListener(Context context, Callbacks callbacks) {
        mPointerIds = new int[3];
        mInitMotionY = new float[3];
        mContext = context;
        mThreshold = (int) (50.0f * context.getResources().getDisplayMetrics().density);
        mThreeGestureThreshold = mThreshold * 3;
        mThreeGestureState = SystemProperties.getBoolean("sys.android.screenshot", false) ?
                                THREE_GESTURE_STATE_DETECTED_TRUE : THREE_GESTURE_STATE_NONE;
        mCallbacks = checkNull("callbacks", callbacks);
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    public void onPointerEvent(MotionEvent event) {
        processMotionEventForThreeGestureDetect(event);
    }

    private void processMotionEventForThreeGestureDetect(MotionEvent event) {
        int i = 0;
        if (!mBootCompleted) {
            mBootCompleted = SystemProperties.getBoolean("sys.boot_completed", false);
            if (!mBootCompleted) {
                return;
            }
        }
        if (!mDeviceProvisioned) {
            mDeviceProvisioned = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED
                    , 0) != 0;
            if (!mDeviceProvisioned) {
                return;
            }
        }
        if (event.getAction() == 0) {
            changeThreeGestureState(THREE_GESTURE_STATE_NONE);
        } else if (mThreeGestureState == 0 && event.getPointerCount() == 3) {
            if (checkIsStartThreeGesture(event)) {
                changeThreeGestureState(THREE_GESTURE_STATE_DETECTING);
                for (int i2 = 0; i2 < 3; i2++) {
                    mPointerIds[i2] = event.getPointerId(i2);
                    mInitMotionY[i2] = event.getY(i2);
                }
            } else {
                changeThreeGestureState(THREE_GESTURE_STATE_NO_DETECT);
            }
        }
        if (mThreeGestureState != THREE_GESTURE_STATE_NO_DETECT && mThreeGestureState != THREE_GESTURE_STATE_NONE && mThreeGestureState == THREE_GESTURE_STATE_DETECTING) {
            if (event.getPointerCount() != 3) {
                changeThreeGestureState(THREE_GESTURE_STATE_DETECTED_FALSE);
                return;
            }
            if (event.getActionMasked() == 2) {
                float distance = 0.0f;
                while (i < 3) {
                    int index = event.findPointerIndex(mPointerIds[i]);
                    if (index < 0 || index >= 3) {
                        changeThreeGestureState(THREE_GESTURE_STATE_DETECTED_FALSE);
                        return;
                    } else {
                        distance += event.getY(index) - mInitMotionY[i];
                        i++;
                    }
                }
                if (distance >= ((float) mThreeGestureThreshold)) {
                    changeThreeGestureState(THREE_GESTURE_STATE_DETECTED_TRUE);
                    mCallbacks.onSwipeThreeFinger();
                }
            }
        }
    }

    private void changeThreeGestureState(int state) {
        if (mThreeGestureState != state) {
            mThreeGestureState = state;
            boolean shouldEnableProp = state == THREE_GESTURE_STATE_DETECTED_TRUE;
            try {
                SystemProperties.set("sys.android.screenshot", shouldEnableProp ? "true" : "false");
            }catch(Exception e){
                Log.e(TAG, "Exception when setprop", e);
            }
        }
    }

    private boolean checkIsStartThreeGesture(MotionEvent event) {
        if (event.getEventTime() - event.getDownTime() > 500) {
            return false;
        }
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (y > ((float) (height - mThreshold))) {
                return false;
            }
            maxX = Math.max(maxX, x);
            minX = Math.min(minX, x);
            maxY = Math.max(maxY, y);
            minY = Math.min(minY, y);
        }
        if (maxY - minY <= displayMetrics.density * 150.0f) {
            return maxX - minX <= ((float) (width < height ? width : height));
        }
        return false;
    }

    interface Callbacks {
        void onSwipeThreeFinger();
    }
}
