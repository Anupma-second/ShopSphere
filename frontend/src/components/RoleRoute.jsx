import { Link, Navigate } from "react-router-dom";
import { getUser, isLoggedIn } from "../utils/auth";
import "../styles/orders.css";

// Only renders the page for the given role. The backend checks roles too;
// this just keeps people away from pages they can't use.
function RoleRoute({ role, children }) {
  const user = getUser();

  if (!isLoggedIn() || !user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== role) {
    return (
      <div className="order-page">
        <div className="state-box">
          <h2>Not available</h2>
          <p className="muted">
            This page is only for {role.toLowerCase()} accounts.
          </p>
          <p style={{ marginTop: 16 }}>
            <Link to="/products">Back to shopping</Link>
          </p>
        </div>
      </div>
    );
  }

  return children;
}

export default RoleRoute;