package com.jamal.aegistest2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


// line chart wali classes hain ye
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;
    private int colour_count=0;
    public double value_bluetooth_double[];
    public float value_bluetooth_float[];

    //variables to store the received BLE data
    public Vector<Double> ecg_raw= new Vector<Double>(0);

    public Vector<Double> acc_x_raw= new Vector<Double>(0);
    public Vector<Double> acc_y_raw= new Vector<Double>(0);
    public Vector<Double> acc_z_raw= new Vector<Double>(0);

    //ECG and Activity Feature Object
    public ECG ecg1 =  new ECG();
    public FeatureCalculator act1= new FeatureCalculator();
    boolean initial_connect=true; //this is for ecg
    boolean initial_connect_activity=true;  // this is for activity
    boolean call_ecg_functions=false;
    boolean call_act_functions=false;

    //Chart Variables
    LineData line_chart_data;
    LineDataSet line_chart_data_set;
    LineChart line_chart;
    ArrayList<String> labels = new ArrayList<String>();
    public Vector<Float> chart_vec = new Vector<Float>(0);
    public Vector<Float> chart_buffer = new Vector<Float>(0);

    private int state;

    private boolean scanStarted=false;
    private boolean scanning=false;
    private boolean connected=false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;

    private ImageButton logoButton;
    private Button enableBluetoothButton;
    private Button disableBluetoothButton;
    private Button scanButton;
    private TextView deviceInfoText;
    private TextView Data_received;
    private TextView Features;

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                    upgradeState(STATE_CONNECTING);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
                initial_connect=false;
                initial_connect_activity = false;
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Data_received = (TextView) findViewById(R.id.data_received);
        Features = (TextView) findViewById(R.id.features);

        //Line Chart Stuff
        line_chart = (LineChart) findViewById(R.id.line_chart);
        line_chart.setDescription("");
        YAxis leftAxis = line_chart.getAxisLeft();
        YAxis rightAxis = line_chart.getAxisRight();
        leftAxis.setAxisMaxValue(1024f);
        leftAxis.setAxisMinValue(0f);
        rightAxis.setAxisMaxValue(1024f);
        rightAxis.setAxisMinValue(0f);
        line_chart.setData(SeedData());
        line_chart.animateXY(2000,2000);
        line_chart.setDrawGridBackground(false);
        line_chart.invalidate();

        deviceInfoText = (TextView) findViewById(R.id.deviceInfo);

        // Logo Button
        logoButton = (ImageButton) findViewById(R.id.main_logo);
        logoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(colour_count==0){
                    logoButton.setImageResource(R.drawable.aegis_150px_red);
                    colour_count++;
                }
                else if(colour_count==1){
                    logoButton.setImageResource(R.drawable.aegis_150px_green);
                    colour_count++;
                }
                else if(colour_count==2){
                    logoButton.setImageResource(R.drawable.aegis_150px_blue);
                    colour_count=0;
                }
                else{
                    logoButton.setImageResource(R.drawable.aegis_150px_blue);
                    colour_count=0;
                }
            }
        });

        // Enable Bluetooth
        enableBluetoothButton = (Button) findViewById(R.id.enableBluetooth);
        enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetoothButton.setText(
                        bluetoothAdapter.enable() ? "Enabling bluetooth..." : "Enable failed!");
                enableBluetoothButton.setEnabled(false);
                logoButton.setImageResource(R.drawable.aegis_150px_red); //means not connected
            }
        });

        // Enable Bluetooth
        disableBluetoothButton = (Button) findViewById(R.id.disableBluetooth);
        disableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.stopLeScan(MainActivity.this);
                disableBluetoothButton.setText(
                        bluetoothAdapter.disable() ? "Disabling bluetooth..." : "Disable failed!");
                if(!bluetoothAdapter.isEnabled()){
                    disableBluetoothButton.setEnabled(false);
                    enableBluetoothButton.setEnabled(true);
                    logoButton.setImageResource(R.drawable.aegis_150px_red); //means not connected
                    scanStarted=false;
                    scanning=false;
                }
            }
        });

        // Find Device and connect to it
        scanButton = (Button) findViewById(R.id.scan);
        scanButton.setEnabled(false);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothAdapter.isEnabled()){
                    scanStarted = true;
                    scanButton.setText("Scanning...");
                    bluetoothAdapter.startLeScan(new UUID[]{RFduinoService.UUID_SERVICE}, MainActivity.this);
                    logoButton.setImageResource(R.drawable.aegis_150px_blue); //means not connected
                }

            }
        });

        // Send
        /*valueEdit = (EditData) findViewById(R.id.value);
        valueEdit.setImeOptions(EditorInfo.IME_ACTION_SEND);
        valueEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendValueButton.callOnClick();
                    return true;
                }
                return false;
            }
        });

        sendZeroButton = (Button) findViewById(R.id.sendZero);
        sendZeroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rfduinoService.send(new byte[]{0});
            }
        });

        sendValueButton = (Button) findViewById(R.id.sendValue);
        sendValueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rfduinoService.send(valueEdit.getData());
            }
        });*/
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    protected void onStop() {
        super.onStop();

        bluetoothAdapter.stopLeScan(this);

        unregisterReceiver(scanModeReceiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
    }

    private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        state = newState;
        updateUi();
    }

    private void updateUi() {
        // Enable Bluetooth
        boolean on = state > STATE_BLUETOOTH_OFF;
        enableBluetoothButton.setEnabled(!on);
        disableBluetoothButton.setEnabled(on);
        enableBluetoothButton.setText(on ? "Bluetooth enabled" : "Enable Bluetooth");
        disableBluetoothButton.setText(!on ? "Bluetooth disabled" : "Disable Bluetooth");
        scanButton.setEnabled(on);

        // Scan
        if (scanStarted && scanning) {
            //scanStatusText.setText("Scanning...");
            //scanButton.setText("Stop Scan");
            scanButton.setEnabled(true);
        } else if (scanStarted) {
            //scanStatusText.setText("Scan started...");
            scanButton.setEnabled(false);
        } else {
            //scanStatusText.setText("");
            scanButton.setText("SCAN FOR AEGIS");
            scanButton.setEnabled(true);
        }

        // Connect
        connected = false;
        /*String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connected = true;
            connectionText = "Connected";
        }
        connectionStatusText.setText(connectionText);*/

        // Send
    }

    private void addData(byte[] data) {

        value_bluetooth_double = HexAsciiHelper.bytestodouble(data);
        Data_received.setText("Data Received: " + HexAsciiHelper.bytesToAsciiMaybe(data)+ "   "+String.valueOf(acc_x_raw.size()));

        if(value_bluetooth_double.length == 5 ){
            ecg_raw.addElement(value_bluetooth_double[0]);
            acc_x_raw.addElement(value_bluetooth_double[1]);
            acc_y_raw.addElement(value_bluetooth_double[2]);
            acc_z_raw.addElement(value_bluetooth_double[3]);
            /*
            if(value_bluetooth_double[4]==4){
                acc_x_raw.addElement(value_bluetooth_double[1]);
                acc_y_raw.addElement(value_bluetooth_double[2]);
                acc_z_raw.addElement(value_bluetooth_double[3]);
            }*/
        }
        /*
        if(value_bluetooth_double.length == 4){
            ecg_raw.addElement(value_bluetooth_double[0]);
            acc_x_raw.addElement(value_bluetooth_double[1]);
            acc_y_raw.addElement(value_bluetooth_double[2]);
            acc_z_raw.addElement(value_bluetooth_double[3]);
        }
        else if (value_bluetooth_double.length == 3){
            ecg_raw.addElement(value_bluetooth_double[0]);
            acc_x_raw.addElement(value_bluetooth_double[1]);
            acc_y_raw.addElement(value_bluetooth_double[2]);
        }
        else if (value_bluetooth_double.length == 2 ){
            ecg_raw.addElement(value_bluetooth_double[0]);
            acc_x_raw.addElement(value_bluetooth_double[1]);
        }
        else if (value_bluetooth_double.length == 1 ){
            ecg_raw.addElement(value_bluetooth_double[0]);
        }*/

        value_bluetooth_float = HexAsciiHelper.bytestofloat(data);
        chart_vec.addElement(value_bluetooth_float[0]);

        //when connected for  first time fill complete buffer then call method
        if(ecg_raw.size()==1500 && initial_connect==true){
            ecg1.load_data(ecg_raw);
            call_ecg_functions=true;
            for (int l=0;l<250;l++){
                ecg_raw.remove(l);
            }
            initial_connect=false;
        }

        //after first run update every few hundered samples
        if(ecg_raw.size()==1500 && initial_connect==false){
            ecg1.update_data(ecg_raw);
            call_ecg_functions=true;
            for (int l=0;l<250;l++){
                ecg_raw.remove(l);
            }
        }



        if(acc_x_raw.size()==128 && initial_connect_activity==true){
            act1.load_data(acc_x_raw, acc_y_raw, acc_z_raw);
            call_act_functions=true;
            for (int l=0;l<25;l++){
                acc_x_raw.remove(l);
                acc_y_raw.remove(l);
                acc_z_raw.remove(l);
            }
            initial_connect_activity=false;
        }

        if(acc_x_raw.size()==128 && initial_connect_activity==false){
            act1.update_data(acc_x_raw, acc_y_raw, acc_z_raw);
            call_act_functions=true;
            for (int l=0;l<25;l++){
                acc_x_raw.remove(l);
                acc_y_raw.remove(l);
                acc_z_raw.remove(l);
            }
        }



        //Threaded process to calculate the R-Peak locations in the ECG object that was created
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (call_ecg_functions == true) {
                    ecg1.r_peaks();
                    //ecg1.qrs_i; <----- This vector has the R peak locations
                    //ecg1.qrs_c; <----- This vector has the R peak amplitudes
                    ecg1.get_RR_BPM(); // <---- This calculates the avg RR interval and BPM from results of r_peaks
                    //ecg1.avg_RR_samp  <---- This 'type double' has the RR interval inters of samples
                    //ecg1.avg_RR_msec  <---- This 'type double' has the RR interval interms of milliseconds
                    //ecg1.BPM;         <---- This 'type double' has the Beats per minute and is public
                    call_ecg_functions = false;
                }

                if (call_act_functions == true) {
                    act1.get_features();
                    Features.setText("Features: " + String.valueOf(act1.features[0]) + "  " + String.valueOf(act1.features[1]));
                    call_ecg_functions = false;
                }
                ///////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
                // call the SMS function HERE
                ///////////////////////////////////////////////////////////////
                //////////////////////////////////////////////////////////////
            }
        });


        // 100 values per second hence update every second = update after buffer has length of 100
        // for seamless update go above 25 Hz refresh rate which means we update the vector at
        // 100/25 = 4 at min
        // ideal rate would be 30 Hz i.e update after 3.33 ~ 3 values are received this would give
        // a refresh rate of 33 Hz
        if(chart_vec.size()==2){
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateLineChart(chart_vec,2);
                    chart_vec.clear();
                }
            });
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;
        scanning=true;

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceInfoText.setText(BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord));
                updateUi();
            }
        });
        if( !(bluetoothDevice.getName().equals("AEGIS"))){
            bluetoothAdapter.startLeScan(new UUID[]{RFduinoService.UUID_SERVICE}, MainActivity.this);
        }
        else{
            Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
            bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
            connected=true;
            scanButton.setText("Connected to AEGIS");
            logoButton.setImageResource(R.drawable.aegis_150px_green); // means connected
        }
    }

    //Chart wala stuff
    protected LineData SeedData() {

        ArrayList<Entry> entries = new ArrayList<>();

        int k=0;
        for(int i=0;i<200 ;i++){
            chart_buffer.addElement((float)k);
            entries.add(new Entry(chart_buffer.elementAt(i), i));
            labels.add(String.valueOf(i));
            k++;
            if(k==10){
                k=0;
            }
        }
        line_chart_data_set = new LineDataSet(entries, "ECG Reading");

        line_chart_data_set.setLineWidth(2.5f);
        line_chart_data_set.setDrawCircles(false);
        line_chart_data_set.setColor(ColorTemplate.VORDIPLOM_COLORS[2]);
        line_chart_data = new LineData(labels, line_chart_data_set);
        return line_chart_data;
    }

    protected void updateLineChart(Vector<Float> data, int len) {
        line_chart_data_set.clear();
        if(data.size() == len ){
            for(int i=0; i<len;i++){
                chart_buffer.remove(0);
                chart_buffer.addElement(data.elementAt(i));
            }
            for(int i=0; i<200;i++){
                line_chart_data_set.addEntry(new Entry(chart_buffer.elementAt(i),i));
            }
        }
        //int random_colour = (int) (Math.random()*5);
        //line_chart_data_set.setColor(ColorTemplate.VORDIPLOM_COLORS[random_colour]);

        line_chart.notifyDataSetChanged();  // notification of update
        line_chart.invalidate(); // refresh graph
    }

    protected void updateLineChart_single(float data) {
        line_chart_data_set.clear();
        chart_buffer.remove(0);
        chart_buffer.addElement(data);
        for(int i=0; i<200;i++){
            line_chart_data_set.addEntry(new Entry(chart_buffer.elementAt(i),i));
        }
        //int random_colour = (int) (Math.random()*5);
        //line_chart_data_set.setColor(ColorTemplate.VORDIPLOM_COLORS[random_colour]);
        line_chart.notifyDataSetChanged();  // notification of update
        line_chart.invalidate(); // refresh graph
    }
}