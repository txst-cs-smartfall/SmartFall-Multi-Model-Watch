package edu.txstate.reu.smartfall_multi_model_native.Database;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.MutableDocument;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.txstate.reu.smartfall_multi_model_native.DataCollection.DataCollection;
import edu.txstate.reu.smartfall_multi_model_native.MainActivity;

/**
 * This class is responsible for additional application specific database
 * logic that should not be included in a particular database API. Such as document tracking,
 * and periodic remote uploading.
 *
 * @author Colin Campbell (c_c953)
 * @version 1.0
 * @since 2021.12.13
 */
public class Database {

    /**
     * A queue of String Couchbase Lite document Ids awaiting feedback in order to update the
     * document label.
     */
    private static ConcurrentLinkedQueue<String> documentIdQueue;

    /**
     * A constant integer representing the rate in which documents are uploaded to the remote server
     * in milliseconds. (1000 milliseconds = 1 second)
     */
    private static final int UPLOAD_RATE = 15000;

    /**
     * This method calls the initialize the database service by calling the Couchbase class
     * initialize method, instantiating a new queue for the documentIdQueue data attribute and
     * scheduling the documents to be uploaded to a remote server using a fixed rate.
     *
     * @param context Context: App context
     */
    public static void initialize (Context context) throws IOException {
        Couchbase.initialize(context);
        documentIdQueue = new ConcurrentLinkedQueue<String>();


        new Timer().scheduleAtFixedRate(new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void run() {
                try {
                    uploadDocuments();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, UPLOAD_RATE);

    }

    /**
     * This method is used to save a document to the local Couchbase Lite database. Before each
     * document is saved into the database, this method checks to see if the document contains a
     * 'label' attribute whose value is '???'. If so then the method sends a request for a true
     * label using the DataCollection utility class. After sending the label request the method
     * proceeds to insert the document into the Couchbase Lite database and adds the corresponding
     * document Id to the documentIdQueue so that when the label can be updated when the feedback
     * arrives at a later time
     *
     * @param document JSONObject: The document to be saved to the local bucket
     * @throws JSONException
     * @throws CouchbaseLiteException
     * @throws IOException
     */
    public static void insertDocument(MutableDocument document) throws JSONException, CouchbaseLiteException, IOException {

        if (document.getString("type").equals("???")) {
            DataCollection.setState(DataCollection.FEEDBACK_STATE);
            documentIdQueue.add(Couchbase.insertDocument(document));
        }
        else
            Couchbase.insertDocument(document);
    }

    /**
     * This method applies any received updates to the document located in the front of the
     * documentIdQueue
     *
     * @param updates JSONObject: An object comprised of the modifications to be made to the
     * pre-existing document.
     * @throws CouchbaseLiteException
     * @throws JSONException
     */
    public static void updateDocument(JSONObject updates) throws CouchbaseLiteException, JSONException {
        Couchbase.updateDocument(documentIdQueue.poll(), updates);
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void uploadDocuments() throws JSONException, IOException, CouchbaseLiteException {
//        Couchbase.writeDataInStorage();
        Couchbase.uploadDocumentsToRemoteURL();
    }


}
