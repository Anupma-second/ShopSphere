import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api from "../api/axios";
import ProductCard from "../components/ProductCard";
import ProductImage from "../components/ProductImage";
import Stars from "../components/Stars";
import { useAddToCart } from "../hooks/useAddToCart";
import { isLoggedIn } from "../utils/auth";
import { getErrorMessage } from "../utils/errors";
import { formatDate, formatMoney } from "../utils/format";
import "../styles/shop.css";
import "../styles/account.css";

const LOW_STOCK = 5;
const MAX_QTY = 10;

function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const addToCart = useAddToCart();

  const [product, setProduct] = useState(null);     // null = loading
  const [notFound, setNotFound] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [related, setRelated] = useState([]);
  const [imageIndex, setImageIndex] = useState(0);
  const [qty, setQty] = useState(1);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);       // {ok, text}
  const [wish, setWish] = useState(null);           // null | "saving" | "saved" | {text}
  const [reloadKey, setReloadKey] = useState(0);
  const [shownId, setShownId] = useState(id);

  // moving to another product (e.g. from "related") resets the page state
  if (shownId !== id) {
    setShownId(id);
    setProduct(null);
    setNotFound(false);
    setReviews([]);
    setRelated([]);
    setImageIndex(0);
    setQty(1);
    setNotice(null);
    setWish(null);
  }

  useEffect(() => {
    let cancelled = false;

    api.get(`/products/${id}`)
      .then((res) => {
        if (cancelled) return;
        setProduct(res.data);
        if (res.data.category?.id) {
          api.get("/products/search", { params: { categoryId: res.data.category.id, size: 5, inStock: true } })
            .then((r) => { if (!cancelled) setRelated(r.data.content.filter((p) => p.id !== res.data.id).slice(0, 4)); })
            .catch(() => {});
        }
      })
      .catch(() => { if (!cancelled) setNotFound(true); });

    api.get(`/reviews/product/${id}`)
      .then((res) => { if (!cancelled) setReviews(res.data); })
      .catch(() => {});

    window.scrollTo(0, 0);
    return () => { cancelled = true; };
  }, [id, reloadKey]);

  if (notFound) {
    return (
      <div className="shop-page empty-state">
        <h1>Product not found</h1>
        <p className="muted">It may have been removed by the seller.</p>
        <p style={{ marginTop: 16 }}><Link to="/products">Browse all products</Link></p>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="shop-page">
        <div className="details">
          <div className="skeleton-card" style={{ aspectRatio: "1 / 1" }} />
          <div className="skeleton-card" style={{ aspectRatio: "auto", height: 260 }} />
        </div>
      </div>
    );
  }

  const images = product.images ?? [];
  const outOfStock = product.stock === 0;
  const maxQty = Math.min(product.stock, MAX_QTY);

    const saveToWishlist = async () => {
      if (!isLoggedIn()) {
        navigate(`/login?next=${encodeURIComponent(`/products/${product.id}`)}`);
        return;
      }
      setWish("saving");
      try {
        await api.post("/wishlist-items", { productId: product.id });
        setWish("saved");
      } catch (err) {
        // 409 = it was already in the wishlist, which is what the shopper wanted anyway
        setWish(err?.response?.status === 409
          ? "saved"
          : { text: getErrorMessage(err, "Could not save to your wishlist.") });
      }
    };


  const handleAdd = async (goToCart) => {
    setBusy(true);
    setNotice(null);
    const result = await addToCart(product.id, qty);
    setBusy(false);
    if (result.ok && goToCart) {
      navigate("/cart");
      return;
    }
    if (result.text) setNotice(result);
  };

  return (
    <div className="shop-page">
      <nav className="breadcrumb">
        <Link to="/">Home</Link> ›{" "}
        <Link to="/products">Products</Link>
        {product.category && (
          <> › <Link to={`/products?category=${product.category.id}`}>{product.category.name}</Link></>
        )}
      </nav>

      <div className="details">
        <div className="gallery">
          <ProductImage
            key={images[imageIndex]?.imageUrl ?? "none"}
            src={images[imageIndex]?.imageUrl}
            name={product.name}
          />
          {images.length > 1 && (
            <div className="thumbs">
              {images.map((img, i) => (
                <button
                  key={img.id}
                  className={i === imageIndex ? "active" : ""}
                  onClick={() => setImageIndex(i)}
                  aria-label={`Photo ${i + 1}`}
                >
                  <ProductImage src={img.imageUrl} name={product.name} />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="buy-box">
          {product.category && <span className="product-card-cat">{product.category.name}</span>}
          <h1>{product.name}</h1>
          <a href="#reviews" style={{ textDecoration: "none" }}>
            <Stars value={product.averageRating} count={product.reviewCount} />
          </a>

          <div className="buy-price">{formatMoney(product.price)}</div>
          <span className="muted">Inclusive of all taxes</span>

          <div className="buy-meta">
            {outOfStock ? (
              <span className="stock-note bad">Out of stock</span>
            ) : product.stock <= LOW_STOCK ? (
              <span className="stock-note warn">Hurry, only {product.stock} left in stock</span>
            ) : (
              <span className="stock-note ok">In stock</span>
            )}
            <span>Sold by <strong>{product.sellerName ?? "ShopSphere"}</strong></span>
          </div>

          {!outOfStock && (
            <div className="buy-actions">
              <div className="qty" aria-label="Quantity">
                <button onClick={() => setQty((q) => q - 1)} disabled={qty <= 1} aria-label="Decrease quantity">−</button>
                <span>{qty}</span>
                <button onClick={() => setQty((q) => q + 1)} disabled={qty >= maxQty} aria-label="Increase quantity">+</button>
              </div>
              <button className="btn primary" onClick={() => handleAdd(false)} disabled={busy}>
                {busy ? "Adding..." : "Add to cart"}
              </button>
              <button className="btn" onClick={() => handleAdd(true)} disabled={busy}>Buy now</button>
            </div>
          )}

          {notice && (
            <p className={`notice ${notice.ok ? "ok" : "bad"}`}>
              {notice.text} {notice.ok && <Link to="/cart">View cart →</Link>}
            </p>
          )}

          <div style={{ marginTop: 12 }}>
            <button
              className={`btn-wish ${wish === "saved" ? "saved" : ""}`}
              onClick={saveToWishlist}
              disabled={wish === "saving" || wish === "saved"}
            >
              {wish === "saved" ? "♥ Saved to wishlist" : "♡ Save to wishlist"}
            </button>
            {wish === "saved" && <Link to="/wishlist" style={{ marginLeft: 10, fontSize: 14 }}>View wishlist →</Link>}
            {wish && typeof wish === "object" && <p className="notice bad">{wish.text}</p>}
          </div>

          {product.description && <p className="description">{product.description}</p>}
        </div>
      </div>

      <section className="section" id="reviews">
        <div className="section-head"><h2>Ratings &amp; reviews</h2></div>
        <Reviews
          product={product}
          reviews={reviews}
          onPosted={() => setReloadKey((k) => k + 1)}
        />
      </section>

      {related.length > 0 && (
        <section className="section">
          <div className="section-head">
            <h2>You may also like</h2>
            <Link to={`/products?category=${product.category.id}`}>See more →</Link>
          </div>
          <div className="product-grid">
            {related.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        </section>
      )}
    </div>
  );
}

function Reviews({ product, reviews, onPosted }) {
  const counts = [5, 4, 3, 2, 1].map((star) => ({
    star,
    n: reviews.filter((r) => r.rating === star).length,
  }));

  return (
    <div className="reviews">
      <div>
        {product.averageRating ? (
          <>
            <div className="rating-big">{product.averageRating.toFixed(1)}</div>
            <Stars value={product.averageRating} />
            <p className="muted" style={{ marginTop: 6 }}>
              Based on {product.reviewCount} review{product.reviewCount === 1 ? "" : "s"}
            </p>
            <div className="rating-bars">
              {counts.map(({ star, n }) => (
                <div key={star} className="rating-bar">
                  <span>{star}★</span>
                  <div className="track">
                    <div className="fill" style={{ width: `${reviews.length ? (n / reviews.length) * 100 : 0}%` }} />
                  </div>
                  <span className="muted">{n}</span>
                </div>
              ))}
            </div>
          </>
        ) : (
          <p className="muted">No reviews yet. Be the first to share your thoughts!</p>
        )}
      </div>

      <div>
        <ReviewForm productId={product.id} onPosted={onPosted} />
        {reviews.map((r) => (
          <div key={r.id} className="review">
            <div className="review-head">
              <Stars value={r.rating} size={13} />
              <strong>{r.userName}</strong>
              <span className="muted">{formatDate(r.createdAt)}</span>
            </div>
            {r.comment && <p>{r.comment}</p>}
          </div>
        ))}
      </div>
    </div>
  );
}

function ReviewForm({ productId, onPosted }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);

  if (!isLoggedIn()) {
    return (
      <p className="review-form muted">
        <Link to={`/login?next=${encodeURIComponent(`/products/${productId}`)}`}>Log in</Link> to write a review.
      </p>
    );
  }

  const submit = async (e) => {
    e.preventDefault();
    if (!rating) {
      setMessage({ ok: false, text: "Please choose a star rating." });
      return;
    }
    setBusy(true);
    setMessage(null);
    try {
      await api.post("/reviews", { productId, rating, comment: comment.trim() || undefined });
      setRating(0);
      setComment("");
      setMessage({ ok: true, text: "Thanks! Your review has been posted." });
      onPosted();
    } catch (err) {
      setMessage({ ok: false, text: getErrorMessage(err, "Could not post your review.") });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="review-form" onSubmit={submit}>
      <strong style={{ color: "var(--text-h)" }}>Write a review</strong>
      <div>
        <span className="star-picker" role="radiogroup" aria-label="Your rating">
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              type="button"
              key={n}
              className={n <= rating ? "on" : ""}
              onClick={() => setRating(n)}
              aria-label={`${n} star${n === 1 ? "" : "s"}`}
              aria-checked={n === rating}
              role="radio"
            >
              ★
            </button>
          ))}
        </span>
      </div>
      <textarea
        rows={3}
        maxLength={1000}
        placeholder="What did you like or dislike? (optional)"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
      />
      <button type="submit" className="btn primary" disabled={busy}>
        {busy ? "Posting..." : "Post review"}
      </button>
      {message && <p className={`notice ${message.ok ? "ok" : "bad"}`}>{message.text}</p>}
    </form>
  );
}

export default ProductDetails;