package danila.org.ticketscanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

    private static final String EVENTS_URL = "http://tickets.docudays.org.ua/v1/mobile_app/usher/get_screenings";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


}
