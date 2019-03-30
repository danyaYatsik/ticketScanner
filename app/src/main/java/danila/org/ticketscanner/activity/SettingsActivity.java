package danila.org.ticketscanner.activity;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.nio.file.Files;

import danila.org.ticketscanner.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText totalField;
    private EditText correctField;
    private Button applyButton;
    private static final String PREFERENCES_FILE = "preferences";
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        totalField = findViewById(R.id.total_amount_settings);
        correctField = findViewById(R.id.correct_amount_settings);
        applyButton = findViewById(R.id.apply_settings);
        preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Налаштування");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        int total = preferences.getInt("total", 3);
        int correct = preferences.getInt("correct", 3);
        totalField.setText(String.valueOf(total));
        correctField.setText(String.valueOf(correct));

        applyButton.setOnClickListener(this::onApplyClick);
    }

    private void onApplyClick(View view) {
        try {
            int newTotal = Integer.parseInt(totalField.getText().toString());
            int newCorrect = Integer.parseInt(correctField.getText().toString());
            if (newCorrect > newTotal) {
                Toast.makeText(this,
                        "Будь-ласка, введіть коректні числа",
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            preferences
                    .edit()
                    .putInt("total", newTotal)
                    .putInt("correct", newCorrect)
                    .apply();
            Toast.makeText(this,
                    "Налаштування застосовані",
                    Toast.LENGTH_SHORT)
                    .show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Будь-ласка, введіть коректні числа",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
