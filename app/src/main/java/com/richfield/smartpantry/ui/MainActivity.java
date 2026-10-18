package com.richfield.smartpantry.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.richfield.smartpantry.R;
import com.richfield.smartpantry.data.DatabaseHelper;
import com.richfield.smartpantry.logic.ExpiryUtils;
import com.richfield.smartpantry.model.PantryItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Screen 1: the Pantry List (launcher screen).
 * Shows every ingredient from the database and lets the user add, edit and delete items.
 * The toolbar menu navigates to Suggested Recipes and Settings.
 */
public class MainActivity extends AppCompatActivity implements PantryAdapter.OnItemActionListener {

    private DatabaseHelper db;
    private PantryAdapter adapter;
    private TextView tvEmpty;
    private TextView tvExpiryBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        db = new DatabaseHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvExpiryBanner = findViewById(R.id.tvExpiryBanner);

        RecyclerView recycler = findViewById(R.id.recyclerPantry);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(this);
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEditIngredientActivity.class)));
    }

    /** onResume runs every time this screen becomes visible, so the list is always fresh
     *  (for example right after returning from the Add/Edit screen). */
    @Override
    protected void onResume() {
        super.onResume();
        loadPantry();
    }

    private void loadPantry() {
        List<PantryItem> items = db.getAllPantryItems();
        adapter.setItems(items);
        setTitle(getString(R.string.app_name) + " (" + items.size() + ")");
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        updateExpiryBanner(items);
    }

    /** Shows the orange warning banner when the alert setting is ON and items expire within 3 days. */
    private void updateExpiryBanner(List<PantryItem> items) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        boolean alertsOn = prefs.getBoolean(SettingsActivity.KEY_EXPIRY_ALERTS, true);

        List<String> names = new ArrayList<>();
        Date today = new Date();
        for (PantryItem item : items) {
            if (ExpiryUtils.isExpiringSoon(item.getExpiry(), today, ExpiryUtils.EXPIRY_WARNING_DAYS)) names.add(item.getName());
        }

        if (alertsOn && !names.isEmpty()) {
            tvExpiryBanner.setText("Use soon (" + names.size() + "): " + android.text.TextUtils.join(", ", names));
            tvExpiryBanner.setVisibility(View.VISIBLE);
        } else {
            tvExpiryBanner.setVisibility(View.GONE);
        }
    }

    // ----- adapter callbacks -----

    @Override
    public void onEdit(PantryItem item) {
        // Explicit Intent: pass the id of the item to edit to the Add/Edit screen
        Intent intent = new Intent(this, AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_ITEM_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(final PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete ingredient")
                .setMessage("Remove " + item.getName() + " from your pantry?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deletePantryItem(item.getId());
                    Toast.makeText(this, item.getName() + " deleted", Toast.LENGTH_SHORT).show();
                    loadPantry();
                })
                .show();
    }

    // ----- toolbar menu navigation -----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_suggested) {
            startActivity(new Intent(this, SuggestedRecipesActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
