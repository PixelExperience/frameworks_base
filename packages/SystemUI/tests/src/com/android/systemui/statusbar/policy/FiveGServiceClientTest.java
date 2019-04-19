/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.systemui.statusbar.policy;

import android.os.RemoteException;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.test.suitebuilder.annotation.SmallTest;
import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper.RunWithLooper;

import java.util.ArrayList;
import java.util.List;

import org.codeaurora.internal.BearerAllocationStatus;
import org.codeaurora.internal.Client;
import org.codeaurora.internal.DcParam;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.INetworkCallback;
import org.codeaurora.internal.NrConfigType;
import org.codeaurora.internal.NrIconType;
import org.codeaurora.internal.ServiceUtil;
import org.codeaurora.internal.Status;
import org.codeaurora.internal.Token;
import org.codeaurora.internal.UpperLayerIndInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.systemui.statusbar.policy.FiveGServiceClient;
import com.android.systemui.statusbar.policy.FiveGServiceClient.FiveGServiceState;
import com.android.systemui.statusbar.policy.TelephonyIcons;

@SmallTest
@RunWith(AndroidTestingRunner.class)
@RunWithLooper
public class FiveGServiceClientTest extends NetworkControllerBaseTest {
    private final static String TAG = "FiveGServiceClientTest";
    private FiveGServiceClient mFiveGServiceClient;
    protected INetworkCallback mCallback;

    Token mToken;
    Status mSuccessStatus;
    Status mFailStatus;
    private int mPhoneId;

    @Before
    public void setupCallback() {
        mPhoneId = 0;
        mToken = new Token(0);
        mSuccessStatus = new Status(Status.SUCCESS);
        mFailStatus = new Status(Status.FAILURE);
        mFiveGServiceClient = mNetworkController.mFiveGServiceClient;
        mCallback = mFiveGServiceClient.mCallback;

    }

    @Test
    public void testSignalStrength() {
        int rsrp = -50;
        int level = 3;
        //Success status case
        org.codeaurora.internal.SignalStrength signalStrength =
                new org.codeaurora.internal.SignalStrength(rsrp, rsrp);
        updateSignalStrength(mPhoneId, mToken, mSuccessStatus, signalStrength);

        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getSignalLevel(), level);

        //Failure status case
        rsrp = org.codeaurora.internal.SignalStrength.INVALID;
        signalStrength =
                new org.codeaurora.internal.SignalStrength(rsrp, rsrp);
        updateSignalStrength(mPhoneId, mToken, mFailStatus, signalStrength);
        fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getSignalLevel(), level);
    }

    @Test
    public void testDcParam() {
        //Success status case
        DcParam dcParam = new DcParam(DcParam.DCNR_UNRESTRICTED, DcParam.DCNR_UNRESTRICTED);
        updateDcParam(mPhoneId, mToken, mSuccessStatus, dcParam);
        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getDcnr(), DcParam.DCNR_UNRESTRICTED);

        //Failure status case
        dcParam = new DcParam(DcParam.DCNR_RESTRICTED, DcParam.DCNR_RESTRICTED);
        updateDcParam(mPhoneId, mToken, mFailStatus, dcParam);
        fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getDcnr(), DcParam.DCNR_UNRESTRICTED);
    }

    @Test
    public void testBearerAllocation() {
        //Success status case
        BearerAllocationStatus allocationStatus =
                new BearerAllocationStatus(BearerAllocationStatus.MMW_ALLOCATED);
        updateBearerAllocation(mPhoneId, mToken, mSuccessStatus, allocationStatus);
        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getAllocated(), BearerAllocationStatus.MMW_ALLOCATED);

        //Failure status case
        allocationStatus =
                new BearerAllocationStatus(BearerAllocationStatus.NOT_ALLOCATED);
        updateBearerAllocation(mPhoneId, mToken, mFailStatus, allocationStatus);
        fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getAllocated(), BearerAllocationStatus.MMW_ALLOCATED);
    }

    @Test
    public void testUpperLayerIndInfo() {
        //Success status case
        UpperLayerIndInfo upperLayerIndInfo =
                new UpperLayerIndInfo(UpperLayerIndInfo.PLMN_INFO_LIST_AVAILABLE,
                        UpperLayerIndInfo.UPPER_LAYER_IND_INFO_AVAILABLE);
        updateUpperLayerIndInfo(mPhoneId, mToken, mSuccessStatus, upperLayerIndInfo);
        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getPlmn(), UpperLayerIndInfo.PLMN_INFO_LIST_AVAILABLE);
        assertEquals(fiveGState.getUpperLayerInd(),
                UpperLayerIndInfo.UPPER_LAYER_IND_INFO_AVAILABLE);

        //Failure status case
        upperLayerIndInfo =
                new UpperLayerIndInfo(UpperLayerIndInfo.PLMN_INFO_LIST_UNAVAILABLE,
                        UpperLayerIndInfo.UPPER_LAYER_IND_INFO_UNAVAILABLE);
        updateUpperLayerIndInfo(mPhoneId,mToken, mFailStatus, upperLayerIndInfo);
        fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getPlmn(), UpperLayerIndInfo.PLMN_INFO_LIST_AVAILABLE);
        assertEquals(fiveGState.getUpperLayerInd(),
                UpperLayerIndInfo.UPPER_LAYER_IND_INFO_AVAILABLE);
    }

    @Test
    public void test5gConfigInfo() {
        //Success status case
        NrConfigType type = new NrConfigType(NrConfigType.SA_CONFIGURATION);
        update5gConfigInfo(mPhoneId, mToken, mSuccessStatus, type);
        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getNrConfigType(), NrConfigType.SA_CONFIGURATION);

        //Failure status case
        type = new NrConfigType(NrConfigType.NSA_CONFIGURATION);
        update5gConfigInfo(mPhoneId, mToken, mFailStatus, type);
        fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getNrConfigType(), NrConfigType.SA_CONFIGURATION);
    }

    @Test
    public void testNrIconType() {
        //Success status case
        NrIconType nrIconType = new NrIconType(NrIconType.TYPE_5G_BASIC);
        updateNrIconType(mPhoneId, mToken, mSuccessStatus, nrIconType);
        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getNrIconType(), NrIconType.TYPE_5G_BASIC);

        //Failure status case
        nrIconType = new NrIconType(NrIconType.TYPE_NONE);
        updateNrIconType(mPhoneId, mToken, mSuccessStatus, nrIconType);
        fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        assertEquals(fiveGState.getNrIconType(), NrIconType.TYPE_5G_BASIC);
    }

    @Test
    public void test5GSaIcon() {
        NrConfigType type = new NrConfigType(NrConfigType.SA_CONFIGURATION);
        update5gConfigInfo(mPhoneId, mToken, mSuccessStatus, type);

        /**
         * Verify that 5G SA icon is shown when
         * NrConfigType is SA_CONFIGURATION and
         * BearerAllocation is MMW_ALLOCATED
         */
        BearerAllocationStatus allocationStatus =
                new BearerAllocationStatus(BearerAllocationStatus.MMW_ALLOCATED);
        updateBearerAllocation(mPhoneId, mToken, mSuccessStatus, allocationStatus);
        verifyIcon(TelephonyIcons.ICON_5G_SA);

        /**
         * Verify that 5G SA icon is not shown when BearerAllocation is NOT_ALLOCATED
         */
        allocationStatus =
                new BearerAllocationStatus(BearerAllocationStatus.NOT_ALLOCATED);
        updateBearerAllocation(mPhoneId, mToken, mSuccessStatus, allocationStatus);
        verifyIcon(0);
    }

    @Test
    public void test5GBasicIcon() {
        NrConfigType configType = new NrConfigType(NrConfigType.NSA_CONFIGURATION);
        update5gConfigInfo(mPhoneId, mToken, mSuccessStatus, configType);

        /**
         * Verify that 5G Basic icon is shown when
         * NrConfigType is NSA_CONFIGURATION and
         * NrIconType is TYPE_5G_BASIC
         */
        NrIconType nrIconType = new NrIconType(NrIconType.TYPE_5G_BASIC);
        updateNrIconType(mPhoneId, mToken, mSuccessStatus, nrIconType);
        verifyIcon(TelephonyIcons.ICON_5G_BASIC);

        /**
         * Verify that 5G Basic icon is not shown when
         * NrConfigType is NSA_CONFIGURATION and
         * NrIconType is TYPE_NONE
         */
        nrIconType = new NrIconType(NrIconType.TYPE_NONE);
        updateNrIconType(mPhoneId, mToken, mSuccessStatus, nrIconType);
        verifyIcon(0);
    }

    @Test
    public void test5GUWBIcon() {
        NrConfigType configType = new NrConfigType(NrConfigType.NSA_CONFIGURATION);
        update5gConfigInfo(mPhoneId, mToken, mSuccessStatus, configType);

        /**
         * Verify that 5G UWB icon is shown when
         * NrConfigType is NSA_CONFIGURATION and
         * NrIconType is TYPE_5G_UWB
         */
        NrIconType nrIconType = new NrIconType(NrIconType.TYPE_5G_UWB);
        updateNrIconType(mPhoneId, mToken, mSuccessStatus, nrIconType);
        verifyIcon(TelephonyIcons.ICON_5G_UWB);

        /**
         * Verify that 5G UWB icon is not shown when
         * NrConfigType is NSA_CONFIGURATION and
         * NrIconType is TYPE_NONE
         */
        nrIconType = new NrIconType(NrIconType.TYPE_NONE);
        updateNrIconType(mPhoneId, mToken, mSuccessStatus, nrIconType);
        verifyIcon(0);
    }

    public void updateDcParam(int phoneId, Token token, Status status, DcParam dcParam) {
        Log.d(TAG, "Sending DcParam");
        try {
            mCallback.onNrDcParam(phoneId, token, status, dcParam);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateSignalStrength(int phoneId, Token token, Status status,
                                     org.codeaurora.internal.SignalStrength signalStrength) {
        Log.d(TAG, "Sending SignalStrength");
        try {
            mCallback.onSignalStrength(phoneId, token, status, signalStrength);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateBearerAllocation(int phoneId, Token token, Status status,
                                       BearerAllocationStatus bearerStatus) {
        Log.d(TAG, "Sending BearerAllocationStatus");
        try {
            mCallback.onAnyNrBearerAllocation(phoneId, token, status, bearerStatus);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateUpperLayerIndInfo(int phoneId, Token token, Status status,
                                        UpperLayerIndInfo uilInfo) {
        Log.d(TAG, "Sending UpperLayerIndInfo");
        try {
            mCallback.onUpperLayerIndInfo(phoneId, token, status, uilInfo);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void update5gConfigInfo(int phoneId, Token token, Status status,
                                   NrConfigType nrConfigType) {
        Log.d(TAG, "Sending 5gConfigInfo");
        try {
            mCallback.on5gConfigInfo(phoneId, token, status, nrConfigType);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateNrIconType(int phoneId, Token token, Status status, NrIconType nrIconType) {
        Log.d(TAG, "Sending NrIconType");
        try {
            mCallback.onNrIconType(phoneId, token, status, nrIconType);
        } catch ( RemoteException e) {
            e.printStackTrace();
        }
    }

    private void verifyIcon(int resIcon) {
        FiveGServiceState fiveGState = mFiveGServiceClient.getCurrentServiceState(mPhoneId);
        int dataType = fiveGState.getIconGroup().mDataType;
        assertEquals(dataType, resIcon);
    }

}
