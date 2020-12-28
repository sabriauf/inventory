package lk.sabri.inventory.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bxl.config.editor.BXLConfigLoader;

import java.util.ArrayList;
import java.util.Set;

import lk.sabri.inventory.R;
import lk.sabri.inventory.activity.invoice.InvoiceActivity;
import lk.sabri.inventory.util.Constants;

public class PrinterConnectActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnTouchListener, View.OnClickListener {
    private static final int REQUEST_PERMISSION = 0;
    private final String DEVICE_ADDRESS_START = " (";
    private final String DEVICE_ADDRESS_END = ")";

    private final ArrayList<CharSequence> bondedDevices = new ArrayList<>();
    private ArrayAdapter<CharSequence> arrayAdapter;
    private SharedPreferences preferences;

    private int portType = BXLConfigLoader.DEVICE_BUS_BLUETOOTH;
    private String logicalName = "";
    private String address = "";

    private ListView listView;
    private EditText editTextIPAddress;
    private LinearLayout layoutAll;
    private TextView textViewBluetooth;

    private Button btnPrinterOpen;

    private ProgressBar mProgressLarge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_connect);

        preferences = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);

        layoutAll = findViewById(R.id.LinearLayout1);
        LinearLayout layoutIPAddress = findViewById(R.id.LinearLayout3);
        layoutIPAddress.setVisibility(View.GONE);

        textViewBluetooth = findViewById(R.id.textViewBluetoothList);
        editTextIPAddress = findViewById(R.id.editTextIPAddr);
        editTextIPAddress.setText("192.168.0.1");

        btnPrinterOpen = findViewById(R.id.btnPrinterOpen);
        btnPrinterOpen.setOnClickListener(this);

        mProgressLarge = findViewById(R.id.progressBar1);
        mProgressLarge.setVisibility(ProgressBar.GONE);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, bondedDevices);
        listView = findViewById(R.id.listViewPairedDevices);
        listView.setAdapter(arrayAdapter);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(this);
        listView.setOnTouchListener(this);

        Spinner modelList = findViewById(R.id.spinnerModelList);

        ArrayAdapter modelAdapter = ArrayAdapter.createFromResource(this, R.array.modelList, android.R.layout.simple_spinner_dropdown_item);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelList.setAdapter(modelAdapter);
        modelList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                logicalName = (String) parent.getItemAtPosition(position);
                preferences.edit().putString(Constants.PREFERENCE_DEVICE_NAME, logicalName).apply();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        portType = BXLConfigLoader.DEVICE_BUS_BLUETOOTH;
        logicalName = preferences.getString(Constants.PREFERENCE_DEVICE_NAME, "");
        address = preferences.getString(Constants.PREFERENCE_BLE_ADDRESS, "");

        int pos = 0;
        int i = 0;
        for (String model : getResources().getStringArray(R.array.modelList)) {
            if (model.equals(logicalName)) {
                pos = i;
                listView.setVisibility(View.GONE);
                textViewBluetooth.setVisibility(View.GONE);
                break;
            }
            i++;
        }

        modelList.setSelection(pos);
        setPairedDevices();

//        if (pos != 0 && !logicalName.equals("")) {
//            btnPrinterOpen.setEnabled(false);
//            layoutAll.setVisibility(View.INVISIBLE);
//            openPrinter(0);
//        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
//                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
//                }
//            }
//        }
    }

    private void setPairedDevices() {
        bondedDevices.clear();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedDeviceSet = bluetoothAdapter.getBondedDevices();

        int pos = 0;
        for (BluetoothDevice device : bondedDeviceSet) {
            String name = device.getName() + DEVICE_ADDRESS_START + device.getAddress() + DEVICE_ADDRESS_END;
            if(name.contains(address)) {
                arrayAdapter.notifyDataSetChanged();
                listView.setItemChecked(pos, true);
            }
            bondedDevices.add(name);
            pos++;
        }

        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged();
        }
    }

//    private void setBleDevices() {
//        mHandler.obtainMessage(0).sendToTarget();
//        BXLBluetoothLE.setBLEDeviceSearchOption(5, 1);
//        new searchBLEPrinterTask().execute();
//    }
//
//    private void setNetworkDevices() {
//        mHandler.obtainMessage(0).sendToTarget();
//        BXLNetwork.setWifiSearchOption(5, 1);
//        new searchNetworkPrinterTask().execute();
//    }

//    private class searchNetworkPrinterTask extends AsyncTask<Integer, Integer, Set<CharSequence>> {
//        private String message;
//
//        @Override
//        protected void onPreExecute() {
//        }
//
//        @Override
//        protected void onPostExecute(Set<CharSequence> NetworkDeviceSet) {
//            bondedDevices.clear();
//
//            String[] items;
//            if (NetworkDeviceSet != null && !NetworkDeviceSet.isEmpty()) {
//                items = NetworkDeviceSet.toArray(new String[NetworkDeviceSet.size()]);
//                for (int i = 0; (items != null) && (i < items.length); i++) {
//                    bondedDevices.add(items[i]);
//                }
//            } else {
//                Toast.makeText(getApplicationContext(), "Can't found network devices. ", Toast.LENGTH_SHORT).show();
//            }
//
//            if (arrayAdapter != null) {
//                arrayAdapter.notifyDataSetChanged();
//            }
//
//            if (message != null) {
//                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
//            }
//
//            mHandler.obtainMessage(1).sendToTarget();
//        }
//
//        @Override
//        protected Set<CharSequence> doInBackground(Integer... params) {
//            try {
//                return BXLNetwork.getNetworkPrinters(PrinterConnectActivity.this, BXLNetwork.SEARCH_WIFI_ALWAYS);
//            } catch (NumberFormatException | JposException e) {
//                message = e.getMessage();
//                return new HashSet<>();
//            }
//        }
//    }

//    private class searchBLEPrinterTask extends AsyncTask<Integer, Integer, Set<BluetoothDevice>> {
//        private String message;
//
//        @Override
//        protected void onPreExecute() {
//        }
//
//        @Override
//        protected void onPostExecute(Set<BluetoothDevice> bluetoothDeviceSet) {
//            bondedDevices.clear();
//
//            if (bluetoothDeviceSet.size() > 0) {
//                for (BluetoothDevice device : bluetoothDeviceSet) {
//                    bondedDevices.add(device.getName() + DEVICE_ADDRESS_START + device.getAddress() + DEVICE_ADDRESS_END);
//                }
//            } else {
//                Toast.makeText(getApplicationContext(), "Can't found BLE devices. ", Toast.LENGTH_SHORT).show();
//            }
//
//            if (arrayAdapter != null) {
//                arrayAdapter.notifyDataSetChanged();
//            }
//
//            if (message != null) {
//                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
//            }
//
//            mHandler.obtainMessage(1).sendToTarget();
//        }
//
//        @Override
//        protected Set<BluetoothDevice> doInBackground(Integer... params) {
//            try {
//                return BXLBluetoothLE.getBLEPrinters(PrinterConnectActivity.this, BXLBluetoothLE.SEARCH_BLE_ALWAYS);
//            } catch (NumberFormatException | JposException e) {
//                message = e.getMessage();
//                return new HashSet<>();
//            }
//        }
//    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP)
            listView.requestDisallowInterceptTouchEvent(false);
        else listView.requestDisallowInterceptTouchEvent(true);
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String device = ((TextView) view).getText().toString();
//        if (portType == BXLConfigLoader.DEVICE_BUS_WIFI) {
//            editTextIPAddress.setText(device);
//            address = device;
//        } else {
        address = device.substring(device.indexOf(DEVICE_ADDRESS_START) + DEVICE_ADDRESS_START.length(), device.indexOf(DEVICE_ADDRESS_END));
        preferences.edit().putString(Constants.PREFERENCE_BLE_ADDRESS, address).apply();
//        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnPrinterOpen:
                if (!address.equals("")) {
                    btnPrinterOpen.setEnabled(false);
                    layoutAll.setVisibility(View.INVISIBLE);
                    mProgressLarge.setVisibility(ProgressBar.GONE);
                    openPrinter(0);
                } else
                    Toast.makeText(PrinterConnectActivity.this, "Please select the printer address", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void openPrinter(final int arg1) {
        mHandler.obtainMessage(0).sendToTarget();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (portType == BXLConfigLoader.DEVICE_BUS_WIFI) {
                    address = editTextIPAddress.getText().toString();
                }

                if (InvoiceActivity.getPrinterInstance().printerOpen(portType, logicalName, address, true)) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    mHandler.obtainMessage(1, arg1, 0, "Fail to connect the printer!").sendToTarget();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnPrinterOpen.setEnabled(true);
                            mProgressLarge.setVisibility(View.GONE);
                            layoutAll.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.VISIBLE);
                            textViewBluetooth.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(getApplicationContext(), "permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public final Handler mHandler = new Handler(new Handler.Callback() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mProgressLarge.setVisibility(ProgressBar.VISIBLE);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    break;
                case 1:
                    if (msg.arg1 < 2) {
                        openPrinter(++msg.arg1);
                    } else {
                        btnPrinterOpen.setEnabled(true);
                        layoutAll.setVisibility(View.VISIBLE);
                        String data = (String) msg.obj;
                        if (data != null && data.length() > 0) {
                            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
                        }
                        mProgressLarge.setVisibility(ProgressBar.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                    break;
            }
            return false;
        }
    });
}