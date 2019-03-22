package danila.org.ticketscanner.util.service;

import android.app.Activity;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import danila.org.ticketscanner.util.function.NoArgFunction;

public class RequestService extends Thread {

    private final static String TAG = "ticketScanner";
    private volatile JsonObjectRequest request;
    private NoArgFunction onLoading;
    private boolean interrupted;
    private RequestQueue queue;
    private Activity context;

    public RequestService(Activity context, NoArgFunction onLoading) {
        this.onLoading = onLoading;
        this.context = context;
        interrupted = false;
        queue = Volley.newRequestQueue(context);
        start();
    }

    private void myWait() {
        try {
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
                    onLoading.invoke();
                    sendRequest(request);
                    Log.d(TAG, "wait");
                    myWait();
                } else {
                    Log.d(TAG, "wait");
                    myWait();
                }
            }
        }
    }

    private void sendRequest(JsonObjectRequest request) {
        Log.d(TAG, "send request" + request.toString());
        queue.add(request);
    }

    public void interrupt() {
        interrupted = true;
    }

    public synchronized void setRequest(JsonObjectRequest request) {
        this.request = request;
        notify();
    }
}
