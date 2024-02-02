# BluetoothLe-Android

A Bluetooth Low Energy API that uses a server-client architecture to send events between two or more connected devices.

### Using the API

To use this API, download and import the android module into your android project. Once imported, dependent on your use case a device will use either the [`BleServer`](#BleServer) or the [`BleClient`](#BleClient) API functionalities.

## Event
An event is generated when either the [`BleServer`](#BleServer) or the [`BleClient`](#BleClient) path receives data and is comprised of the following items:
| Attribute | Description | Method | Description |
| :---: | :-----------: | :---: | :-----------: |
| Data | Float array  with size <= 20 | getData | Returns the event's float array values |
| Timestamp | Time in which the event occurred | getTimestamp | Returns the event's timestamp |
| Path | A String identifier for the event | getPath | Returns the event's path string |

## BleServer
A BleServer allows a device to broadcast data to many connected clients. Typically when choosing a role, the device which has some sort of service to offer (such as a sensor service on a wearable device) will be selected to be the BleServer. In addition to being able to broadcast and send data to any connected BleClient, the server can also receive messages from any of it's clients.

#### Starting the BleServer
To start the BleServer and allow for the connection of BleClients use the `startBleServer` method and provide it with a list of all the paths the will be used to broadcast the data throughout the application. The below snippet shows a wearable device which will use an `ACCELEROMETER_EVENT_PATH`, `GYROSCOPE_EVENT_PATH` and a `STATE_EVENT_PATH` to send accelerometer and gyroscope sensor data along with data reguarding the device's state to it's connected BleClients. 

```ruby
public static final String ACCELEROMETER_EVENT_PATH = "accelerometer";
public static final String GYROSCOPE_EVENT_PATH = "gyroscope";
public static final String STATE_EVENT_PATH = "state";
  ...
public static String [] paths = new String []{ACCELEROMETER_EVENT_PATH, GYROSCOPE_EVENT_PATH, STATE_EVENT_PATH};
  ...
private void startBleServer() {
  Context context = getApplicationContext();
  BluetoothLe.getBleServer(context).startBleServer(paths);
}
```

### Send an Event
To send a message to connected devices use the `send` method provided by the [`BleServer`](#BleServer) class. The example below demonstrates how to send accelerometer data from a wearable device.

```ruby
public static final String ACCELEROMETER_EVENT_PATH = "accelerometer";
  ...
private void sendAccelerometerData(float [] accelerometerData) {
  Context context = getApplicationContext();
  BluetoothLe.getBleServer(context).send(ACCELEROMETER_EVENT_PATH, accelerometerData);
}
```

### Receive an Event

To be notified of any received data/event(s), implement the `BleServer.OnEventReceivedListener` interface to provide
a listener for Ble data events. Then, register the listener with the `addListener` method. The example below shows a 
basic implementation of a `OnEventReceivedListener` which upon being notified of an event logs both the data contents and the associated path.

```ruby
 @Override
 public void onEventReceived(Event event) {
   String path = event.getPath();
   float[] data = event.getData();
   Log.d(path, String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
 }
 ```

## BleClient
A BleClient allows a device to receive broadcasted data from many connected servers. Typically when choosing a role, the device in which relies on some sort of remote service (such as a prediction model running on a smartphone) will be selected to be the client and use the functionaliies provided by the BleClient class. In addition to being able to receive broadcasted data from a connected server, a client can also sends messages to a server.

#### Starting the BleClient
To start the BleClient and allow for the connection to one or more BleServers use the `startBleClient` method and provide it with a list of all the paths the will be used to broadcast the data throughout the application. The below snippet shows a mobile device which will use an `ACCELEROMETER_EVENT_PATH`, `GYROSCOPE_EVENT_PATH` and a `STATE_EVENT_PATH` to receive accelerometer and gyroscope sensor data from a connected BleServer in addition to  sending data reguarding the device's state to any BleServer. 

```ruby
public static final String ACCELEROMETER_EVENT_PATH = "accelerometer";
public static final String GYROSCOPE_EVENT_PATH = "gyroscope";
public static final String STATE_EVENT_PATH = "state";
  ...
public static String [] paths = new String []{ACCELEROMETER_EVENT_PATH, GYROSCOPE_EVENT_PATH, STATE_EVENT_PATH};
  ...
private void startBleClient() {
  Context context = getApplicationContext();
  BluetoothLe.getBleClient(context).startBleClient(paths);
}
```

### Send an Event
To send a message to a connected [`BleServer`](#BleServer) use the `send` method provided by the [`BleClient`](#BleClient) class. The example below demonstrates how to send system state data from a mobile device.

```ruby
public static final String STATE_EVENT_PATH = "state";
  ...
public static final float ON = 1.0f;
public static final float OFF = 0.0f;
  ...
private void updateCurrentState(float state) {
  Context context = getApplicationContext();
  BluetoothLe.getBleClient(context).send(STATE_EVENT_PATH, new float[] {state});
}
```

### Receive an Event
To be notified of any received data/event(s), implement the `BleClient.OnEventReceivedListener` interface to provide a listener for Ble data events. Then, register the listener with the `addListener` method. The example below shows a basic implementation of a `OnEventReceivedListener` which upon being notified of an event logs both the data contents and the associated path.

```ruby
 @Override
 public void onEventReceived(Event event) {
   String path = event.getPath();
   float[] data = event.getData();
   Log.d(path, String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
 }
 ```
