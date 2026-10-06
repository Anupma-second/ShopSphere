import { useEffect, useState } from "react";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import { getErrorMessage } from "../../utils/errors";
import { formatDate, formatMoney } from "../../utils/format";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

const EMPTY = {
  id: null,
  code: "",
  type: "PERCENT",
  percent: "",
  maxDiscount: "",
  flatAmount: "",
  minOrderAmount: "",
  expiresOn: "",        // yyyy-mm-dd, coupon works until the end of that day
  usageLimit: "",
  oncePerCustomer: false,
  active: true,
};

const num = (v) => (v === "" || v === null || v === undefined ? null : Number(v));

const toForm = (c) => ({
  id: c.id,
  code: c.code,
  type: c.discountType ?? "PERCENT",
  percent: c.discountPercentage ?? "",
  maxDiscount: c.maxDiscount ?? "",
  flatAmount: c.flatAmount ?? "",
  minOrderAmount: c.minOrderAmount ?? "",
  expiresOn: c.expiresAt ? c.expiresAt.slice(0, 10) : "",
  usageLimit: c.usageLimit ?? "",
  oncePerCustomer: !!c.oncePerCustomer,
  active: c.active,
});

const toBody = (f) => ({
  code: f.code.trim(),
  discountType: f.type,
  discountPercentage: f.type === "PERCENT" ? num(f.percent) : null,
  maxDiscount: f.type === "PERCENT" ? num(f.maxDiscount) : null,
  flatAmount: f.type === "FIXED" ? num(f.flatAmount) : null,
  minOrderAmount: num(f.minOrderAmount),
  expiresAt: f.expiresOn ? `${f.expiresOn}T23:59:59` : null,
  usageLimit: num(f.usageLimit),
  oncePerCustomer: f.oncePerCustomer,
  active: f.active,
});

const describe = (c) => {
  const main = c.discountType === "FIXED"
    ? `${formatMoney(c.flatAmount)} off`
    : `${c.discountPercentage}% off${c.maxDiscount ? `, up to ${formatMoney(c.maxDiscount)}` : ""}`;
  return main;
};

const status = (c) => {
  if (!c.active) return { tone: "muted", text: "Off" };
  if (c.expired) return { tone: "bad", text: "Expired" };
  if (c.usedUp) return { tone: "warn", text: "Used up" };
  return { tone: "ok", text: "Active" };
};

function AdminCoupons() {
  const [coupons, setCoupons] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [form, setForm] = useState(null);       // null = form closed
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    api.get("/coupons")
      .then((res) => setCoupons([...res.data].sort((a, b) => b.id - a.id)))
      .catch((err) => {
        setMessage({ tone: "bad", text: getErrorMessage(err, "Failed to load coupons.") });
        setCoupons([]);
      });
  }, [reloadKey]);

  const reload = () => setReloadKey((k) => k + 1);
  const set = (field) => (e) => {
    const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setForm((f) => ({ ...f, [field]: field === "code" ? value.toUpperCase() : value }));
  };

  const save = async (e) => {
    e.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      if (form.id) {
        await api.put(`/coupons/${form.id}`, toBody(form));
        setMessage({ tone: "ok", text: `Coupon ${form.code} updated.` });
      } else {
        await api.post("/coupons", toBody(form));
        setMessage({ tone: "ok", text: `Coupon ${form.code} created. Customers can use it at checkout now.` });
      }
      setForm(null);
      reload();
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not save the coupon.") });
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (c) => {
    setBusy(true);
    setMessage(null);
    try {
      await api.put(`/coupons/${c.id}`, toBody({ ...toForm(c), active: !c.active }));
      setMessage({ tone: "ok", text: `Coupon ${c.code} turned ${c.active ? "off" : "on"}.` });
      reload();
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not update the coupon.") });
    } finally {
      setBusy(false);
    }
  };

  const remove = async (c) => {
    if (!window.confirm(`Delete coupon ${c.code}? Past orders keep their discount.`)) return;
    setBusy(true);
    setMessage(null);
    try {
      await api.delete(`/coupons/${c.id}`);
      setMessage({ tone: "ok", text: `Coupon ${c.code} deleted.` });
      reload();
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not delete the coupon.") });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="order-page">
      <DashboardNav area="admin" />
      <h1>Coupons</h1>
      <p className="muted">Discount codes customers can enter at checkout.</p>

      {message && <div className={`banner banner-${message.tone}`}>{message.text}</div>}

      {form ? (
        <form className="card" onSubmit={save} style={{ marginTop: 16 }}>
          <h2>{form.id ? `Edit ${form.code}` : "New coupon"}</h2>
          <div className="form-grid">
            <label>
              Code
              <input value={form.code} onChange={set("code")} required minLength={3} maxLength={30}
                     pattern="[A-Za-z0-9_\-]{3,30}" title="3-30 letters, numbers, - or _" placeholder="e.g. DIWALI10" />
            </label>
            <label>
              Discount type
              <select value={form.type} onChange={set("type")}>
                <option value="PERCENT">Percentage off</option>
                <option value="FIXED">Fixed amount off (₹)</option>
              </select>
            </label>

            {form.type === "PERCENT" ? (
              <>
                <label>
                  Percentage (%)
                  <input type="number" min="1" max="90" step="0.5" value={form.percent} onChange={set("percent")} required />
                </label>
                <label>
                  Maximum discount (₹, optional)
                  <input type="number" min="1" step="1" value={form.maxDiscount} onChange={set("maxDiscount")} placeholder="No limit" />
                </label>
              </>
            ) : (
              <label className="full">
                Amount off (₹)
                <input type="number" min="1" step="1" value={form.flatAmount} onChange={set("flatAmount")} required />
              </label>
            )}

            <label>
              Minimum order (₹, optional)
              <input type="number" min="0" step="1" value={form.minOrderAmount} onChange={set("minOrderAmount")} placeholder="Any amount" />
            </label>
            <label>
              Valid until (optional)
              <input type="date" value={form.expiresOn} onChange={set("expiresOn")} />
            </label>
            <label>
              Total uses (optional)
              <input type="number" min="1" step="1" value={form.usageLimit} onChange={set("usageLimit")} placeholder="Unlimited" />
            </label>
            <label style={{ flexDirection: "row", alignItems: "center", gap: 8, marginTop: 22 }}>
              <input type="checkbox" checked={form.oncePerCustomer} onChange={set("oncePerCustomer")} />
              Once per customer
            </label>
            <label style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
              <input type="checkbox" checked={form.active} onChange={set("active")} />
              Active (customers can use it)
            </label>
          </div>
          <div className="order-actions">
            <button type="submit" className="primary" disabled={busy}>
              {busy ? "Saving..." : form.id ? "Save changes" : "Create coupon"}
            </button>
            <button type="button" onClick={() => setForm(null)} disabled={busy}>Cancel</button>
          </div>
        </form>
      ) : (
        <div className="toolbar">
          <span className="spacer" />
          <button className="primary" onClick={() => { setMessage(null); setForm({ ...EMPTY }); }}>+ New coupon</button>
        </div>
      )}

      {coupons === null && <div className="skeleton" style={{ height: 44 }} />}

      {coupons && coupons.length === 0 && !form && (
        <div className="state-box">
          <h2>No coupons yet</h2>
          <p className="muted">Create one, for example 10% off with a ₹200 cap.</p>
        </div>
      )}

      {coupons && coupons.length > 0 && (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Discount</th>
                <th>Rules</th>
                <th className="num">Used</th>
                <th>Status</th>
                <th className="actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {coupons.map((c) => {
                const s = status(c);
                const rules = [
                  c.minOrderAmount ? `Min ${formatMoney(c.minOrderAmount)}` : null,
                  c.expiresAt ? `Until ${formatDate(c.expiresAt)}` : null,
                  c.oncePerCustomer ? "Once per customer" : null,
                ].filter(Boolean);
                return (
                  <tr key={c.id}>
                    <td><strong>{c.code}</strong></td>
                    <td>{describe(c)}</td>
                    <td className="muted">{rules.length ? rules.join(" · ") : "None"}</td>
                    <td className="num">{c.usedCount}{c.usageLimit ? ` / ${c.usageLimit}` : ""}</td>
                    <td><span className={`badge badge-${s.tone}`}>{s.text}</span></td>
                    <td className="actions">
                      <button onClick={() => { setMessage(null); setForm(toForm(c)); window.scrollTo({ top: 0, behavior: "smooth" }); }} disabled={busy}>Edit</button>
                      <button onClick={() => toggle(c)} disabled={busy}>{c.active ? "Turn off" : "Turn on"}</button>
                      <button className="danger" onClick={() => remove(c)} disabled={busy}>Delete</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default AdminCoupons;