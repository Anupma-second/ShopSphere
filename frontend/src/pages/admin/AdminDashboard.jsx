import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import { OrderStatusBadge } from "../../components/StatusBadge";
import { getErrorMessage } from "../../utils/errors";
import { formatMoney } from "../../utils/format";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

const ROLE_LABELS = { CUSTOMER: "Customers", SELLER: "Sellers", ADMIN: "Admins" };

function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/admin/stats")
      .then((res) => setStats(res.data))
      .catch((err) => setError(getErrorMessage(err, "Failed to load stats.")));
  }, []);

  const totalUsers = stats ? Object.values(stats.usersByRole).reduce((a, b) => a + b, 0) : 0;
  const totalOrders = stats ? Object.values(stats.ordersByStatus).reduce((a, b) => a + b, 0) : 0;
  const toShip = stats?.ordersByStatus?.CONFIRMED ?? 0;

  return (
    <div className="order-page">
      <DashboardNav area="admin" />
      <h1>Admin overview</h1>

      {error && <div className="banner banner-bad">{error}</div>}
      {!stats && !error && <div className="skeleton" style={{ height: 90 }} />}

      {stats && (
        <>
          {toShip > 0 && (
            <div className="banner banner-warn">
              <strong>{toShip}</strong> paid order{toShip === 1 ? " is" : "s are"} waiting to be shipped.{" "}
              <Link to="/admin/orders">Go to orders →</Link>
            </div>
          )}

          <div className="stats-grid">
            <Stat label="Total revenue" value={formatMoney(stats.totalRevenue)} />
            <Stat label="Last 30 days" value={formatMoney(stats.revenueLast30Days)} />
            <Stat label="Orders" value={totalOrders} />
            <Stat label="Users" value={totalUsers} />
            <Stat label="Products" value={stats.totalProducts} />
            <Stat label="Low stock" value={stats.lowStockProducts} warn={stats.lowStockProducts > 0} />
          </div>

          <div className="card">
            <h2>Revenue, last 30 days</h2>
            <RevenueChart days={stats.dailyRevenueLast30Days} />
          </div>

          <div className="dash-grid">
            <div className="card">
              <h2>Orders by status</h2>
              <div className="chips">
                {Object.entries(stats.ordersByStatus).map(([status, count]) => (
                  <span key={status} className="inline-form">
                    <OrderStatusBadge status={status} /> <strong>{count}</strong>
                  </span>
                ))}
              </div>

              <h2 style={{ marginTop: 22 }}>Users by role</h2>
              <div className="chips">
                {Object.entries(stats.usersByRole).map(([role, count]) => (
                  <Link key={role} to={`/admin/users?role=${role}`} className="badge badge-muted">
                    {ROLE_LABELS[role] ?? role}: {count}
                  </Link>
                ))}
              </div>
            </div>

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
          </div>
        </>
      )}
    </div>
  );
}

function RevenueChart({ days }) {
  const max = Math.max(...days.map((d) => d.revenue), 0);

  if (max === 0) {
    return <p className="muted">No paid orders in the last 30 days.</p>;
  }

  const label = (iso) =>
    new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short" });

  return (
    <>
      <div className="bar-chart">
        {days.map((d) => (
          <div
            key={d.date}
            className="bar-col"
            title={`${label(d.date)}: ${formatMoney(d.revenue)} (${d.orders} order${d.orders === 1 ? "" : "s"})`}
          >
            <div
              className={`bar ${d.revenue === 0 ? "empty" : ""}`}
              style={{ height: `${(d.revenue / max) * 100}%` }}
            />
          </div>
        ))}
      </div>
      <div className="bar-labels">
        {days.map((d, i) => (
          <span key={d.date}>{i % 5 === 0 || i === days.length - 1 ? label(d.date) : ""}</span>
        ))}
      </div>
    </>
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

export default AdminDashboard;