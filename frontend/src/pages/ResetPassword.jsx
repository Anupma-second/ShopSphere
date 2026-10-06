import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";

// Opened from the link in the reset email: /reset-password?token=...
function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get("token");

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    if (password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await api.post("/auth/reset-password", { token, newPassword: password });
      navigate("/login", {
        replace: true,
        state: { notice: "Your password has been changed. Please log in." },
      });
    } catch (err) {
      setError(getErrorMessage(err, "Could not reset your password."));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Link not valid</h1>
          <p className="auth-sub">
            This reset link is incomplete. Please request a new one.
          </p>
          <Link to="/forgot-password" className="auth-btn" style={{ display: "block", textAlign: "center", textDecoration: "none", color: "#fff" }}>
            Request a new link
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>Choose a new password</h1>
        <p className="auth-sub">Make it at least 8 characters.</p>

        {error && (
          <div className="auth-msg bad">
            {error}
            {/expired|invalid/i.test(error) && (
              <> <Link to="/forgot-password">Request a new link</Link></>
            )}
          </div>
        )}

        <label className="auth-field">
          New password
          <input
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={72}
            autoFocus
          />
        </label>

        <label className="auth-field">
          Confirm new password
          <input
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />
        </label>

        <button type="submit" className="auth-btn" disabled={loading}>
          {loading ? "Saving..." : "Change password"}
        </button>
      </form>
    </div>
  );
}

export default ResetPassword;