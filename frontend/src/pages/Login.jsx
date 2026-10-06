import { useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { homeFor, saveSession } from "../utils/auth";

function Login() {
  const location = useLocation();
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const [email, setEmail] = useState(location.state?.email ?? "");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const notice = location.state?.notice;   // e.g. "Password changed, please log in"
  const next = params.get("next");         // page to return to after login

  const handleLogin = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const { data } = await api.post("/auth/login", { email, password });
      saveSession(data);

      // only follow "next" for in-site paths
      const target = next && next.startsWith("/") && !next.startsWith("//")
        ? next
        : homeFor(data.user);
      navigate(target, { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, "Invalid email or password."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleLogin}>
        <h1>Welcome back</h1>
        <p className="auth-sub">Log in to your ShopSphere account.</p>

        {notice && <div className="auth-msg ok">{notice}</div>}
        {error && <div className="auth-msg bad">{error}</div>}

        <label className="auth-field">
          Email
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus={!email}
          />
        </label>

        <label className="auth-field">
          Password
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoFocus={!!email}
          />
        </label>

        <div className="auth-row">
          <Link to="/forgot-password">Forgot password?</Link>
        </div>

        <button type="submit" className="auth-btn" disabled={loading}>
          {loading ? "Logging in..." : "Log in"}
        </button>

        <p className="auth-alt">
          New to ShopSphere? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </div>
  );
}

export default Login;