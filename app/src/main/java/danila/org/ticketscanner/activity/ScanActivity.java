package danila.org.ticketscanner.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import danila.org.ticketscanner.R;
import danila.org.ticketscanner.util.service.BarcodeProcessor;
import danila.org.ticketscanner.util.service.RequestService;

public class ScanActivity extends AppCompatActivity {

    private static final String CHECK_BARCODE_URL = "http://tickets.docudays.org.ua/v1/mobile_app/usher/check_babrcode";
    private final static String TAG = "ticketScanner";

    private SurfaceView cameraView;

    private EditText manuallyEdit;
    private Button manuallySubmit;
    private View statusMessage;
    private ImageView statusMessagePictureView;
    private TextView statusMessageTextView;
    private Drawable statusMessageSuccessPicture;
    private Drawable statusMessageDeniedPicture;
    private Drawable statusMessageInfoPicture;
    private Drawable statusMessageLoadingPicture;

    private SoundPool soundPool;
    private int successSound, failedSound;

    private AtomicBoolean waitingForResponse = new AtomicBoolean(false);

    private BarcodeProcessor barcodeProcessor;
    private RequestService requestService;

    private static  final int FOCUS_AREA_SIZE= 70;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String titleText = getIntent().getStringExtra("event-name");
            String subtitleText = getIntent().getStringExtra("event-meta");
            actionBar.setTitle(titleText);
            actionBar.setSubtitle(subtitleText);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        successSound = loadSound("success.mp3");
        failedSound = loadSound("failed.mp3");

        cameraView = findViewById(R.id.camera);
        manuallyEdit = findViewById(R.id.manually_edit);
        manuallySubmit = findViewById(R.id.manually_submit);
        manuallySubmit.setOnClickListener(this::onManuallySubmit);

        statusMessage = findViewById(R.id.status_message);
        statusMessageTextView = findViewById(R.id.status_message_text);
        statusMessagePictureView = findViewById(R.id.status_message_picture);
        statusMessageSuccessPicture = getResources().getDrawable(R.drawable.success);
        statusMessageDeniedPicture = getResources().getDrawable(R.drawable.denied);
        statusMessageInfoPicture = getResources().getDrawable(R.drawable.info);
        statusMessageLoadingPicture = getResources().getDrawable(R.drawable.proceeding);

        requestService = new RequestService(this);
        barcodeProcessor = new BarcodeProcessor(this, this::onBarcodeDetected);
        createCameraSource();
    }

    private void startShowLoading() {
        updateStatusMessage(getResources().getColor(R.color.colorYellow),
                "Обробка даних...",
                statusMessageLoadingPicture);
    }

    private void onBarcodeDetected(String barcode) {
        Log.d(TAG, "onBarcodeDetected - " + barcode);
        if (!waitingForResponse.get()) {
            Log.d(TAG, "setting request");
            waitingForResponse.set(true);
            startShowLoading();
            requestService.setRequest(createRequest(barcode));
        } else {
            Log.d(TAG, "previous operation is't finished yet, barcode ignored");
        }
    }

    private void onManuallySubmit(View view) {
        Log.d(TAG, "onManuallySubmit");
        String code = manuallyEdit.getText().toString();
        if (TextUtils.isDigitsOnly(code) && !code.isEmpty()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(manuallyEdit.getWindowToken(), 0);
                manuallyEdit.setText("");
                manuallyEdit.clearFocus();
            }
            vibrate(false);
            onBarcodeDetected(code);
        } else {
            vibrate(true);
            Toast.makeText(this,
                    "Код невалідний",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void vibrate(boolean error) {
        try {
            if (error) {
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(new long[]{0, 200, 50, 200}, -1);
            } else {
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
            }
        } catch (NullPointerException e) {
            Toast.makeText(this,
                    "Помилка ініціалізації вібратора",
                    Toast.LENGTH_LONG)
                    .show();
            throw new RuntimeException(e);
        }
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
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.CODE_128 | Barcode.EAN_8)
                .build();
        CameraSource cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(15.0f)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(ScanActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(cameraView.getHolder());

                        /*Camera camera = getCamera(cameraSource);
                        Camera.Parameters params = camera.getParameters();
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                        camera.setParameters(params);*/
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

        cameraView.setOnTouchListener((v, e) -> {
            focusOnTouch(cameraSource, e);
            return false;
        });

        Log.d(TAG, "detector status " + String.valueOf(barcodeDetector.isOperational()));
        if (barcodeDetector.isOperational()) {
            barcodeDetector.setProcessor(barcodeProcessor);
        } else {
            Log.d(TAG, "barcode detector is't operational");
            Toast.makeText(getApplicationContext(),
                    "Детектор кодів недоступний, переконайтеся що Google Services встановлено",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void focusOnTouch(CameraSource source, MotionEvent event) {
        Camera mCamera = getCamera(source);
        if (mCamera != null ) {

            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0){
                Log.i(TAG,"fancy !");
                Rect rect = calculateFocusArea(event.getX(), event.getY());

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);

                mCamera.setParameters(parameters);
                mCamera.autoFocus(mAutoFocusTakePictureCallback);
            } else {
                Log.d(TAG, "fucking metering areas");
                mCamera.autoFocus(mAutoFocusTakePictureCallback);
            }
        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / cameraView.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / cameraView.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }

    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = (success, camera) -> {
        if (success) {
            // do something...
            Log.d(TAG,"focused success!");
        } else {
            // do something...
            Log.d(TAG," focusing failed!");
        }
    };

    //do not call before the camera source will be started
    private Camera getCamera(CameraSource cameraSource) {

        Field[] declaredFields = CameraSource.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
        throw new RuntimeException("the camera does not available");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void updateStatusMessage(int colorResId, String message, Drawable picture) {
        runOnUiThread(() -> {
            if (statusMessage.getVisibility() != View.VISIBLE) {
                statusMessage.setVisibility(View.VISIBLE);
            }
            statusMessage.setBackgroundColor(colorResId);
            statusMessageTextView.setText(message);
            statusMessagePictureView.setImageDrawable(picture);
        });
    }

    private JsonObjectRequest createRequest(String barcode) {
        System.out.println("createRequest");
        JSONObject object = new JSONObject();
        try {
            object.put("screening_code", getIntent().getStringExtra("event-id"));
            object.put("barcode", barcode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "obj " + object.toString());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                CHECK_BARCODE_URL,
                object,
                (response) -> {
                    waitingForResponse.set(false);
                    Log.d(TAG, "got response");
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String status = response.getString("status");
                            String message = response.getString("description");
                            if (status.equals("ok")) {
                                Log.d(TAG, barcode + " status: ok");
                                soundPool.play(successSound, 1, 1, 1, 0, 1);
                                updateStatusMessage(getResources().getColor(R.color.colorAllow),
                                        "Квиток " + barcode + ":\n" + message,
                                        statusMessageSuccessPicture);
                            } else if (status.equals("error")) {
                                Log.d(TAG, barcode + " status: error");
                                soundPool.play(failedSound, 1, 1, 1, 0, 1);
                                updateStatusMessage(getResources().getColor(R.color.colorDenied),
                                        "Квиток " + barcode + ":\n" + message,
                                        statusMessageDeniedPicture);
                            } else if (status.equals("warning")) {
                                Log.d(TAG, barcode + " status: warning");
                                updateStatusMessage(getResources().getColor(R.color.colorYellow),
                                        "Квиток " + barcode + ":\n" + message,
                                        statusMessageSuccessPicture);
                            } else {
                                Log.d(TAG, barcode + " no status, message: " + response.toString());
                                updateStatusMessage(getResources().getColor(R.color.colorYellow),
                                        response.toString(),
                                        statusMessageInfoPicture);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, barcode + " " + e.getMessage());
                            updateStatusMessage(getResources().getColor(R.color.colorBlack),
                                    "Квиток " + barcode + "\nПомилка на боці сервера",
                                    statusMessageInfoPicture);
                            e.printStackTrace();
                        }
                    }
                    Log.d(TAG, "\n\n");
                },
                (error) -> {
                    waitingForResponse.set(false);
                    Log.d(TAG, barcode + " " + error.getMessage());
                    updateStatusMessage(getResources().getColor(R.color.colorBlack),
                            "Квиток " + barcode + "\nПомилка мережі",
                            statusMessageInfoPicture);
                }
        );
        Log.d(TAG, "request object created");
        return request;
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeProcessor.updatePreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem settingsItem = menu.findItem(R.id.app_bar_settings);
        menu.removeItem(R.id.app_bar_search);

        settingsItem.setOnMenuItemClickListener((e) -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        });
        return true;
    }

}