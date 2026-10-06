import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import ProductImage from "../components/ProductImage";
import { notifyCartChanged } from "../utils/cart";
import { getErrorMessage } from "../utils/errors";
import { formatMoney } from "../utils/format";
import "../styles/shop.css";
import "../styles/account.css";

const MAX_QTY = 10;

function Cart() {
  const navigate = useNavigate();
  const [items, setItems] = useState(null);   // null = loading
  const [busyId, setBusyId] = useState(null);
  const [message, setMessage] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    api.get("/cart-items")
      .then((res) => {
        setItems(res.data);
        notifyCartChanged();   // keep the header's cart count in sync
      })
      .catch((err) => {
        setMessage({ ok: false, text: getErrorMessage(err, "Could not load your cart.") });
        setItems([]);
      });
  }, [reloadKey]);

  const reload = () => setReloadKey((k) => k + 1);

  const run = async (id, action, failText) => {
    setBusyId(id);
    setMessage(null);
    try {
      await action();
      reload();
    } catch (err) {
      setMessage({ ok: false, text: getErrorMessage(err, failText) });
    } finally {
      setBusyId(null);
    }
  };

  const setQuantity = (item, quantity) =>
    run(item.id, () => api.put(`/cart-items/${item.id}/quantity`, { quantity }), "Could not update the quantity.");

  const remove = (item) =>
    run(item.id, () => api.delete(`/cart-items/${item.id}`), "Could not remove the item.");

  const saveForLater = (item) =>
    run(item.id, async () => {
      try {
        await api.post("/wishlist-items", { productId: item.productId });
      } catch (err) {
        if (err?.response?.status !== 409) throw err;   // already saved is fine
      }
      await api.delete(`/cart-items/${item.id}`);
      setMessage({ ok: true, text: `“${item.productName}” moved to your wishlist.` });
    }, "Could not save the item for later.");

  if (items === null) {
    return (
      <div className="shop-page">
        <h1>Your cart</h1>
        <div className="skeleton-card" style={{ aspectRatio: "auto", height: 200, marginTop: 18 }} />
      </div>
    );
  }

  const count = items.reduce((n, i) => n + i.quantity, 0);
  const subtotal = items.reduce((sum, i) => sum + i.totalPrice, 0);
  const problems = items.filter((i) => i.stock === 0 || i.quantity > i.stock);

  return (
    <div className="shop-page">
      <h1>Your cart {count > 0 && <span className="muted">({count} item{count === 1 ? "" : "s"})</span>}</h1>

      {message && (
        <p className={`notice ${message.ok ? "ok" : "bad"}`}>
          {message.text} {message.ok && <Link to="/wishlist">View wishlist →</Link>}
        </p>
      )}

      {items.length === 0 ? (
        <div className="empty-state">
          <h2>Your cart is empty</h2>
          <p className="muted" style={{ margin: "8px 0 18px" }}>Looks like you haven't added anything yet.</p>
          <Link to="/products" className="btn primary">Start shopping</Link>
        </div>
      ) : (
        <div className="cart-layout">
          <div>
            {items.map((item) => {
              const busy = busyId === item.id;
              const maxQty = Math.min(Math.max(item.stock, 1), MAX_QTY);
              return (
                <div key={item.id} className="cart-line">
                  <Link to={`/products/${item.productId}`}>
                    <ProductImage src={item.imageUrl} name={item.productName} />
                  </Link>

                  <div className="cart-line-info">
                    <Link to={`/products/${item.productId}`} className="cart-line-name">{item.productName}</Link>
                    <span className="muted">{formatMoney(item.price)} each</span>

                    {item.stock === 0 ? (
                      <span className="stock-note bad">Out of stock. Remove it to continue.</span>
                    ) : item.quantity > item.stock ? (
                      <span className="stock-note bad">Only {item.stock} left. Lower the quantity to continue.</span>
                    ) : item.stock <= 5 ? (
                      <span className="stock-note warn">Only {item.stock} left in stock</span>
                    ) : null}

                    <div className="qty small" aria-label={`Quantity of ${item.productName}`}>
                      <button
                        onClick={() => setQuantity(item, item.quantity - 1)}
                        disabled={busy || item.quantity <= 1}
                        aria-label="Decrease quantity"
                      >
                        −
                      </button>
                      <span>{item.quantity}</span>
                      <button
                        onClick={() => setQuantity(item, item.quantity + 1)}
                        disabled={busy || item.quantity >= maxQty}
                        aria-label="Increase quantity"
                      >
                        +
                      </button>
                    </div>

                    <div className="cart-line-actions">
                      <button className="btn link" onClick={() => saveForLater(item)} disabled={busy}>
                        Save for later
                      </button>
                      <button className="btn link" style={{ color: "#dc2626" }} onClick={() => remove(item)} disabled={busy}>
                        Remove
                      </button>
                    </div>
                  </div>

                  <div className="cart-line-total">{formatMoney(item.totalPrice)}</div>
                </div>
              );
            })}
          </div>

          <aside className="summary">
            <h2>Order summary</h2>
            <div className="summary-row">
              <span>Subtotal ({count} item{count === 1 ? "" : "s"})</span>
              <span>{formatMoney(subtotal)}</span>
            </div>
            <div className="summary-row">
              <span>Delivery</span>
              <span className="free">FREE</span>
            </div>
            <div className="summary-row total">
              <span>Total</span>
              <span>{formatMoney(subtotal)}</span>
            </div>
            <button
              className="btn primary"
              onClick={() => navigate("/checkout")}
              disabled={problems.length > 0}
            >
              Proceed to checkout
            </button>
            {problems.length > 0 && (
              <p className="notice bad">Fix the items marked in red before checking out.</p>
            )}
            <p className="muted" style={{ marginTop: 12, textAlign: "center" }}>
              <Link to="/products">Continue shopping</Link>
            </p>
          </aside>
        </div>
      )}
    </div>
  );
}

export default Cart;