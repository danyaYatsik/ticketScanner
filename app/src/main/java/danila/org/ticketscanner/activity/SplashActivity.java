package danila.org.ticketscanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import danila.org.ticketscanner.model.Event;

public class SplashActivity extends AppCompatActivity {

    private static final String EVENTS_URL = "http://tickets.docudays.org.ua/v1/mobile_app/usher/get_screenings";
    private final static String TAG = "ticketScanner";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ( isExternalStorageWritable() ) {

            DateFormat dateFormat = new SimpleDateFormat("dd_HH:mm:ss", Locale.getDefault());

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/TicketScanner" );
            File logDirectory = new File( appDirectory + "/log" );
            File logFile = new File( logDirectory, dateFormat.format(new Date()) + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
        Log.d(TAG, "Splash onCreate");
        requestEvents();
    }

    private void requestEvents() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                EVENTS_URL,
                null,
                this::onRequestEventsResult,
                (error) -> {
                    Toast.makeText(this,
                            "Помилка: мережа недоступна",
                            Toast.LENGTH_LONG)
                            .show();
                    error.printStackTrace();
                    finish();
                }
        );
        queue.add(request);
    }

    private void onRequestEventsResult(JSONArray response) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            try {
                events.add(new Event(
                        response.getJSONObject(i).getString("name"),
                        response.getJSONObject(i).getString("description"),
                        response.getJSONObject(i).getString("id")
                ));
            } catch (JSONException e) {
                Toast.makeText(this,
                        "Відповідь сервера невалідна",
                        Toast.LENGTH_LONG)
                        .show();
                e.printStackTrace();
                finish();
            }
        }
        Event[] arr = events.toArray(new Event[0]);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("events", arr);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }


}
