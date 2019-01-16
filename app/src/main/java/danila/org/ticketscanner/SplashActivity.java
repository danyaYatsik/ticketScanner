package danila.org.ticketscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import danila.org.ticketscanner.model.Event;

public class SplashActivity extends AppCompatActivity {

    private final String EVENTS_URL = "http://tickets.docudays.org.ua/v1/mobile_app/usher/get_screenings";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        requestPermissions();
        requestEvents();
        super.onCreate(savedInstanceState);
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
                            "Ошибка подключения к серверу",
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
                        "Некорректный ответ от сервера",
                        Toast.LENGTH_SHORT)
                        .show();
                e.printStackTrace();
                finish();
            }
        }
        Event[] arr = events.toArray(new Event[0]);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("events", arr);
        startActivity(intent);
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this,
                            "Пожалуйста, предоставте все разрешения в настройках",
                            Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
    }
}
