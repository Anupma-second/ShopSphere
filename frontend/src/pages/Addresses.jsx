import { useEffect, useState } from "react";
import api from "../api/axios";
import AccountNav from "../components/AccountNav";
import AddressForm from "../components/AddressForm";
import { getErrorMessage } from "../utils/errors";
import "../styles/shop.css";
import "../styles/account.css";

function Addresses() {
  const [addresses, setAddresses] = useState(null);
  const [editing, setEditing] = useState(null);   // null | "new" | address object
  const [message, setMessage] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    api.get("/addresses")
      .then((res) => setAddresses(res.data))
      .catch((err) => {
        setMessage({ ok: false, text: getErrorMessage(err, "Could not load your addresses.") });
        setAddresses([]);
      });
  }, [reloadKey]);

  const save = async (values) => {
    if (editing === "new") {
      await api.post("/addresses", values);
      setMessage({ ok: true, text: "Address added." });
    } else {
      await api.put(`/addresses/${editing.id}`, values);
      setMessage({ ok: true, text: "Address updated." });
    }
    setEditing(null);
    setReloadKey((k) => k + 1);
  };

  const remove = async (a) => {
    if (!window.confirm(`Delete the address for ${a.fullName}, ${a.city}?`)) return;
    setMessage(null);
    try {
      await api.delete(`/addresses/${a.id}`);
      setMessage({ ok: true, text: "Address deleted." });
      setReloadKey((k) => k + 1);
    } catch (err) {
      setMessage({ ok: false, text: getErrorMessage(err, "Could not delete the address.") });
    }
  };

  return (
    <div className="shop-page">
      <h1>My account</h1>
      <div className="account-layout">
        <AccountNav />

        <section className="panel">
          <h2>Saved addresses</h2>
          <p className="muted sub">Choose one of these at checkout. Old orders keep the address they were sent to.</p>

          {message && <p className={`notice ${message.ok ? "ok" : "bad"}`} style={{ marginBottom: 14 }}>{message.text}</p>}

          {editing ? (
            <>
              <h3 style={{ margin: "0 0 12px", color: "var(--text-h)" }}>
                {editing === "new" ? "Add a new address" : "Edit address"}
              </h3>
              <AddressForm
                key={editing === "new" ? "new" : editing.id}
                initial={editing === "new" ? undefined : editing}
                onSave={save}
                onCancel={() => setEditing(null)}
              />
            </>
          ) : addresses === null ? (
            <div className="skeleton-card" style={{ aspectRatio: "auto", height: 120 }} />
          ) : (
            <div className="address-grid">
              {addresses.map((a) => (
                <div key={a.id} className="address-card">
                  <strong>{a.fullName}</strong>
                  <span>{a.addressLine}</span>
                  <span>{a.city}, {a.state} {a.postalCode}</span>
                  <span>{a.country}</span>
                  <span className="muted">Phone: {a.phone}</span>
                  <div className="card-actions">
                    <button className="btn small" onClick={() => { setMessage(null); setEditing(a); }}>Edit</button>
                    <button className="btn small danger" onClick={() => remove(a)}>Delete</button>
                  </div>
                </div>
              ))}
              <button className="add-card" onClick={() => { setMessage(null); setEditing("new"); }}>
                + Add a new address
              </button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default Addresses;