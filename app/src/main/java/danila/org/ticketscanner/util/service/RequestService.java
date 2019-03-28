package danila.org.ticketscanner.util.service;

import android.app.Activity;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class RequestService extends Thread {

    private final static String TAG = "ticketScanner";
    private volatile JsonObjectRequest request;
    private RequestQueue queue;
    private Activity context;
    private boolean interrupted = false;

    public RequestService(Activity context) {
        this.context = context;
        queue = Volley.newRequestQueue(context);
        start();
    }

    public void myWait() {
        try {
            Log.d(TAG, "wait");
            wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            while (!interrupted) {
                if (request != null) {
                    sendRequest(request);
                    myWait();
                } else {
                    myWait();
                }
            }
        }
    }

    private void sendRequest(JsonObjectRequest request) {
        Log.d(TAG, "send request");
        queue.add(request);
    }

    public void interrupt() {
        interrupted = true;
    }

    public synchronized void setRequest(JsonObjectRequest request) {
        Log.d(TAG, "setRequest");
        this.request = request;
        notify();
    }
}
