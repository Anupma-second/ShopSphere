const ORDER_LABELS = {
  PENDING: "Awaiting payment",
  CONFIRMED: "Confirmed",
  SHIPPED: "Shipped",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

const PAYMENT_LABELS = {
  CREATED: "Awaiting payment",
  PAID: "Paid",
  FAILED: "Payment failed",
  CANCELLED: "Not charged",
  REFUND_REQUIRED: "Refund pending",
  REFUND_INITIATED: "Refund in progress",
  REFUNDED: "Refunded",
};

// colour group for each status (see orders.css)
const ORDER_TONE = {
  PENDING: "warn",
  CONFIRMED: "info",
  SHIPPED: "info",
  DELIVERED: "ok",
  CANCELLED: "bad",
};

const PAYMENT_TONE = {
  CREATED: "warn",
  PAID: "ok",
  FAILED: "bad",
  CANCELLED: "muted",
  REFUND_REQUIRED: "warn",
  REFUND_INITIATED: "info",
  REFUNDED: "ok",
};

export function OrderStatusBadge({ status }) {
  return (
    <span className={`badge badge-${ORDER_TONE[status] || "muted"}`}>
      {ORDER_LABELS[status] || status}
    </span>
  );
}

export function PaymentStatusBadge({ status }) {
  if (!status) return null;
  return (
    <span className={`badge badge-${PAYMENT_TONE[status] || "muted"}`}>
      {PAYMENT_LABELS[status] || status}
    </span>
  );
}
