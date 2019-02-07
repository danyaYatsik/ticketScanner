package danila.org.ticketscanner.util;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeReciever implements Detector.Processor<Barcode> {

    private String[] codes;
    private Vibrator vibrator;
    private Function<String> method;

    private final static String TAG = "moi";

    public BarcodeReciever(Activity context, Function<String> method) {
        codes = new String[3];
        this.method = method;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {

        SparseArray<Barcode> barcodes = detections.getDetectedItems();
        String currentCode = barcodes.valueAt(0).displayValue;
        Log.d(TAG, "code deected " + currentCode);
        for (String s : codes) {
            if (s == null) {
                s = currentCode;
            }
        }

    }

    private void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
