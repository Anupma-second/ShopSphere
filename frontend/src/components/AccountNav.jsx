import { NavLink } from "react-router-dom";

// Side menu shared by the "My account" pages.
function AccountNav() {
  return (
    <nav className="account-nav" aria-label="My account">
      <NavLink to="/account" end>Profile</NavLink>
      <NavLink to="/orders">My orders</NavLink>
      <NavLink to="/account/addresses">Addresses</NavLink>
      <NavLink to="/wishlist">Wishlist</NavLink>
    </nav>
  );
}

export default AccountNav;