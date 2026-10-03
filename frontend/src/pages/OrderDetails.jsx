import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { openRazorpay } from "../utils/razorpay";

function OrderDetails() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
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

  // Retry payment for an order that is still PENDING
  const payNow = async () => {
    setBusy(true);
    setNotice("");
    try {
      const { data: payment } = await api.post("/payments/create", {
        orderId: order.id,
      });
      openRazorpay({
        payment,
        user,
        onPaid: async () => {
          setNotice("Payment successful! Your order is confirmed.");
          setBusy(false);
          await loadOrder();
        },
        onVerifyError: (err) => {
          setBusy(false);
          setNotice(getErrorMessage(err, "Payment verification failed."));
          loadOrder();
        },
        onDismiss: () => setBusy(false),
        onFailed: (msg) => {
          setBusy(false);
          setNotice(msg);
        },
      });
    } catch (err) {
      setBusy(false);
      setNotice(getErrorMessage(err, "Could not start payment."));
    }
  };

  const cancelOrder = async () => {
    if (!window.confirm("Cancel this order?")) return;
    setBusy(true);
    try {
      await api.put(`/orders/${order.id}/cancel`);
      setNotice("Order cancelled.");
      await loadOrder();
    } catch (err) {
      setNotice(getErrorMessage(err, "Could not cancel order."));
    } finally {
      setBusy(false);
    }
  };

  if (error) {
    return (
      <div>
        <p>{error}</p>
        <button onClick={() => navigate("/orders")}>Back to My Orders</button>
      </div>
    );
  }

  if (!order) {
    return <p>Loading order...</p>;
  }

  return (
    <div>
      <h1>Order #{order.id}</h1>
      {notice && <p>{notice}</p>}

      <h2>Order Summary</h2>
      <p><strong>Status:</strong> {order.status}</p>
      <p><strong>Total:</strong> ₹{order.totalAmount}</p>
      <p>
        <strong>Order Date:</strong>{" "}
        {order.orderDate ? new Date(order.orderDate).toLocaleString() : "N/A"}
      </p>

      {order.status === "PENDING" && (
        <p>
          <button onClick={payNow} disabled={busy}>Pay now</button>{" "}
          <button onClick={cancelOrder} disabled={busy}>Cancel order</button>
        </p>
      )}

      <h2>Delivery Address</h2>
      {order.address && (
        <p>
          {order.address.fullName}
          <br />
          {order.address.phone}
          <br />
          {order.address.addressLine}
          <br />
          {order.address.city}, {order.address.state}
          <br />
          {order.address.postalCode}
        </p>
      )}

      <h2>Items</h2>
      {order.items && order.items.length > 0 ? (
        order.items.map((item) => (
          // FIX: the API returns productId/productName, not a nested product
          <div key={item.productId}>
            <p>Product: {item.productName}</p>
            <p>Quantity: {item.quantity}</p>
            <p>Price: ₹{item.price}</p>
            <p>Subtotal: ₹{(item.price * item.quantity).toFixed(2)}</p>
            <hr />
          </div>
        ))
      ) : (
        <p>No items found.</p>
      )}

      <button onClick={() => navigate("/orders")}>Back to My Orders</button>
      <button onClick={() => navigate("/products")}>Continue Shopping</button>
    </div>
  );
}

export default OrderDetails;
