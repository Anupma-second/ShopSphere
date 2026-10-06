import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { getUser, isLoggedIn, logout } from "../utils/auth";
import { CART_EVENT } from "../utils/cart";

function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = isLoggedIn() ? getUser() : null;

  const [search, setSearch] = useState(
    () => new URLSearchParams(location.search).get("q") ?? ""
  );
  const [cartCount, setCartCount] = useState(0);

  // Cart badge: reload on every page change and whenever a page says the cart changed
  useEffect(() => {
    if (!user) return undefined;

    let cancelled = false;
    const load = () =>
      api.get("/cart-items")
        .then((res) => {
          if (!cancelled) setCartCount(res.data.reduce((n, item) => n + item.quantity, 0));
        })
        .catch(() => {});

    load();
    window.addEventListener(CART_EVENT, load);
    return () => {
      cancelled = true;
      window.removeEventListener(CART_EVENT, load);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname, user?.id]);

  const submitSearch = (e) => {
    e.preventDefault();
    const q = search.trim();
    navigate(q ? `/products?q=${encodeURIComponent(q)}` : "/products");
  };

  const handleLogout = () => {
    logout();
    setCartCount(0);
    navigate("/");
  };

  const dashboard =
    user?.role === "ADMIN" ? { to: "/admin", label: "Admin dashboard" }
    : user?.role === "SELLER" ? { to: "/seller", label: "Seller dashboard" }
    : null;

  return (
    <header className="site-header">
      <Link to="/" className="logo">
        Shop<span>Sphere</span>
      </Link>

      <form className="header-search" onSubmit={submitSearch} role="search">
        <input
          type="search"
          placeholder="Search for products"
          aria-label="Search for products"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>

      <nav className="header-links">
        <NavLink to="/products">Products</NavLink>

        <NavLink to="/cart" className="cart-link">
          Cart
          {user && cartCount > 0 && <span className="cart-count">{cartCount}</span>}
        </NavLink>

        {user ? (
          // key resets the menu (closes it) on every navigation
          <details className="account-menu" key={location.key}>
            <summary>Hi, {user.name?.split(" ")[0] || "there"}</summary>
            <div className="menu">
              <div className="menu-head">
                <strong>{user.name}</strong>
                <span>{user.email}</span>
              </div>
              {dashboard && <Link to={dashboard.to}>{dashboard.label}</Link>}
              <Link to="/account">My account</Link>
              <Link to="/orders">My orders</Link>
              <Link to="/wishlist">Wishlist</Link>
              <Link to="/account/addresses">Addresses</Link>
              <button type="button" onClick={handleLogout}>Log out</button>
            </div>
          </details>
        ) : (
          <>
            <NavLink to="/login">Log in</NavLink>
            <Link to="/register" className="signup-btn">Sign up</Link>
          </>
        )}
      </nav>
    </header>
  );
}

export default Header;