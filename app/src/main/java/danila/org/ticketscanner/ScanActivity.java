package danila.org.ticketscanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ScanActivity extends AppCompatActivity {

    private static final String CHECK_BARCODE_URL = "";
    private SurfaceView camera;
    private TextView statusMessage;
    private Vibrator vibrator;
    private SoundPool soundPool;
    private int successSound, failedSound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        String titleText = getIntent().getStringExtra("event-name");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(titleText);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        statusMessage = findViewById(R.id.status_message);
        camera = findViewById(R.id.camera);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        successSound = loadSound("success.mp3");
        //failedSound = loadSound("");


        createCameraSource();
    }

    private int loadSound(String fileName) {
        AssetFileDescriptor afd;
        try {
            afd = getAssets().openFd(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Не могу загрузить файл " + fileName,
                    Toast.LENGTH_SHORT).show();
            return -1;
        }
        return soundPool.load(afd, 1);
    }

    private void createCameraSource() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        CameraSource cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
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

        final Barcode[] previousCode = {null};
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() == 1) {
                    Barcode currentCode = barcodes.valueAt(0);
                    if (previousCode[0] == null || !currentCode.displayValue.equals(previousCode[0].displayValue)) {
                        vibrator.vibrate(300);
                        createRequest(currentCode.displayValue);
                        previousCode[0] = currentCode;
                    }
                }
            }
        });
    }

    private void createRequest(String barcode) {
        soundPool.play(successSound, 1, 1, 1, 0, 1);
        System.out.println(barcode.toUpperCase());
    }

    /*private void createRequest(String barcode) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject object = new JSONObject();
        try {
            object.put("barcode", barcode);
            object.put("event", getIntent().getStringExtra("event-name"));
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
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                if (response.get("").equals("")) {
                                    statusMessage.setBackgroundColor(getResources().getColor(R.color.colorDenied));
                                    System.out.println();
                                } else {
                                    statusMessage.setBackgroundColor(getResources().getColor(R.color.colorAllow));
                                    System.out.println();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                null
        );
        queue.add(request);
    }*/
}

