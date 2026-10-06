package com.shopsphere.ecommerce.util;

import java.util.Locale;

public final class Money {

    private Money() {
    }

    /** 1499.5 -> "₹1499.50" (used in timeline messages). */
    public static String inr(double amount) {
        return String.format(Locale.US, "₹%.2f", amount);
    }
}
