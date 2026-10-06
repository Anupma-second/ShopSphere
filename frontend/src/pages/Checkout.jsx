import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import AddressForm from "../components/AddressForm";
import ProductImage from "../components/ProductImage";
import { getUser } from "../utils/auth";
import { notifyCartChanged } from "../utils/cart";
import { getErrorMessage } from "../utils/errors";
import { formatMoney } from "../utils/format";
import { openRazorpay } from "../utils/razorpay";
import "../styles/shop.css";
import "../styles/account.css";

function Checkout() {
  const navigate = useNavigate();
  const user = getUser();

  const [cart, setCart] = useState(null);           // null = loading
  const [addresses, setAddresses] = useState(null);
  const [selectedAddress, setSelectedAddress] = useState(null);
  const [addingAddress, setAddingAddress] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [placing, setPlacing] = useState(false);

  const [couponInput, setCouponInput] = useState("");
  const [coupon, setCoupon] = useState(null);       // quote from the server once applied
  const [couponError, setCouponError] = useState("");
  const [applying, setApplying] = useState(false);

  useEffect(() => {
    api.get("/cart-items")
      .then((res) => setCart(res.data))
      .catch((err) => { setError(getErrorMessage(err, "Could not load your cart.")); setCart([]); });

    api.get("/addresses")
      .then((res) => {
        setAddresses(res.data);
        setSelectedAddress(res.data[0]?.id ?? null);
        setAddingAddress(res.data.length === 0);
      })
      .catch((err) => { setError(getErrorMessage(err, "Could not load your addresses.")); setAddresses([]); });
  }, []);

  const saveAddress = async (values) => {
    const { data } = await api.post("/addresses", values);
    setAddresses((list) => [...list, data]);
    setSelectedAddress(data.id);
    setAddingAddress(false);
  };

  const applyCoupon = async (e) => {
    e.preventDefault();
    if (!couponInput.trim()) return;
    setApplying(true);
    setCouponError("");
    try {
      const { data } = await api.post("/orders/apply-coupon", { code: couponInput.trim() });
      setCoupon(data);
      setCouponInput("");
    } catch (err) {
      setCoupon(null);
      setCouponError(getErrorMessage(err, "This coupon can't be used."));
    } finally {
      setApplying(false);
    }
  };

  const removeCoupon = () => {
    setCoupon(null);
    setCouponError("");
  };

  const placeOrder = async () => {
    if (!selectedAddress) {
      setError("Please choose a delivery address.");
      return;
    }

    setError("");
    setMessage("");
    setPlacing(true);

    let createdOrderId;

    try {
      // 1. Create the order (stock is reserved, cart is cleared)
      const orderRes = await api.post("/orders/checkout", {
        addressId: selectedAddress,
        couponCode: coupon?.code,
      });
      createdOrderId = orderRes.data.id;
      notifyCartChanged();

      // 2. Create the Razorpay order
      const { data: payment } = await api.post("/payments/create", { orderId: createdOrderId });

      // 3. Open the popup
      openRazorpay({
        payment,
        user,
        onPaid: () => {
          setMessage("Payment successful! Your order has been confirmed.");
          setTimeout(() => navigate(`/orders/${createdOrderId}`), 1200);
        },
        onVerifyError: (err) => {
          setPlacing(false);
          setError(getErrorMessage(err, "Payment verification failed."));
        },
        onDismiss: () => {
          setPlacing(false);
          // the order exists and can be paid or cancelled from its page
          navigate(`/orders/${createdOrderId}`);
        },
        onFailed: (msg) => {
          setPlacing(false);
          setError(msg);
        },
      });
    } catch (err) {
      setPlacing(false);
      setError(getErrorMessage(err, "Failed to place order."));
      // if the order was created but payment setup failed, don't strand it
      if (createdOrderId) navigate(`/orders/${createdOrderId}`);
    }
  };

  if (cart === null || addresses === null) {
    return (
      <div className="shop-page">
        <h1>Checkout</h1>
        <div className="skeleton-card" style={{ aspectRatio: "auto", height: 220, marginTop: 18 }} />
      </div>
    );
  }

  if (cart.length === 0 && !placing && !message) {
    return (
      <div className="shop-page empty-state">
        <h1>Checkout</h1>
        <p className="muted" style={{ margin: "8px 0 18px" }}>Your cart is empty, so there's nothing to check out.</p>
        <Link to="/products" className="btn primary">Browse products</Link>
      </div>
    );
  }

  const count = cart.reduce((n, i) => n + i.quantity, 0);
  const subtotal = cart.reduce((sum, i) => sum + i.totalPrice, 0);
  const discount = coupon?.discount ?? 0;
  const total = Math.max(0, subtotal - discount);

  return (
    <div className="shop-page">
      <h1>Checkout</h1>
      {error && <p className="notice bad">{error}</p>}
      {message && <p className="notice ok">{message}</p>}

      <div className="cart-layout">
        <div>
          <section className="panel">
            <h2>1. Delivery address</h2>
            <p className="muted sub">Where should we send your order?</p>

            {addingAddress ? (
              <AddressForm
                onSave={saveAddress}
                onCancel={addresses.length > 0 ? () => setAddingAddress(false) : undefined}
                saveLabel="Use this address"
              />
            ) : (
              <div className="address-grid">
                {addresses.map((a) => (
                  <label key={a.id} className={`address-card ${selectedAddress === a.id ? "selected" : ""}`}>
                    <input
                      type="radio"
                      name="address"
                      checked={selectedAddress === a.id}
                      onChange={() => setSelectedAddress(a.id)}
                    />
                    <strong>{a.fullName}</strong>
                    <span>{a.addressLine}</span>
                    <span>{a.city}, {a.state} {a.postalCode}</span>
                    <span className="muted">Phone: {a.phone}</span>
                  </label>
                ))}
                <button type="button" className="add-card" onClick={() => setAddingAddress(true)}>
                  + Add a new address
                </button>
              </div>
            )}
          </section>

          <section className="panel">
            <h2>2. Review your items</h2>
            <p className="muted sub">
              {count} item{count === 1 ? "" : "s"} · <Link to="/cart">Edit cart</Link>
            </p>
            {cart.map((item) => (
              <div key={item.id} className="mini-line">
                <ProductImage src={item.imageUrl} name={item.productName} />
                <span>
                  {item.productName}
                  <span className="muted"> × {item.quantity}</span>
                </span>
                <strong>{formatMoney(item.totalPrice)}</strong>
              </div>
            ))}
          </section>
        </div>

        <aside className="summary">
          <h2>Order summary</h2>
          <div className="summary-row">
            <span>Items ({count})</span>
            <span>{formatMoney(subtotal)}</span>
          </div>
          {coupon && (
            <div className="summary-row discount">
              <span>Coupon {coupon.code}</span>
              <span>−{formatMoney(discount)}</span>
            </div>
          )}
          <div className="summary-row">
            <span>Delivery</span>
            <span className="free">FREE</span>
          </div>
          <div className="summary-row total">
            <span>Total</span>
            <span>{formatMoney(total)}</span>
          </div>

          <div className="coupon-box">
            {coupon ? (
              <div className="coupon-applied">
                <span>
                  <strong>{coupon.code}</strong> applied
                  <span className="muted"> · {coupon.summary}</span>
                </span>
                <button type="button" className="btn link" onClick={removeCoupon}>Remove</button>
              </div>
            ) : (
              <form className="coupon-form" onSubmit={applyCoupon}>
                <input
                  placeholder="Coupon code"
                  aria-label="Coupon code"
                  value={couponInput}
                  onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
                  maxLength={30}
                />
                <button type="submit" className="btn" disabled={applying || !couponInput.trim()}>
                  {applying ? "..." : "Apply"}
                </button>
              </form>
            )}
            {couponError && <p className="notice bad">{couponError}</p>}
            {coupon && <p className="notice ok">You save {formatMoney(discount)} on this order.</p>}
          </div>

          <button
            className="btn primary"
            onClick={placeOrder}
            disabled={placing || addingAddress || !selectedAddress || cart.length === 0}
          >
            {placing ? "Processing..." : `Place order & pay ${formatMoney(total)}`}
          </button>
          <p className="muted" style={{ marginTop: 12, textAlign: "center" }}>
            🔒 Payments are processed securely by Razorpay.
          </p>
        </aside>
      </div>
    </div>
  );
}

export default Checkout;