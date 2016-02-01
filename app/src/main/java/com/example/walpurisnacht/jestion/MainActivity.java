package com.example.walpurisnacht.jestion;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG";
    private static final float ALPHA = 0.25f;

    private StringBuilder data;
    private boolean Debug = false;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        data = new StringBuilder();

        final Switch aSwitch = (Switch) findViewById(R.id.swtModel);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Debug = true;
                    aSwitch.setText("LINEAR");
                } else {
                    Debug = false;
                    aSwitch.setText("RBF");
                }
            }
        });
    }

    private void HaltSensor() {
        mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

        Log.d(TAG, data.toString());

        StringBuilder url = new StringBuilder();
        url.append("http://jestion.jslab.xyz");

        SendData(data.toString().substring(0,data.length()-3), url.toString());
    }

    private void SendData(String data, String url) {
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);

        try {
            List<NameValuePair> lq = new ArrayList<NameValuePair>();
            lq.add(new BasicNameValuePair("query", data.toString()));
            if (Debug) {
                lq.add(new BasicNameValuePair("model","linear"));
                Log.d(TAG,"linear");
            }
            else {
                lq.add(new BasicNameValuePair("model","rbf"));
                Log.d(TAG,"RBF");
            }
            Log.d(TAG,data);

            httpPost.setEntity(new UrlEncodedFormEntity(lq));
            Log.d(TAG, "Sended!");
            HttpResponse resp = client.execute(httpPost);
            HttpEntity entity = resp.getEntity();

            if (entity != null) {
                StringBuilder res = new StringBuilder();
                byte[] b = EntityUtils.toByteArray(entity);
                res.append(new String(b,0,b.length));

                Log.d(TAG,"Received: " + res.toString());

                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setMovementMethod(new ScrollingMovementMethod());
                textView.setText(res.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //endregion

    private void RecordSensor() {

        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);


        mSensorListener = new SensorEventListener() {
            float[] acc = new float[3];
            float[] mag = new float[3];
            float[] gyr = new float[3];

            int cnt = 0;
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (cnt == 64) HaltSensor();
                Sensor sensor = event.sensor;

                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)  {
                    acc = event.values.clone();
                }
                else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)    {
                    mag = event.values.clone();
                }
                else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gyr = event.values.clone();
                }

                for (float f:acc) {
                    data.append(f);
                    data.append(",");
                }

                for (float f:mag) {
                    data.append(f);
                    data.append(",");
                }

                for (int i = 0; i < 3; i++) {
                    data.append(gyr[i]);
                    if (i != 2) data.append(",");
                    else data.append("=");
                }

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
        data.delete(0, data.length());
        RecordSensor();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    data.delete(0, data.length());
                    RecordSensor();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    data.delete(0, data.length());
                    RecordSensor();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }
    //endregion
}
