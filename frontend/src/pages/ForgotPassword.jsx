import { useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";

function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      await api.post("/auth/forgot-password", { email: email.trim() });
      setSent(true);
    } catch (err) {
      setError(getErrorMessage(err, "Could not send the reset link."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>Reset your password</h1>

        {sent ? (
          <>
            <div className="auth-msg ok">
              If an account exists for <strong>{email.trim()}</strong>, we've sent a link to reset
              your password. It works for 15 minutes.
            </div>
            <p className="auth-sub">Didn't get it? Check your spam folder or try again.</p>
            <button type="button" className="auth-btn" onClick={() => setSent(false)}>
              Send again
            </button>
          </>
        ) : (
          <>
            <p className="auth-sub">Enter your email and we'll send you a reset link.</p>
            {error && <div className="auth-msg bad">{error}</div>}
            <label className="auth-field">
              Email
              <input
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
              />
            </label>
            <button type="submit" className="auth-btn" disabled={loading}>
              {loading ? "Sending..." : "Send reset link"}
            </button>
          </>
        )}

        <p className="auth-alt">
          Remembered it? <Link to="/login">Back to log in</Link>
        </p>
      </form>
    </div>
  );
}

export default ForgotPassword;