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

    private final static String TAG = "ticketScanner";
    private static final String PREFERENCES_FILE = "preferences";
    private String[] codes;
    private Function<String> method;
    private Activity context;
    private String prevCode;
    private long interval;
    private Vibrator vibrator;
    /*private List<Pair<String, Integer>> list;
    private SharedPreferences preferences;
    private int counter;
    private int total;
    private int correct;*/

    public BarcodeProcessor(Activity context, Function<String> method) {
        this.context = context;
        this.method = method;
        codes = new String[3];
        interval = 0;
        prevCode = "";
        vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
        //preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        //counter = 0;
        //list = new ArrayList<>();
        //updatePreferences();
    }

    /*public void updatePreferences() {
        total = preferences.getInt("total", 3);
        correct = preferences.getInt("correct", 3);
        Log.d(TAG, String.valueOf(total));
        Log.d(TAG, String.valueOf(correct));
    }*/

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {
        Log.d(TAG, "receiveDetections");
        if (detections.getDetectedItems().size() == 1) {
            String currentCode = detections.getDetectedItems().valueAt(0).displayValue;
            Log.d(TAG, "received code: " + currentCode);

            long time = System.currentTimeMillis();
            if (time - interval <= 2000) {
                Log.d(TAG, "timeout isn't over: " + String.valueOf(time - interval));
                return;
            }
            /*if (currentCode.equals(prevCode)) {
                Log.d(TAG, "codes are equal " + prevCode + " - " + String.valueOf(currentCode));
                vibrator.vibrate(new long[]{0, 200, 50, 200}, -1);
                interval = System.currentTimeMillis();
                return;
            }*/
            /*if (counter++ < total) {
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
            if (counter >= total){
                Pair<String, Integer> pair = Collections.max(list, (p1, p2) -> {
                    if (p1.second.equals(p2.second)) return 0;
                    else if (p1.second > p2.second) return 1;
                    else return -1;
                });
                if (pair.second >= correct) {
                    vibrator.vibrate(200);
                    prevCode = pair.first;
                    method.invoke(pair.first);
                }
                interval = System.currentTimeMillis();
                list = new ArrayList<>();
                counter = 0;
            }*/

            for (int i = 0; i < codes.length; i++) {
                if (codes[i] == null) {
                    codes[i] = currentCode;
                    Log.d(TAG, "code put at " + i);
                    break;
                }
            }
            if (codes[2] != null) {
                Log.d(TAG, "array filled");
                if (codes[0].equals(codes[1]) && codes[0].equals(codes[2])) {
                    Log.d(TAG, "all codes are equal");
                    vibrator.vibrate(200);
                    prevCode = currentCode;
                    interval = System.currentTimeMillis();
                    method.invoke(currentCode);
                }
                codes = new String[3];
            }
        }
    }

    @Override
    public void release() {

    }
}