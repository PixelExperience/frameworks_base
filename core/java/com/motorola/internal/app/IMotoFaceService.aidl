/**
 * Copyright (C) 2020 The PixelExperience Project
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

package com.motorola.internal.app;

import com.motorola.internal.app.IMotoFaceServiceReceiver;

/** @hide */
interface IMotoFaceService {
    void authenticate(long operationId);

    void cancel();

    void enroll(byte[] cryptoToken, int timeout, int[] dfs);

    int enumerate();

    long generateChallenge(int timeout);

    int getAuthenticatorId();

    boolean getFeature(int feature, int faceId);

    int getFeatureCount();

    void remove(int biometricId);

    void resetLockout(byte[] cryptoToken);

    int revokeChallenge();

    void setCallback(in IMotoFaceServiceReceiver receiver);

    void setFeature(int feature, boolean enable, byte[] token, int faceId);
}