/*
 * Copyright (c) 2017, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.bluetooth;

import android.Manifest;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import com.android.internal.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This class provides the public APIs to control the Bluetooth Broadcast Audio Transmitter
 * profile.
 *
 *<p>BluetoothBroadcastAudio is a proxy object for controlling the Bluetooth Broadcast Audio
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothA2dp proxy object.
 *
 * @hide
 */
public final class BluetoothBATransmitter implements BluetoothProfile {
    private static final String TAG = "BluetoothBAT";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static final int SEND_DELAYED_MSG_TO_RESET = 1;
    private static final int BIND_TIMEOUT_MS = 3000; //3sec

    /**
     * Intent used to update state change of Broadcast Audio Transmitter.
     *
     * This intent will have 2 extras:
     * #EXTRA_STATE - The current state of the profile.
     * #EXTRA_PREVIOUS_STATE - The previous state of the profile.
     *
     * value of states can be any of
     * STATE_DISABLED: Broadcast Audio is disabled.
     * STATE_PAUSED:   Broadcast Audio is enabled but streaming is paused.
     * STATE_PLAYING:  Broadcast Audio is enabled but streaming is ongoing.
     *
     */
    public static final String ACTION_BAT_STATE_CHANGED =
        "android.bluetooth.bat.profile.action.BA_STATE_CHANGED";
    public static final String EXTRA_STATE =
            "android.bluetooth.bat.profile.extra.STATE";

    /**
     * Intent used to update encryption key .
     *
     * This intent will have 1 extra:
     * #EXTRA_ENCRYPTION_KEY - The current value of encryption key.
     *
     * value of EncyptionKey would be 128-bit value. This value would change
     * on every new BA session. We will send BluetoothBAEncryptionKey object.
     *
     */
    public static final String ACTION_BAT_ENCRYPTION_KEY_CHANGED =
            "android.bluetooth.bat.profile.action.BA_ENC_KEY_CHANGED";
    public static final String EXTRA_ECNRYPTION_KEY =
            "android.bluetooth.bat.profile.extra.ENC_KEY";

    /**
     * Intent used to update DIV value .
     *
     * This intent will have 1 extras:
     * #EXTRA_DIV_VALUE - The current value of DIV.
     *
     * value of DIV would be 2 byte value. This value would change
     * on every new BA session. We will send integer value.
     *
     */
    public static final String ACTION_BAT_DIV_CHANGED =
            "android.bluetooth.bat.profile.action.BA_DIV_CHANGED";
    public static final String EXTRA_DIV_VALUE =
            "android.bluetooth.bat.profile.extra.DIV";

    /**
     * Intent used to update  active stream id for Broadcast Audio Transmitter.
     *
     * This intent will have 1 extra:
     * #EXTRA_STREAM_ID - The active streaming id.
     *
     * value of states can be any of
     * 0: Broadcast Audio is not in STATE_PLAYING.
     * valid streaming id:   Valid streaming id if BA is in STATE_PLAYING.
     */
    public static final String ACTION_BAT_STREAMING_ID_CHANGED =
            "android.bluetooth.bat.profile.action.BA_STR_ID_CHANGED";
    public static final String EXTRA_STREAM_ID =
            "android.bluetooth.bat.profile.extra.STR_ID";

    /**
     * Intent used to update  Vendor Specific AVRCP Command.
     *
     * This intent will be sent whenever there is Vendor Specific AVRCP command
     * received from primary headset.
     *
     * This intent will have 2 extra:
     * #EXTRA_AVRCP_VS_ENABLE_BA - value describing enable/disable of BA.
     * #EXTRA_AVRCP_VS_ENABLE_RA - value describing enable/disable of receiver
     *                              association mode.
     *
     * value of states can be any of
     * 0: Disable  BA/RA.
     * 1: ENABLE  BA/RA.
     */
    public static final String ACTION_BAT_AVRCP_VS_CMD =
            "android.bluetooth.bat.profile.action.BA_AVRCP_VS_CMD";
    public static final String EXTRA_AVRCP_VS_ENABLE_BA =
            "android.bluetooth.bat.profile.extra.ENABLE_BA";
    public static final String EXTRA_AVRCP_VS_ENABLE_RA =
            "android.bluetooth.bat.profile.extra.ENABLE_RA";


    public static final int STATE_DISABLED   =  0;
    public static final int STATE_PAUSED     =  1;
    public static final int STATE_PLAYING    =  2;

    public static final int ENABLE_BA_TRANSMITTER    =  0;
    public static final int DISABLE_BA_TRANSMITTER   =  1;

    public static final int INVALID_DIV = 0xFFFF;

    private Context mContext;
    private ServiceListener mServiceListener;
    private boolean mBinding;
    private boolean mUnbinding;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();
    @GuardedBy("mServiceLock") private IBluetoothBATransmitter mService;
    private BluetoothAdapter mAdapter;

    final private IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
                public void onBluetoothStateChange(boolean up) {
                    if (DBG) Log.d(TAG, "onBluetoothStateChange: up=" + up);
                    if (!up) {
                        if (DBG) Log.d(TAG, "Unbinding service...");
                        try {
                            mServiceLock.writeLock().lock();
                            if (mService != null) {
                                if (DBG) Log.d(TAG, "mService is not null, So unbind BATService.");
                                mService = null;
                                mContext.unbindService(mConnection);
                            }
                        } catch (Exception re) {
                            Log.e(TAG, "", re);
                        } finally {
                            mServiceLock.writeLock().unlock();
                        }
                    } else {
                        try {
                            mServiceLock.readLock().lock();
                            if (mService == null) {
                                if (DBG) Log.d(TAG,"Binding service...");
                                doBind();
                            }
                        } catch (Exception re) {
                            Log.e(TAG,"",re);
                        } finally {
                            mServiceLock.readLock().unlock();
                        }
                    }
                }
        };
    /**
     * Create a BluetoothBAT proxy object for interacting with the local
     * Bluetooth Broadcast Audio Transmitter service.
     *
     */
    /*package*/ BluetoothBATransmitter(Context context, ServiceListener l) {
        mContext = context;
        mServiceListener = l;
        mBinding = false;
        mUnbinding = false;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                if (DBG) Log.d(TAG, "BAT constructor: pid: " + android.os.Process.myPid() +
                    " Caller API: " + Thread.currentThread().getStackTrace()[1].getMethodName() +
                    " Caller API: " + Thread.currentThread().getStackTrace()[2].getMethodName());
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG,"",e);
            }
        }

        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothBATransmitter.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || mBinding) {
            Log.e(TAG, "comp = " + comp + "mBinding ="+ mBinding + "Bluetooth Broadcast Audio Transmitter Service " + intent);
            return false;
        }
        else if(!mContext.bindServiceAsUser(intent, mConnection, 0,
                android.os.Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth Broadcast Audio Transmitter Service " + intent);
            return false;
        } else {
            mBinding = true;
            mBindingReset();
            return true;
        }
    }

    /*package*/ void close() {
        mServiceListener = null;
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                if (DBG) Log.d(TAG, "close(): pid: " + android.os.Process.myPid() +
                   " Caller API: " + Thread.currentThread().getStackTrace()[1].getMethodName() +
                   " Caller API: " + Thread.currentThread().getStackTrace()[2].getMethodName());
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG,"",e);
            }
        }

        try {
            mServiceLock.writeLock().lock();
            if (mUnbinding) {
                return;
            }
            mUnbinding = true;
            if (mService != null) {
                mService = null;
                mContext.unbindService(mConnection);
                mUnbinding = false;
            } else {
                mUnbinding = false;
            }
        } catch (Exception re) {
            Log.e(TAG, "", re);
        } finally {
            mServiceLock.writeLock().unlock();
        }
    }

    public void finalize() {
        // The empty finalize needs to be kept or the
        // cts signature tests would fail.
    }

    /**
     * Enable/Diasble BroadcastAudio Transmitter session.
     *
     * This API returns false  if Broadcast Audio Transmitter is in
     * same state as requested or Bluetooth is not turned on.
     * When this API returns true, it is guaranteed that
     * state change intent would be sent with the current state.
     * Users can get the current state of BA_Transmitter
     * from this intent.
     *
     * @param state: ENABLE_BA_TRANSMITTER/DISABLE_BA_TRANSMITTER
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean setBATState(int state) {
        if (DBG) log("setBATState(" + state + ")");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.setBATState(state);
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * get the current state of Broadcast Audio Transmitter.
     *
     * This API returns current state of BA Transmitter, which
     * is one of the values mentioned above.
     * STATE_DISABLED, STATE_PAUSED, STATE_PLAYING
     * If BT is turned off or BATService is not launched, we would
     * always return STATE_DISABLED
     *
     * @return current state of BA, if BT is on and BA Service launched
     *               STATE_DISABLED otherwise
     * @hide
     */
    public int getBATState() {
        if (DBG) log("getBATState");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.getBATState();
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
                return STATE_DISABLED;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return STATE_DISABLED;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * get the current of Dynamic Interrupt Vector for this BA SESSION.
     *
     * This API returns current value of DIV for ongoing BA session.
     * Only last 2 bytes are significant.
     * If BA is in STATE_DISABLED, or Bluetooth is not turned on
     * this api would return INVALID_DIV as defined above.
     *
     * @return current value of DIV, if BT is on and BA is not in disabled state.
     *               INVALID_DIV otherwise
     * @hide
     */
    public int getDIV() {
        if (DBG) log("getDIV");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.getDIV();
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
            return INVALID_DIV;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return INVALID_DIV;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * get the current stream ID.
     *
     * This API returns current value of stream id if BA is in STATE_PLAYING.
     * Otherwise it will always return 0
     *
     * @return current value of stream id, if BT is on and BA is in STATE_PLAYING.
     *               0 otherwise
     * @hide
     */
    public long getStreamId() {
        if (DBG) log("getStreamId");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.getStreamId();
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return 0;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * get the current encryption key.
     *
     * This API returns current value of encryption key if BA is not in in STATE_DISABLED.
     * Otherwise it will always return null
     *
     * @return current value of encryption key(BluetoothBAEncryptionKey), if BT is on and
     * BA is not in STATE_DISABLED, null otherwise
     *
     * @hide
     */
    public BluetoothBAEncryptionKey getEncryptionKey() {
        if (DBG) log("getEncryptionKey");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.getEncryptionKey();
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * Refreshes current Encryption Key.
     *
     * This API  refreshes encryption key BA is not in in STATE_DISABLED, or BT is enabled
     * if request is accepted, a guranteed intent would be posted with new value of Encryption key
     *
     * @return immediate value of request , true if BT is on and
     * BA is not in STATE_DISABLED, false otherwise
     *
     * @hide
     */
    public boolean refreshEncryptionKey() {
        if (DBG) log("getEncryptionKey");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.refreshEncryptionKey();
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * get supported service records.
     *
     * This API returns supported service record, if BT is on and BAService is running.
     * Otherwise it will always return null
     *
     * @return supported service record, if BT is on and BAService is running.
     *               null otherwise
     * @hide
     */
    public BluetoothBAStreamServiceRecord getBAServiceRecord() {
        if (DBG) log("getBAServiceRecord");
        try {
            mServiceLock.readLock().lock();
            if (mService != null && isEnabled()) {
                return mService.getBAServiceRecord();
            }
            if (mService == null) Log.w(TAG, "Proxy not attached to service");
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        } finally {
            mServiceLock.readLock().unlock();
        }
    }

    /**
     * this is dummpy implementation, because we have to inherit an interface BluetoothProfile
     * This will always return empty list.
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) log("getConnectedDevices() dummy impl");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * this is dummpy implementation, because we have to inherit an interface BluetoothProfile
     * This will always return empty list.
     */
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (DBG) log("getDevicesMatchingStates() dummy impl");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * this is dummpy implementation, because we have to inherit an interface BluetoothProfile
     * This will always return STATE_DISCONNECTED as there is no connection in BA.
     */
    public int getConnectionState(BluetoothDevice device) {
        if (DBG) log("getConnectionState() dummy impl");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) Log.d(TAG, "Proxy object connected");
            try {
                mServiceLock.writeLock().lock();
                mBinding = false;
                mService = IBluetoothBATransmitter.Stub.asInterface(Binder.allowBlocking(service));
            } finally {
                mServiceLock.writeLock().unlock();
            }

            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.BA_TRANSMITTER,
                        BluetoothBATransmitter.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (DBG) Log.d(TAG, "Proxy object disconnected");
            try {
                mServiceLock.writeLock().lock();
                mUnbinding = false;
                mBinding = false;
                mService = null;
            } finally {
                mServiceLock.writeLock().unlock();
            }
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected(BluetoothProfile.BA_TRANSMITTER);
            }
        }
    };

    private boolean isEnabled() {
       if (mAdapter.getState() == BluetoothAdapter.STATE_ON) return true;
       return false;
    }

    private static void log(String msg) {
      Log.d(TAG, msg);
    }

    private void mBindingReset() {
       Message message = mHandler.obtainMessage(SEND_DELAYED_MSG_TO_RESET);
       mHandler.sendMessageDelayed(message, BIND_TIMEOUT_MS);
    }

    private final Handler mHandler = new Handler() {
       @Override
       public void handleMessage(Message msg) {
           if (DBG) Log.d(TAG, " Handle the message: " + msg.what);
           switch (msg.what) {
               case SEND_DELAYED_MSG_TO_RESET:
                   if (DBG) Log.d(TAG, "Reset mBinding flag");
                   if (mBinding) mBinding = false;
                   break;
               default:
                   break;
           }
       }
    };
}
