package danila.org.ticketscanner.util.service;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import danila.org.ticketscanner.util.function.Function;

public class BarcodeProcessor implements Detector.Processor<Barcode> {

    private static final String TAG = "ticketScanner";
    private static final String PREFERENCES_FILE = "preferences";
    private static final int TIMEOUT = 2000;
    private List<Pair<String, Integer>> list = new ArrayList<>();
    private long prevScanTime = 0;
    private int counter = 0;
    private Function<String> method;
    private Activity context;
    private Vibrator vibrator;
    private SharedPreferences preferences;
    private int total;
    private int correct;

    public BarcodeProcessor(Activity context, Function<String> method) {
        this.context = context;
        this.method = method;
        vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
        preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        updatePreferences();
    }

    public void updatePreferences() {
        total = preferences.getInt("total", 3);
        correct = preferences.getInt("correct", 3);
        Log.d(TAG, String.valueOf(total));
        Log.d(TAG, String.valueOf(correct));
    }

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {
        Log.d(TAG, "receiveDetections");
        if (detections.getDetectedItems().size() == 1) {
            String currentCode = detections.getDetectedItems().valueAt(0).displayValue;
            Log.d(TAG, "received code: " + currentCode);

            long time = System.currentTimeMillis();
            if (time - prevScanTime <= TIMEOUT) {
                Log.d(TAG, "timeout isn't over: " + String.valueOf(time - prevScanTime));
                return;
            }
            if (counter++ < total) {
                Log.d(TAG, "counter " + String.valueOf(counter));
                boolean contains = false;
                for (int i = 0; i < list.size(); i++) {
                    if (currentCode.equals(list.get(i).first)) {
                        list.set(i, Pair.create(list.get(i).first, (list.get(i).second + 1)));
                        Log.d(TAG, currentCode + " in process " + list.get(i).toString());
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    list.add(new Pair<>(currentCode, 1));
                    Log.d(TAG, String.valueOf(currentCode) + " added");
                }
            }
            if (counter >= total) {
                Pair<String, Integer> pair = Collections.max(list, (p1, p2) -> {
                    if (p1.second.equals(p2.second)) return 0;
                    else if (p1.second > p2.second) return 1;
                    else return -1;
                });
                if (pair.second >= correct) {
                    vibrator.vibrate(200);
                    method.invoke(pair.first);
                }
                prevScanTime = System.currentTimeMillis();
                list = new ArrayList<>();
                counter = 0;
            }
        }
    }

    @Override
    public void release() {

    }
}
