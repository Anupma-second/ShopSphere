// Read-only star rating, e.g. <Stars value={4.5} count={12} />
function Stars({ value, count, size = 15 }) {
  if (!value) {
    return count === undefined ? null : <span className="stars-none">No reviews yet</span>;
  }

  const pct = Math.max(0, Math.min(5, value)) * 20;

  return (
    <span className="stars" style={{ fontSize: size }} title={`${value} out of 5`}>
      <span className="stars-track" aria-hidden="true">
        ★★★★★
        <span className="stars-fill" style={{ width: `${pct}%` }}>★★★★★</span>
      </span>
      <span className="stars-num">{value.toFixed(1)}</span>
      {count !== undefined && <span className="stars-count">({count})</span>}
    </span>
  );
}

export default Stars;