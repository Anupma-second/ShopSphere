import { Outlet } from "react-router-dom";
import Header from "./Header";
import "../styles/layout.css";

// Shared frame for all shopper pages: header on top, footer at the bottom.
function Layout() {
  return (
    <>
      <Header />
      <main className="site-main">
        <Outlet />
      </main>
      <footer className="site-footer">
        © {new Date().getFullYear()} ShopSphere · Secure payments by Razorpay
      </footer>
    </>
  );
}

export default Layout;