// Always returns a string, so React never tries to render an object.
// Backend errors look like {"error": "..."} (or older plain strings / {"message": ""}).
export const getErrorMessage = (err, fallback = "Something went wrong.") => {
  const data = err?.response?.data;
  if (typeof data === "string" && data) return data;
  if (data && typeof data === "object") {
    if (typeof data.error === "string") return data.error;
    if (typeof data.message === "string") return data.message;
  }
  if (err?.code === "ERR_NETWORK") return "Cannot reach the server.";
  return fallback;
};
