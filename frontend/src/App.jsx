import { BrowserRouter, Routes, Route } from "react-router-dom";

import Layout from "./components/Layout";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import Products from "./pages/Products";
import ProductDetails from "./pages/ProductDetails";
import Cart from "./pages/Cart";
import Checkout from "./pages/Checkout";
import MyOrders from "./pages/MyOrders";
import OrderDetails from "./pages/OrderDetails";
import Profile from "./pages/Profile";
import Addresses from "./pages/Addresses";
import Wishlist from "./pages/Wishlist";

import RequireLogin from "./components/RequireLogin";
import RoleRoute from "./components/RoleRoute";
import SellerDashboard from "./pages/seller/SellerDashboard";
import SellerProducts from "./pages/seller/SellerProducts";
import SellerOrders from "./pages/seller/SellerOrders";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminOrders from "./pages/admin/AdminOrders";
import AdminProducts from "./pages/admin/AdminProducts";
import AdminCategories from "./pages/admin/AdminCategories";
import AdminCoupons from "./pages/admin/AdminCoupons";
import AdminUsers from "./pages/admin/AdminUsers";

const loggedIn = (page) => <RequireLogin>{page}</RequireLogin>;
const seller = (page) => <RoleRoute role="SELLER">{page}</RoleRoute>;
const admin = (page) => <RoleRoute role="ADMIN">{page}</RoleRoute>;

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Shopper pages: share the header + footer */}
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/products" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetails />} />

          {/* Need a login: logged-out visitors go to /login and come back */}
          <Route path="/cart" element={loggedIn(<Cart />)} />
          <Route path="/checkout" element={loggedIn(<Checkout />)} />
          <Route path="/orders" element={loggedIn(<MyOrders />)} />
          <Route path="/orders/:id" element={loggedIn(<OrderDetails />)} />
          <Route path="/account" element={loggedIn(<Profile />)} />
          <Route path="/account/addresses" element={loggedIn(<Addresses />)} />
          <Route path="/wishlist" element={loggedIn(<Wishlist />)} />
        </Route>

        {/* Seller dashboard (has its own navigation) */}
        <Route path="/seller" element={seller(<SellerDashboard />)} />
        <Route path="/seller/products" element={seller(<SellerProducts />)} />
        <Route path="/seller/orders" element={seller(<SellerOrders />)} />

        {/* Admin dashboard (has its own navigation) */}
        <Route path="/admin" element={admin(<AdminDashboard />)} />
        <Route path="/admin/orders" element={admin(<AdminOrders />)} />
        <Route path="/admin/products" element={admin(<AdminProducts />)} />
        <Route path="/admin/categories" element={admin(<AdminCategories />)} />
        <Route path="/admin/coupons" element={admin(<AdminCoupons />)} />
        <Route path="/admin/users" element={admin(<AdminUsers />)} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;