import { NavLink, useNavigate } from "react-router-dom";
import { getUser, logout } from "../utils/auth";

const LINKS = {
  seller: [
    { to: "/seller", label: "Overview", end: true },
    { to: "/seller/products", label: "Products" },
    { to: "/seller/orders", label: "Orders" },
  ],
  admin: [
    { to: "/admin", label: "Overview", end: true },
    { to: "/admin/orders", label: "Orders" },
    { to: "/admin/products", label: "Products" },
    { to: "/admin/categories", label: "Categories" },
    { to: "/admin/coupons", label: "Coupons" },
    { to: "/admin/users", label: "Users" },
  ],
};

function DashboardNav({ area }) {
  const user = getUser();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="dash-nav">
      <span className="dash-brand">
        ShopSphere <span className="muted">· {area === "admin" ? "Admin" : "Seller"}</span>
      </span>

      <div className="dash-links">
        {LINKS[area].map((link) => (
          <NavLink key={link.to} to={link.to} end={link.end}>
            {link.label}
          </NavLink>
        ))}
        <NavLink to="/products">Store</NavLink>
      </div>

      <span className="dash-user">
        <span className="muted">{user?.name}</span>
        <button onClick={handleLogout}>Log out</button>
      </span>
    </nav>
  );
}

export default DashboardNav;