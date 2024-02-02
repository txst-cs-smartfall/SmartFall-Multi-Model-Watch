package edu.txstate.reu.smartfall_multi_model_native.DataCollection;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import edu.txstate.reu.ble.BleClient;
import edu.txstate.reu.ble.BluetoothLe;
import edu.txstate.reu.ble.Event;
import edu.txstate.reu.smartfall_multi_model_native.Database.Database;
import edu.txstate.reu.smartfall_multi_model_native.Prediction.Prediction;

/**
 * This class utilizes the BluetoothLe module to collect and route
 * ble events to the Prediction and Database classes.
 *
 * @author Colin Campbell (c_c953)
 * @version 1.0
 * @since 2021.12.13
 * **/
public class DataCollection {

    /**
     * A string TAG for Debugging
     **/
    private static final String TAG = "DataCollection";

    /**
     * A reference to the application context
     */
    private static Context context;

    /**
     * A constant integer representing that the received feedback label is a false positive label
     */
    private static final int FEEDBACK_LABEL_FP = 0;

    /**
     * A constant integer representing that the received feedback is a true positive label
     */
    private static final int FEEDBACK_LABEL_TP = 1;

    /**
     * A constant integer representing that the received feedback is a false negative label
     */
    private static final int FEEDBACK_LABEL_FN = 3;

    /**
     * A constant integer representing that the received feedback is a request for help
     */
    private static final int FEEDBACK_NEEDS_HELP = 2;

    /**
     * A constant integer representing that the state of the data collection is actively collecting
     */
    public static final int COLLECTION_STATE = 0;

    /**
     * A constant integer representing that the state of the data collection is awaiting feedback
     */
    public static final int FEEDBACK_STATE = 1;

    /**
     * A constant integer representing that the state of the data collection is inactive
     */
    public static final int IDLE_STATE = 2;

    /**
     * A constant string representing the designated path for receiving accelerometer data
     */
    private static final String ACCELEROMETER_EVENT_PATH = "accelerometer";

    /**
     * A constant string representing the designated path for receiving state data
     */
    private static final String STATE_EVENT_PATH = "state";

    /**
     * A constant string representing the designated path for receiving feedback data
     */
    private static final String FEEDBACK_EVENT_PATH = "feedback";

    /**
     * A reference to the current state of the data collection
     */
    private static int state;

    /**
     * A list of all declared paths used for sending and receiving ble data
     */
    public static String [] blePaths = new String []
            { ACCELEROMETER_EVENT_PATH, STATE_EVENT_PATH, FEEDBACK_EVENT_PATH };

    /**
     * This method starts the BluetoothLe client and registers the declared paths that will be used
     * by the application
     *
     * @param context Context: App context
     */
    public static void initialize (Context context) {
        BluetoothLe.getBleClient(context).startBleClient(blePaths);
        context = context;
    }

    /**
     * This method starts the data collection by instantiating and registering an
     * OnEventReceivedListener to the BluetoothLe client.
     *
     * @param context1 Context: App context
     */
    public static void start(Context context1) {
        context = context1;
        BluetoothLe.getBleClient(context1).addEventListener(new BleServerListener());
    }

    /**
     * This method stops the the data collection by removing the OnEventReceivedListener from
     * the BluetoothLe client.
     *
     * @param context Context: App context
     */
    public static void stop (Context context) {
        context = context;
        BluetoothLe.getBleClient(context).removeAllListeners();
    }

    /**
     * This method sends an array of float values less than 20 in length
     * to the connected BLE Server through the provided path.
     *
     * @param path String: Provided path to send the values to.
     * @param values float: A float array of values (less than 20 in length) to send.
     * */
    public static void send(String path, float[] values) {

        if(values.length > 20)
            Log.d(TAG, "send: error ");
        else {
            Log.d(TAG,"send");
            BluetoothLe.getBleClient(context).send(path, values);
        }
    }

    public static void send(String path, String values) {

        if(values.length() > 20)
            Log.d(TAG, "send: error ");
        else {
            Log.d(TAG,"send");
            BluetoothLe.getBleClient(context).send(path, values);
        }
    }

    /**
     * This method sets the state of the collection and notifies it's connected BleServer of the
     * change.
     *
     * @param newState int: The new state of the collection
     */
    public static void setState (int newState) {
        state = newState;
        send("state", new float[]{state});
    }

    /**
     * This method gets the current state of the collection
     *
     * @return int: The current state of the collection
     */
    public static int getState() {
        return state;
    }

    /**
     * A class implementation of the BleClient.OnEventReceivedListener that listens for events
     * from a connected ble server.
     */
    private static class BleServerListener implements BleClient.OnEventReceivedListener {

        /**
         * This method implements the BleClient.OnEventReceivedListener method for receiving and
         * handling event notifications
         *
         * Upon receiving data from the connected device, this method either sends the received data
         * to the Prediction class for prediction or the Database class for updating an document
         * awaiting a feedback label.
         *
         * @param event Event: The received ble event
         */
        @Override
        public void onEventReceived(Event event) {
            try {

                float [] data = event.getData();
                String path = event.getPath();
                switch (path) {

                    // If the received data is accelerometer data then make a prediction
                    case ACCELEROMETER_EVENT_PATH:
                        Intent accIntent = new Intent("AccelerometerData");
                        accIntent.putExtra("data", Float.toString(data[0]));
                        LocalBroadcastManager.getInstance(context).sendBroadcast(accIntent);
                        Prediction.makePrediction(event);
                        break;

                    // If the received data is feedback create a json to update the corresponding label
                    // to the feedback provided then signal the watch to restart collection
                    case FEEDBACK_EVENT_PATH:
                        int feedback = (int) data[0];
                        JSONObject updates = new JSONObject();

                        if (feedback == FEEDBACK_LABEL_TP){
                            updates.put("type", "TP");
                            Database.updateDocument(updates);
                        }
                        else if (feedback == FEEDBACK_LABEL_FP){
                            updates.put("type", "FP");
                            Database.updateDocument(updates);
                        }
                        if (feedback == FEEDBACK_LABEL_FN){
                            Prediction.onFellClickedInWatch();
                        }
                        else if (feedback == FEEDBACK_NEEDS_HELP){
                            Intent helpIntent = new Intent("HelpData");
                            helpIntent.putExtra("data", Float.toString(feedback));
                            LocalBroadcastManager.getInstance(context).sendBroadcast(helpIntent);
                        }
                        if(state == FEEDBACK_STATE) {
                            setState(COLLECTION_STATE);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * This method implements the BleClient.OnEventReceivedListener method for receiving ble
         * connection state notifications.
         *
         * Upon receiving a state change notification this method relays the connection state
         * through a toast message.
         *
         * @param state int: The current state of the ble connection
         */
        @Override
        public void onConnectionStateChange(int state) {
            switch (state) {
                case BleClient.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Connected");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Device is now connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                case BleClient.STATE_DISCONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Disconnected");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Device was disconnected", Toast.LENGTH_SHORT).show();
                        }
                    });

            }
        }
    }
}
