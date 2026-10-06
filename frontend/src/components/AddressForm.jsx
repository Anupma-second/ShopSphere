import { useState } from "react";
import { getErrorMessage } from "../utils/errors";

const EMPTY_ADDRESS = {
  fullName: "",
  phone: "",
  addressLine: "",
  city: "",
  state: "",
  postalCode: "",
  country: "India",
};

/**
 * Add / edit an address. Used by the address book and by checkout.
 * onSave(values) must return a promise; errors from it are shown here.
 */
function AddressForm({ initial = EMPTY_ADDRESS, onSave, onCancel, saveLabel = "Save address" }) {
  const [values, setValues] = useState({ ...EMPTY_ADDRESS, ...initial });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const trimmed = Object.fromEntries(
        Object.entries(values).map(([k, v]) => [k, typeof v === "string" ? v.trim() : v])
      );
      await onSave(trimmed);
    } catch (err) {
      setError(getErrorMessage(err, "Could not save the address."));
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit}>
      <div className="field-grid">
        <label className="field">
          Full name
          <input value={values.fullName} onChange={set("fullName")} required maxLength={100} autoComplete="name" />
        </label>
        <label className="field">
          Phone number
          <input
            value={values.phone}
            onChange={set("phone")}
            required
            pattern="[0-9+\- ]{7,15}"
            title="7 to 15 digits"
            inputMode="tel"
            autoComplete="tel"
          />
        </label>
        <label className="field full">
          Address (house no., street, area)
          <input value={values.addressLine} onChange={set("addressLine")} required maxLength={255} autoComplete="street-address" />
        </label>
        <label className="field">
          City
          <input value={values.city} onChange={set("city")} required autoComplete="address-level2" />
        </label>
        <label className="field">
          State
          <input value={values.state} onChange={set("state")} required autoComplete="address-level1" />
        </label>
        <label className="field">
          PIN code
          <input value={values.postalCode} onChange={set("postalCode")} required maxLength={12} inputMode="numeric" autoComplete="postal-code" />
        </label>
        <label className="field">
          Country
          <input value={values.country} onChange={set("country")} required autoComplete="country-name" />
        </label>
      </div>

      {error && <p className="notice bad">{error}</p>}

      <div className="form-actions">
        <button type="submit" className="btn primary" disabled={busy}>
          {busy ? "Saving..." : saveLabel}
        </button>
        {onCancel && (
          <button type="button" className="btn" onClick={onCancel} disabled={busy}>Cancel</button>
        )}
      </div>
    </form>
  );
}

export default AddressForm;