import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../api/axios";
import ProductCard from "../components/ProductCard";
import { getErrorMessage } from "../utils/errors";
import "../styles/shop.css";

const PAGE_SIZE = 12;

// URL value -> backend sort
const SORTS = {
  newest: { label: "Newest first", sort: "id,desc" },
  "price-asc": { label: "Price: low to high", sort: "price,asc" },
  "price-desc": { label: "Price: high to low", sort: "price,desc" },
  name: { label: "Name: A to Z", sort: "name,asc" },
};

/**
 * Every filter lives in the URL (?q=&category=&min=&max=&stock=1&sort=&page=),
 * so results can be bookmarked/shared and the back button works.
 */
function Products() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q")?.trim() ?? "";
  const category = params.get("category") ?? "";
  const min = params.get("min") ?? "";
  const max = params.get("max") ?? "";
  const inStock = params.get("stock") === "1";
  const sortKey = SORTS[params.get("sort")] ? params.get("sort") : "newest";
  const page = Math.max(0, Number(params.get("page") ?? 0) || 0);

  const [categories, setCategories] = useState([]);
  const [result, setResult] = useState(null);   // { key, data } or { key, error }
  const [priceDraft, setPriceDraft] = useState({ min, max, from: `${min}|${max}` });

  // keep the price boxes in sync when the URL changes (e.g. "Clear all")
  if (priceDraft.from !== `${min}|${max}`) {
    setPriceDraft({ min, max, from: `${min}|${max}` });
  }

  useEffect(() => {
    api.get("/categories").then((res) => setCategories(res.data)).catch(() => {});
  }, []);

  const queryKey = params.toString();

  useEffect(() => {
    let cancelled = false;
    api.get("/products/search", {
      params: {
        q: q || undefined,
        categoryId: category || undefined,
        minPrice: min || undefined,
        maxPrice: max || undefined,
        inStock: inStock || undefined,
        sort: SORTS[sortKey].sort,
        page,
        size: PAGE_SIZE,
      },
    })
      .then((res) => { if (!cancelled) setResult({ key: queryKey, data: res.data }); })
      .catch((err) => {
        if (!cancelled) setResult({ key: queryKey, error: getErrorMessage(err, "Could not load products.") });
      });
    return () => { cancelled = true; };
  }, [queryKey]); // eslint-disable-line react-hooks/exhaustive-deps

  // change one or more URL params; any filter change goes back to page 1
  const update = (changes, keepPage = false) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => {
      if (v === "" || v === null || v === undefined || v === false) next.delete(k);
      else next.set(k, v);
    });
    if (!keepPage) next.delete("page");
    setParams(next);
  };

  const applyPrice = (e) => {
    e.preventDefault();
    update({ min: priceDraft.min, max: priceDraft.max });
  };

  const loading = !result || result.key !== queryKey;
  const data = result?.data;
  const categoryName = categories.find((c) => String(c.id) === category)?.name;
  const hasFilters = q || category || min || max || inStock;

  const title = q ? `Results for “${q}”` : categoryName ?? "All products";

  return (
    <div className="shop-page">
      <h1>{title}</h1>
      {data && !loading && (
        <p className="muted">{data.totalElements} product{data.totalElements === 1 ? "" : "s"}</p>
      )}

      <div className="listing">
        <aside className="filters">
          <div className="filter-group">
            <h3>Category</h3>
            <label className="filter-option">
              <input type="radio" name="cat" checked={!category} onChange={() => update({ category: "" })} />
              All categories
            </label>
            {categories.map((c) => (
              <label key={c.id} className="filter-option">
                <input
                  type="radio"
                  name="cat"
                  checked={category === String(c.id)}
                  onChange={() => update({ category: c.id })}
                />
                {c.name}
              </label>
            ))}
          </div>

          <form className="filter-group" onSubmit={applyPrice}>
            <h3>Price (₹)</h3>
            <div className="price-inputs">
              <input
                type="number" min="0" placeholder="Min" aria-label="Minimum price"
                value={priceDraft.min}
                onChange={(e) => setPriceDraft((d) => ({ ...d, min: e.target.value }))}
              />
              <span>–</span>
              <input
                type="number" min="0" placeholder="Max" aria-label="Maximum price"
                value={priceDraft.max}
                onChange={(e) => setPriceDraft((d) => ({ ...d, max: e.target.value }))}
              />
            </div>
            <button type="submit" className="btn" style={{ marginTop: 8, width: "100%" }}>Apply</button>
          </form>

          <div className="filter-group">
            <h3>Availability</h3>
            <label className="filter-option">
              <input type="checkbox" checked={inStock} onChange={(e) => update({ stock: e.target.checked ? "1" : "" })} />
              In stock only
            </label>
          </div>
        </aside>

        <div>
          <div className="listing-bar">
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
              {q && <button className="chip" onClick={() => update({ q: "" })}>“{q}” ✕</button>}
              {categoryName && <button className="chip" onClick={() => update({ category: "" })}>{categoryName} ✕</button>}
              {(min || max) && (
                <button className="chip" onClick={() => update({ min: "", max: "" })}>
                  ₹{min || 0} – {max ? `₹${max}` : "any"} ✕
                </button>
              )}
              {inStock && <button className="chip" onClick={() => update({ stock: "" })}>In stock ✕</button>}
              {hasFilters && (
                <button className="chip" onClick={() => setParams(sortKey !== "newest" ? { sort: sortKey } : {})}>
                  Clear all
                </button>
              )}
            </div>
            <select value={sortKey} onChange={(e) => update({ sort: e.target.value === "newest" ? "" : e.target.value })} aria-label="Sort by">
              {Object.entries(SORTS).map(([key, s]) => <option key={key} value={key}>{s.label}</option>)}
            </select>
          </div>

          {result?.error && !loading && <p className="notice bad">{result.error}</p>}

          {loading && (
            <div className="product-grid">
              {Array.from({ length: 8 }, (_, i) => <div key={i} className="skeleton-card" />)}
            </div>
          )}

          {!loading && data && data.content.length === 0 && (
            <div className="empty-state">
              <h2>No products found</h2>
              <p className="muted" style={{ marginTop: 8 }}>Try removing a filter or searching for something else.</p>
            </div>
          )}

          {!loading && data && data.content.length > 0 && (
            <div className="product-grid">
              {data.content.map((p) => <ProductCard key={p.id} product={p} />)}
            </div>
          )}

          {!loading && data && data.totalPages > 1 && (
            <div className="pager">
              <button className="btn" disabled={page === 0} onClick={() => update({ page: page - 1 || "" }, true)}>
                ← Previous
              </button>
              <span className="muted">Page {page + 1} of {data.totalPages}</span>
              <button className="btn" disabled={data.last} onClick={() => update({ page: page + 1 }, true)}>
                Next →
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Products;