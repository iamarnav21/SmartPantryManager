package com.richfield.smartpantry.model;

/**
 * A single ingredient the user currently has at home.
 * Plain Java object (no Android dependencies) so it can be unit tested.
 */
public class PantryItem {
    private long id;
    private String name;
    private double quantity;
    private String unit;
    /** Expiry date as "yyyy-MM-dd", or an empty string when none was entered. */
    private String expiry;

    public PantryItem(long id, String name, double quantity, String unit, String expiry) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiry = expiry == null ? "" : expiry;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry == null ? "" : expiry; }
}
