import { Navigate, useLocation } from "react-router-dom";
import { isLoggedIn } from "../utils/auth";

// Sends logged-out visitors to /login and brings them back afterwards.
function RequireLogin({ children }) {
  const location = useLocation();

  if (!isLoggedIn()) {
    const next = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?next=${next}`} replace />;
  }

  return children;
}

export default RequireLogin;