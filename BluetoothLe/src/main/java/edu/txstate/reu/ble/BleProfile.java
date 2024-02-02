package edu.txstate.reu.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *  Represents a Bluetooth Low Energy profile which is used to for the BLE service. This class
 *  maintains all UUIDs used for the service including the service itself, each characteristic and
 *  the means of creating them.
 *
 *  @author Colin Campbell (c_c953)
 *  @version 1.0
 *  @since 2021.12.13
 */
public class BleProfile {

    /**
     *  A string TAG for debugging
     */
    private static final String TAG = "BleProfile";

    /**
     *  A constant UUID for the service created from an arbitrary String value
     *  (This could be anything)
     */
    public final static UUID SERVER_SERVICE = UUID.fromString("0000fe81-0000-1000-8000-00805f9b34fb");

    /**
     *  A HashMap mapping a String value to a corresponding UUID
     */
    private static Map <String,UUID> UUIDs = new HashMap<>();

    /**
     *  A HashMap mapping a UUID to a corresponding String value
     */
    private static Map <UUID,String> paths = new HashMap<>();

    /**
     *  A method for determining whether a String path has already been assigned a corresponding
     *  UUID
     *
     *  @param path String: A value representing a UUID used within the profile for sending data
     *  @return boolean: True if a UUID exists for the provided path, otherwise false.
     */
    public static boolean hasPath(String path) { return UUIDs.containsKey(path);}

    /**
     *  A method for retrieving a UUID's String path
     *
     *  @param uuid String: A value representing a UUID used within the profile for sending data
     *  @return String: The corresponding value belonging to the provided UUID
     */
    public static String getPathFromUUID(UUID uuid) { return paths.get(uuid);}

    /**
     *  A method for assigning a String path a corresponding UUID
     *
     *  @param path String: A value representing a UUID used within the profile for sending data
     */
    public static void addPath(String path) {
        UUID uuid = UUID.nameUUIDFromBytes(path.getBytes());
        UUIDs.put(path,uuid);
        paths.put(uuid,path);
    }

    /**
     *  A method for creating a BLE service. Requires Android SDK that supports Bluetooth.
     *
     *  @return BluetoothGattService: A reference to a newly created Bluetooth GATT service
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static BluetoothGattService createService() {
        BluetoothGattService service = new BluetoothGattService(SERVER_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        return service;
    }

    /**
     *  A method for creating a Bluetooth Low Energy characteristic using a provided path.
     *  The resulting characteristic created will allow read/write/notify permissions.
     *
     *  @param path String: A value representing a UUID used within the profile for sending data
     *  @return BluetoothGattCharacteristic: A reference to a newly created Bluetooth GATT
     *  characteristic
     */
    public static BluetoothGattCharacteristic createCharacteristic (String path) {
        UUID uuid = UUIDs.get(path);

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid,
                BluetoothGattCharacteristic.PROPERTY_READ
                        | BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
                        | BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(uuid,
                BluetoothGattDescriptor.PERMISSION_READ
                        | BluetoothGattDescriptor.PERMISSION_WRITE);
        characteristic.addDescriptor(descriptor);
        return characteristic;
    }
}