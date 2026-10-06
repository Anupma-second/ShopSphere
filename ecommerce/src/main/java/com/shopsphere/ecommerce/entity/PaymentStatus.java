package com.shopsphere.ecommerce.entity;

/**
 * Payment status values. Kept as plain strings (not an enum) so the existing
 * payments.status column keeps working without any migration.
 */
public final class PaymentStatus {

    private PaymentStatus() {
    }

    public static final String CREATED = "CREATED";                    // Razorpay order created, not paid
    public static final String PAID = "PAID";                          // money received
    public static final String FAILED = "FAILED";                      // attempt failed, can retry
    public static final String CANCELLED = "CANCELLED";                // order cancelled before payment
    public static final String REFUND_REQUIRED = "REFUND_REQUIRED";    // paid, order cancelled, refund not sent yet
    public static final String REFUND_INITIATED = "REFUND_INITIATED";  // refund created at Razorpay, processing
    public static final String REFUNDED = "REFUNDED";                  // refund completed
}
