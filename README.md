# Smart Pantry Manager

A Java Android application that helps reduce food waste. The user records the ingredients they
already have at home (their "pantry"), and the app suggests recipes they can cook using
**strictly** those ingredients: no shopping trip required.

Built for **Mobile App Development 700 – Practical Assignment** (Richfield Graduate Institute of Technology).

## Features

- **Pantry management** – add, edit and delete ingredients (name, quantity, unit, optional expiry date)
- **Pantry list** – RecyclerView with a custom adapter, bound to the SQLite database
- **Recipe collection** – 21 recipes pre-loaded into the database on first run
- **Suggested Recipes** – runs the strict-matching rule and lists only recipes the user can make right now
- **Recipe detail** – full ingredient list (with a have / missing marker) and method
- **Settings** – expiring-soon alerts on/off and default unit for new ingredients
- **Friendly empty state** – "No recipes match your pantry yet - add more ingredients"
- **Bonus** – a separate "Almost there" list of recipes missing exactly one ingredient
  (kept apart from the strict list)

## The strict-matching rule

A recipe is suggested **only if every ingredient it needs is in the pantry in at least the required
quantity**. A recipe that needs 5 ingredients when the pantry has 4 is never suggested.

The matcher (`logic/RecipeMatcher.java`) is robust to simple real-world messiness:

| Messiness | Handling |
|-----------|----------|
| Capital letters / extra spaces | names are lower-cased and trimmed |
| Singular vs plural | `tomato` = `tomatoes`, `egg` = `eggs`, `berry` = `berries` |
| Unit differences | `1 kg` = `1000 g`, `1 cup` = `240 ml`, `1 tbsp` = `15 ml`, `1 tsp` = `5 ml` |
| Same item entered twice | quantities are added up (`100 g + 100 g flour = 200 g`) |

Units from different families (e.g. grams vs pieces) are never treated as equal.

## Database choice: SQLite

I chose **SQLite** (`SQLiteOpenHelper`) because:

1. The pantry is personal data that should work **offline** in a kitchen with no internet.
2. It needs **no server, account or API key**, so the project runs on any machine straight away.
3. It is built into Android and is the approach covered in the module's persistent data chapter.
4. Data is stored in a file on the device, so it **persists** after the app is closed and reopened.

Tables: `pantry`, `recipes`, `recipe_ingredients` (one recipe has many ingredient lines).

## Project structure

```
app/src/main/java/com/richfield/smartpantry/
  model/   PantryItem, Recipe, RecipeIngredient, RecipeMatch
  data/    DatabaseHelper            (SQLite: CRUD + recipe seeding)
  logic/   RecipeMatcher, ExpiryUtils (pure Java, unit tested)
  ui/      MainActivity, AddEditIngredientActivity, SuggestedRecipesActivity,
           RecipeDetailActivity, SettingsActivity, PantryAdapter, RecipeAdapter
app/src/test/  RecipeMatcherTest     (JUnit tests for the strict rule)
```

## Setup and run instructions

**Requirements:** Android Studio (Hedgehog 2023.1.1 or newer) with JDK 17 (bundled with Android Studio).

1. Clone the repository (or unzip the project) and open the **SmartPantryManager** folder in
   Android Studio using **File > Open**.
2. Wait for **Gradle sync** to finish (first sync downloads dependencies and needs internet).
3. Create an emulator: **Tools > Device Manager > Create Device**, pick a Pixel phone and an
   API 34 system image. (Or plug in a physical phone with USB debugging enabled.)
4. Press the green **Run** button. The app opens on the Pantry List screen.
5. To run the unit tests: right-click `RecipeMatcherTest` > **Run 'RecipeMatcherTest'**.

## Quick demo of the strict rule

1. Add `Eggs 3 pcs`, `Tomato 1 pcs`, `Butter 10 g` and `Salt 1 tsp`.
2. Open **Suggested Recipes** (toolbar button): *Tomato Omelette* appears.
3. Go back and delete the Salt: *Tomato Omelette* disappears (and moves to "Almost there").

## Restrictions respected

No Google Maps, mapping SDK, GPS or location features. No payments. Not published to the Play Store.
