package com.felhr.serialportexamplesync;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_SELECT_CODE = 0;
    List<String> lines = new ArrayList<>();
    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private MyHandler mHandler;
    TextView label;
    TextView currentCardLbl;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    private String TAG = "Niranjan";
    private String design_file;
    private int total_lines=0;
    private MyAsyn task;
    private Button BtnPause;
    private Button runButton;
    private boolean CTS_line = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this);
        runButton = (Button) findViewById(R.id.btnRun);
        label = (TextView) findViewById(R.id.fileAttributesLabel);
        currentCardLbl = (TextView) findViewById(R.id.currentcardTxt);
        Button choosefileButton = (Button) findViewById(R.id.choosefileBtn);
        Button BtnWeaveUp = (Button) findViewById(R.id.BtnWeaveUp);
        BtnWeaveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(task!=null){
                    task.weaveUp();
                }
            }
        });

        Button BtnWeaveDown= (Button) findViewById(R.id.BtnWeaveDown);
        BtnWeaveDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(task!=null){
                    task.weaveDown();
                }
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            runButton.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View v) {
//                    try (BufferedReader br = new BufferedReader(new FileReader(design_file))) {
//                        String line;
//                        byte[] bytes_to_send = new byte[3];
//                        int c = 0;
//
//                        while ((line = br.readLine()) != null) {
//                            byte[] b = line.getBytes();
//                            c++;
//                            if (c > 2) {
//                                currentCardLbl.setText(String.format("Current Card: %d", c - 2));
//                                for (int h = 0; h < b.length / 4; h = h + 3) {
//                                    System.arraycopy(b, h, bytes_to_send, 0, 3);
//                                    usbService.write(bytes_to_send);
//                                    try {
//                                        Thread.sleep(10);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//
//                        }
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }


                       if(task == null) {
                            // --- create a new task --
                            task = new MyAsyn();
                            task.execute(100);
                            runButton.setText("PAUSE");
                        }
                        else if(task.getStatus() == AsyncTask.Status.FINISHED){
                            // --- the task finished, so start another one --
                            task = new MyAsyn();
                            task.execute(100);
                            runButton.setText("PAUSE");
                        }
                        else if(task.getStatus() == AsyncTask.Status.RUNNING && !task.getPause()){
                            // --- the task is running, call pause function --
                            task.pauseMyTask();
                            runButton.setText("RUN");
                        }
                        else {
                            // --- task paused, so wake up him --
                            task.wakeUp();
                            runButton.setText("PAUSE");
                        }

                }
            });
        }

        choosefileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    design_file = FileUtils.getPath(this.getApplicationContext(),uri);
                    lines.clear();
                    try (BufferedReader br = new BufferedReader(new FileReader(design_file))) {
                        String line;
                        int line_length=0;
                        int count=0;

                        while ((line = br.readLine()) != null) {
                            if(line.contains("$")||line.contains("EOF")){

                            }
                            else lines.add(line);
                        }
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                String pa = design_file.substring(design_file.lastIndexOf("/")+1,design_file.length());
                                label.setText(String.format("File name = %s\nTotal cards = %d", pa,lines.size()));
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    File f = new File(patha);
//                    String abp = f.getAbsolutePath();
                    // Get the path
//                    String path = null;
//                    try {
//                        path = FileUtils.getPath(this, uri);
//                    } catch (URISyntaxException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d(TAG, "File Path: " + path);
                    // Get the file instance
                    // File file = new File(path);
                    // Initiate the upload
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a design file"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    Toast.makeText(mActivity.get(), "Data recieved: "+data,Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    mActivity.get().CTS_line = true;
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;
                    Toast.makeText(mActivity.get(), "Sync Data recieved: "+buffer,Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    class MyAsyn extends AsyncTask<Integer, Integer, Void> {
        boolean resume = true;
        boolean pause = false;
        int counter =0;
        private String WATCH_DOG = "barabulka";

        @Override
        protected Void doInBackground(Integer... params) {
            counter = 2;

            while (resume) {
                // --- show progress in text field --
                publishProgress(counter);
                if(lines.size()==0){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Please choose a design file", Toast.LENGTH_SHORT).show();
                        }
                    });

                    return null;
                }
                byte[] bytes=null, bytes_to_send = new byte[3];
//                for(int p=0;p<lines.size();p++){
                    if(lines.get(counter).contains("$")||lines.get(counter).contains("EOF")){

                    }else {
                        bytes = lines.get(counter).getBytes();
                        if (bytes == null) return null;
                        for (int h = 0; h < bytes.length / 4; h = h + 3) {
                            System.arraycopy(bytes, h, bytes_to_send, 0, 3);
                            usbService.write(bytes_to_send);
                            CTS_line = false;
                        }
                    }




                // --- when the counter reaches the end, change the loop flag --
                resume = (counter++ == lines.size()-1) ? false : true;
                try {
                    // --- put here any time expensive code --
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (pause) {
                    synchronized (WATCH_DOG) {
                        try {
                            // --- set text field status to 'paused' --
//                            publishProgress(-1);
                            // --- sleep tile wake-up method will be called --
                            WATCH_DOG.wait();

                        } catch (InterruptedException e) {e.printStackTrace();
                        }
                        pause = false;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // --- update text with percentage --
            currentCardLbl.setText("Current card = "+values[0]);
            if(values[0] == -1){
                // --- show the 'pause' sight --
//                currentCardLbl.setText("PAUSED");
                runButton.setText("RUN");
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            currentCardLbl.setText("FINISHED");
            counter = 0;
            runButton.setText("RUN");
        }

        /**Pause task for a while*/
        public void pauseMyTask() {
            pause = true;
        }

        public void weaveUp() {
            if(counter<lines.size()-1) {
                counter = counter + 1;
            }
            publishProgress(counter);
        }
        public void weaveDown() {
            if(counter>0){
                counter = counter - 1;
            }
            publishProgress(counter);
        }

        /**Wake up task from sleeping*/
        public void wakeUp() {
            synchronized(WATCH_DOG){
                WATCH_DOG.notify();
            }
        }

        /**Get a loop-flag*/
        public boolean getPause() {
            return pause;
        }

    }
    }
