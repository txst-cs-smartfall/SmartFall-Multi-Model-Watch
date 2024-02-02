package edu.txstate.reu.ble;

import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *  Represents a Bluetooth Low Energy server built using Android's Bluetooth API. Some documentation
 *  borrowed from the official Android Developers Docs which can be found here:
 *  https://developer.android.com/reference/android/bluetooth/package-summary
 *
 *  @author Colin Campbell (c_c953)
 *  @version 1.0
 *  @since 2021.12.13
 */
public class BleServer {

    /**
     *  A string TAG for debugging
     */
    private final String TAG = "BleServer";

    /**
     *  A reference to the application context
     */
    private Context mContext;

    /**
     *  High level manager used to obtain an instance of an BluetoothAdapter and to conduct overall
     *  Bluetooth Management
     */
    private BluetoothManager mBluetoothManager;

    /**
     *  Represents the local device Bluetooth adapter
     */
    private BluetoothAdapter bleAdapter;

    /**
     *  Public API for the Bluetooth GATT Profile server role.
     */
    private BluetoothGattServer mBluetoothGattServer;

    /**
     *  Allows Bluetooth LE advertise operations, such as starting and stopping advertising.
     */
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    /**
     *  Represents a Bluetooth GATT Service. Gatt Service contains a collection of
     *  BluetoothGattCharacteristic, as well as referenced services.
     */
    private BluetoothGattService mBluetoothService;

    /**
     *  A reference to an implementation of the OnEventReceivedListener interface
     */
    private OnEventReceivedListener listener = null;

    /**
     * Collection of notification subscribers
     */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    public static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;

    public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;

    /**
     *  A constructor which uses it's received parameters to initialize the BleServer class.
     *  This is done by using the app context to get the device's bluetooth manager and adapter,
     *  enable the adapter and to start the ble service advertisement and server service.
     *
     *  @param context Context: App context
     */
    public BleServer(Context context) {

        mContext = context;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = mBluetoothManager.getAdapter();

        if (!checkBluetoothSupport(bleAdapter)) {
        }

        if (!bleAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bleAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();

            mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
            if (mBluetoothGattServer == null) {
                Log.w(TAG, "Unable to create GATT server");
                return;
            }
            mBluetoothService = BleProfile.createService();
        }
    }

    /**
     *  A method for checking if a device supports bluetooth capabilities.
     *
     *  @param bluetoothAdapter BluetoothAdapter: Represents the local device Bluetooth adapter.
     *  @return boolean: False if the device does not support bluetooth or bluetooth low energy.
     *  True if both are supported by the device.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    /**
     *  An implementation of Bluetooth LE advertising callbacks, used to deliver advertising
     *  operation status.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: " + errorCode);
        }
    };

    /**
     *  A method to start advertise the BleProfile Server Service using Bluetooth LE.
     */
    private void startAdvertising() {
        mBluetoothLeAdvertiser = bleAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(BleProfile.SERVER_SERVICE))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    /**
     *  A method for stopping the Bluetooth LE advertisement
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }


    /**
     *  A method for starting the BleServer and enabling data to be sent to
     *  BleClients using the provide paths.
     *
     *  Upon registering the provided paths, this method proceeds to iterate through
     *  a list of paired devices and attempts to connect to a paired GATT using a low energy
     *  connection (transport = 2).
     *
     *  @param paths String: The list of paths where data will be sent and received
     */
    public void startBleServer(String [] paths) {
        for (String path: paths) {
            if (!BleProfile.hasPath(path)) {
                BleProfile.addPath(path);
                mBluetoothService.addCharacteristic(BleProfile.createCharacteristic(path));
            }
        }
        mBluetoothGattServer.addService(mBluetoothService);
    }

    /**
     *  A method for stopping the BleServer and closing the GATT
     */
    public void stopBleServer() {
        if (bleAdapter.isEnabled()) {
            if (mBluetoothGattServer != null)
                for (BluetoothDevice device : mRegisteredDevices)
                    mBluetoothGattServer.cancelConnection(device);
                mBluetoothGattServer.close();
            //stopAdvertising();
        }
    }

    /**
     *  An implementation of various BluetoothGatt Server Callbacks
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        /**
         *  Callback indicating when a remote device has been connected or disconnected.
         *
         *  @param device BluetoothDevice: Remote device that has been connected or disconnected.
         *  @param status int: Status of the connect or disconnect operation.
         *  @param newState int: Returns the new connection state.
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

            dispatchConnectionState(newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                    //Remove device from any active subscriptions
                    mRegisteredDevices.remove(device);
                    stopAdvertising();
                    startAdvertising();
                    break;
            }
        }

        /**
         *  A remote client has requested to read a local characteristic.
         *
         *  @param device BluetoothDevice: The remote device that has requested the read operation
         *  @param requestId int: The Id of the request
         *  @param offset int: Offset into the value of the characteristic
         *  @param characteristic BluetoothGattCharacteristic: Characteristic to be read
         */
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
        }

        /**
         *  A remote client has requested to write to a local characteristic.
         *
         * @param device BluetoothDevice: The remote device that has requested the write operation
         * @param requestId int: The Id of the request
         * @param characteristic BluetoothGattCharacteristic: Characteristic to be written to.
         * @param preparedWrite boolean: true, if this write operation should be queued for later
         *                      execution.
         * @param responseNeeded boolean: true, if the remote device requires a response
         * @param offset int: The offset given for the value
         * @param value byte: The value the client wants to assign to the characteristic
         */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Event event = new Event(value, new Timestamp(System.currentTimeMillis()), characteristic.getUuid());
            dispatchEvent(event);
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
        }

        /**
         *  A remote client has requested to read a local descriptor.
         *
         *  @param device BluetoothDevice: The remote device that has requested the read operation
         *  @param requestId int: The Id of the request
         *  @param offset int: Offset into the value of the characteristic
         *  @param descriptor BluetoothGattDescriptor: Descriptor to be read
         */
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "onDescriptorReadRequest: request");
            byte[] returnValue;
            if (mRegisteredDevices.contains(device)) {
                returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            } else {
                mRegisteredDevices.add(device);
                returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }
            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    returnValue);
        }

        /**
         *  A remote client has requested to write to a local descriptor.
         *
         * @param device BluetoothDevice: The remote device that has requested the write operation
         * @param requestId int: The Id of the request
         * @param descriptor BluetoothGattDescriptor: Descriptor to be written to.
         * @param preparedWrite boolean: true, if this write operation should be queued for
         * later execution.
         * @param responseNeeded boolean: true, if the remote device requires a response
         * @param offset int: The offset given for the value
         * @param value byte: The value the client wants to assign to the descriptor
         */
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                Log.d(TAG, "Subscribe device to notifications: " + device);
                mRegisteredDevices.add(device);
            } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                Log.d(TAG, "Unsubscribe device from notifications: " + device);
                mRegisteredDevices.remove(device);
            }

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null);
            }
        }
    };

    /**
     *  A method for sending float data to a connected BleClient using ble.
     *
     *  Upon converting the float data to bytes and writing it to the characteristic associated
     *  with the provided path destination. All registered clients are notified that there's
     *  new data available to be read.
     *
     *  @param path String: The path where the server wants to send the values.
     *  @param values float: The values the server wants to send to registered devices
     */
    public void send(String path, float[] values) {

        try {

            if (!BleProfile.hasPath(path)) {
                Log.e(TAG, "path is not registered");
            }
            ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

            for (float value : values){
                buffer.putFloat(value);
            }

            byte[] bytes =  buffer.array();

            BluetoothGattCharacteristic characteristic = mBluetoothGattServer.getService(BleProfile.SERVER_SERVICE).getCharacteristic(UUID.nameUUIDFromBytes(path.getBytes()));
            characteristic.setValue(bytes);

            for (BluetoothDevice device : mRegisteredDevices) {
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            }

        } catch (Exception e) {

        }
    }

    /**
     *  A public interface for implementing a listener for BLE event notifications
     */
    public interface OnEventReceivedListener {
        void onEventReceived(Event event);
        void onConnectionStateChange(int state);
    }

    /**
     *  A method for notifying the registered listener to the arrival of a new ble event.
     *
     *  @param event Event: The new ble event that has occurred.
     */
    public void dispatchEvent(Event event) {
        if (this.listener != null) {
            this.listener.onEventReceived(event);
        }
    }

    /**
     * A method for notifying the registered listener to a change in the ble connection state.
     *
     * @param state
     */
    public void dispatchConnectionState(int state) {
        if(this.listener != null) {
            this.listener.onConnectionStateChange(state);
        }
    }

    /**
     *  A method for registering an implementation of the OnEventReceivedListener interface to
     *  the BleServer's listener attribute.
     *
     *  @param listener BleServer.OnEventReceivedListener: an implementation of the
     *  OnEventReceivedListener interface
     */
    public void addEventListener(BleServer.OnEventReceivedListener listener) {
        this.listener = listener;
    }

    /**
     *  A method for unregistering an implementation of the OnEventReceivedListener interface
     *  from the the BleServer's listener attribute.
     */
    public void removeAllListeners() {
        this.listener = null;
    }
}