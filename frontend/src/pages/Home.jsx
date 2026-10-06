import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/axios";
import ProductCard from "../components/ProductCard";
import { getUser, isLoggedIn } from "../utils/auth";
import "../styles/shop.css";

function Home() {
  const user = isLoggedIn() ? getUser() : null;
  const [categories, setCategories] = useState([]);
  const [newest, setNewest] = useState(null);
  const [deals, setDeals] = useState([]);

  useEffect(() => {
    api.get("/categories").then((res) => setCategories(res.data)).catch(() => {});

    api.get("/products/search", { params: { size: 8, sort: "id,desc", inStock: true } })
      .then((res) => setNewest(res.data.content))
      .catch(() => setNewest([]));

    api.get("/products/search", { params: { size: 4, maxPrice: 999, inStock: true, sort: "price,asc" } })
      .then((res) => setDeals(res.data.content))
      .catch(() => {});
  }, []);

  return (
    <div className="shop-page">
      <section className="hero">
        <h1>{user ? `Welcome back, ${user.name.split(" ")[0]}!` : "Everything you love, in one place."}</h1>
        <p>
          Shop products from independent sellers across India. Pay securely and
          track every order from checkout to your doorstep.
        </p>
        <Link to="/products" className="btn">Shop all products</Link>
        {!user && <Link to="/register" className="btn">Create an account</Link>}
        {user?.role === "SELLER" && <Link to="/seller" className="btn">Seller dashboard</Link>}
        {user?.role === "ADMIN" && <Link to="/admin" className="btn">Admin dashboard</Link>}
      </section>

      <div className="perks">
        <div className="perk"><span>🔒</span><span>Secure payments with Razorpay</span></div>
        <div className="perk"><span>📦</span><span>Track every order step by step</span></div>
        <div className="perk"><span>↩️</span><span>Cancel any time before it ships</span></div>
      </div>

      {categories.length > 0 && (
        <section className="section">
          <div className="section-head">
            <h2>Shop by category</h2>
          </div>
          <div className="category-grid">
            {categories.map((c) => (
              <Link key={c.id} to={`/products?category=${c.id}`} className="category-tile">
                <strong>{c.name}</strong>
                {c.description && <span>{c.description}</span>}
              </Link>
            ))}
          </div>
        </section>
      )}

      <section className="section">
        <div className="section-head">
          <h2>New arrivals</h2>
          <Link to="/products?sort=newest">See all →</Link>
        </div>
        {newest === null ? (
          <div className="product-grid">
            {[1, 2, 3, 4].map((i) => <div key={i} className="skeleton-card" />)}
          </div>
        ) : newest.length === 0 ? (
          <p className="muted">No products yet. Check back soon!</p>
        ) : (
          <div className="product-grid">
            {newest.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>

      {deals.length > 0 && (
        <section className="section">
          <div className="section-head">
            <h2>Under ₹999</h2>
            <Link to="/products?max=999&sort=price-asc">See all →</Link>
          </div>
          <div className="product-grid">
            {deals.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        </section>
      )}
    </div>
  );
}

export default Home;