import { useState } from "react";
import "../styles/shop.css";

// Shows the image, or a coloured tile with the product's first letter when
// there is no photo (or the link is broken).
function ProductImage({ src, name, className = "" }) {
  const [broken, setBroken] = useState(false);

  if (!src || broken) {
    const letter = (name || "?").trim().charAt(0).toUpperCase();
    // same product name -> same colour
    const hue = [...(name || "")].reduce((h, c) => (h * 31 + c.charCodeAt(0)) % 360, 7);
    return (
      <div
        className={`product-img placeholder ${className}`}
        style={{ background: `hsl(${hue} 70% 92%)`, color: `hsl(${hue} 45% 35%)` }}
        aria-label={name}
      >
        {letter}
      </div>
    );
  }

  return (
    <img
      className={`product-img ${className}`}
      src={src}
      alt={name}
      loading="lazy"
      onError={() => setBroken(true)}
    />
  );
}

export default ProductImage;