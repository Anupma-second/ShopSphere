import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";

function Cart() {
  const [cartItems, setCartItems] = useState([]);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const loadCart = () => {
    api.get("/cart-items")
      .then((response) => {
        setCartItems(response.data);
        setError("");
      })
      .catch((err) => setError(getErrorMessage(err, "Failed to load cart.")));
  };

  useEffect(() => {
    loadCart();
  }, []);

  const updateQuantity = async (id, quantity) => {
    try {
      await api.put(`/cart-items/${id}/quantity`, { quantity });
      loadCart();
    } catch (err) {
      setError(getErrorMessage(err, "Failed to update quantity."));
    }
  };

  const removeItem = async (id) => {
    try {
      await api.delete(`/cart-items/${id}`);
      loadCart();
    } catch (err) {
      setError(getErrorMessage(err, "Failed to remove item."));
    }
  };

  const cartTotal = cartItems.reduce((total, item) => total + item.totalPrice, 0);

  return (
    <div>
      <h1>My Cart</h1>
      {error && <p>{error}</p>}

      {cartItems.length === 0 ? (
        <p>Your cart is empty.</p>
      ) : (
        <>
          {cartItems.map((item) => (
            <div key={item.id}>
              <h2>{item.productName}</h2>
              <p>Price: ₹{item.price}</p>
              <p>
                Quantity:
                <button
                  onClick={() => updateQuantity(item.id, item.quantity - 1)}
                  disabled={item.quantity === 1}
                >
                  -
                </button>
                {" "}{item.quantity}{" "}
                <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>
                  +
                </button>
              </p>
              <p>Total: ₹{item.totalPrice}</p>
              <button onClick={() => removeItem(item.id)}>Remove</button>
              <hr />
            </div>
          ))}

          <h2>Cart Total: ₹{cartTotal.toFixed(2)}</h2>
          {/* FIX: client-side navigation instead of a full page reload */}
          <button onClick={() => navigate("/checkout")}>Checkout</button>
        </>
      )}
    </div>
  );
}

export default Cart;
