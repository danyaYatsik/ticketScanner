package danila.org.ticketscanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class ScanActivity extends AppCompatActivity {

    private static final String CHECK_BARCODE_URL = "http://tickets.docudays.org.ua/v1/mobile_app/usher/check_babrcode";
    private SurfaceView camera;
    private TextView statusMessage;
    private Vibrator vibrator;
    private SoundPool soundPool;
    private int successSound, failedSound;
    private final static String TAG = "moi";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        String titleText = getIntent().getStringExtra("event-name");
        String subtitleText = getIntent().getStringExtra("event-meta");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(titleText);
            actionBar.setSubtitle(subtitleText);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        statusMessage = findViewById(R.id.status_message);
        camera = findViewById(R.id.camera);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        successSound = loadSound("success.mp3");
        failedSound = loadSound("failed.mp3");

        createCameraSource();
    }

    private int loadSound(String fileName) {
        AssetFileDescriptor afd;
        try {
            afd = getAssets().openFd(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Помилка при завантаженні звукового файлу " + fileName,
                    Toast.LENGTH_LONG).show();
            return -1;
        }
        return soundPool.load(afd, 1);
    }

    private void createCameraSource() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        CameraSource cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .setRequestedFps(1.0f)
                .build();

        camera.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(ScanActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(camera.getHolder());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

            private String prevCode;

            @Override
            public void release() {

            }

            private void startTimeout(long timeout) {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                String currentCode = barcodes.valueAt(0).displayValue;
                Log.d(TAG, "code deected " + currentCode);
                if (!currentCode.equals(prevCode)) {
                    Log.d(TAG, "unique");
                    prevCode = currentCode;
                    vibrator.vibrate(200);
                    createRequest(detections.getDetectedItems().valueAt(0).displayValue);
                    sleep(1500);
                } else {
                    Log.d(TAG, "duplicate");
                    vibrator.vibrate(new long[]{0, 200, 50, 200}, -1);
                    sleep(1500);
                }
                Log.d(TAG, "\n");

            }

            private void sleep(long mills) {
                try {
                    Thread.sleep(mills);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void createRequest(String barcode) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject object = new JSONObject();
        try {
            object.put("screening_code", getIntent().getStringExtra("event-id"));
            object.put("barcode", barcode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                CHECK_BARCODE_URL,
                object,
                (response) -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            if (statusMessage.getVisibility() != View.VISIBLE) {
                                statusMessage.setVisibility(View.VISIBLE);
                            }
                            String status = response.getString("status");
                            String message = response.getString("description");
                            if (status.equals("ok")) {
                                soundPool.play(successSound, 1, 1, 1, 0, 1);
                                statusMessage.setBackgroundColor(getResources().getColor(R.color.colorAllow));
                                statusMessage.setText(message);
                            } else if (status.equals("error")) {
                                soundPool.play(failedSound, 1, 1, 1, 0, 1);
                                statusMessage.setBackgroundColor(getResources().getColor(R.color.colorDenied));
                                statusMessage.setText(message);
                            } else if (message != null) {
                                statusMessage.setText(message);
                            } else {
                                statusMessage.setText("Помилка при обробці даних");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                null
        );
        queue.add(request);
    }
}

/*Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        timeoutGone = true;
                        timer.cancel();

                    }
                };
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() == 1) {
                    String currentCode = barcodes.valueAt(0).displayValue;
                    Log.d(TAG, "code present");
                    if (!currentCode.equals(prevCode)) {
                        Log.d(TAG, "new code scaned " + currentCode);
                        vibrator.vibrate(200);
                        prevCode = currentCode;
                        timeoutGone = false;
                        createRequest(currentCode);
                        timer.schedule(task, 1500);
                    } else if (timeoutGone) {
                        Log.d(TAG, "the same code after timeut");
                        vibrator.vibrate(new long[]{0, 200, 50, 200}, -1);
                        timeoutGone = false;
                        timer.schedule(task, 1500);
                    } else
                        Log.d(TAG, "do nothing");
                }*/

