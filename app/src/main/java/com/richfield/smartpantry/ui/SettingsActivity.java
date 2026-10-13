package com.richfield.smartpantry.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.richfield.smartpantry.R;

/**
 * Screen 5: Settings.
 * Two preferences stored in SharedPreferences (a small key/value file on the phone):
 *  - expiring-soon alerts on/off (controls the banner on the pantry screen)
 *  - default unit pre-selected when adding a new ingredient
 * Changes are saved immediately.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "pantry_prefs";
    public static final String KEY_EXPIRY_ALERTS = "expiry_alerts";
    public static final String KEY_DEFAULT_UNIT = "default_unit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        SwitchCompat switchAlerts = findViewById(R.id.switchExpiryAlerts);
        switchAlerts.setChecked(prefs.getBoolean(KEY_EXPIRY_ALERTS, true));
        switchAlerts.setOnCheckedChangeListener((button, isChecked) ->
                prefs.edit().putBoolean(KEY_EXPIRY_ALERTS, isChecked).apply());

        Spinner spinner = findViewById(R.id.spinnerDefaultUnit);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, AddEditIngredientActivity.UNITS));

        String saved = prefs.getString(KEY_DEFAULT_UNIT, "g");
        for (int i = 0; i < AddEditIngredientActivity.UNITS.length; i++) {
            if (AddEditIngredientActivity.UNITS[i].equals(saved)) spinner.setSelection(i);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_DEFAULT_UNIT,
                        AddEditIngredientActivity.UNITS[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
