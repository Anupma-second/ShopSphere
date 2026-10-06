import { useEffect, useState } from "react";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import { OrderStatusBadge, PaymentStatusBadge } from "../../components/StatusBadge";
import { getErrorMessage } from "../../utils/errors";
import { formatDate, formatMoney } from "../../utils/format";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

const FILTERS = [
  { value: "", label: "All orders" },
  { value: "CONFIRMED", label: "To ship" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "PENDING", label: "Awaiting payment" },
  { value: "CANCELLED", label: "Cancelled" },
];

function AdminOrders() {
  const [orders, setOrders] = useState(null);
  const [filter, setFilter] = useState("");
  const [message, setMessage] = useState(null);

  useEffect(() => {
    api.get("/admin/orders")
      .then((res) => setOrders(res.data))
      .catch((err) => {
        setMessage({ tone: "bad", text: getErrorMessage(err, "Failed to load orders.") });
        setOrders([]);
      });
  }, []);

  const onUpdated = (updated, text) => {
    setOrders((list) => list.map((o) => (o.id === updated.id ? updated : o)));
    setMessage({ tone: "ok", text });
  };

  const visible = orders?.filter((o) => !filter || o.status === filter) ?? null;

  return (
    <div className="order-page">
      <DashboardNav area="admin" />
      <h1>All orders</h1>

      <div className="toolbar">
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          {FILTERS.map((f) => {
            const count = orders?.filter((o) => !f.value || o.status === f.value).length;
            return (
              <option key={f.value} value={f.value}>
                {f.label}{count !== undefined ? ` (${count})` : ""}
              </option>
            );
          })}
        </select>
      </div>

      {message && <div className={`banner banner-${message.tone}`}>{message.text}</div>}

      {visible === null && (
        <>
          <div className="skeleton" style={{ height: 110 }} />
          <div className="skeleton" style={{ height: 110 }} />
        </>
      )}

      {visible && visible.length === 0 && (
        <div className="state-box">
          <h2>No orders here</h2>
          <p className="muted">Try another filter.</p>
        </div>
      )}

      {visible && visible.map((order) => (
        <AdminOrderCard
          key={order.id}
          order={order}
          onUpdated={onUpdated}
          onError={(text) => setMessage({ tone: "bad", text })}
        />
      ))}
    </div>
  );
}

function AdminOrderCard({ order, onUpdated, onError }) {
  const [carrier, setCarrier] = useState("");
  const [tracking, setTracking] = useState("");
  const [busy, setBusy] = useState(false);
  const a = order.address;
  const names = order.items.map((i) => `${i.productName} × ${i.quantity}`);

  const run = async (request, successText) => {
    setBusy(true);
    try {
      const { data } = await request();
      onUpdated(data, successText);
    } catch (err) {
      onError(getErrorMessage(err, "Could not update the order."));
    } finally {
      setBusy(false);
    }
  };

  const setStatus = (status, text, extra = {}) =>
    run(() => api.put(`/admin/orders/${order.id}/status`, null, { params: { status, ...extra } }), text);

  const ship = () =>
    setStatus("SHIPPED", `Order #${order.id} marked as shipped.`, {
      carrier: carrier.trim() || undefined,
      trackingNumber: tracking.trim() || undefined,
    });

  const cancel = () => {
    const paid = order.status === "CONFIRMED";
    const warning = paid
      ? `Cancel order #${order.id}? The customer has paid, so a refund of ${formatMoney(order.totalAmount)} will be started.`
      : `Cancel order #${order.id}?`;
    if (window.confirm(warning)) {
      setStatus("CANCELLED", `Order #${order.id} cancelled${paid ? " and refund started" : ""}.`);
    }
  };

  const retryRefund = () =>
    run(() => api.post(`/admin/orders/${order.id}/refund`), `Refund retried for order #${order.id}.`);

  const refundStuck = order.payment?.status === "REFUND_REQUIRED";

  return (
    <div className="order-card">
      <div className="order-card-top">
        <span className="order-card-title">Order #{order.id}</span>
        <span className="order-head-badges">
          <OrderStatusBadge status={order.status} />
          <PaymentStatusBadge status={order.payment?.status} />
          <span className="muted">{formatDate(order.orderDate)}</span>
        </span>
      </div>

      <p className="order-card-items muted">
        {names.slice(0, 4).join(", ")}
        {names.length > 4 ? ` and ${names.length - 4} more` : ""}
      </p>

      <div className="order-card-bottom">
        <span className="muted">
          {a ? `${a.fullName}, ${a.city}, ${a.state}` : "No address"}
          {order.trackingNumber && ` · ${order.carrier ? `${order.carrier} ` : ""}${order.trackingNumber}`}
        </span>
        <span className="order-card-total">{formatMoney(order.totalAmount)}</span>
      </div>

      {order.payment?.failureReason && order.payment.status !== "PAID" && (
        <div className="banner banner-warn">{order.payment.failureReason}</div>
      )}

      {order.status === "CONFIRMED" && (
        <div className="order-actions inline-form">
          <input placeholder="Carrier" value={carrier} onChange={(e) => setCarrier(e.target.value)} />
          <input placeholder="Tracking number" value={tracking} onChange={(e) => setTracking(e.target.value)} />
          <button className="primary" onClick={ship} disabled={busy}>Mark as shipped</button>
          <button className="danger" onClick={cancel} disabled={busy}>Cancel &amp; refund</button>
        </div>
      )}

      {order.status === "PENDING" && (
        <div className="order-actions">
          <button className="danger" onClick={cancel} disabled={busy}>Cancel order</button>
        </div>
      )}

      {order.status === "SHIPPED" && (
        <div className="order-actions">
          <button
            className="primary"
            onClick={() => setStatus("DELIVERED", `Order #${order.id} marked as delivered.`)}
            disabled={busy}
          >
            Mark as delivered
          </button>
        </div>
      )}

      {refundStuck && (
        <div className="order-actions">
          <button onClick={retryRefund} disabled={busy}>Retry refund</button>
        </div>
      )}
    </div>
  );
}

export default AdminOrders;