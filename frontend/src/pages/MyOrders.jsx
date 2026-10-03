import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";

function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");

  const navigate = useNavigate();

  useEffect(() => {
    api.get("/orders")
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        setError(
          error.response?.data?.message ||
          "Failed to load orders."
        );
      });
  }, []);

  return (
    <div>
      <h1>My Orders</h1>

      {error && <p>{error}</p>}

      {orders.length === 0 ? (
        <p>You have no orders yet.</p>
      ) : (
        orders.map((order) => (
          <div key={order.id}>
            <h2>Order #{order.id}</h2>

            <p>Amount: ₹{order.totalAmount}</p>
            <p>Status: {order.status}</p>
            <p>
              Date:{" "}
              {order.orderDate
                ? new Date(order.orderDate).toLocaleString()
                : "N/A"}
            </p>

            <button
              onClick={() => navigate(`/orders/${order.id}`)}
            >
              View Details
            </button>

            <hr />
          </div>
        ))
      )}
    </div>
  );
}

export default MyOrders;