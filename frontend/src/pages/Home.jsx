import { Link } from "react-router-dom";

function Home() {
  return (
    <div>
      <h1>ShopSphere</h1>

      <p>Welcome to ShopSphere</p>

      <Link to="/login">
        Login
      </Link>

      <br />

      <Link to="/products">
        View Products
      </Link>
    </div>
  );
}

export default Home;