package com.richfield.smartpantry.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small helpers for the optional expiry date ("yyyy-MM-dd"). */
public final class ExpiryUtils {

    public static final String FORMAT = "yyyy-MM-dd";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private ExpiryUtils() { }

    /** Parses a date string; returns null when it is empty or not a real date. */
    public static Date parse(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        SimpleDateFormat f = new SimpleDateFormat(FORMAT, Locale.US);
        f.setLenient(false);
        try {
            return f.parse(text.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Whole days from "today" until the expiry date (negative = already expired).
     * Returns null when there is no valid expiry date.
     */
    public static Long daysUntil(String expiry, Date today) {
        Date exp = parse(expiry);
        if (exp == null) return null;
        SimpleDateFormat f = new SimpleDateFormat(FORMAT, Locale.US);
        Date todayStart = parse(f.format(today));   // strip the time of day
        return (exp.getTime() - todayStart.getTime()) / DAY_MS;
    }

    /** True when the item expires within {@code withinDays} days (or has already expired). */
    public static boolean isExpiringSoon(String expiry, Date today, int withinDays) {
        Long days = daysUntil(expiry, today);
        return days != null && days <= withinDays;
    }
}
