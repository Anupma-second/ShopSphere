import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import { getErrorMessage } from "../../utils/errors";
import { formatMoney } from "../../utils/format";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

function SellerDashboard() {
  const [stats, setStats] = useState(null);
  const [lowStock, setLowStock] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/seller/stats")
      .then((res) => setStats(res.data))
      .catch((err) => setError(getErrorMessage(err, "Failed to load your stats.")));

    api.get("/seller/products/low-stock")
      .then((res) => setLowStock(res.data))
      .catch(() => setLowStock([]));
  }, []);

  return (
    <div className="order-page">
      <DashboardNav area="seller" />
      <h1>Seller overview</h1>

      {error && <div className="banner banner-bad">{error}</div>}

      {!stats && !error && <div className="skeleton" style={{ height: 90 }} />}

      {stats && (
        <>
          {stats.ordersToShip > 0 && (
            <div className="banner banner-warn">
              You have <strong>{stats.ordersToShip}</strong> paid order
              {stats.ordersToShip === 1 ? "" : "s"} waiting to be shipped.{" "}
              <Link to="/seller/orders">Go to orders →</Link>
            </div>
          )}

          <div className="stats-grid">
            <Stat label="Revenue" value={formatMoney(stats.revenue)} />
            <Stat label="Units sold" value={stats.unitsSold} />
            <Stat label="Paid orders" value={stats.paidOrders} />
            <Stat label="To ship" value={stats.ordersToShip} warn={stats.ordersToShip > 0} />
            <Stat label="Products" value={stats.totalProducts} />
            <Stat label="Low stock" value={stats.lowStockProducts} warn={stats.lowStockProducts > 0} />
          </div>

          <div className="dash-grid">
            <div className="card">
              <h2>Best sellers</h2>
              {stats.topProducts.length === 0 ? (
                <p className="muted">No sales yet.</p>
              ) : (
                <table className="table">
                  <thead>
                    <tr><th>Product</th><th className="num">Units</th><th className="num">Revenue</th></tr>
                  </thead>
                  <tbody>
                    {stats.topProducts.map((p) => (
                      <tr key={p.productId}>
                        <td>{p.productName}</td>
                        <td className="num">{p.unitsSold}</td>
                        <td className="num">{formatMoney(p.revenue)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="card">
              <div className="dash-section-title">
                <h2>Low stock</h2>
                <Link to="/seller/products">Manage →</Link>
              </div>
              {lowStock.length === 0 ? (
                <p className="muted">All products are well stocked.</p>
              ) : (
                <table className="table">
                  <thead><tr><th>Product</th><th className="num">Stock</th></tr></thead>
                  <tbody>
                    {lowStock.map((p) => (
                      <tr key={p.id}>
                        <td>{p.name}</td>
                        <td className="num">
                          <span className={`badge badge-${p.stock === 0 ? "bad" : "warn"}`}>{p.stock}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function Stat({ label, value, warn }) {
  return (
    <div className={`stat ${warn ? "stat-warn" : ""}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
    </div>
  );
}

export default SellerDashboard;