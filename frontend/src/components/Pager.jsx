// Works with the backend's PageResponse: {content, page, size, totalElements, totalPages, last}
function Pager({ data, onPage }) {
  if (!data || data.totalPages <= 1) return null;

  return (
    <div className="pager">
      <button disabled={data.page === 0} onClick={() => onPage(data.page - 1)}>
        ← Previous
      </button>
      <span className="muted">
        Page {data.page + 1} of {data.totalPages} · {data.totalElements} total
      </span>
      <button disabled={data.last} onClick={() => onPage(data.page + 1)}>
        Next →
      </button>
    </div>
  );
}

export default Pager;