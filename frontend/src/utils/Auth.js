// The logged-in user is saved by Login.jsx as {id, name, email, role}.
export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem("user"));
  } catch {
    return null;
  }
};

export const isLoggedIn = () => !!localStorage.getItem("accessToken");

// Where each role lands after logging in
export const homeFor = (user) => {
  if (user?.role === "ADMIN") return "/admin";
  if (user?.role === "SELLER") return "/seller";
  return "/products";
};

// Called after a successful login (other pages rely on these keys).
export const saveSession = (data) => {
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  localStorage.setItem("user", JSON.stringify(data.user));
};

// After the user edits their profile (keeps the header name in sync).
export const updateStoredUser = (user) => {
  localStorage.setItem("user", JSON.stringify(user));
};

export const logout = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
};