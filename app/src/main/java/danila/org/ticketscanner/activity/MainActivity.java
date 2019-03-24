package danila.org.ticketscanner.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import danila.org.ticketscanner.R;
import danila.org.ticketscanner.model.Event;

public class MainActivity extends AppCompatActivity {

    private List<Map<String, String>> itemsList;
    private SimpleAdapter listAdapter;
    private List<Map<String, String>> itemsListCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Оберіть сеанс");
        }

        itemsList = new ArrayList<>();
        listAdapter = new SimpleAdapter(this, itemsList, R.layout.list_item,
                new String[]{"text", "subtext", "id"}, new int[]{R.id.list_item_text, R.id.list_item_subtext, R.id.list_item});
        fillItemsList();
        itemsListCopy = new ArrayList<>(itemsList);
    }

    private void fillItemsList() {
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            String eventName = (String) ((HashMap) parent.getItemAtPosition(position)).get("text");
            intent.putExtra("event-name", eventName);
            String eventId = (String) ((HashMap) parent.getItemAtPosition(position)).get("id");
            intent.putExtra("event-id", eventId);
            String eventMeta = (String) ((HashMap) parent.getItemAtPosition(position)).get("subtext");
            intent.putExtra("event-meta", eventMeta);
            startActivity(intent);
        });

        List<Parcelable> eventsDemo = Arrays.asList(getIntent().getParcelableArrayExtra("events"));
        for (Parcelable p : eventsDemo) {
            Event event = (Event) p;
            Map<String, String> map = new HashMap<>();
            map.put("text", event.getName());
            map.put("subtext", event.getDescription());
            map.put("id", event.getId());
            itemsList.add(map);
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem searchItem = menu.findItem(R.id.app_bar_search);
        //menu.removeItem(R.id.app_bar_settings);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                for (Map<String, String> map : itemsListCopy) {
                    if (!map.get("text").toLowerCase().contains(newText.toLowerCase())) {
                        itemsList.remove(map);
                        listAdapter.notifyDataSetChanged();
                    } else if (!itemsList.contains(map)) {
                        itemsList.add(map);
                        listAdapter.notifyDataSetChanged();
                    }
                }
                return true;
            }
        });
        return true;
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
                            "Будь-ласка, надайте всі повноваження в налаштуваннях",
                            Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            }
        }
    }
}
