import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { openRazorpay } from "../utils/razorpay";

const emptyForm = {
  fullName: "",
  phone: "",
  addressLine: "",
  city: "",
  state: "",
  postalCode: "",
  country: "India",
};

function Checkout() {
  const [addresses, setAddresses] = useState([]);
  const [selectedAddress, setSelectedAddress] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [placing, setPlacing] = useState(false);
  const navigate = useNavigate();

  const user = JSON.parse(localStorage.getItem("user") || "null");

  const loadAddresses = () =>
    api.get("/addresses")
      .then((response) => {
        setAddresses(response.data);
        if (response.data.length > 0) {
          setSelectedAddress((current) => current || response.data[0].id);
        } else {
          setShowForm(true);
        }
      })
      .catch((err) => setError(getErrorMessage(err, "Failed to load addresses.")));

  useEffect(() => {
    loadAddresses();
  }, []);

  const saveAddress = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const { data } = await api.post("/addresses", form);
      setForm(emptyForm);
      setShowForm(false);
      await loadAddresses();
      setSelectedAddress(data.id);
    } catch (err) {
      setError(getErrorMessage(err, "Could not save address."));
    }
  };

  const placeOrder = async () => {
    if (!selectedAddress) {
      setError("Please select an address.");
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
      });
      createdOrderId = orderRes.data.id;

      // 2. Create the Razorpay order
      const { data: payment } = await api.post("/payments/create", {
        orderId: createdOrderId,
      });

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

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  return (
    <div>
      <h1>Checkout</h1>
      {error && <p>{error}</p>}
      {message && <p>{message}</p>}

      <h2>Select Delivery Address</h2>
      {addresses.map((address) => (
        <div key={address.id}>
          <label>
            <input
              type="radio"
              name="address"
              value={address.id}
              checked={selectedAddress === address.id}
              onChange={() => setSelectedAddress(address.id)}
            />{" "}
            {address.fullName}, {address.phone}, {address.addressLine},{" "}
            {address.city}, {address.state} - {address.postalCode}
          </label>
          <hr />
        </div>
      ))}

      <button type="button" onClick={() => setShowForm(!showForm)}>
        {showForm ? "Cancel" : "Add new address"}
      </button>

      {showForm && (
        <form onSubmit={saveAddress}>
          <h3>New address</h3>
          {[
            ["fullName", "Full name"],
            ["phone", "Phone"],
            ["addressLine", "Address"],
            ["city", "City"],
            ["state", "State"],
            ["postalCode", "Postal code"],
            ["country", "Country"],
          ].map(([name, label]) => (
            <div key={name}>
              <input
                name={name}
                placeholder={label}
                value={form[name]}
                onChange={onChange}
                required
              />
            </div>
          ))}
          <button type="submit">Save address</button>
        </form>
      )}

      <hr />
      <button onClick={placeOrder} disabled={addresses.length === 0 || placing}>
        {placing ? "Processing..." : "Place Order"}
      </button>
    </div>
  );
}

export default Checkout;
