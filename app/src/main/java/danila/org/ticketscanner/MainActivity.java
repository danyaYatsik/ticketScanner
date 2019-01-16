package danila.org.ticketscanner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import danila.org.ticketscanner.model.Event;

public class MainActivity extends AppCompatActivity {

    private List<Map<String, String>> itemsList;
    private SimpleAdapter listAdapter;
    private EditText searchField;
    private List<Map<String, String>> itemsListCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
        }


        ListView listView = findViewById(R.id.list_view);
        itemsList = new ArrayList<>();
        listAdapter = new SimpleAdapter(this, itemsList, R.layout.list_item,
                new String[]{"text", "subtext", "id"}, new int[]{R.id.list_item_text, R.id.list_item_subtext, R.id.list_item});
        listView.setAdapter(listAdapter);
        getEvents();
        itemsListCopy = new ArrayList<>(itemsList);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            String eventName = (String) ((HashMap)parent.getItemAtPosition(position)).get("text");
            intent.putExtra("event-name", eventName);
            startActivity(intent);
        });


        searchField = findViewById(R.id.search_view);
        searchField.addTextChangedListener(new TextWatcher() {
            {
                ArrayList<Map<String, String>> arrayCopy = new ArrayList<>(itemsList);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                System.out.println(Arrays.toString(itemsListCopy.toArray())) ;
                for (Map<String, String> map : itemsListCopy) {
                    if (!map.get("text").toLowerCase().contains(s.toString().toLowerCase())) {
                        itemsList.remove(map);
                        listAdapter.notifyDataSetChanged();
                        System.out.println("item removed");
                    } else if (!itemsList.contains(map)) {
                        itemsList.add(map);
                        listAdapter.notifyDataSetChanged();
                        System.out.println("item added");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void getEvents() {

        List<Parcelable> eventsDemo = Arrays.asList(getIntent().getParcelableArrayExtra("events"));
        for (Parcelable p : eventsDemo) {

            addItemToList((Event)p);

        }
    }



    /*private TextView prepareTextView(JSONObject obj) throws JSONException {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(20, 20, 20, 20);
        view.setLayoutParams(params);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        view.setText(obj.getString("name"));
        view.setClickable(true);
        view.setTextColor(Color.BLACK);
        view.setBackground(getResources().getDrawable(R.drawable.item_bg));

        view.setOnClickListener((e) -> {
            Intent intent = new Intent(this, ScanActivity.class);
            try {
                intent.putExtra("event-name", obj.getString("name"));
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            startActivity(intent);
        });
        return view;
    }*/



   /* private void getEvents() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                EVENTS_URL,
                null,
                (response) -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            addItemToList(response.getJSONObject(i).getString("name"),
                                    response.getJSONObject(i).getString("description"));
                        } catch (JSONException e) {
                            Toast.makeText(this,
                                    "Некорректный ответ от сервера",
                                    Toast.LENGTH_SHORT)
                                    .show();
                            e.printStackTrace();
                        }
                    }
                    itemsListCopy = new ArrayList<>(itemsList);
                },
                (error) -> {
                    Toast.makeText(this,
                            "Ошибка подключения к серверу",
                            Toast.LENGTH_SHORT)
                            .show();
                    error.printStackTrace();
                }
        );
        queue.add(request);
    }*/

    private void addItemToList(Event event) {
        Map<String, String> map = new HashMap<>();
        map.put("text", event.getName());
        map.put("subtext", event.getDescription());
        map.put("id", event.getId());
        itemsList.add(map);
        listAdapter.notifyDataSetChanged();
    }




}
