package com.example.walpurisnacht.jestion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ConcurrentModificationException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG";
    private static final float ALPHA = 0.25f;

    private boolean Debug = false;
    private SVM svm_train = SVM.create();
    String trainPath;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch aSwitch = (Switch) findViewById(R.id.swtDebug);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Debug = true;
                } else {
                    Debug = false;
                }
            }
        });
    }

    public void trainClick(View view) {

        SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(MainActivity.this, "FileOpen",
                new SimpleFileDialog.SimpleFileDialogListener()
                {
                    @Override
                    public void onChosenDir(String chosenDir)
                    {
                        // The code in this function will be executed when the dialog OK button is pushed
                        trainPath = chosenDir;

                        TextView textView = (TextView) findViewById(R.id.textView);
                        textView.setText(trainPath);
                        InitTrain();
                    }
                });

        //You can change the default filename using the public variable "Default_File_Name"
        FileOpenDialog.Default_File_Name = "";
        FileOpenDialog.chooseFile_or_Dir();
    }

    //region Tooltip
    private Dataset MatParser(String text) {
        String[] lines = text.split("\n");

        int row = lines.length;
        int col = lines[0].split(",").length-1;

        Dataset dataset = new Dataset(row,col);

        int[] i_target = new int[1];
        float[] i_digit = new float[col];

        //Parse
        for (int i = 0; i < row; i++) {
            String[] s_data = lines[i].split(",");


//            i_target[0] = Integer.parseInt(s_data[s_data.length-1]);
            i_target[0] = Integer.parseInt(s_data[s_data.length - 1]);


            for (int j = 0; j < col; j++) {
//                i_digit[j] = Integer.parseInt(s_data[j]);
                i_digit[j] = Float.parseFloat(s_data[j]);
            }

            dataset.putSample(i,0,i_digit);
            dataset.putResponse(i, 0, i_target);
        }

        return dataset;
    }

    private void Train(Dataset dataset) {
        //Param
        svm_train.setC(1000);
        svm_train.setGamma(0.1);
        svm_train.setKernel(SVM.RBF);
        svm_train.setType(SVM.C_SVC);
        //Train
        svm_train.train(dataset.getSample(), Ml.ROW_SAMPLE, dataset.getResponse());
        if (svm_train.isTrained()) Log.d(TAG,"Trained");
    }

    protected float[] LowPass(float[] input, float[] output){
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private void HaltSensor(Dataset dataset) {
        mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

        if (Debug) {
            float[] data = new float[9];
            StringBuilder text = new StringBuilder();
            Mat sample = dataset.getSample();

            for (int i = 0; i < 64; i++) {
                sample.get(i,0,data);
                for (int k = 0; k < 9; k++) {
                    text.append(data[k]);
                    text.append(" ");
                }
                text.append("endl" + i + "\n");
            }

            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setMovementMethod(new ScrollingMovementMethod());
            textView.setText(text);
        }

//        Mat sample = dataset.getSample();
//        float[][] data = new float[64][9];
//        for (int i = 0; i < 9; i++) {
//            Mat norm = sample.col(i);
//            Core.normalize(norm,norm,-1,1,Core.NORM_L2);
//
//            float[] tmp = new float[64];
//            norm.get(0,0,tmp);
//
//            for (int j = 0; j < 64; j++) {
//
//            }
//        }
    }
    //endregion

    //region Process
    private void InitTrain() {
        StringBuilder text = new StringBuilder();
        File file = new File(trainPath);

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            Log.d(TAG,"Read done");
        }
        catch (IOException e) {
            Log.d(TAG,e.toString());
        }

        //Parse Mat
        Train(MatParser(text.toString()));
    }

    private void RecordSensor() {

        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);


        mSensorListener = new SensorEventListener() {
            float[] acc = new float[3];
            float[] mag = new float[3];
            float[] gyr = new float[3];

            Dataset dataset = new Dataset(64,9);

            int cnt = 0;
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (cnt >= 64) HaltSensor(dataset);
                Sensor sensor = event.sensor;

                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)  {
                    acc = LowPass(event.values.clone(),acc);
                }
                else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)    {
                    mag = LowPass(event.values.clone(),mag);
                }
                else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gyr = LowPass(event.values.clone(),gyr);
                }

                dataset.putSample(cnt,0,acc);
                dataset.putSample(cnt,3,mag);
                dataset.putSample(cnt,6,gyr);
                cnt++;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);

    }

    public void scanClick(View view) {
        RecordSensor();
    }
    //endregion
}
