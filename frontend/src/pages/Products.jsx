import { useEffect, useState } from "react";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";

function Products() {
  const [products, setProducts] = useState([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    api.get("/products")
      .then((response) => setProducts(response.data))
      .catch((err) => setMessage(getErrorMessage(err, "Failed to load products.")));
  }, []);

  const addToCart = async (productId) => {
    try {
      await api.post("/cart-items/add", { productId, quantity: 1 });
      setMessage("Product added to cart!");
    } catch (err) {
      // a 401 is handled globally (redirect to /login)
      setMessage(getErrorMessage(err, "Failed to add product to cart."));
    }
  };

  return (
    <div>
      <h1>Products</h1>
      {message && <p>{message}</p>}
      {products.map((product) => (
        <div key={product.id}>
          <h2>{product.name}</h2>
          <p>{product.description}</p>
          <p>Price: ₹{product.price}</p>
          <p>Stock: {product.stock}</p>
          <button
            onClick={() => addToCart(product.id)}
            disabled={product.stock === 0}
          >
            {product.stock === 0 ? "Out of stock" : "Add to Cart"}
          </button>
          <hr />
        </div>
      ))}
    </div>
  );
}

export default Products;
