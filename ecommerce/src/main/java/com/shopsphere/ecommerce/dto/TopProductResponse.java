package com.shopsphere.ecommerce.dto;

public record TopProductResponse(
        Long productId,
        String productName,
        long unitsSold,
        double revenue) {

    /** Maps a [productId, productName, unitsSold, revenue] query row. */
    public static TopProductResponse fromRow(Object[] row) {
        return new TopProductResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue(),
                ((Number) row[3]).doubleValue());
    }
}
