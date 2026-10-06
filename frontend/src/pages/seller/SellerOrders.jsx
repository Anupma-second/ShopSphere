import { useEffect, useState } from "react";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import { OrderStatusBadge } from "../../components/StatusBadge";
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

function SellerOrders() {
  const [status, setStatus] = useState("CONFIRMED");
  const [orders, setOrders] = useState(null);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    let cancelled = false;
    api.get("/seller/orders", { params: { status: status || undefined } })
      .then((res) => { if (!cancelled) setOrders(res.data); })
      .catch((err) => {
        if (!cancelled) {
          setMessage({ tone: "bad", text: getErrorMessage(err, "Failed to load orders.") });
          setOrders([]);
        }
      });
    return () => { cancelled = true; };
  }, [status]);

  const changeFilter = (value) => {
    setOrders(null);
    setMessage(null);
    setStatus(value);
  };

  // replace one order in the list after it was updated
  const onUpdated = (updated, text) => {
    setOrders((list) => list.map((o) => (o.orderId === updated.orderId ? updated : o)));
    setMessage({ tone: "ok", text });
  };

  return (
    <div className="order-page">
      <DashboardNav area="seller" />
      <h1>Orders</h1>
      <p className="muted">Orders that contain your products. You only see your own items.</p>

      <div className="toolbar">
        <select value={status} onChange={(e) => changeFilter(e.target.value)}>
          {FILTERS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
        </select>
      </div>

      {message && <div className={`banner banner-${message.tone}`}>{message.text}</div>}

      {orders === null && (
        <>
          <div className="skeleton" style={{ height: 120 }} />
          <div className="skeleton" style={{ height: 120 }} />
        </>
      )}

      {orders && orders.length === 0 && (
        <div className="state-box">
          <h2>No orders here</h2>
          <p className="muted">
            {status === "CONFIRMED" ? "Nothing waiting to be shipped. 🎉" : "Try another filter."}
          </p>
        </div>
      )}

      {orders && orders.map((order) => (
        <SellerOrderCard
          key={order.orderId}
          order={order}
          onUpdated={onUpdated}
          onError={(text) => setMessage({ tone: "bad", text })}
        />
      ))}
    </div>
  );
}

function SellerOrderCard({ order, onUpdated, onError }) {
  const [carrier, setCarrier] = useState("");
  const [tracking, setTracking] = useState("");
  const [busy, setBusy] = useState(false);
  const a = order.shippingAddress;
  const active = order.status === "CONFIRMED" || order.status === "SHIPPED";

  const update = async (newStatus) => {
    setBusy(true);
    try {
      const { data } = await api.put(`/seller/orders/${order.orderId}/status`, null, {
        params: {
          status: newStatus,
          carrier: carrier.trim() || undefined,
          trackingNumber: tracking.trim() || undefined,
        },
      });
      onUpdated(data, newStatus === "SHIPPED"
        ? `Order #${order.orderId} marked as shipped.`
        : `Order #${order.orderId} marked as delivered.`);
    } catch (err) {
      onError(getErrorMessage(err, "Could not update the order."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="order-card">
      <div className="order-card-top">
        <span className="order-card-title">Order #{order.orderId}</span>
        <span className="order-head-badges">
          <OrderStatusBadge status={order.status} />
          <span className="muted">{formatDate(order.orderDate)}</span>
        </span>
      </div>

      <div className="dash-grid" style={{ marginTop: 12 }}>
        <table className="table">
          <thead>
            <tr><th>Item</th><th className="num">Qty</th><th className="num">Price</th></tr>
          </thead>
          <tbody>
            {order.items.map((i) => (
              <tr key={i.productId}>
                <td>{i.productName}</td>
                <td className="num">{i.quantity}</td>
                <td className="num">{formatMoney(i.price * i.quantity)}</td>
              </tr>
            ))}
            <tr>
              <td><strong>Your total</strong></td>
              <td />
              <td className="num"><strong>{formatMoney(order.sellerSubtotal)}</strong></td>
            </tr>
          </tbody>
        </table>

        <div className="address">
          <div className="muted">Ship to</div>
          {a ? (
            <>
              <strong>{a.fullName}</strong><br />
              {a.addressLine}<br />
              {a.city}, {a.state} {a.postalCode}<br />
              {a.country}<br />
              📞 {a.phone}
            </>
          ) : (
            <span className="muted">{order.customerName}</span>
          )}
          {order.trackingNumber && (
            <p style={{ marginTop: 10 }}>
              <span className="muted">Tracking:</span> {order.carrier ? `${order.carrier} · ` : ""}
              {order.trackingNumber}
            </p>
          )}
        </div>
      </div>

      {active && !order.canUpdateStatus && (
        <div className="banner banner-warn">
          This order also has items from other sellers, so the store admin will ship it.
        </div>
      )}

      {order.canUpdateStatus && order.status === "CONFIRMED" && (
        <div className="order-actions inline-form">
          <input placeholder="Carrier (e.g. Delhivery)" value={carrier} onChange={(e) => setCarrier(e.target.value)} />
          <input placeholder="Tracking number" value={tracking} onChange={(e) => setTracking(e.target.value)} />
          <button className="primary" onClick={() => update("SHIPPED")} disabled={busy}>
            {busy ? "Saving..." : "Mark as shipped"}
          </button>
        </div>
      )}

      {order.canUpdateStatus && order.status === "SHIPPED" && (
        <div className="order-actions">
          <button className="primary" onClick={() => update("DELIVERED")} disabled={busy}>
            {busy ? "Saving..." : "Mark as delivered"}
          </button>
        </div>
      )}
    </div>
  );
}

export default SellerOrders;