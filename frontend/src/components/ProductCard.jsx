import { useState } from "react";
import { Link } from "react-router-dom";
import { useAddToCart } from "../hooks/useAddToCart";
import { formatMoney } from "../utils/format";
import ProductImage from "./ProductImage";
import Stars from "./Stars";

const LOW_STOCK = 5;

function ProductCard({ product }) {
  const addToCart = useAddToCart();
  const [state, setState] = useState(null);   // null | "adding" | {ok, text}

  const outOfStock = product.stock === 0;

  const handleAdd = async () => {
    setState("adding");
    const result = await addToCart(product.id, 1);
    setState(result.text ? result : null);
    if (result.ok) setTimeout(() => setState(null), 2000);
  };

  return (
    <div className="product-card">
      <Link to={`/products/${product.id}`} className="product-card-link">
        <ProductImage src={product.images?.[0]?.imageUrl} name={product.name} />
        <div className="product-card-body">
          <span className="product-card-cat">{product.category?.name}</span>
          <h3 className="product-card-name">{product.name}</h3>
          <Stars value={product.averageRating} count={product.reviewCount || undefined} size={13} />
          <div className="product-card-price">{formatMoney(product.price)}</div>
          {outOfStock && <span className="stock-note bad">Out of stock</span>}
          {!outOfStock && product.stock <= LOW_STOCK && (
            <span className="stock-note warn">Only {product.stock} left</span>
          )}
        </div>
      </Link>

      <button
        className={`btn-cart ${state?.ok ? "added" : ""}`}
        onClick={handleAdd}
        disabled={outOfStock || state === "adding"}
      >
        {outOfStock ? "Out of stock"
          : state === "adding" ? "Adding..."
          : state?.ok ? "✓ Added"
          : "Add to cart"}
      </button>
      {state && state !== "adding" && !state.ok && <p className="card-error">{state.text}</p>}
    </div>
  );
}

export default ProductCard;