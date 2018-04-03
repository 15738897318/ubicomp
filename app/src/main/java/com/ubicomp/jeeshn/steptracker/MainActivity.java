package com.ubicomp.jeeshn.steptracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mStepCounter;

    GraphView rawGraph = null;
    GraphView smoothGraph = null;
    LineGraphSeries<DataPoint> rawSeries;
    LineGraphSeries<DataPoint> smoothSeries;

    double[] gravity = {0, 0, 0};

    int readingCount = 0;
    int stepCount = 0;
    int calculatedStepCount = 0;
    int initialCounterValue = 0;

    TextView txtStepCount;
    TextView txtCalculatedStepCount;
    int StepDetected = 1;
    int Idle = 0;
    int state;
    int previousstate;

    long streakStartTime;
    long streakPrevTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStepCount = findViewById((R.id.txtStepCount));
        txtCalculatedStepCount = findViewById((R.id.txtCalculatedStep));

        rawGraph = findViewById(R.id.graphRaw);
        smoothGraph = findViewById(R.id.graphSmooth);

        rawSeries = new LineGraphSeries<DataPoint>();
        this.InitializeGraphControl(rawGraph,rawSeries,20,80);

        smoothSeries = new LineGraphSeries<DataPoint>();
        this.InitializeGraphControl(smoothGraph,smoothSeries,5,80);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepCounter  = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        StartSensor();
    }

    void InitializeGraphControl(
            GraphView graph, LineGraphSeries<DataPoint> series, double maxY, double maxX)
    {
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(3);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(maxY);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(maxX);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        graph.addSeries(series);
    }

    private void StartSensor() {
        mSensorManager.registerListener(
                this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(
                this,
                mStepCounter,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                this.CalculateMovement(event);
                break;
            case Sensor.TYPE_STEP_COUNTER:
                this.IncrementStepCount(event);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(
                this,
                mStepCounter,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // Calculates the horizontal , vertical and in space movement based on Accelerometer data.
    void CalculateMovement(SensorEvent event) {
        // event object contains values of acceleration, read those
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        double rawd = Math.sqrt(x * x + y * y + z * z);

        readingCount= readingCount+1;

        this.PlotGraph(rawd,readingCount, rawSeries);

        final double alpha = 0.8; // constant for our filter below

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        x = event.values[0] - gravity[0];
        y = event.values[1] - gravity[1];
        z = event.values[2] - gravity[2];

        double smoothd = Math.sqrt(x * x + y * y + z * z);

        if(smoothd>=1.5)
        {
            this.state = this.StepDetected;
            if(this.previousstate!=this.state)
            {
                streakStartTime = System.currentTimeMillis();
                if ((streakStartTime - streakPrevTime) <= 250f) {
                    streakPrevTime = System.currentTimeMillis();
                    return;
                }
                streakPrevTime = streakStartTime;
                calculatedStepCount = calculatedStepCount+1;
                txtCalculatedStepCount.setText((String.valueOf(calculatedStepCount)));
            }
        }
        else
        {
            this.state = this.Idle;
            this.previousstate = this.state;
        }

        this.PlotGraph(smoothd,readingCount, smoothSeries);
    }

    void PlotGraph(double reading, int readingCount,  LineGraphSeries<DataPoint> series)
    {
        series.appendData(new DataPoint(readingCount,reading), true,20000);
    }

    void IncrementStepCount(SensorEvent event)
    {
        if(initialCounterValue==0)
        {
            initialCounterValue = (int) event.values[0];
        }
        if(event.values[0]>0) {
            stepCount = (int)event.values[0] - initialCounterValue;
            txtStepCount.setText(String.valueOf(stepCount));
        }
    }
}
