package com.richfield.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.richfield.smartpantry.model.PantryItem;
import com.richfield.smartpantry.model.Recipe;
import com.richfield.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local SQLite database for the Smart Pantry Manager.
 *
 * Tables:
 *   pantry             - the user's ingredients (full CRUD)
 *   recipes            - the recipe collection (seeded on first run)
 *   recipe_ingredients - the ingredient lines of each recipe (many per recipe)
 *
 * SQLite stores the data in a file on the phone, so it is still there
 * after the app is closed and reopened.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    // pantry table
    private static final String T_PANTRY = "pantry";
    private static final String P_ID = "_id";
    private static final String P_NAME = "name";
    private static final String P_QTY = "quantity";
    private static final String P_UNIT = "unit";
    private static final String P_EXPIRY = "expiry";

    // recipes table
    private static final String T_RECIPES = "recipes";
    private static final String R_ID = "_id";
    private static final String R_NAME = "name";
    private static final String R_STEPS = "steps";

    // recipe_ingredients table
    private static final String T_RI = "recipe_ingredients";
    private static final String RI_ID = "_id";
    private static final String RI_RECIPE = "recipe_id";
    private static final String RI_NAME = "name";
    private static final String RI_QTY = "quantity";
    private static final String RI_UNIT = "unit";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_PANTRY + " ("
                + P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + P_NAME + " TEXT NOT NULL, "
                + P_QTY + " REAL NOT NULL, "
                + P_UNIT + " TEXT NOT NULL, "
                + P_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + T_RECIPES + " ("
                + R_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + R_NAME + " TEXT NOT NULL, "
                + R_STEPS + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_RI + " ("
                + RI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RI_RECIPE + " INTEGER NOT NULL, "
                + RI_NAME + " TEXT NOT NULL, "
                + RI_QTY + " REAL NOT NULL, "
                + RI_UNIT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + RI_RECIPE + ") REFERENCES " + T_RECIPES + "(" + R_ID + "))");

        seedRecipes(db);   // pre-load the recipe collection on first run
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_RI);
        db.execSQL("DROP TABLE IF EXISTS " + T_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + T_PANTRY);
        onCreate(db);
    }

    // ================================================================ PANTRY CRUD

    /** CREATE: inserts a new pantry item and returns its new id (or -1 on failure). */
    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(T_PANTRY, null, toValues(item));
    }

    /** READ: returns every pantry item, sorted by name. */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_PANTRY, null, null, null, null, null,
                P_NAME + " COLLATE NOCASE ASC");
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    /** READ: returns one pantry item by id, or null when it does not exist. */
    public PantryItem getPantryItem(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_PANTRY, null, P_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        try {
            return c.moveToFirst() ? fromCursor(c) : null;
        } finally {
            c.close();
        }
    }

    /** UPDATE: overwrites an existing pantry item; returns the number of rows changed. */
    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(T_PANTRY, toValues(item), P_ID + "=?",
                new String[]{String.valueOf(item.getId())});
    }

    /** DELETE: removes a pantry item; returns the number of rows deleted. */
    public int deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(T_PANTRY, P_ID + "=?", new String[]{String.valueOf(id)});
    }

    private ContentValues toValues(PantryItem item) {
        ContentValues v = new ContentValues();
        v.put(P_NAME, item.getName());
        v.put(P_QTY, item.getQuantity());
        v.put(P_UNIT, item.getUnit());
        v.put(P_EXPIRY, item.getExpiry());
        return v;
    }

    private PantryItem fromCursor(Cursor c) {
        return new PantryItem(
                c.getLong(c.getColumnIndexOrThrow(P_ID)),
                c.getString(c.getColumnIndexOrThrow(P_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(P_QTY)),
                c.getString(c.getColumnIndexOrThrow(P_UNIT)),
                c.getString(c.getColumnIndexOrThrow(P_EXPIRY)));
    }

    // ============================================================ RECIPES (read)

    /** Returns all recipes together with their ingredient lines. */
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Map<Long, Recipe> byId = new LinkedHashMap<>();

        Cursor rc = db.query(T_RECIPES, null, null, null, null, null, R_NAME + " ASC");
        try {
            while (rc.moveToNext()) {
                long id = rc.getLong(rc.getColumnIndexOrThrow(R_ID));
                byId.put(id, new Recipe(id,
                        rc.getString(rc.getColumnIndexOrThrow(R_NAME)),
                        rc.getString(rc.getColumnIndexOrThrow(R_STEPS))));
            }
        } finally {
            rc.close();
        }

        Cursor ic = db.query(T_RI, null, null, null, null, null, RI_ID + " ASC");
        try {
            while (ic.moveToNext()) {
                Recipe r = byId.get(ic.getLong(ic.getColumnIndexOrThrow(RI_RECIPE)));
                if (r != null) {
                    r.addIngredient(new RecipeIngredient(
                            ic.getString(ic.getColumnIndexOrThrow(RI_NAME)),
                            ic.getDouble(ic.getColumnIndexOrThrow(RI_QTY)),
                            ic.getString(ic.getColumnIndexOrThrow(RI_UNIT))));
                }
            }
        } finally {
            ic.close();
        }
        return new ArrayList<>(byId.values());
    }

    /** Returns one recipe (with ingredients) by id, or null if not found. */
    public Recipe getRecipe(long id) {
        for (Recipe r : getAllRecipes()) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    // ================================================================ SEED DATA

    /**
     * Inserts the starter recipe collection (21 recipes).
     * Ingredient format: "name|quantity|unit".
     */
    private void seedRecipes(SQLiteDatabase db) {
        addRecipe(db, "Tomato Omelette",
                "1. Chop the tomato.\n2. Beat the eggs with the salt.\n3. Melt the butter in a pan.\n"
                        + "4. Pour in the eggs, add the tomato and cook until set. Fold and serve.",
                "egg|3|pcs", "tomato|1|pcs", "butter|10|g", "salt|1|tsp");

        addRecipe(db, "Cheese Toast",
                "1. Butter the bread.\n2. Top with the cheese.\n3. Grill or toast until golden and melted.",
                "bread|2|pcs", "cheese|50|g", "butter|10|g");

        addRecipe(db, "Fluffy Pancakes",
                "1. Whisk flour, sugar, eggs and milk into a smooth batter.\n2. Melt butter in a pan.\n"
                        + "3. Pour small rounds of batter and cook 2 minutes per side.",
                "flour|200|g", "milk|300|ml", "egg|2|pcs", "sugar|20|g", "butter|20|g");

        addRecipe(db, "Egg Fried Rice",
                "1. Chop the onion and fry in oil.\n2. Push aside, scramble the eggs in the pan.\n"
                        + "3. Add cooked rice and soy sauce.\n4. Stir-fry for 4 minutes and serve hot.",
                "rice|200|g", "egg|2|pcs", "onion|1|pcs", "soy sauce|2|tbsp", "oil|1|tbsp");

        addRecipe(db, "Simple Tomato Pasta",
                "1. Boil the pasta in salted water.\n2. Fry chopped onion and garlic in oil.\n"
                        + "3. Add chopped tomatoes and simmer for 10 minutes.\n4. Toss with the drained pasta.",
                "pasta|200|g", "tomato|4|pcs", "garlic|2|pcs", "onion|1|pcs", "oil|2|tbsp", "salt|1|tsp");

        addRecipe(db, "Soft Scrambled Eggs",
                "1. Beat the eggs with milk and salt.\n2. Melt butter on low heat.\n"
                        + "3. Stir gently until creamy and just set.",
                "egg|3|pcs", "butter|10|g", "milk|50|ml", "salt|0.5|tsp");

        addRecipe(db, "Garlic Butter Rice",
                "1. Cook the rice.\n2. Melt butter and fry minced garlic for 1 minute.\n"
                        + "3. Stir through the rice and season with salt.",
                "rice|200|g", "garlic|3|pcs", "butter|20|g", "salt|0.5|tsp");

        addRecipe(db, "Creamy Mashed Potato",
                "1. Peel and boil the potatoes until soft.\n2. Drain and mash.\n"
                        + "3. Mix in butter, warm milk and salt.",
                "potato|4|pcs", "butter|30|g", "milk|100|ml", "salt|1|tsp");

        addRecipe(db, "Banana Pancakes",
                "1. Mash the bananas.\n2. Mix with eggs and flour.\n3. Fry spoonfuls until golden on both sides.",
                "banana|2|pcs", "egg|2|pcs", "flour|100|g");

        addRecipe(db, "Cheese Omelette",
                "1. Beat the eggs with salt.\n2. Melt butter in a pan and add the eggs.\n"
                        + "3. Sprinkle the cheese on top, fold and serve.",
                "egg|3|pcs", "cheese|50|g", "butter|10|g", "salt|0.5|tsp");

        addRecipe(db, "Veggie Stir Fry",
                "1. Slice the carrots, onion and pepper.\n2. Heat oil and fry the garlic.\n"
                        + "3. Add the vegetables and stir-fry 5 minutes.\n4. Finish with soy sauce.",
                "carrot|2|pcs", "onion|1|pcs", "bell pepper|1|pcs", "soy sauce|2|tbsp",
                "oil|1|tbsp", "garlic|2|pcs");

        addRecipe(db, "Chicken Rice Bowl",
                "1. Season and pan-fry the chicken in oil until cooked through.\n2. Fry the onion until soft.\n"
                        + "3. Serve over hot rice.",
                "chicken breast|300|g", "rice|200|g", "onion|1|pcs", "oil|1|tbsp", "salt|1|tsp");

        addRecipe(db, "Tuna Pasta",
                "1. Boil the pasta.\n2. Fry the onion in oil.\n3. Add the drained tuna and season.\n"
                        + "4. Toss with the pasta.",
                "pasta|200|g", "tuna|1|pcs", "onion|1|pcs", "oil|1|tbsp", "salt|0.5|tsp");

        addRecipe(db, "Baked Potatoes",
                "1. Heat the oven to 200 C.\n2. Cut the potatoes, toss in oil and salt.\n"
                        + "3. Bake 40 minutes, then top with butter.",
                "potato|4|pcs", "oil|2|tbsp", "salt|1|tsp", "butter|20|g");

        addRecipe(db, "French Toast",
                "1. Whisk eggs, milk and sugar.\n2. Dip the bread slices.\n"
                        + "3. Fry in butter until golden on both sides.",
                "bread|4|pcs", "egg|2|pcs", "milk|100|ml", "sugar|10|g", "butter|15|g");

        addRecipe(db, "Roasted Tomato Soup",
                "1. Fry onion and garlic in butter.\n2. Add the chopped tomatoes and a cup of water.\n"
                        + "3. Simmer 20 minutes, season and blend smooth.",
                "tomato|6|pcs", "onion|1|pcs", "garlic|2|pcs", "butter|20|g", "salt|1|tsp");

        addRecipe(db, "Creamy Oat Porridge",
                "1. Combine oats and milk in a pot.\n2. Cook on low heat, stirring, for 5 minutes.\n"
                        + "3. Sweeten with sugar and serve warm.",
                "oats|80|g", "milk|300|ml", "sugar|10|g");

        addRecipe(db, "Egg Fried Noodles",
                "1. Cook the noodles and drain.\n2. Fry the onion in oil.\n3. Scramble in the eggs.\n"
                        + "4. Add noodles and soy sauce, toss well.",
                "noodles|200|g", "egg|2|pcs", "soy sauce|2|tbsp", "oil|1|tbsp", "onion|1|pcs");

        addRecipe(db, "Carrot Soup",
                "1. Fry the onion in butter.\n2. Add sliced carrots and enough water to cover.\n"
                        + "3. Simmer 20 minutes, season and blend smooth.",
                "carrot|4|pcs", "onion|1|pcs", "butter|20|g", "salt|1|tsp");

        addRecipe(db, "Garlic Bread",
                "1. Mash butter with minced garlic.\n2. Spread on the bread.\n3. Bake 10 minutes until crisp.",
                "bread|4|pcs", "butter|40|g", "garlic|3|pcs");

        addRecipe(db, "Lentil Curry",
                "1. Fry onion and garlic in oil.\n2. Add curry powder and tomatoes.\n"
                        + "3. Add lentils and water, simmer 25 minutes.\n4. Season with salt.",
                "lentils|200|g", "onion|1|pcs", "garlic|2|pcs", "tomato|2|pcs", "oil|1|tbsp",
                "curry powder|2|tsp", "salt|1|tsp");
    }

    private void addRecipe(SQLiteDatabase db, String name, String steps, String... ingredients) {
        ContentValues rv = new ContentValues();
        rv.put(R_NAME, name);
        rv.put(R_STEPS, steps);
        long recipeId = db.insert(T_RECIPES, null, rv);

        for (String line : ingredients) {
            String[] p = line.split("\\|");
            ContentValues iv = new ContentValues();
            iv.put(RI_RECIPE, recipeId);
            iv.put(RI_NAME, p[0]);
            iv.put(RI_QTY, Double.parseDouble(p[1]));
            iv.put(RI_UNIT, p[2]);
            db.insert(T_RI, null, iv);
        }
    }
}
