import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../../api/axios";
import DashboardNav from "../../components/DashboardNav";
import Pager from "../../components/Pager";
import { getUser } from "../../utils/auth";
import { getErrorMessage } from "../../utils/errors";
import "../../styles/orders.css";
import "../../styles/dashboard.css";

const ROLES = ["CUSTOMER", "SELLER", "ADMIN"];

function AdminUsers() {
  const me = getUser();
  const [params] = useSearchParams();

  const [role, setRole] = useState(params.get("role") ?? "");
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    let cancelled = false;
    api.get("/admin/users", {
      params: { q: query || undefined, role: role || undefined, page, size: 20, sort: "id,desc" },
    })
      .then((res) => { if (!cancelled) setData(res.data); })
      .catch((err) => {
        if (!cancelled) {
          setMessage({ tone: "bad", text: getErrorMessage(err, "Failed to load users.") });
          setData({ content: [], page: 0, totalPages: 0, totalElements: 0, last: true });
        }
      });
    return () => { cancelled = true; };
  }, [query, role, page]);

  const replaceUser = (updated) =>
    setData((d) => ({ ...d, content: d.content.map((u) => (u.id === updated.id ? updated : u)) }));

  const changeRole = async (user, newRole) => {
    if (!window.confirm(`Change ${user.email} from ${user.role} to ${newRole}?`)) return;
    setBusyId(user.id);
    setMessage(null);
    try {
      const { data: updated } = await api.put(`/admin/users/${user.id}/role`, null, { params: { role: newRole } });
      replaceUser(updated);
      setMessage({ tone: "ok", text: `${updated.email} is now ${updated.role}. They may need to log in again.` });
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not change the role.") });
    } finally {
      setBusyId(null);
    }
  };

  const toggleEnabled = async (user) => {
    const enable = !user.enabled;
    if (!enable && !window.confirm(`Block ${user.email}? They will be logged out immediately.`)) return;
    setBusyId(user.id);
    setMessage(null);
    try {
      const { data: updated } = await api.put(`/admin/users/${user.id}/enabled`, null, { params: { enabled: enable } });
      replaceUser(updated);
      setMessage({ tone: "ok", text: `${updated.email} ${updated.enabled ? "unblocked" : "blocked"}.` });
    } catch (err) {
      setMessage({ tone: "bad", text: getErrorMessage(err, "Could not update the account.") });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="order-page">
      <DashboardNav area="admin" />
      <h1>Users</h1>
      <p className="muted">Search accounts, make someone a seller, or block an account.</p>

      <div className="toolbar">
        <form
          className="inline-form"
          style={{ flex: 1 }}
          onSubmit={(e) => { e.preventDefault(); setPage(0); setQuery(search.trim()); }}
        >
          <input
            type="search"
            placeholder="Search by name or email"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ flex: 1, width: "auto" }}
          />
          <button type="submit">Search</button>
        </form>
        <select value={role} onChange={(e) => { setPage(0); setRole(e.target.value); }}>
          <option value="">All roles</option>
          {ROLES.map((r) => <option key={r} value={r}>{r.charAt(0) + r.slice(1).toLowerCase()}s</option>)}
        </select>
      </div>

      {message && <div className={`banner banner-${message.tone}`}>{message.text}</div>}

      {data === null && (
        <>
          <div className="skeleton" style={{ height: 44 }} />
          <div className="skeleton" style={{ height: 44 }} />
        </>
      )}

      {data && data.content.length === 0 && (
        <div className="state-box">
          <h2>No users found</h2>
          <p className="muted">Try a different search or role.</p>
        </div>
      )}

      {data && data.content.length > 0 && (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Email verified</th>
                <th>Status</th>
                <th className="actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((u) => {
                const isMe = u.id === me?.id;
                const busy = busyId === u.id;
                return (
                  <tr key={u.id}>
                    <td>
                      {u.name} {isMe && <span className="badge badge-info">You</span>}
                      <div className="muted">{u.email}</div>
                    </td>
                    <td>
                      <select
                        value={u.role}
                        disabled={isMe || busy}
                        onChange={(e) => changeRole(u, e.target.value)}
                      >
                        {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                      </select>
                    </td>
                    <td>{u.emailVerified ? "Yes" : <span className="muted">No</span>}</td>
                    <td>
                      <span className={`badge badge-${u.enabled ? "ok" : "bad"}`}>
                        {u.enabled ? "Active" : "Blocked"}
                      </span>
                    </td>
                    <td className="actions">
                      <button
                        className={u.enabled ? "danger" : ""}
                        disabled={isMe || busy}
                        onClick={() => toggleEnabled(u)}
                      >
                        {u.enabled ? "Block" : "Unblock"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pager data={data} onPage={setPage} />
    </div>
  );
}

export default AdminUsers;