package edu.txstate.reu.ble;

import android.content.Context;

/**
 *  Represents a Bluetooth Low Energy utility API that allows an android device to act as either a
 *  client or server for sending float data to it's connected devices via BLE. This class acts as a
 *  gateway to the entire API by requiring the user to retrieve an instance of either of the roles
 *  in order to gain access and utilize all API functionalities.
 *
 *  @author Colin Campbell (c_c953)
 *  @version 1.0
 *  @since 2021.12.13
 */
public class BluetoothLe {

    /**
     *  A BleServer object used to access the BleServer API
     */
    private static BleServer bleServer;

    /**
     *  A BleClient object used to access the BleClient API
     */
    private static BleClient bleClient;

    /**
     *  BluetoothLe default constructor
     */
    private BluetoothLe () {}

    /**
     *  A method for retrieving an BleServer instance
     *
     *  @param context Context: App context
     *  @return BleServer: A reference to the current BleServer instance
     */
    public static BleServer getBleServer(Context context) {
        if (bleServer == null) {
            bleServer = new BleServer(context);
        }
        return bleServer;
    }

    /**
     *  A method for retrieving an BleClient instance
     *
     *  @param context Context: App context
     *  @return BleClient: A reference to the current BleClient instance
     */
    public static BleClient getBleClient(Context context) {
        if (bleClient == null) {
            bleClient = new BleClient(context);
        }
        return bleClient;
    }

}