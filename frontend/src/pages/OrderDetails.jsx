import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { formatDateTime, formatMoney } from "../utils/format";
import { openRazorpay } from "../utils/razorpay";
import { OrderStatusBadge, PaymentStatusBadge } from "../components/StatusBadge";
import "../styles/orders.css";

const STEPS = ["Placed", "Confirmed", "Shipped", "Delivered"];
const STEP_INDEX = { PENDING: 0, CONFIRMED: 1, SHIPPED: 2, DELIVERED: 3 };

const METHOD_LABELS = {
  upi: "UPI",
  card: "Card",
  netbanking: "Net banking",
  wallet: "Wallet",
  emi: "EMI",
};

function OrderDetails() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null); // { tone, text }
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  const user = JSON.parse(localStorage.getItem("user") || "null");

  const loadOrder = useCallback(() => {
    return api.get(`/orders/${id}`)
      .then((response) => {
        setOrder(response.data);
        setError("");
      })
      .catch((err) => setError(getErrorMessage(err, "Failed to load order.")));
  }, [id]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  // A refund that is waiting to be sent: re-check every 15s
  const paymentStatus = order?.payment?.status;
  useEffect(() => {
    if (paymentStatus !== "REFUND_REQUIRED") return undefined;
    const timer = setInterval(loadOrder, 15000);
    return () => clearInterval(timer);
  }, [paymentStatus, loadOrder]);

  const payNow = async () => {
    setBusy(true);
    setNotice(null);
    try {
      const { data: payment } = await api.post("/payments/create", {
        orderId: order.id,
      });
      openRazorpay({
        payment,
        user,
        onPaid: async () => {
          setNotice({ tone: "ok", text: "Payment successful! Your order is confirmed." });
          setBusy(false);
          await loadOrder();
        },
        onVerifyError: (err) => {
          setBusy(false);
          setNotice({ tone: "bad", text: getErrorMessage(err, "Payment verification failed.") });
          loadOrder();
        },
        onDismiss: () => setBusy(false),
        onFailed: (text) => {
          setBusy(false);
          setNotice({ tone: "bad", text });
          loadOrder();
        },
      });
    } catch (err) {
      setBusy(false);
      setNotice({ tone: "bad", text: getErrorMessage(err, "Could not start payment.") });
    }
  };

  const cancelOrder = async () => {
    const paid = order.payment?.status === "PAID";
    const question = paid
      ? `Cancel this order? ${formatMoney(order.totalAmount)} will be refunded to your original payment method.`
      : "Cancel this order?";
    if (!window.confirm(question)) return;

    setBusy(true);
    setNotice(null);
    try {
      const { data } = await api.put(`/orders/${order.id}/cancel`);
      setOrder(data);
      setNotice({
        tone: "ok",
        text: paid ? "Order cancelled. Your refund has been started." : "Order cancelled.",
      });
    } catch (err) {
      setNotice({ tone: "bad", text: getErrorMessage(err, "Could not cancel order.") });
    } finally {
      setBusy(false);
    }
  };

  // ---------- loading / error states ----------
  if (error) {
    return (
      <div className="order-page">
        <div className="state-box">
          <h2>We couldn't open this order</h2>
          <p className="muted">{error}</p>
          <p style={{ marginTop: 16 }}>
            <button onClick={() => navigate("/orders")}>Back to My Orders</button>
          </p>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="order-page">
        <div className="skeleton" style={{ width: 240, height: 28, marginTop: 28 }} />
        <div className="skeleton" />
        <div className="skeleton" style={{ height: 120 }} />
      </div>
    );
  }

  const payment = order.payment;
  const cancelled = order.status === "CANCELLED";
  const stepIdx = STEP_INDEX[order.status] ?? 0;
  const subtotal = order.items.reduce((sum, i) => sum + i.price * i.quantity, 0);
  const timeline = [...(order.timeline || [])].reverse(); // newest first

  return (
    <div className="order-page">
      <div className="order-head">
        <div>
          <h1>Order #{order.id}</h1>
          <p className="muted">Placed on {formatDateTime(order.orderDate)}</p>
        </div>
        <div className="order-head-badges">
          <OrderStatusBadge status={order.status} />
          <PaymentStatusBadge status={payment?.status} />
        </div>
      </div>

      {notice && <div className={`banner banner-${notice.tone}`}>{notice.text}</div>}

      {/* status-specific messages */}
      {order.status === "PENDING" && (
        <div className="banner banner-warn">
          {payment?.status === "FAILED"
            ? "Your last payment attempt failed. You can try again."
            : "This order is waiting for payment. Unpaid orders are cancelled automatically after 30 minutes."}
        </div>
      )}
      {cancelled && (
        <div className="banner banner-bad">
          This order was cancelled{order.cancelledAt ? ` on ${formatDateTime(order.cancelledAt)}` : ""}.
        </div>
      )}
      {payment?.status === "REFUND_REQUIRED" && (
        <div className="banner banner-warn">
          Your refund of {formatMoney(payment.amount)} is being processed. This page updates automatically.
        </div>
      )}
      {payment?.status === "REFUND_INITIATED" && (
        <div className="banner banner-ok">
          Refund of {formatMoney(payment.amount)} has been sent to your bank. It usually takes 5–7 business days.
        </div>
      )}
      {payment?.status === "REFUNDED" && (
        <div className="banner banner-ok">
          {formatMoney(payment.amount)} was refunded
          {payment.refundedAt ? ` on ${formatDateTime(payment.refundedAt)}` : ""}.
        </div>
      )}

      {/* progress */}
      {!cancelled && (
        <ol className="stepper">
          {STEPS.map((label, i) => (
            <li key={label} className={i <= stepIdx ? "done" : ""}>{label}</li>
          ))}
        </ol>
      )}

      {order.status === "SHIPPED" || order.status === "DELIVERED" ? (
        order.trackingNumber && (
          <p className="muted" style={{ textAlign: "center" }}>
            {order.carrier ? `${order.carrier} · ` : ""}Tracking number: <strong>{order.trackingNumber}</strong>
          </p>
        )
      ) : null}

      {(order.canPay || order.canCancel) && (
        <div className="order-actions">
          {order.canPay && (
            <button className="primary" onClick={payNow} disabled={busy}>
              {busy ? "Please wait…" : `Pay ${formatMoney(order.totalAmount)}`}
            </button>
          )}
          {order.canCancel && (
            <button className="danger" onClick={cancelOrder} disabled={busy}>
              Cancel order
            </button>
          )}
        </div>
      )}

      <div className="order-grid">
        {/* left column */}
        <div>
          <div className="card">
            <h2>Items</h2>
            <table className="items">
              <thead>
                <tr>
                  <th>Product</th>
                  <th className="num">Price</th>
                  <th className="num">Qty</th>
                  <th className="num">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item) => (
                  <tr key={item.productId}>
                    <td>{item.productName}</td>
                    <td className="num">{formatMoney(item.price)}</td>
                    <td className="num">{item.quantity}</td>
                    <td className="num">{formatMoney(item.price * item.quantity)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="card">
            <h2>Order timeline</h2>
            <ul className="timeline">
              {timeline.map((event, i) => (
                <li key={`${event.type}-${i}`}>
                  {event.message}
                  <time>{formatDateTime(event.createdAt)}</time>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* right column */}
        <div>
          <div className="card">
            <h2>Payment</h2>
            <dl className="kv">
              <dt>Items</dt>
              <dd>{formatMoney(subtotal)}</dd>
              {order.discountAmount > 0 && (
                <>
                    <dt>Coupon {order.couponCode}</dt>
                    <dd style={{ color: "#16a34a" }}>−{formatMoney(order.discountAmount)}</dd>
                </>
            )}
              <dt className="total">Total</dt>
              <dd className="total">{formatMoney(order.totalAmount)}</dd>
            </dl>

            <hr style={{ border: 0, borderTop: "1px solid var(--border)", margin: "14px 0" }} />

            {payment ? (
              <dl className="kv">
                <dt>Status</dt>
                <dd><PaymentStatusBadge status={payment.status} /></dd>
                {payment.method && (
                  <>
                    <dt>Method</dt>
                    <dd>{METHOD_LABELS[payment.method] || payment.method}</dd>
                  </>
                )}
                {payment.paidAt && (
                  <>
                    <dt>Paid on</dt>
                    <dd>{formatDateTime(payment.paidAt)}</dd>
                  </>
                )}
                {payment.razorpayPaymentId && (
                  <>
                    <dt>Payment ID</dt>
                    <dd>{payment.razorpayPaymentId}</dd>
                  </>
                )}
                {payment.refundId && (
                  <>
                    <dt>Refund ID</dt>
                    <dd>{payment.refundId}</dd>
                  </>
                )}
              </dl>
            ) : (
              <p className="muted">No payment has been started for this order.</p>
            )}
          </div>

          <div className="card">
            <h2>Delivery address</h2>
            {order.address ? (
              <p className="address">
                <strong>{order.address.fullName}</strong><br />
                {order.address.addressLine}<br />
                {order.address.city}, {order.address.state} {order.address.postalCode}<br />
                {order.address.country}<br />
                Phone: {order.address.phone}
              </p>
            ) : (
              <p className="muted">No address on record.</p>
            )}
          </div>
        </div>
      </div>

      <div className="order-actions" style={{ marginTop: 24 }}>
        <button onClick={() => navigate("/orders")}>Back to My Orders</button>
        <button onClick={() => navigate("/products")}>Continue shopping</button>
      </div>
    </div>
  );
}

export default OrderDetails;
