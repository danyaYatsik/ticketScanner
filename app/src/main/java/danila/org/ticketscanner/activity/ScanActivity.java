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
import android.support.annotation.NonNull;
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

    private BarcodeProcessor barcodeProcessor;
    private RequestService requestService;

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

        requestService = new RequestService(this, this::loading);
        barcodeProcessor = new BarcodeProcessor(this, this::onBarcodeDetected);
        createCameraSource();
    }

    private void loading() {
        updateStatusMessage(getResources().getColor(R.color.colorYellow),
                "Обробка даних...",
                statusMessageLoadingPicture);
    }

    private void onBarcodeDetected(String barcode) {
        requestService.setRequest(createRequest(barcode));
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
            requestService.setRequest(createRequest(code));
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
                .setRequestedPreviewSize(1024, 720)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(ScanActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(cameraView.getHolder());
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

        if (barcodeDetector.isOperational()) {
            barcodeDetector.setProcessor(barcodeProcessor);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Детектор кодів недоступний, переконайтеся що Google Services встановлено",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        /*cameraView.setOnTouchListener((v, event) -> {
            Log.d(TAG, "setting focus");
            cameraFocus(event, cameraSource);

            return false;
        });*/
    }

    private boolean cameraFocus(MotionEvent event, @NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        float touchMajor = event.getTouchMajor();
        float touchMinor = event.getTouchMinor();

        Rect touchRect = new Rect(
                (int) (x - touchMajor / 2),
                (int) (y - touchMinor / 2),
                (int) (x + touchMajor / 2),
                (int) (y + touchMinor / 2));

        Rect focusArea = new Rect();
        focusArea.set(touchRect.left * 2000 / cameraView.getWidth() - 1000,
                touchRect.top * 2000 / cameraView.getHeight() - 1000,
                touchRect.right * 2000 / cameraView.getWidth() - 1000,
                touchRect.bottom * 2000 / cameraView.getHeight() - 1000);
        ArrayList<Camera.Area> focusAreas = new ArrayList<>();
        focusAreas.add(new Camera.Area(focusArea, 1000));

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        Camera.Parameters params = camera.getParameters();
                        StringBuilder builder = new StringBuilder();
                        for (String mode : params.getSupportedFocusModes()) {
                            builder.append(mode).append(" ");
                            Log.d(TAG, mode);
                        }
                        Toast.makeText(getApplicationContext(),
                                builder.toString(),
                                Toast.LENGTH_LONG).show();
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                        params.setFocusAreas(focusAreas);
                        camera.setParameters(params);
                        camera.autoFocus((b, camera1) -> {

                        });
                        return true;
                    }

                    return false;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
        return false;
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
                    Log.d(TAG, "got response: " + response.toString());
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String status = response.getString("status");
                            String message = response.getString("description");
                            if (status.equals("ok")) {
                                soundPool.play(successSound, 1, 1, 1, 0, 1);
                                updateStatusMessage(getResources().getColor(R.color.colorAllow),
                                        "Квиток " + barcode + ":\n" + message,
                                        statusMessageSuccessPicture);
                            } else if (status.equals("error")) {
                                soundPool.play(failedSound, 1, 1, 1, 0, 1);
                                updateStatusMessage(getResources().getColor(R.color.colorDenied),
                                        "Квиток " + barcode + ":\n" + message,
                                        statusMessageDeniedPicture);
                            } else if (status.equals("warning")) {
                                updateStatusMessage(getResources().getColor(R.color.colorYellow),
                                        "Квиток " + barcode + ":\n" + message,
                                        statusMessageSuccessPicture);
                            } else {
                                updateStatusMessage(getResources().getColor(R.color.colorYellow),
                                        response.toString(),
                                        statusMessageInfoPicture);
                            }
                        } catch (Exception e) {
                            updateStatusMessage(getResources().getColor(R.color.colorBlack),
                                    e.getMessage(),
                                    statusMessageInfoPicture);
                            e.printStackTrace();
                        }
                    }
                },
                (error) -> {
                    updateStatusMessage(getResources().getColor(R.color.colorBlack),
                            error.getMessage(),
                            statusMessageInfoPicture);
                    Log.d(TAG, error.getMessage());
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