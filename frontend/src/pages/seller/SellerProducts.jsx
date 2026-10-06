import DashboardNav from "../../components/DashboardNav";
import ProductManager from "../../components/ProductManager";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

function SellerProducts() {
  return (
    <div className="order-page">
      <DashboardNav area="seller" />
      <h1>My products</h1>
      <p className="muted">
        Add products, edit details and keep stock up to date. Rows highlighted in orange are low on stock.
      </p>
      <ProductManager mode="seller" />
    </div>
  );
}

export default SellerProducts;