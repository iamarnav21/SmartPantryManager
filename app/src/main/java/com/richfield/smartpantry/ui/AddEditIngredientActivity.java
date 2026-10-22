package com.richfield.smartpantry.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.richfield.smartpantry.R;
import com.richfield.smartpantry.data.DatabaseHelper;
import com.richfield.smartpantry.logic.ExpiryUtils;
import com.richfield.smartpantry.model.PantryItem;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen 2: Add / Edit Ingredient.
 * If an item id arrives in the Intent the screen is in EDIT mode (fields are pre-filled
 * and Save runs an UPDATE); otherwise it is in ADD mode (Save runs an INSERT).
 * All input is validated before anything is written to the database.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String[] UNITS = {"g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs"};

    private DatabaseHelper db;
    private EditText etName;
    private EditText etQuantity;
    private EditText etExpiry;
    private Spinner spinnerUnit;
    private long editId = -1;   // -1 means "adding a new item"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);   // back arrow in the toolbar
        }

        db = new DatabaseHelper(this);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        etExpiry.setClickable(true);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnClear = findViewById(R.id.btnClearExpiry);

        spinnerUnit.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UNITS));

        // Read the Intent extra: present = edit mode, absent = add mode
        editId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        if (editId != -1) {
            setTitle(R.string.title_edit_ingredient);
            PantryItem item = db.getPantryItem(editId);
            if (item != null) {
                etName.setText(item.getName());
                etQuantity.setText(PantryAdapter.formatQuantity(item.getQuantity()));
                etExpiry.setText(item.getExpiry());
                spinnerUnit.setSelection(indexOfUnit(item.getUnit()));
            }
        } else {
            setTitle(R.string.title_add_ingredient);
            // Use the default unit chosen on the Settings screen
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
            spinnerUnit.setSelection(indexOfUnit(prefs.getString(SettingsActivity.KEY_DEFAULT_UNIT, "g")));
        }

        etExpiry.setOnClickListener(v -> showDatePicker());
        btnClear.setOnClickListener(v -> etExpiry.setText(""));
        btnSave.setOnClickListener(v -> save());
    }

    private int indexOfUnit(String unit) {
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equalsIgnoreCase(unit)) return i;
        }
        return 0;
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                etExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /** Validates the form; on success inserts or updates the record and closes the screen. */
    private void save() {
        String name = etName.getText().toString().trim();
        String qtyText = etQuantity.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();

        // ---- input validation ----
        if (name.isEmpty()) {
            etName.setError("Please enter an ingredient name");
            etName.requestFocus();
            return;
        }
        if (!name.matches("[\\p{L} \\-']+")) {
            etName.setError("Use letters only (no numbers or symbols)");
            etName.requestFocus();
            return;
        }
        if (qtyText.isEmpty()) {
            etQuantity.setError("Please enter a quantity");
            etQuantity.requestFocus();
            return;
        }
        double quantity;
        try {
            quantity = Double.parseDouble(qtyText);
        } catch (NumberFormatException e) {
            etQuantity.setError("Enter a valid number");
            etQuantity.requestFocus();
            return;
        }
        if (quantity <= 0) {
            etQuantity.setError("Quantity must be greater than 0");
            etQuantity.requestFocus();
            return;
        }
        if (!expiry.isEmpty() && ExpiryUtils.parse(expiry) == null) {
            etExpiry.setError("Invalid date");
            return;
        }

        String unit = spinnerUnit.getSelectedItem().toString();

        if (editId == -1) {
            db.addPantryItem(new PantryItem(0, name, quantity, unit, expiry));          // CREATE
            Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
        } else {
            db.updatePantryItem(new PantryItem(editId, name, quantity, unit, expiry));  // UPDATE
            Toast.makeText(this, name + " updated", Toast.LENGTH_SHORT).show();
        }
        finish();   // go back to the pantry list
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
