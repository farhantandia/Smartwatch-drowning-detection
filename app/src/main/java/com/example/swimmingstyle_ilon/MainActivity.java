package com.example.swimmingstyle_ilon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends FragmentActivity implements
        AmbientModeSupport.AmbientCallbackProvider,SensorEventListener , TextToSpeech.OnInitListener {
    private static final int N_SAMPLES = 100;
    private static int prevIdx = -1;

    private static List<Float> ax;
    private static List<Float> ay;
    private static List<Float> az;

    private static List<Float> lx;
    private static List<Float> ly;
    private static List<Float> lz;

    private static List<Float> gx;
    private static List<Float> gy;
    private static List<Float> gz;
    //
    private static List<Float> ma;
    private static List<Float> ml;
    private static List<Float> mg;
    private static List<Float> mm;

    private static List<Float> mx;
    private static List<Float> my;
    private static List<Float> mz;


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mLinearAcceleration;
    private Sensor mMagnetometer;
    private Sensor heartRate;

    private  TextView Heart;
    private TextView Status;
    private TextView breastTextView;
    private TextView backTextView;
    private TextView restTextView;

    private TableRow breastTableRow;
    private TableRow backTableRow;
    private TableRow restTableRow;

    private TextToSpeech textToSpeech;
    private float[] results;
    private TFClassifier classifier;
    private String[] labels = {"Breaststroke", "Backstroke", "Resting"};

    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    private Sensor accelerometer, mGravity, mLinAccel, mGyro, mMagno, mLight, mProx, mPressure, mTemp, mHumi, mOrientation;
    Button save, record;
    private Button mButtonStartPause;
    private Button mButtonReset;
    private boolean permission_to_record = false;
    String DATA = "";
    String newline = "";
    String modified_DATA = "";
    String dateCurrent;
    String dateCurrentTemp = "";
    private FileWriter writer;
    File gpxfile;
    int Breaststroke = 0;
    int Backstroke = 0;
    int Rest = 0;
    String Stat ="";
    String heartValue ="";
    Context context = this;

    private EditText userID;
    String InputID;
    int CounterForSave = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ax = new ArrayList<>(); ay = new ArrayList<>(); az = new ArrayList<>();
        lx = new ArrayList<>(); ly = new ArrayList<>(); lz = new ArrayList<>();
        gx = new ArrayList<>(); gy = new ArrayList<>(); gz = new ArrayList<>();
        ma = new ArrayList<>(); ml = new ArrayList<>(); mg = new ArrayList<>();
        mx = new ArrayList<>(); my = new ArrayList<>(); mz = new ArrayList<>();
        mm = new ArrayList<>();

        AmbientModeSupport.attach(this);
        breastTextView = (TextView) findViewById(R.id.breast_prob);
        backTextView = (TextView) findViewById(R.id.back_prob);
        restTextView = (TextView) findViewById(R.id.rest_prob);

        breastTableRow = (TableRow) findViewById(R.id.breast_row);
        backTableRow = (TableRow) findViewById(R.id.back_row);
        restTableRow = (TableRow) findViewById(R.id.rest_row);
        userID = findViewById(R.id.id);

        userID.addTextChangedListener(activityTextWatcher);
        Status = (TextView) findViewById(R.id.status);
        Heart = (TextView) findViewById(R.id.heart);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        heartRate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);

//        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        mSensorManager.registerListener(this, mLinearAcceleration , SensorManager.SENSOR_DELAY_FASTEST);

        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mGyroscope , SensorManager.SENSOR_DELAY_FASTEST);

//        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        mSensorManager.registerListener(this, mMagnetometer , SensorManager.SENSOR_DELAY_FASTEST);


        classifier = new TFClassifier(getApplicationContext());

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);

        mButtonStartPause = findViewById(R.id.Record);
        mButtonReset = findViewById(R.id.Save);
        save = (Button) findViewById(R.id.Save);
        record = (Button) findViewById(R.id.Record);

        startMeasure();
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record.setBackgroundColor(Color.RED);
                save.setBackgroundColor(Color.DKGRAY);
                permission_to_record = true;
                Toast.makeText(MainActivity.this,"Start Recording...", Toast.LENGTH_SHORT).show();
                startMeasure();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record.setBackgroundColor(Color.DKGRAY);
                save.setBackgroundColor(Color.GREEN);
                permission_to_record = false;
                Toast.makeText(MainActivity.this, "File Created & Saved", Toast.LENGTH_SHORT).show();

                long date2 = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH-mm-ss");
                String dateString = sdf.format(date2);

                File folder = context.getExternalFilesDir("/storage");
                gpxfile = new File(folder, InputID+"_SmartWatch"+dateString+".csv");
                try {
                    writer = new FileWriter(gpxfile);
                    String line = "ID,DATE,TIME,BREASTSTROKE,BACKSTROKE,REST,STATUS,HEART RATE \n";
                    writer.write(line);
                    writer.write(modified_DATA);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                stopMeasure();
            }
        });
    }

    protected void onResume() {
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
//        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
//        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
    private void stopMeasure() {
        mSensorManager.unregisterListener(this,heartRate);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        long date = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
        dateCurrent = sdf.format(date);
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax.add(event.values[0]);
            ay.add(event.values[1]);
            az.add(event.values[2]);
//            Status.setText(String.valueOf(event.values[0]));

        } else if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            lx.add(event.values[0]);
            ly.add(event.values[1]);
            lz.add(event.values[2]);

        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx.add(event.values[0]);
            gy.add(event.values[1]);
            gz.add(event.values[2]);

        }
        else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mx.add(event.values[0]);
            my.add(event.values[1]);
            mz.add(event.values[2]);

        }

        if (sensor.getType() == Sensor.TYPE_HEART_RATE){
            float mHeartRateFloat = event.values[0];
            int mHeartRate = Math.round(mHeartRateFloat);
            Heart.setText(Integer.toString(mHeartRate));
            heartValue = Integer.toString(mHeartRate);
        }
        activityPrediction();

        if (!dateCurrentTemp.equals(dateCurrent)){
            dateCurrentTemp = dateCurrent;
            CounterForSave = 0;
        }
        if (CounterForSave<60 & permission_to_record) {
            DATA = InputID+","+dateCurrent + "," + Breaststroke + "," + Backstroke + "," + Rest + "," + Stat +"," + heartValue +"\n"; //"DATE,TIME,WALK,JUMP,STATIC,FALLDOWN\n"
            modified_DATA = newline + DATA;
            newline = modified_DATA;
            CounterForSave = CounterForSave + 3;
        }
    }
    private void startMeasure() {
        boolean sensorRegistered = mSensorManager.registerListener(this, heartRate, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d("Sensor Status:", " Sensor registered: " + (sensorRegistered ? "yes" : "no"));
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void activityPrediction() {

        List<Float> data = new ArrayList<>();

        if (ax.size() >= N_SAMPLES && ay.size() >= N_SAMPLES && az.size() >= N_SAMPLES
//                && lx.size() >= N_SAMPLES && ly.size() >= N_SAMPLES && lz.size() >= N_SAMPLES
                && gx.size() >= N_SAMPLES && gy.size() >= N_SAMPLES && gz.size() >= N_SAMPLES
        ) {
//            double maValue; double mgValue; double mlValue;
//
//            for( int i = 0; i < N_SAMPLES ; i++ ) {
//                maValue = Math.sqrt(Math.pow(ax.get(i), 2) + Math.pow(ay.get(i), 2) + Math.pow(az.get(i), 2));
//                mlValue = Math.sqrt(Math.pow(lx.get(i), 2) + Math.pow(ly.get(i), 2) + Math.pow(lz.get(i), 2));
//                mgValue = Math.sqrt(Math.pow(gx.get(i), 2) + Math.pow(gy.get(i), 2) + Math.pow(gz.get(i), 2));
//
//                ma.add((float)maValue);
//                ml.add((float)mlValue);
//                mg.add((float)mgValue);
//            }

            data.addAll(ax.subList(0, N_SAMPLES));
            data.addAll(ay.subList(0, N_SAMPLES));
            data.addAll(az.subList(0, N_SAMPLES));

            data.addAll(gx.subList(0, N_SAMPLES));
            data.addAll(gy.subList(0, N_SAMPLES));
            data.addAll(gz.subList(0, N_SAMPLES));

//            data.addAll(lx.subList(0, N_SAMPLES));
//            data.addAll(ly.subList(0, N_SAMPLES));
//            data.addAll(lz.subList(0, N_SAMPLES));
//
//
//            data.addAll(ma.subList(0, N_SAMPLES));
//            data.addAll(ml.subList(0, N_SAMPLES));
//            data.addAll(mg.subList(0, N_SAMPLES));

            results = classifier.predictProbabilities(toFloatArray(data));

            float max = -1;
            int idx = -1;
            for (int i = 0; i < results.length; i++) {
                if (results[i] > max) {
                    idx = i;
                    max = results[i];
                }
            }

            setProbabilities();
            setRowsColor(idx);

            ax.clear(); ay.clear(); az.clear();
//            lx.clear(); ly.clear(); lz.clear();
            gx.clear(); gy.clear(); gz.clear();
//            ma.clear(); ml.clear(); mg.clear();
        }
    }

    private void setProbabilities() {
        breastTextView.setText(Float.toString(round(results[0], 2)));
        backTextView.setText(Float.toString(round(results[1], 2)));
        restTextView.setText(Float.toString(round(results[2], 2)));
    }

    private void setRowsColor(int idx) {
        breastTableRow.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorTransparent, null));
        backTableRow.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorTransparent, null));
        restTableRow.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorTransparent, null));

        if(idx == 0)
            breastTableRow.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
        else if (idx == 1)
            backTableRow.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
        else if (idx == 2)
            restTableRow.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
    }
    private TextWatcher activityTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            InputID = userID.getText().toString();

            record.setEnabled(!InputID.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_STEM_1:
                Toast.makeText(this, "Stop key pressed", Toast.LENGTH_SHORT).show();
                finish();
                System.exit(0);
                return true;


        }

        return super.onKeyDown(keyCode, event);

    }
    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public void onInit(int status) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (results == null || results.length == 0) {
                    return;
                }
                float max = -1;
                int idx = -1;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] > max) {
                        idx = i;
                        max = results[i];
                    }
                }

                if(max > 0.50 && idx != prevIdx) {
                    textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null,
                            Integer.toString(new Random().nextInt()));
                    Status.setText(labels[idx]);
                    if(idx==0){
                        Breaststroke = 1;
                        Backstroke = 0;
                        Rest = 0;
                        Stat = labels[idx];
                    }
                    else if (idx ==1){
                        Breaststroke = 0;
                        Backstroke = 1;
                        Rest = 0;
                        Stat = labels[idx];
                    }
                    else if (idx ==2){
                        Breaststroke = 0;
                        Backstroke = 0;
                        Rest = 1;
                        Stat = labels[idx];
                    }
                    prevIdx = idx;
                }
            }
        }, 1000, 3000);
    }
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }
    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /** Prepares the UI for ambient mode. */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        /**
         * Updates the display in ambient mode on the standard interval. Since we're using a custom
         * refresh cycle, this method does NOT update the data in the display. Rather, this method
         * simply updates the positioning of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        /** Restores the UI to active (non-ambient) mode. */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }
}
