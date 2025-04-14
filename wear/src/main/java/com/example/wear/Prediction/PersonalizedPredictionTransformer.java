package com.example.wear.Prediction;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.wear.Database.Couchbase;
import com.example.wear.config.ModelConfig;
import com.example.wear.config.SmartFallConfig;
import com.google.gson.Gson;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class is responsible for making inference if the personalization strategy is LSTM.
 */
public class PersonalizedPredictionTransformer {

    private static final String TAG = "PersonalizedPredictionTransformer";
    private static ArrayList<Interpreter> interpreters = new ArrayList<>();
    private static float[] MODEL_WEIGHTS = {1.0f};
    private static float[] MODEL_THRESHOLDS = {0.20f};

    public static Context con;
    /**
     * Initializes the interpreters with downloaded models present in the local storage.
     * @param context
     * @throws IOException
     */
    public static void initialize(Context context) throws IOException {
        con = context;
        if(interpreters.size()>0) {
            for(Interpreter i:interpreters){
                i.close();
            }
        }
        interpreters = new ArrayList<>();
        if(SmartFallConfig.MODEL_FROM == "ONLINE") {
            checkAndDownloadNewModel(context);
            for (String fileName : ModelConfig.getModelConfig(context).modelNames) {
                Interpreter interpreter = new Interpreter(new File(context.getFilesDir().getAbsolutePath() + "/" + fileName));
                interpreter.allocateTensors();
                interpreters.add(interpreter);
            }
            MODEL_THRESHOLDS = ModelConfig.getModelConfig(context).thresholds;
        }
        else {
            Interpreter interpreter = new Interpreter(loadMappedFile(SmartFallConfig.OFFLINE_MODEL_FILE));
            interpreter.allocateTensors();
            interpreters.add(interpreter);
            MODEL_THRESHOLDS[0] = SmartFallConfig.OFFLINE_MODEL_THRESHOLD;
        }
    }

    public static int getNumberOfInterpreter(){
        return interpreters.size();
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
        float[][] outputs = new float[1][2];
//        float[] outputs = new float[1];
        outputs[0][0] = 0f;
        outputs[0][1] = 0f;

        boolean sendForPrediction = false;
        for(int i=0; i<SmartFallConfig.BETA_LIMIT; i++) {
            int num = 0;
            for(int j=0;j<3;j++) {
                if (flattenedSamples[0][i][j]>8 || flattenedSamples[0][i][j]<-8){
                    num++;
                }
            }

            if(num>=2) {
                sendForPrediction = true;
                break;
            }
        }

        if(!sendForPrediction) {
            float ret_val = new Random().nextInt(15);
            return ret_val/50;
        }

        for (Interpreter interpreter : interpreters) {
            //interpreter.allocateTensors();
            interpreter.run(flattenedSamples, outputs);   // make inference of beta samples
            System.out.println("Output length: "+outputs[0].length);
//            System.out.println("Output length: "+outputs.length);
            inference += outputs[0][0] * MODEL_WEIGHTS[interpreters.indexOf(interpreter)];
        }

        return inference;
    }


//    public static float makeInference(float[][] samples) throws Exception {
//        float inference = 0.0f;
//
//        // Compute statistical features
//        float[][] statisticalFeatures = computeStatisticalFeatures(samples);
//
//        // Reshape to match the expected input shape (assuming 3D input is needed)
//        float[][][] processedSamples = flattenInputTo3D(statisticalFeatures);
//
//        float[][] outputs = new float[1][2];
//        outputs[0][0] = 0f;
//        outputs[0][1] = 0f;
//
//        boolean sendForPrediction = false;
//
//        // Check condition based on statistical features
//        for (int i = 0; i < 128; i++) {
//            int num = 0;
//            for (int j = 0; j < 3; j++) {
//                if (processedSamples[0][i][j] > 8 || processedSamples[0][i][j] < -8) {
//                    num++;
//                }
//            }
//            if (num >= 2) {
//                sendForPrediction = true;
//                break;
//            }
//        }
//
//        // If no extreme values are detected, return a random fallback value
//        if (!sendForPrediction) {
//            float ret_val = new Random().nextInt(15);
//            return ret_val / 50;
//        }
//
//        // Perform inference using statistical features
//        for (Interpreter interpreter : interpreters) {
//            interpreter.run(processedSamples, outputs);  // Run inference
//            System.out.println("Output length: " + outputs[0].length);
//            inference += outputs[0][0] * MODEL_WEIGHTS[interpreters.indexOf(interpreter)];
//        }
//
//        return inference;
//    }
//
//    /**
//     * Computes mean, min, max, and standard deviation for each sample.
//     * @param samples 2D array where each row is a sample with multiple features.
//     * @return 2D array with statistical features (one row per sample).
//     */
//    private static float[][] computeStatisticalFeatures(float[][] samples) {
//        int numSamples = samples.length;
//        int numFeatures = samples[0].length;
//        float[][] stats = new float[numSamples][4]; // Mean, Min, Max, Std Dev
//
//        for (int i = 0; i < numSamples; i++) {
//            float sum = 0, min = Float.MAX_VALUE, max = Float.MIN_VALUE, sumSq = 0;
//
//            for (int j = 0; j < numFeatures; j++) {
//                float value = samples[i][j];
//                sum += value;
//                sumSq += value * value;
//                if (value < min) min = value;
//                if (value > max) max = value;
//            }
//
//            float mean = sum / numFeatures;
//            float variance = (sumSq / numFeatures) - (mean * mean);
//            float stdDev = (float) Math.sqrt(variance);
//
//            stats[i][0] = mean;
//            stats[i][1] = min;
//            stats[i][2] = max;
//            stats[i][3] = stdDev;
//        }
//
//        return stats;
//    }







    public static float getThreshold() {
        return MODEL_THRESHOLDS[0];
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

    public static MappedByteBuffer loadMappedFile(String filePath) throws IOException {
        AssetFileDescriptor fileDescriptor = con.getAssets().openFd(filePath);

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


}


