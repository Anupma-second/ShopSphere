import { useEffect, useState } from "react";
import api from "../api/axios";
import AccountNav from "../components/AccountNav";
import { updateStoredUser } from "../utils/auth";
import { getErrorMessage } from "../utils/errors";
import "../styles/shop.css";
import "../styles/account.css";

const ROLE_LABELS = { CUSTOMER: "Customer", SELLER: "Seller", ADMIN: "Admin" };

function Profile() {
  const [me, setMe] = useState(null);
  const [name, setName] = useState("");
  const [nameMsg, setNameMsg] = useState(null);
  const [savingName, setSavingName] = useState(false);

  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const [pwMsg, setPwMsg] = useState(null);
  const [savingPw, setSavingPw] = useState(false);

  useEffect(() => {
    api.get("/users/me")
      .then((res) => { setMe(res.data); setName(res.data.name); })
      .catch((err) => setNameMsg({ ok: false, text: getErrorMessage(err, "Could not load your profile.") }));
  }, []);

  const saveName = async (e) => {
    e.preventDefault();
    setSavingName(true);
    setNameMsg(null);
    try {
      const { data } = await api.put("/users/me", { name: name.trim() });
      setMe(data);
      updateStoredUser(data);
      setNameMsg({ ok: true, text: "Your name has been updated." });
    } catch (err) {
      setNameMsg({ ok: false, text: getErrorMessage(err, "Could not update your name.") });
    } finally {
      setSavingName(false);
    }
  };

  const savePassword = async (e) => {
    e.preventDefault();
    setPwMsg(null);
    if (pw.next.length < 8) {
      setPwMsg({ ok: false, text: "New password must be at least 8 characters." });
      return;
    }
    if (pw.next !== pw.confirm) {
      setPwMsg({ ok: false, text: "New passwords do not match." });
      return;
    }
    setSavingPw(true);
    try {
      await api.put("/users/me/password", { currentPassword: pw.current, newPassword: pw.next });
      setPw({ current: "", next: "", confirm: "" });
      setPwMsg({ ok: true, text: "Password changed. Use the new one next time you log in." });
    } catch (err) {
      setPwMsg({ ok: false, text: getErrorMessage(err, "Could not change your password.") });
    } finally {
      setSavingPw(false);
    }
  };

  const setPwField = (field) => (e) => setPw((p) => ({ ...p, [field]: e.target.value }));

  return (
    <div className="shop-page">
      <h1>My account</h1>
      <div className="account-layout">
        <AccountNav />

        <div>
          <section className="panel">
            <h2>Profile</h2>
            <p className="muted sub">Your name appears on orders and reviews.</p>

            {!me && !nameMsg && <div className="skeleton-card" style={{ aspectRatio: "auto", height: 120 }} />}

            {me && (
              <form onSubmit={saveName}>
                <div className="field-grid">
                  <label className="field">
                    Full name
                    <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
                  </label>
                  <label className="field">
                    Email
                    <input value={me.email} disabled />
                    <span className="hint">Your email is your login and can't be changed here.</span>
                  </label>
                  <label className="field">
                    Account type
                    <input value={ROLE_LABELS[me.role] ?? me.role} disabled />
                  </label>
                </div>
                <div className="form-actions">
                  <button className="btn primary" disabled={savingName || name.trim() === me.name || !name.trim()}>
                    {savingName ? "Saving..." : "Save changes"}
                  </button>
                </div>
              </form>
            )}
            {nameMsg && <p className={`notice ${nameMsg.ok ? "ok" : "bad"}`}>{nameMsg.text}</p>}
          </section>

          <section className="panel">
            <h2>Change password</h2>
            <p className="muted sub">For your security, enter your current password first.</p>
            <form onSubmit={savePassword}>
              <div className="field-grid">
                <label className="field full">
                  Current password
                  <input type="password" value={pw.current} onChange={setPwField("current")} required autoComplete="current-password" />
                </label>
                <label className="field">
                  New password
                  <input type="password" value={pw.next} onChange={setPwField("next")} required minLength={8} maxLength={72} autoComplete="new-password" />
                  <span className="hint">At least 8 characters.</span>
                </label>
                <label className="field">
                  Confirm new password
                  <input type="password" value={pw.confirm} onChange={setPwField("confirm")} required autoComplete="new-password" />
                </label>
              </div>
              <div className="form-actions">
                <button className="btn primary" disabled={savingPw}>
                  {savingPw ? "Saving..." : "Change password"}
                </button>
              </div>
            </form>
            {pwMsg && <p className={`notice ${pwMsg.ok ? "ok" : "bad"}`}>{pwMsg.text}</p>}
          </section>
        </div>
      </div>
    </div>
  );
}

export default Profile;