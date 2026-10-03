import api from "../api/axios";

/**
 * Opens the Razorpay popup for an already-created payment and verifies it
 * on the backend. Shared by Checkout and Order Details ("Pay now").
 *
 * payment = response of POST /payments/create
 *           { razorpayOrderId, orderId, amount (in rupees), status }
 */
export function openRazorpay({
  payment,
  user,
  onPaid,        // verified on the server
  onVerifyError, // popup succeeded but the server rejected it (err)
  onDismiss,     // user closed the popup
  onFailed,      // payment failed inside the popup (message)
}) {
  if (!window.Razorpay) {
    onFailed?.("Payment gateway failed to load. Check your connection and retry.");
    return;
  }

  const options = {
    key: (import.meta.env.VITE_RAZORPAY_KEY_ID || "").trim(),
    amount: Math.round(payment.amount * 100), // paise
    currency: "INR",
    name: "ShopSphere",
    description: `Payment for Order #${payment.orderId}`,
    order_id: payment.razorpayOrderId,
    handler: async (response) => {
      try {
        await api.post("/payments/verify", {
          razorpayOrderId: response.razorpay_order_id,
          razorpayPaymentId: response.razorpay_payment_id,
          razorpaySignature: response.razorpay_signature,
        });
        onPaid?.();
      } catch (err) {
        onVerifyError?.(err);
      }
    },
    modal: { ondismiss: () => onDismiss?.() },
    prefill: { name: user?.name || "", email: user?.email || "" },
    theme: { color: "#3399cc" },
  };

  const razorpay = new window.Razorpay(options);
  razorpay.on("payment.failed", (failure) => {
    onFailed?.(failure?.error?.description || "Payment failed. Please try again.");
  });
  razorpay.open();
}
