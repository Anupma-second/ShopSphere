import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/axios";
import AccountNav from "../components/AccountNav";
import ProductImage from "../components/ProductImage";
import { notifyCartChanged } from "../utils/cart";
import { getErrorMessage } from "../utils/errors";
import { formatMoney } from "../utils/format";
import "../styles/shop.css";
import "../styles/account.css";

function Wishlist() {
  const [items, setItems] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    api.get("/wishlist-items")
      .then((res) => setItems(res.data))
      .catch((err) => {
        setMessage({ ok: false, text: getErrorMessage(err, "Could not load your wishlist.") });
        setItems([]);
      });
  }, []);

  const removeLocal = (id) => setItems((list) => list.filter((i) => i.id !== id));

  const remove = async (item) => {
    setBusyId(item.id);
    setMessage(null);
    try {
      await api.delete(`/wishlist-items/${item.id}`);
      removeLocal(item.id);
    } catch (err) {
      setMessage({ ok: false, text: getErrorMessage(err, "Could not remove the item.") });
    } finally {
      setBusyId(null);
    }
  };

  const moveToCart = async (item) => {
    setBusyId(item.id);
    setMessage(null);
    try {
      await api.post("/cart-items/add", { productId: item.productId, quantity: 1 });
      notifyCartChanged();
      await api.delete(`/wishlist-items/${item.id}`);
      removeLocal(item.id);
      setMessage({ ok: true, text: `“${item.productName}” moved to your cart.` });
    } catch (err) {
      setMessage({ ok: false, text: getErrorMessage(err, "Could not move the item to your cart.") });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="shop-page">
      <h1>My account</h1>
      <div className="account-layout">
        <AccountNav />

        <section className="panel">
          <h2>Wishlist {items && items.length > 0 && <span className="muted">({items.length})</span>}</h2>
          <p className="muted sub">Products you've saved for later.</p>

          {message && (
            <p className={`notice ${message.ok ? "ok" : "bad"}`} style={{ marginBottom: 14 }}>
              {message.text} {message.ok && <Link to="/cart">View cart →</Link>}
            </p>
          )}

          {items === null && <div className="skeleton-card" style={{ aspectRatio: "auto", height: 160 }} />}

          {items && items.length === 0 && (
            <div className="empty-state" style={{ padding: "32px 8px" }}>
              <h2>Your wishlist is empty</h2>
              <p className="muted" style={{ margin: "8px 0 16px" }}>
                Tap “Save to wishlist” on any product to keep it here.
              </p>
              <Link to="/products" className="btn primary">Browse products</Link>
            </div>
          )}

          {items && items.length > 0 && (
            <div className="product-grid">
              {items.map((item) => (
                <div key={item.id} className="wish-card">
                  <Link to={`/products/${item.productId}`}>
                    <ProductImage src={item.imageUrl} name={item.productName} />
                  </Link>
                  <Link to={`/products/${item.productId}`} className="name">{item.productName}</Link>
                  <div className="product-card-price">{formatMoney(item.price)}</div>
                  {item.stock === 0 && <span className="stock-note bad">Out of stock</span>}
                  <div className="actions">
                    <button
                      className="btn primary"
                      onClick={() => moveToCart(item)}
                      disabled={busyId === item.id || item.stock === 0}
                    >
                      Move to cart
                    </button>
                    <button className="btn" onClick={() => remove(item)} disabled={busyId === item.id}>
                      Remove
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default Wishlist;