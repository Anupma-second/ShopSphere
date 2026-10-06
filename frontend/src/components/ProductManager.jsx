import { useEffect, useState } from "react";
import api from "../api/axios";
import { getErrorMessage } from "../utils/errors";
import { formatMoney } from "../utils/format";
import Pager from "./Pager";
import ProductImage from "./ProductImage";

const LOW_STOCK = 5;
const EMPTY_FORM = { id: null, name: "", description: "", price: "", stock: "", categoryId: "" };

/**
 * Product table + add/edit form.
 *   mode="seller": only my products (GET /seller/products), quick stock via PATCH
 *   mode="admin":  every product (GET /products/search), shows the seller column
 */
function ProductManager({ mode }) {
  const isAdmin = mode === "admin";

  const [data, setData] = useState(null);         // PageResponse, null = loading
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");       // admin: text in the box
  const [query, setQuery] = useState("");         // admin: submitted search
  const [reloadKey, setReloadKey] = useState(0);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(null);         // null = form closed
  const [stockEdits, setStockEdits] = useState({});
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);   // {tone, text}

  useEffect(() => {
    api.get("/categories")
      .then((res) => setCategories(res.data))
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    let cancelled = false;
    const request = isAdmin
      ? api.get("/products/search", { params: { q: query || undefined, page, size: 10, sort: "id,desc" } })
      : api.get("/seller/products", { params: { page, size: 10, sort: "id,desc" } });

    request
      .then((res) => { if (!cancelled) setData(res.data); })
      .catch((err) => {
        if (!cancelled) {
          setMessage({ tone: "bad", text: getErrorMessage(err, "Failed to load products.") });
          setData({ content: [], page: 0, totalPages: 0, totalElements: 0, last: true });
        }
      });

    return () => { cancelled = true; };
  }, [isAdmin, page, query, reloadKey]);

  const reload = () => setReloadKey((k) => k + 1);

  const openNew = () => {
    setForm({ ...EMPTY_FORM, categoryId: categories[0]?.id ?? "" });
    setMessage(null);
  };

  const toForm = (p) => ({
    id: p.id,
    name: p.name ?? "",
    description: p.description ?? "",
    price: p.price ?? "",
    stock: p.stock ?? "",
    categoryId: p.category?.id ?? "",
    images: p.images ?? [],
  });

  const openEdit = (p) => {
    setForm(toForm(p));
    setMessage(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const toBody = (f) => ({
    name: f.name.trim(),
    description: f.description.trim(),
    price: Number(f.price),
    stock: Number(f.stock),
    category: { id: Number(f.categoryId) },
  });

  const saveForm = async (event) => {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      if (form.id) {
        await api.put(`/products/${form.id}`, toBody(form));
        setMessage({ tone: "ok", text: "Product updated." });
        setForm(null);
      } else {
        // keep the form open on the new product so photos can be added right away
        const { data: created } = await api.post("/products", toBody(form));
        setMessage({ tone: "ok", text: "Product created! Now add a few photos so it stands out in the shop." });
        setForm(toForm(created));
        setPage(0);
      }
      reload();
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not save the product.") });
    } finally {
      setBusy(false);
    }
  };

  const saveStock = async (p) => {
    const value = stockEdits[p.id];
    if (value === undefined || value === "" || Number(value) === p.stock) return;

    setBusy(true);
    setMessage(null);
    try {
      if (isAdmin) {
        await api.put(`/products/${p.id}`, toBody({
          name: p.name, description: p.description ?? "", price: p.price,
          stock: value, categoryId: p.category?.id,
        }));
      } else {
        await api.patch(`/seller/products/${p.id}/stock`, null, { params: { stock: Number(value) } });
      }
      setStockEdits((s) => { const next = { ...s }; delete next[p.id]; return next; });
      setMessage({ tone: "ok", text: `Stock for "${p.name}" set to ${value}.` });
      reload();
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not update stock.") });
    } finally {
      setBusy(false);
    }
  };

  const remove = async (p) => {
    if (!window.confirm(`Delete "${p.name}"? This cannot be undone.`)) return;

    setBusy(true);
    setMessage(null);
    try {
      await api.delete(`/products/${p.id}`);
      setMessage({ tone: "ok", text: `"${p.name}" deleted.` });
      reload();
    } catch (err) {
      const text = err?.response?.status === 409
        ? "This product is part of existing orders, so it can't be deleted. Set its stock to 0 instead."
        : getErrorMessage(err, "Could not delete the product.");
      setMessage({ tone: "bad", text });
    } finally {
      setBusy(false);
    }
  };

  const setField = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.value }));

  return (
    <>
      {message && <div className={`banner banner-${message.tone}`}>{message.text}</div>}

      {form && (
        <form className="card" onSubmit={saveForm}>
          <h2>{form.id ? `Edit product #${form.id}` : "Add a product"}</h2>
          <div className="form-grid">
            <label className="full">
              Name
              <input value={form.name} onChange={setField("name")} required maxLength={150} />
            </label>
            <label className="full">
              Description
              <textarea rows={3} value={form.description} onChange={setField("description")} />
            </label>
            <label>
              Price (₹)
              <input type="number" min="0.01" step="0.01" value={form.price} onChange={setField("price")} required />
            </label>
            <label>
              Stock
              <input type="number" min="0" step="1" value={form.stock} onChange={setField("stock")} required />
            </label>
            <label className="full">
              Category
              <select value={form.categoryId} onChange={setField("categoryId")} required>
                <option value="" disabled>Choose a category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </label>
          </div>
          {categories.length === 0 && (
            <p className="muted" style={{ marginTop: 10 }}>
              There are no categories yet. An admin has to create one first.
            </p>
          )}
          <div className="order-actions">
            <button type="submit" className="primary" disabled={busy}>
              {busy ? "Saving..." : form.id ? "Save changes" : "Create product"}
            </button>
            <button type="button" onClick={() => setForm(null)} disabled={busy}>
              {form.id ? "Close" : "Cancel"}
            </button>
          </div>
        </form>
      )}

      {form?.id && (
        <PhotoEditor
          productId={form.id}
          productName={form.name}
          images={form.images}
          onChange={(images) => { setForm((f) => ({ ...f, images })); reload(); }}
        />
      )}

      <div className="toolbar">
        {isAdmin && (
          <form
            className="inline-form"
            style={{ flex: 1 }}
            onSubmit={(e) => { e.preventDefault(); setPage(0); setQuery(search.trim()); }}
          >
            <input
              type="search"
              placeholder="Search products by name or description"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ flex: 1, width: "auto" }}
            />
            <button type="submit">Search</button>
          </form>
        )}
        {!isAdmin && <span className="spacer" />}
        {!form && <button className="primary" onClick={openNew}>+ Add product</button>}
      </div>

      {data === null && (
        <>
          <div className="skeleton" style={{ height: 44 }} />
          <div className="skeleton" style={{ height: 44 }} />
          <div className="skeleton" style={{ height: 44 }} />
        </>
      )}

      {data && data.content.length === 0 && (
        <div className="state-box">
          <h2>{query ? "No products match your search" : "No products yet"}</h2>
          <p className="muted">
            {query ? "Try a different search." : "Click “Add product” to create your first one."}
          </p>
        </div>
      )}

      {data && data.content.length > 0 && (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Category</th>
                {isAdmin && <th>Seller</th>}
                <th className="num">Price</th>
                <th>Stock</th>
                <th className="actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((p) => {
                const edited = stockEdits[p.id];
                return (
                  <tr key={p.id} className={p.stock <= LOW_STOCK ? "row-low" : ""}>
                    <td>
                      <span className="inline-form" style={{ flexWrap: "nowrap" }}>
                        <span style={{ width: 44, flex: "none" }}>
                          <ProductImage src={p.images?.[0]?.imageUrl} name={p.name} className="thumb-sm" />
                        </span>
                        <span>
                          {p.name}
                          <div className="muted">#{p.id} · {p.images?.length ?? 0} photo{p.images?.length === 1 ? "" : "s"}</div>
                        </span>
                      </span>
                    </td>
                    <td>{p.category?.name ?? "—"}</td>
                    {isAdmin && <td>{p.sellerName ?? <span className="muted">Store</span>}</td>}
                    <td className="num">{formatMoney(p.price)}</td>
                    <td>
                      <span className="inline-form">
                        <input
                          className="stock-input"
                          type="number"
                          min="0"
                          value={edited ?? p.stock}
                          onChange={(e) => setStockEdits((s) => ({ ...s, [p.id]: e.target.value }))}
                          onKeyDown={(e) => { if (e.key === "Enter") saveStock(p); }}
                        />
                        {edited !== undefined && Number(edited) !== p.stock && (
                          <button onClick={() => saveStock(p)} disabled={busy}>Save</button>
                        )}
                      </span>
                      {p.stock === 0 && <span className="badge badge-bad">Out of stock</span>}
                      {p.stock > 0 && p.stock <= LOW_STOCK && <span className="badge badge-warn">Low</span>}
                    </td>
                    <td className="actions">
                      <button onClick={() => openEdit(p)} disabled={busy}>Edit</button>
                      <button className="danger" onClick={() => remove(p)} disabled={busy}>Delete</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pager data={data} onPage={setPage} />
    </>
  );
}

// Photos for one product. The first photo is the main one shown in the shop.
function PhotoEditor({ productId, productName, images, onChange }) {
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const add = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const { data } = await api.post("/product-images", {
        imageUrl: url.trim(),
        product: { id: productId },
      });
      onChange([...images, { id: data.id, imageUrl: data.imageUrl }]);
      setUrl("");
    } catch (err) {
      setError(getErrorMessage(err, "Could not add the photo."));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (image) => {
    setBusy(true);
    setError("");
    try {
      await api.delete(`/product-images/${image.id}`);
      onChange(images.filter((i) => i.id !== image.id));
    } catch (err) {
      setError(getErrorMessage(err, "Could not remove the photo."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card">
      <h2>Photos</h2>
      <p className="muted" style={{ marginBottom: 12 }}>
        Paste a link to an image (it must start with https://). The first photo is the main one customers see.
      </p>

      {images.length > 0 && (
        <div className="photo-grid">
          {images.map((img, i) => (
            <div key={img.id} className="photo-item">
              <ProductImage src={img.imageUrl} name={productName} />
              {i === 0 && <span className="badge badge-info photo-main">Main</span>}
              <button type="button" className="danger" onClick={() => remove(img)} disabled={busy}>
                Remove
              </button>
            </div>
          ))}
        </div>
      )}

      <form className="inline-form" onSubmit={add} style={{ marginTop: 12 }}>
        <input
          type="url"
          placeholder="https://example.com/photo.jpg"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          required
          style={{ flex: 1, minWidth: 220, width: "auto" }}
        />
        <button type="submit" className="primary" disabled={busy}>
          {busy ? "Saving..." : "Add photo"}
        </button>
      </form>
      {error && <div className="banner banner-bad">{error}</div>}
    </div>
  );
}

export default ProductManager;