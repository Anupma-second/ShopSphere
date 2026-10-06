import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { formatDate, formatMoney } from "../utils/format";
import { OrderStatusBadge, PaymentStatusBadge } from "../components/StatusBadge";
import "../styles/orders.css";

function MyOrders() {
  const [orders, setOrders] = useState(null); // null = still loading
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    api.get("/orders")
      .then((response) => setOrders(response.data))
      .catch((err) => {
        setError(getErrorMessage(err, "Failed to load orders."));
        setOrders([]);
      });
  }, []);

  return (
    <div className="order-page">
      <h1>My Orders</h1>

      {error && <div className="banner banner-bad">{error}</div>}

      {orders === null && (
        <>
          <div className="skeleton" style={{ height: 96 }} />
          <div className="skeleton" style={{ height: 96 }} />
        </>
      )}

      {orders && orders.length === 0 && !error && (
        <div className="state-box">
          <h2>No orders yet</h2>
          <p className="muted">When you place an order it will show up here.</p>
          <p style={{ marginTop: 16 }}>
            <Link to="/products">Start shopping</Link>
          </p>
        </div>
      )}

      {orders && orders.map((order) => {
        const names = order.items.map((i) => `${i.productName} × ${i.quantity}`);
        return (
          <div className="order-card" key={order.id}>
            <div className="order-card-top">
              <span className="order-card-title">Order #{order.id}</span>
              <span className="muted">Placed {formatDate(order.orderDate)}</span>
            </div>

            <p className="order-card-items muted">
              {names.slice(0, 3).join(", ")}
              {names.length > 3 ? ` and ${names.length - 3} more` : ""}
            </p>

            <div className="order-card-bottom">
              <span className="order-head-badges">
                <OrderStatusBadge status={order.status} />
                <PaymentStatusBadge status={order.payment?.status} />
              </span>
              <span className="order-card-total">{formatMoney(order.totalAmount)}</span>
              <button onClick={() => navigate(`/orders/${order.id}`)}>
                View details
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default MyOrders;
