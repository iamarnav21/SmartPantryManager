package com.richfield.smartpantry.logic;

import com.richfield.smartpantry.model.PantryItem;
import com.richfield.smartpantry.model.Recipe;
import com.richfield.smartpantry.model.RecipeIngredient;
import com.richfield.smartpantry.model.RecipeMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * THE CORE BUSINESS LOGIC OF THE APP: the strict-matching rule.
 *
 * A recipe is "suggested" only if EVERY ingredient it needs is in the pantry
 * in at least the required quantity. One missing ingredient = not suggested.
 *
 * To cope with simple real-world messiness the matcher:
 *  - ignores upper/lower case and extra spaces        ("  Milk " == "milk")
 *  - treats singular/plural names as the same         ("tomato" == "tomatoes")
 *  - converts units inside the same family            (1 kg == 1000 g, 1 cup == 240 ml)
 *  - adds up several pantry entries of the same item  (2 x 100 g flour = 200 g)
 *
 * This class has no Android dependencies, so it can be unit tested on a PC.
 */
public final class RecipeMatcher {

    private RecipeMatcher() { }


    /** Different words for the same ingredient are mapped to one standard name. */
    private static final Map<String, String> SYNONYMS = new HashMap<>();
    static {
        SYNONYMS.put("aubergine", "eggplant");
        SYNONYMS.put("courgette", "zucchini");
        SYNONYMS.put("capsicum", "bell pepper");
        SYNONYMS.put("coriander", "cilantro");
        SYNONYMS.put("prawn", "shrimp");
    }

    // ---------------------------------------------------------------- names

    /** Lower-cases, trims and singularises the LAST word of an ingredient name. */
    public static String normalizeName(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (s.isEmpty()) return s;
        int lastSpace = s.lastIndexOf(' ');
        String head = lastSpace >= 0 ? s.substring(0, lastSpace + 1) : "";
        String word = lastSpace >= 0 ? s.substring(lastSpace + 1) : s;
        String name = head + singular(word);
        String synonym = SYNONYMS.get(name);
        return synonym != null ? synonym : name;
    }

    /** Very small English singulariser - enough for ingredient names, not full NLP. */
    private static String singular(String w) {
        if (w.length() <= 3) return w;
        if (w.endsWith("ies")) return w.substring(0, w.length() - 3) + "y";   // berries -> berry
        if (w.endsWith("oes") || w.endsWith("ches") || w.endsWith("shes")
                || w.endsWith("sses") || w.endsWith("xes")) {
            return w.substring(0, w.length() - 2);                            // tomatoes -> tomato
        }
        if (w.endsWith("ss") || w.endsWith("us") || w.endsWith("is")) return w; // hummus, glass
        if (w.endsWith("s")) return w.substring(0, w.length() - 1);           // eggs -> egg
        return w;
    }

    // ---------------------------------------------------------------- units

    /** Maps the many ways of writing a unit onto one short canonical form. */
    public static String normalizeUnit(String raw) {
        String u = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(".", "");
        switch (u) {
            case "g": case "gm": case "gram": case "grams":
                return "g";
            case "kg": case "kgs": case "kilogram": case "kilograms":
                return "kg";
            case "ml": case "milliliter": case "milliliters":
            case "millilitre": case "millilitres":
                return "ml";
            case "l": case "liter": case "liters": case "litre": case "litres":
                return "l";
            case "tsp": case "teaspoon": case "teaspoons":
                return "tsp";
            case "tbsp": case "tbs": case "tblsp": case "tablespoon": case "tablespoons":
                return "tbsp";
            case "cup": case "cups":
                return "cup";
            case "": case "pcs": case "pc": case "piece": case "pieces":
            case "unit": case "units": case "whole": case "each":
                return "pcs";
            default:
                return u;
        }
    }

    /** "mass", "volume" or "count": only units of the same family can be compared. */
    public static String dimension(String unit) {
        switch (normalizeUnit(unit)) {
            case "g": case "kg":
                return "mass";
            case "ml": case "l": case "tsp": case "tbsp": case "cup":
                return "volume";
            case "pcs":
                return "count";
            default:
                return "other:" + normalizeUnit(unit);
        }
    }

    /** Converts an amount into the base unit of its family (g, ml or pcs). */
    public static double toBase(String unit, double amount) {
        switch (normalizeUnit(unit)) {
            case "kg":   return amount * 1000.0;
            case "l":    return amount * 1000.0;
            case "tsp":  return amount * 5.0;
            case "tbsp": return amount * 15.0;
            case "cup":  return amount * 240.0;
            default:     return amount;     // g, ml, pcs and unknown units
        }
    }

    // ------------------------------------------------------------- matching

    /**
     * Totals the pantry as: normalised name -> (dimension -> amount in base unit).
     * Items with zero or negative quantity are ignored.
     */
    public static Map<String, Map<String, Double>> buildPantryTotals(List<PantryItem> pantry) {
        Map<String, Map<String, Double>> totals = new HashMap<>();
        for (PantryItem item : pantry) {
            if (item.getQuantity() <= 0) continue;
            String name = normalizeName(item.getName());
            String dim = dimension(item.getUnit());
            Map<String, Double> byDim = totals.get(name);
            if (byDim == null) {
                byDim = new HashMap<>();
                totals.put(name, byDim);
            }
            Double existing = byDim.get(dim);
            double add = toBase(item.getUnit(), item.getQuantity());
            byDim.put(dim, (existing == null ? 0.0 : existing) + add);
        }
        return totals;
    }

    /** True when the pantry holds at least the quantity this recipe line requires. */
    public static boolean hasEnough(RecipeIngredient need, Map<String, Map<String, Double>> totals) {
        Map<String, Double> byDim = totals.get(normalizeName(need.getName()));
        if (byDim == null) return false;
        Double have = byDim.get(dimension(need.getUnit()));
        if (have == null) return false;   // e.g. pantry has it in grams, recipe wants pieces
        return have + 1e-9 >= toBase(need.getUnit(), need.getQuantity());
    }

    /** Lists the ingredients of a recipe that the pantry does not cover. */
    public static List<RecipeIngredient> missingIngredients(
            Recipe recipe, Map<String, Map<String, Double>> totals) {
        List<RecipeIngredient> missing = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (!hasEnough(ingredient, totals)) missing.add(ingredient);
        }
        return missing;
    }

    /** Tests every recipe against the pantry. */
    public static List<RecipeMatch> evaluate(List<PantryItem> pantry, List<Recipe> recipes) {
        Map<String, Map<String, Double>> totals = buildPantryTotals(pantry);
        List<RecipeMatch> results = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getIngredients().isEmpty()) continue;   // nothing to cook with
            results.add(new RecipeMatch(recipe, missingIngredients(recipe, totals)));
        }
        return results;
    }

    /** STRICT list: only recipes where nothing is missing. */
    public static List<RecipeMatch> strictMatches(List<PantryItem> pantry, List<Recipe> recipes) {
        List<RecipeMatch> out = new ArrayList<>();
        for (RecipeMatch m : evaluate(pantry, recipes)) {
            if (m.isStrictMatch()) out.add(m);
        }
        return out;
    }

    /** BONUS list (kept separate from the strict list): recipes missing exactly one ingredient. */
    public static List<RecipeMatch> almostThere(List<PantryItem> pantry, List<Recipe> recipes) {
        List<RecipeMatch> out = new ArrayList<>();
        for (RecipeMatch m : evaluate(pantry, recipes)) {
            if (m.getMissing().size() == 1) out.add(m);
        }
        return out;
    }


    }


