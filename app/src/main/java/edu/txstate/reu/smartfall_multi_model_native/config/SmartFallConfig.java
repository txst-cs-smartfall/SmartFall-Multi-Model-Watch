package edu.txstate.reu.smartfall_multi_model_native.config;

/**
 * This class is the single location to change personalization strategy or remote server configuration.
 * Any new type of strategy or new remote server configuration has to be added in this class
 *
 * @author Bhargav Balusu (b_b515)
 * @version 1.0
 * @since 2022.05.15
 */
public class SmartFallConfig {

    /**
     * Below is the key where top level strategy can be switched.
     * To change strategy, just uncomment required line and comment current strategy.
     */
    public static final String MODEL_TYPE  = "PERSONALIZED";
    //public static final String MODEL_TYPE  = "LSTM";
    //public static final String MODEL_TYPE  = "DEFAULT";


    /**
     * Below is the key where personalization strategy can be switched.
     * To change strategy, just uncomment required line and comment current strategy.
     */
    //public static final String PERSONALIZED_STRATEGY  = "ENSEMBLE";
    public static final String PERSONALIZED_STRATEGY  = "LSTM";


    /**
     * This is the key that holds the information about which remote server has to be accessed.
     */
    public static String ENV_FLAG = "GANYMEDE";

    /**
     * This is the key to check if it is first time install or not. This key is set to true in ProfileFragment.java
     * if the user fills profile section. This property is used in ModelDownloader.java
     */
    public static boolean isFirst = false;


    public static class CloudIPConfig {
        public String downloadModel=null;
        public String dataUpload = null;
        public String checkVersion = null;
    }

    /**
     * This function returns the configuration of the above mentioned remote server.
     * @return CloudIPConfig object.
     */

    public static CloudIPConfig getCloudConfig() {
        CloudIPConfig urlPaths = new CloudIPConfig();
        if (ENV_FLAG == "MERCURY") {
            System.out.println("It is Mercury URL Path");
            urlPaths.checkVersion = "http://eil.cs.txstate.edu/couchbase-prod/checkversion.php";
            urlPaths.downloadModel = "http://eil.cs.txstate.edu/couchbase-prod/downloadmodels.php";
            urlPaths.dataUpload = "http://eil.cs.txstate.edu/couchbase-prod/uploadcouch.php";
        } else if (ENV_FLAG == "GANYMEDE") {
            System.out.println("It is Ganymede URL Path");
            urlPaths.checkVersion = "http://ganymede.cs.txstate.edu/checkversion.php";
            urlPaths.downloadModel = "http://ganymede.cs.txstate.edu/downloadmodels.php";
            urlPaths.dataUpload = "http://ganymede.cs.txstate.edu/upsertdocuments.php";
        }
        else if (ENV_FLAG == "EUROPA") {
            System.out.println("It is Europa URL Path");
            urlPaths.checkVersion = " http://eil.cs.txstate.edu/couchbase-dev2/checkversion.php";
            urlPaths.downloadModel = " http://eil.cs.txstate.edu/couchbase-dev2/downloadmodels.php";
            urlPaths.dataUpload = " http://eil.cs.txstate.edu/couchbase-dev2/uploadcouch.php";
        }

        return urlPaths;
    }

}
