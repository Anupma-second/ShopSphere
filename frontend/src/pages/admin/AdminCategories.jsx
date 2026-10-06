import { useEffect, useState } from "react";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import { getErrorMessage } from "../../utils/errors";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

const EMPTY = { id: null, name: "", description: "" };

function AdminCategories() {
  const [categories, setCategories] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    api.get("/categories")
      .then((res) => setCategories(res.data))
      .catch((err) => {
        setMessage({ tone: "bad", text: getErrorMessage(err, "Failed to load categories.") });
        setCategories([]);
      });
  }, [reloadKey]);

  const save = async (event) => {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    const body = { name: form.name.trim(), description: form.description.trim() };
    try {
      if (form.id) {
        await api.put(`/categories/${form.id}`, body);
        setMessage({ tone: "ok", text: `Category "${body.name}" updated.` });
      } else {
        await api.post("/categories", body);
        setMessage({ tone: "ok", text: `Category "${body.name}" created.` });
      }
      setForm(EMPTY);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not save the category.") });
    } finally {
      setBusy(false);
    }
  };

  const remove = async (c) => {
    if (!window.confirm(`Delete category "${c.name}"?`)) return;
    setBusy(true);
    setMessage(null);
    try {
      await api.delete(`/categories/${c.id}`);
      setMessage({ tone: "ok", text: `Category "${c.name}" deleted.` });
      setReloadKey((k) => k + 1);
    } catch (err) {
      const text = err?.response?.status === 409
        ? `"${c.name}" still has products in it. Move or delete those products first.`
        : getErrorMessage(err, "Could not delete the category.");
      setMessage({ tone: "bad", text });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="order-page">
      <DashboardNav area="admin" />
      <h1>Categories</h1>

      {message && <div className={`banner banner-${message.tone}`}>{message.text}</div>}

      <form className="card" onSubmit={save}>
        <h2>{form.id ? `Edit category #${form.id}` : "Add a category"}</h2>
        <div className="form-grid">
          <label>
            Name
            <input
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              required
              maxLength={100}
            />
          </label>
          <label>
            Description
            <input
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </label>
        </div>
        <div className="order-actions">
          <button type="submit" className="primary" disabled={busy}>
            {form.id ? "Save changes" : "Add category"}
          </button>
          {form.id && (
            <button type="button" onClick={() => setForm(EMPTY)} disabled={busy}>Cancel</button>
          )}
        </div>
      </form>

      {categories === null && <div className="skeleton" style={{ height: 44 }} />}

      {categories && categories.length === 0 && (
        <div className="state-box">
          <h2>No categories yet</h2>
          <p className="muted">Sellers need at least one category before they can add products.</p>
        </div>
      )}

      {categories && categories.length > 0 && (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr><th>Name</th><th>Description</th><th className="actions">Actions</th></tr>
            </thead>
            <tbody>
              {categories.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}<div className="muted">#{c.id}</div></td>
                  <td>{c.description || <span className="muted">—</span>}</td>
                  <td className="actions">
                    <button
                      onClick={() => setForm({ id: c.id, name: c.name, description: c.description ?? "" })}
                      disabled={busy}
                    >
                      Edit
                    </button>
                    <button className="danger" onClick={() => remove(c)} disabled={busy}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default AdminCategories;