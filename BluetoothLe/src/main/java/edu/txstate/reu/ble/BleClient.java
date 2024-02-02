package edu.txstate.reu.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 *  Represents a Bluetooth Low Energy client device for sending and receiving float [] data
 *  from a connected Bluetooth Low Energy server device. Some documentation borrowed from the
 *  official Android Developers Docs which can be found here:
 *  https://developer.android.com/reference/android/bluetooth/package-summary
 *
 *  @author Colin Campbell (c_c953)
 *  @version 1.0
 *  @since 2021.12.13
 */
public class BleClient {

    /**
     *  A string TAG for Debugging
     **/
    private final String TAG = "BleClient";

    /**
     *  A reference to the application context
     */
    private Context mContext;

    /**
     *  Represents the local device Bluetooth adapter
     */
    private BluetoothAdapter bleAdapter;

    /**
     *  Public API for the Bluetooth GATT Profile
     */
    private BluetoothGatt bluetoothGatt;

    /**
     *  A reference to an implementation of the OnEventReceivedListener interface
     */
    private OnEventReceivedListener listener = null;

    /**
     *  A boolean flag representing the connection state between the client and the GATT server.
     */
    private boolean connected = false;

    /**
     * A constant integer representing that connection state is connecting
     */
    public static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;

    /**
     * A constant integer representing that connection state is connected
     */
    public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    /**
     * A constant integer representing that connection state is disconnecting
     */
    public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    /**
     * A constant integer representing that connection state is disconnected
     */
    public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;

    /**
     *  An implementation of various BluetoothGatt Callbacks
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {



        /**
         *  Callback indicating when GATT client has connected/disconnected to/from a remote GATT server.
         *
         *  When the client is connected to the server, the connected flag is set to true and the
         *  the gatt begins discovery of any services offered by the GATT server. When the client
         *  is disconnected from the server, the connected flag is set to false and the client
         *  gatt is closed.
         *
         *  @param gatt BluetoothGatt: GATT client
         *  @param status int: Status of the connect or disconnect operation.
         *  @param newState int: Returns the new connection state.
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            dispatchConnectionState(newState);


            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "onConnectionStateChange: Connecting to GATT server.");
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Connected to GATT server.");
                    connected = true;
                    bluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "onConnectionStateChange: Disconnecting from GATT server");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Disconnected from GATT server.");
                    connected = false;
                    gatt.close();
                    break;
            }
        }


        /**
         *  Callback invoked when the list of remote services, characteristics and descriptors for
         *  the remote device have been updated, ie new services have been discovered.
         *
         *  Upon receiving a list of GATT services from the connected device the list is iterated
         *  until the BleProfile Server Service is found. Once found all available characteristics
         *  are discovered, read and notification enabled.
         *
         *  @param gatt BluetoothGatt: GATT client invoked
         *  @param status int: Status of the discover operation.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();

            for(BluetoothGattService service:services) {
                if(service.getUuid().equals(BleProfile.SERVER_SERVICE)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic: characteristics) {
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            gatt.readDescriptor(descriptor);
                        }
                        gatt.setCharacteristicNotification(characteristic,true);
                        gatt.readCharacteristic(characteristic);
                    }
                }
            }
        }

        /**
         *  Callback triggered as a result of a remote characteristic notification.
         *
         *  Upon characteristic change an event is created and dispatched using the newly changed
         *  value of the characteristic.
         *
         *  @param gatt BluetoothGatt: GATT client the characteristic is associated with
         *  @param characteristic BluetoothGattCharacteristic: Characteristic that has been
         *  updated as a result of a remote notification event.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Event event = new Event(characteristic.getValue(), new Timestamp(System.currentTimeMillis()), characteristic.getUuid());
            dispatchEvent(event);
        }
    };

    /**
     *  A constructor which uses it's received parameters to initialize the BleClient class.
     *  This is done by using the app context to get the device's bluetooth adapter.
     *
     *  @param context Context: App context
     */
    public BleClient(Context context) {
        mContext = context;

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        }
        else
            Log.e(TAG, "NO BLE SUPPORT" );
    }

    /**
     *  A method for sending float data to a connected BleServer using ble.
     *
     *  This method converts the float data to bytes and writes it to the characteristic
     *  associated with the provided path destination.
     *
     *  @param path String: The path where the client wants to send the values.
     *  @param values float: The values the client wants to send to registered devices
     */
    public void send(String path, float[] values) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);

            for (float value : values){
                buffer.putFloat(value);
            }

            byte[] bytes =  buffer.array();
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(BleProfile.SERVER_SERVICE).getCharacteristic(UUID.nameUUIDFromBytes(path.getBytes()));
            characteristic.setValue(bytes);
            bluetoothGatt.writeCharacteristic(characteristic);
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     *  A method for sending float data to a connected BleServer using ble.
     *
     *  This method converts the float data to bytes and writes it to the characteristic
     *  associated with the provided path destination.
     *
     *  @param path String: The path where the client wants to send the values.
     *  @param values float: The values the client wants to send to registered devices
     */
    public void send(String path, String values) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4 * values.length());

            buffer.put(values.getBytes());

            byte[] bytes =  buffer.array();
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(BleProfile.SERVER_SERVICE).getCharacteristic(UUID.nameUUIDFromBytes(path.getBytes()));
            characteristic.setValue(bytes);
            bluetoothGatt.writeCharacteristic(characteristic);
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     *  A method for starting the BleClient and connecting to a BleServer.
     *
     *  Upon registering the provided paths, this method proceeds to iterate through
     *  a list of paired devices and attempts to connect to a paired GATT using a low energy
     *  connection (transport = 2).
     *
     *  @param paths String: The list of paths where data will be sent and received
     */
    public void startBleClient(String [] paths) {
        for (String path: paths) {
            if (!BleProfile.hasPath(path)) {
                BleProfile.addPath(path);
            }
        }

        Set<BluetoothDevice> pairedDevices = bleAdapter.getBondedDevices();

        for(BluetoothDevice device: pairedDevices) {
            bluetoothGatt = device.connectGatt(mContext ,false, bluetoothGattCallback,2);
        }
    }

    public int checkConnectionState(){
        BluetoothManager bluetoothManager = (BluetoothManager)  mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if(devices.size()==0) return 0;
        else return 1;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                Log.e(TAG, String.valueOf(bool));
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
        }
        return false;
    }


    /**
     *  A method for stopping the BleClient and disconnecting from a BleServer.
     */
    public void stopBleClient() {
        bluetoothGatt.disconnect();
    }

    /**
     *  A public interface for implementing a listener for BLE event notifications and BLE
     *  connection states
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
        if(this.listener != null) {
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
     *  the BleClient's listener attribute.
     *
     *  @param listener BleClient.OnEventReceivedListener: An implementation of the
     *  OnEventReceivedListener interface
     */
    public void addEventListener(BleClient.OnEventReceivedListener listener) {
        this.listener = listener;
    }

    /**
     *  A method for unregistering an implementation of the OnEventReceivedListener interface
     *  from the the BleClient's listener attribute.
     */
    public void removeAllListeners() {
        this.listener = null;
    }

}
