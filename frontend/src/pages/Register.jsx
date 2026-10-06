import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { homeFor, saveSession } from "../utils/auth";

function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleRegister = async (event) => {
    event.preventDefault();
    setError("");

    if (form.password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (form.password !== form.confirm) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    const email = form.email.trim();

    try {
      await api.post("/auth/register", {
        name: form.name.trim(),
        email,
        password: form.password,
      });
    } catch (err) {
      setError(getErrorMessage(err, "Could not create your account."));
      setLoading(false);
      return;
    }

    // Account created - log straight in. If the store requires email
    // verification, login is refused and we send them to /login with a note.
    try {
      const { data } = await api.post("/auth/login", { email, password: form.password });
      saveSession(data);
      navigate(homeFor(data.user), { replace: true });
    } catch (err) {
      const verify = err?.response?.status === 403;
      navigate("/login", {
        replace: true,
        state: {
          email,
          notice: verify
            ? "Account created! Check your email for a verification link, then log in."
            : "Account created! Please log in.",
        },
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleRegister}>
        <h1>Create your account</h1>
        <p className="auth-sub">Shop faster, track orders and save your addresses.</p>

        {error && <div className="auth-msg bad">{error}</div>}

        <label className="auth-field">
          Full name
          <input
            autoComplete="name"
            value={form.name}
            onChange={set("name")}
            required
            maxLength={100}
            autoFocus
          />
        </label>

        <label className="auth-field">
          Email
          <input
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={set("email")}
            required
          />
        </label>

        <label className="auth-field">
          Password
          <input
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={set("password")}
            required
            minLength={8}
            maxLength={72}
          />
          <span className="hint">At least 8 characters.</span>
        </label>

        <label className="auth-field">
          Confirm password
          <input
            type="password"
            autoComplete="new-password"
            value={form.confirm}
            onChange={set("confirm")}
            required
          />
        </label>

        <button type="submit" className="auth-btn" disabled={loading}>
          {loading ? "Creating account..." : "Create account"}
        </button>

        <p className="auth-alt">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}

export default Register;