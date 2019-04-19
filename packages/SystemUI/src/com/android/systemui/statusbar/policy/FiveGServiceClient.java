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

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.Exception;

import org.codeaurora.internal.BearerAllocationStatus;
import org.codeaurora.internal.Client;
import org.codeaurora.internal.DcParam;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.INetworkCallback;
import org.codeaurora.internal.NrConfigType;
import org.codeaurora.internal.NrIconType;
import org.codeaurora.internal.ServiceUtil;
import org.codeaurora.internal.SignalStrength;
import org.codeaurora.internal.Status;
import org.codeaurora.internal.Token;
import org.codeaurora.internal.UpperLayerIndInfo;
import org.codeaurora.internal.NetworkCallbackBase;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileSignalController.MobileIconGroup;

public class FiveGServiceClient {
    private static final String TAG = "FiveGServiceClient";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG)||true;
    private static final int MESSAGE_REBIND = 1024;
    private static final int MESSAGE_REINIT = MESSAGE_REBIND+1;
    private static final int MAX_RETRY = 4;
    private static final int DELAY_MILLISECOND = 3000;
    private static final int DELAY_INCREMENT = 2000;
    private final int mRsrpThresholds[];
    private final int mSnrThresholds[];

    @VisibleForTesting
    final SparseArray<IFiveGStateListener> mStatesListeners = new SparseArray<>();
    private final SparseArray<FiveGServiceState> mCurrentServiceStates = new SparseArray<>();
    private final SparseArray<FiveGServiceState> mLastServiceStates = new SparseArray<>();

    private Context mContext;
    private boolean mServiceConnected;
    private IExtTelephony mNetworkService;
    private String mPackageName;
    private Client mClient;
    private int mBindRetryTimes = 0;
    private int mInitRetryTimes = 0;

    public static class FiveGServiceState{
        private int mBearerAllocationStatus;
        private int mPlmn;
        private int mUpperLayerInd;
        private int mDcnr;
        private int mLevel;
        private int mNrConfigType;
        private int mNrIconType;
        private MobileIconGroup mIconGroup;

        public FiveGServiceState(){
            mBearerAllocationStatus = BearerAllocationStatus.NOT_ALLOCATED;
            mPlmn = UpperLayerIndInfo.PLMN_INFO_LIST_UNAVAILABLE;
            mUpperLayerInd = UpperLayerIndInfo.UPPER_LAYER_IND_INFO_UNAVAILABLE;
            mDcnr = DcParam.DCNR_RESTRICTED;
            mLevel = 0;
            mNrConfigType = NrConfigType.NSA_CONFIGURATION;
            mNrIconType = NrIconType.INVALID;
            mIconGroup = TelephonyIcons.UNKNOWN;
        }

        public boolean isConnectedOnSaMode() {
            boolean connected = false;
            if ( mNrConfigType == NrConfigType.SA_CONFIGURATION
                    &&  mIconGroup != TelephonyIcons.UNKNOWN) {
                connected = true;
            }
            return connected;
        }

        public boolean isConnectedOnNsaMode() {
            boolean connected = false;
            if ( mNrConfigType == NrConfigType.NSA_CONFIGURATION
                    && mIconGroup != TelephonyIcons.UNKNOWN) {
                connected = true;
            }
            return connected;
        }

        @VisibleForTesting
        public MobileIconGroup getIconGroup() {
            return mIconGroup;
        }

        @VisibleForTesting
        public int getSignalLevel() {
            return mLevel;
        }

        @VisibleForTesting
        public int getAllocated() {
            return mBearerAllocationStatus;
        }

        @VisibleForTesting
        int getNrConfigType() {
            return mNrConfigType;
        }

        @VisibleForTesting
        int getDcnr() {
            return mDcnr;
        }

        @VisibleForTesting
        int getPlmn() {
            return mPlmn;
        }

        @VisibleForTesting
        int getUpperLayerInd() {
            return mUpperLayerInd;
        }

        @VisibleForTesting
        int getNrIconType() {
            return mNrIconType;
        }

        public void copyFrom(FiveGServiceState state) {
            this.mBearerAllocationStatus = state.mBearerAllocationStatus;
            this.mPlmn = state.mPlmn;
            this.mUpperLayerInd = state.mUpperLayerInd;
            this.mDcnr = state.mDcnr;
            this.mLevel = state.mLevel;
            this.mNrConfigType = state.mNrConfigType;
            this.mIconGroup = state.mIconGroup;
            this.mNrIconType = state.mNrIconType;
        }

        public boolean equals(FiveGServiceState state) {
            return this.mBearerAllocationStatus == state.mBearerAllocationStatus
                    && this.mPlmn == state.mPlmn
                    && this.mUpperLayerInd == state.mUpperLayerInd
                    && this.mDcnr == state.mDcnr
                    && this.mLevel == state.mLevel
                    && this.mNrConfigType == state.mNrConfigType
                    && this.mIconGroup == state.mIconGroup
                    && this.mNrIconType == state.mNrIconType;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("mBearerAllocationStatus=").
                    append(mBearerAllocationStatus).append(", ").
                    append("mPlmn=").append(mPlmn).append(", ").
                    append("mUpperLayerInd=").append(mUpperLayerInd).append(", ").
                    append("mDcnr=" + mDcnr).append(", ").
                    append("mLevel=").append(mLevel).append(", ").
                    append("mNrConfigType=").append(mNrConfigType).append(", ").
                    append("mIconGroup=").append(mIconGroup).
                    append("mNrIconType=").append(mNrIconType);

            return builder.toString();
        }
    }

    public FiveGServiceClient(Context context) {
        mContext = context;
        mPackageName = mContext.getPackageName();

        mRsrpThresholds =
                mContext.getResources().getIntArray(R.array.config_5g_signal_rsrp_thresholds);
        mSnrThresholds =
                mContext.getResources().getIntArray(R.array.config_5g_signal_snr_thresholds);
    }

    public void registerListener(int phoneId, IFiveGStateListener listener) {
        Log.d(TAG, "registerListener phoneId=" + phoneId);

        mStatesListeners.put(phoneId, listener);
        if ( !isServiceConnected() ) {
            binderService();
        }else{
            initFiveGServiceState(phoneId);
        }
    }

    public void unregisterListener(int phoneId) {
        Log.d(TAG, "unregisterListener phoneId=" + phoneId);
        mStatesListeners.remove(phoneId);
        mCurrentServiceStates.remove(phoneId);
        mLastServiceStates.remove(phoneId);
    }

    private void binderService() {
        boolean success = ServiceUtil.bindService(mContext, mServiceConnection);
        Log.d(TAG, " bind service " + success);
        if ( !success && mBindRetryTimes < MAX_RETRY && !mHandler.hasMessages(MESSAGE_REBIND)) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_REBIND,
                    DELAY_MILLISECOND + mBindRetryTimes*DELAY_INCREMENT);
            mBindRetryTimes+=1;
        }
    }

    public static int getNumLevels(Context context) {
        return context.getResources().getInteger(R.integer.config_5g_num_signal_strength_bins);
    }

    public boolean isServiceConnected() {
        return mServiceConnected;
    }

    @VisibleForTesting
    FiveGServiceState getCurrentServiceState(int phoneId) {
        return getServiceState(phoneId, mCurrentServiceStates);
    }

    private FiveGServiceState getLastServiceState(int phoneId) {
        return getServiceState(phoneId, mLastServiceStates);
    }

    private static FiveGServiceState getServiceState(int key,
                                                     SparseArray<FiveGServiceState> array) {
        FiveGServiceState state = array.get(key);
        if ( state == null ) {
            state = new FiveGServiceState();
            array.put(key, state);
        }
        return state;
    }

    private int getSnrLevel(int snr) {
        return getLevel(snr, mSnrThresholds);
    }

    private int getRsrpLevel(int rsrp) {
        return getLevel(rsrp, mRsrpThresholds);
    }

    private static int getLevel(int value, int[]thresholds) {
        int level = 0;
        if ( thresholds[thresholds.length-1] < value || value < thresholds[0] ) {
            level = 0;
        } else{
            level = 1;
            for( int i=0; i < thresholds.length-1; ++i ) {
                if (thresholds[i] < value && value <= thresholds[i+1]) {
                    level = i+1;
                    break;
                }
            }
        }
        if ( DEBUG ) {
            Log.d(TAG, "value=" + value + " level=" + level);
        }
        return level;
    }

    private void notifyListenersIfNecessary(int phoneId) {
        FiveGServiceState currentState = getCurrentServiceState(phoneId);
        FiveGServiceState lastState = getLastServiceState(phoneId);
        if ( !currentState.equals(lastState) ) {

            if ( DEBUG ) {
                Log.d(TAG, "phoneId(" + phoneId + ") Change in state from " + lastState + " \n"+
                        "\tto " + currentState);

            }

            lastState.copyFrom(currentState);
            IFiveGStateListener listener = mStatesListeners.get(phoneId);
            if (listener != null) {
                listener.onStateChanged(currentState);
            }

        }
    }

    private void initFiveGServiceState() {
        Log.d(TAG, "initFiveGServiceState size=" + mStatesListeners.size());
        for( int i=0; i < mStatesListeners.size(); ++i ) {
            int phoneId = mStatesListeners.keyAt(i);
            initFiveGServiceState(phoneId);
        }
    }

    private void initFiveGServiceState(int phoneId) {
        Log.d(TAG, "mNetworkService=" + mNetworkService + " mClient=" + mClient);
        if ( mNetworkService != null && mClient != null) {
            Log.d(TAG, "query 5G service state for phoneId " + phoneId);
            try {
                Token token = mNetworkService.queryNrDcParam(phoneId, mClient);
                Log.d(TAG, "queryNrDcParam result:" + token);

                token = mNetworkService.queryNrBearerAllocation(phoneId, mClient);
                Log.d(TAG, "queryNrBearerAllocation result:" + token);

                token = mNetworkService.queryNrSignalStrength(phoneId, mClient);
                Log.d(TAG, "queryNrSignalStrength result:" + token);

                token = mNetworkService.queryUpperLayerIndInfo(phoneId, mClient);
                Log.d(TAG, "queryUpperLayerIndInfo result:" + token);

                token = mNetworkService.query5gConfigInfo(phoneId, mClient);
                Log.d(TAG, "query5gConfigInfo result:" + token);

                token = mNetworkService.queryNrIconType(phoneId, mClient);
                Log.d(TAG, "queryNrIconType result:" + token);
            }catch (Exception e) {
                Log.d(TAG, "initFiveGServiceState: Exception = " + e);
                if ( mInitRetryTimes < MAX_RETRY && !mHandler.hasMessages(MESSAGE_REINIT) ) {
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REINIT,
                            DELAY_MILLISECOND + mInitRetryTimes*DELAY_INCREMENT);
                    mInitRetryTimes +=1;
                }
            }
        }
    }

    @VisibleForTesting
    void update5GIcon(FiveGServiceState state,int phoneId) {
        if ( state.mNrConfigType == NrConfigType.SA_CONFIGURATION ) {
            state.mIconGroup = getSaIcon(state);
        }else if ( state.mNrConfigType == NrConfigType.NSA_CONFIGURATION){
            state.mIconGroup = getNrIconGroup(state.mNrIconType, phoneId);
        }else {
            state.mIconGroup = TelephonyIcons.UNKNOWN;
        }
    }

    private MobileIconGroup getSaIcon(FiveGServiceState state) {
        if ( state.mBearerAllocationStatus > BearerAllocationStatus.NOT_ALLOCATED ) {
            return TelephonyIcons.FIVE_G_SA;
        }else {
            return TelephonyIcons.UNKNOWN;
        }
    }

    private MobileIconGroup getNrIconGroup(int nrIconType , int phoneId) {
        MobileIconGroup iconGroup = TelephonyIcons.UNKNOWN;
        switch (nrIconType){
            case NrIconType.TYPE_5G_BASIC:
                iconGroup = TelephonyIcons.FIVE_G_BASIC;
                break;
            case NrIconType.TYPE_5G_UWB:
                iconGroup = TelephonyIcons.FIVE_G_UWB;
                break;
        }
        return iconGroup;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch ( msg.what ) {
                case MESSAGE_REBIND:
                    binderService();
                    break;

                case MESSAGE_REINIT:
                    initFiveGServiceState();
                    break;
            }

        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected:" + service);

            try {
                mNetworkService = IExtTelephony.Stub.asInterface(service);
                mClient = mNetworkService.registerCallback(mPackageName, mCallback);
                mServiceConnected = true;
                initFiveGServiceState();
                Log.d(TAG, "Client = " + mClient);
            } catch (Exception e) {
                Log.d(TAG, "onServiceConnected: Exception = " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected:" + name);
            cleanup();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "onBindingDied:" + name);
            cleanup();
            if ( mBindRetryTimes < MAX_RETRY ) {
                Log.d(TAG, "try to re-bind");
                mHandler.sendEmptyMessageDelayed(MESSAGE_REBIND,
                        DELAY_MILLISECOND+mBindRetryTimes*DELAY_INCREMENT);
            }
        }

        private void cleanup() {
            Log.d(TAG, "cleanup");
            mServiceConnected = false;
            mNetworkService = null;
            mClient = null;
        }
    };

    @VisibleForTesting
    INetworkCallback mCallback = new NetworkCallbackBase() {
        @Override
        public void on5gStatus(int slotId, Token token, Status status, boolean enableStatus) throws
                RemoteException {
            if ( DEBUG ) {
                Log.d(TAG, "on5gStatus: slotId= " + slotId + " token=" + token + " status=" +
                        status + " enableStatus=" + enableStatus);
            }
        }

        @Override
        public void onNrDcParam(int slotId, Token token, Status status, DcParam dcParam) throws
                RemoteException {

            if ( DEBUG ) {
                Log.d(TAG, "onNrDcParam: slotId=" + slotId + " token=" + token + " status=" +
                        status + " dcParam=" + dcParam);
            }

            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mDcnr = dcParam.getDcnr();
                update5GIcon(state, slotId);
                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void onSignalStrength(int slotId, Token token, Status status,
                                     org.codeaurora.internal.SignalStrength
                                             signalStrength) throws RemoteException {
            if ( DEBUG ) {
                Log.d(TAG, "onSignalStrength: slotId=" + slotId + " token=" + token
                        + " status=" + status + " signalStrength=" + signalStrength);
            }

            if (status.get() == Status.SUCCESS && signalStrength != null) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mLevel = getRsrpLevel(signalStrength.getRsrp());
                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void onAnyNrBearerAllocation(int slotId, Token token, Status status,
                                            BearerAllocationStatus bearerStatus) throws RemoteException {
            if ( DEBUG ) {
                Log.d(TAG, "onAnyNrBearerAllocation bearerStatus=" + bearerStatus.get());
            }

            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mBearerAllocationStatus = bearerStatus.get();
                update5GIcon(state, slotId);
                notifyListenersIfNecessary(slotId);
            }

        }

        @Override
        public void onUpperLayerIndInfo(int slotId, Token token, Status status,
                                        UpperLayerIndInfo uilInfo) throws RemoteException {
            if ( DEBUG ) {
                Log.d(TAG, "onUpperLayerIndInfo plmn=" + uilInfo.getPlmnInfoListAvailable()
                        + " upperLayerIndInfo=" + uilInfo.getUpperLayerIndInfoAvailable());
            }

            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mPlmn = uilInfo.getPlmnInfoListAvailable();
                state.mUpperLayerInd = uilInfo.getUpperLayerIndInfoAvailable();
                update5GIcon(state, slotId);
                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void on5gConfigInfo(int slotId, Token token, Status status, NrConfigType
                nrConfigType) throws RemoteException {
            Log.d(TAG,
                    "on5gConfigInfo: slotId = " + slotId + " token = " + token + " " + "status"
                            + status + " NrConfigType = " + nrConfigType);
            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mNrConfigType = nrConfigType.get();
                update5GIcon(state, slotId);
                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void onNrIconType(int slotId, Token token, Status status, NrIconType
                nrIconType) throws RemoteException {
            Log.d(TAG,
                    "onNrIconType: slotId = " + slotId + " token = " + token + " " + "status"
                            + status + " NrIconType = " + nrIconType);
            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mNrIconType = nrIconType.get();
                update5GIcon(state, slotId);
                notifyListenersIfNecessary(slotId);
            }
        }
    };

    public interface IFiveGStateListener {
        public void onStateChanged(FiveGServiceState state);
    }
}
