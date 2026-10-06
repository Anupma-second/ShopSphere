import DashboardNav from "../../components/DashboardNav";
import ProductManager from "../../components/ProductManager";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

function AdminProducts() {
  return (
    <div className="order-page">
      <DashboardNav area="admin" />
      <h1>All products</h1>
      <p className="muted">
        Every product in the store. Products you add here belong to the store, not to a seller.
      </p>
      <ProductManager mode="admin" />
    </div>
  );
}

export default AdminProducts;