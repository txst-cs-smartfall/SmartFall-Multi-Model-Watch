package edu.txstate.reu.smartfall_multi_model_native.Prediction;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.couchbase.lite.MutableDocument;
import com.google.gson.Gson;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.txstate.reu.smartfall_multi_model_native.Database.Couchbase;
import edu.txstate.reu.smartfall_multi_model_native.config.ModelConfig;

/**
 * This class is responsible for making inference if the personalization strategy is LSTM.
 *
 * @author Bhargav Balusu (b_b515)
 * @version 1.0
 * @since 2022.05.15
 */
public class PersonalizedPredictionLSTM {

    private static final String TAG = "PersonalizedPredictionLSTM";
    private static ArrayList<Interpreter> interpreters = new ArrayList<>();
    private static float[] MODEL_WEIGHTS = {1.0f};
    private static float[] MODEL_THRESHOLDS = {0.20f};

    /**
     * Initializes the interpreters with downloaded models present in the local storage.
     * @param context
     * @throws IOException
     */
    public static void initialize(Context context) throws IOException {
//        checkAndDownloadNewModel(context);
//
//        for (String fileName : ModelConfig.getModelConfig(context).modelNames) {
//            interpreters.add(new Interpreter(new File(context.getFilesDir().getAbsolutePath() + "/" + fileName)));
//        }
//        MODEL_THRESHOLDS = ModelConfig.getModelConfig(context).thresholds;


        interpreters.add(new Interpreter(loadModelFile(context,"xiaomin_trial2.tflite")));
    }

    /**
     * This method performs a weighted sum inference on received float samples using
     * the list of interpreters.
     *
     * @param samples float: Two-dimensional float array of samples
     * @return float: The weighted sum inference made my the interpreters
     */

    public static float makeInference(float[][] samples) throws Exception {
        float inference = 0.0f;
        float[][][] flattenedSamples = flattenInputTo3D(samples);
        float[][] outputs = new float[1][1];
        outputs[0][0] = 0f;

        for (Interpreter interpreter : interpreters) {
            interpreter.run(flattenedSamples, outputs);   // make inference of beta samples
            inference += outputs[0][0] * MODEL_WEIGHTS[interpreters.indexOf(interpreter)];
        }

        return inference;
    }

    public static float getThreshold() {
//        return MODEL_THRESHOLDS[0];
        return 0.17f;
    }


    /**
     * This method calls the ModelDownloader and writes the model files to local storage.
     * @param context
     */
    private static void checkAndDownloadNewModel(Context context) {

        try {
            ModelConfig modelConfig = ModelConfig.getModelConfig(context);
            ModelConfig config = new ModelDownloader().execute(modelConfig).get();

            if(config.isDownloaded){
                String[] modelNames = config.modelNames;
                MODEL_WEIGHTS = config.modelWeights;
                MODEL_THRESHOLDS = config.thresholds;
                byte[][] modelContent = config.modelContent;
                for(int i=0; i<modelNames.length; i++){
                    Log.e(TAG,modelContent[i].toString());


                    try (FileOutputStream fos = context.openFileOutput(modelNames[i], Context.MODE_PRIVATE)) {
                        fos.write(modelContent[i]);
                    }
                    catch (Exception e){
                        Log.e(TAG, "Error while writing model content : " + e.getMessage());
                    }
                    Log.e(TAG, context.getFilesDir().getAbsolutePath());
                }
                Couchbase.initialize(context);
                updateConfigFile(context);

            }
        }catch(Exception e){
            Log.e(TAG, "Exception while downloading new model : "+ e.getMessage());
        }

    }
    /**
     * This method serializes the ModelConfig object and writes the result to SmartWatchValues.json
     * @param context
     */
    public static void updateConfigFile(Context context){
        ModelConfig config = ModelConfig.getModelConfig(context);
        String filename =  "SmartWatchValues.json" ;

        Gson gson = new Gson();
        String jsonString = gson.toJson(config);

        Intent modelIntent = new Intent("modelInfo");
        modelIntent.putExtra("data", jsonString);
        LocalBroadcastManager.getInstance(context).sendBroadcast(modelIntent);

        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(jsonString.getBytes());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static ByteBuffer loadModelFile(Context context, String fileName) throws IOException {

        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(fileName);
        FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,declaredLength);
        return byteBuffer;
    }


    /**
     * This method is used to transforms samples from a two-dimensional float array
     * into a three-dimensional float array
     *
     * @param samples float: Two-dimensional float array
     * @return float: Three-dimensional float array
     */
    private static float[][][] flattenInputTo3D(float[][] samples) {
        float[][][] multiDimArray = new float[1][samples.length][samples[0].length];
        multiDimArray[0] = samples;
        return multiDimArray;
    }


}


