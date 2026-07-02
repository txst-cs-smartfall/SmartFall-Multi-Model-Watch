## 📂 PHP Scripts – Server Tunnel for Couchbase Integration

This directory contains PHP scripts that act as a web-based tunnel between the SmartFall Android application and the Couchbase database. These scripts are intended to be deployed on a PHP-enabled web server and are used by the watch app to upload data, check model updates, and download personalized models.

### 🧩 Script Descriptions

| Script Name            | Description                                                                 |
|------------------------|-----------------------------------------------------------------------------|
| `checkversion.php`     | Checks if a newer personalized model version is available.                  |
| `downloadmodels.php`   | Sends the latest trained TensorFlow Lite model to the client device.        |
| `upsertdocuments.php`  | (Alternative) Handles data insertion and updates in Couchbase.              |

These scripts work alongside the `ENV_FLAG` setting in the Android app’s configuration to route requests to the correct server environment (e.g., MERCURY, GANYMEDE, EUROPA, SMART_FALL).

> 🔐 **Note:** Before deployment, ensure your server is PHP-enabled and can reach Couchbase. Modify database credentials or paths in each script as required for your environment.

