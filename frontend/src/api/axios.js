import axios from "axios";

const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const clearSession = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
};

const goToLogin = () => {
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
};

// One refresh at a time, even if several requests fail together
let refreshPromise = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthCall = original?.url?.includes("/auth/");

    if (status === 401 && original && !isAuthCall) {
      const refreshToken = localStorage.getItem("refreshToken");

      // Access token expired -> try once to get a new one
      if (refreshToken && !original._retry) {
        original._retry = true;
        try {
          refreshPromise =
            refreshPromise ||
            axios
              .post(`${baseURL}/auth/refresh`, refreshToken, {
                headers: { "Content-Type": "text/plain" },
              })
              .finally(() => {
                refreshPromise = null;
              });

          const { data } = await refreshPromise;
          localStorage.setItem("accessToken", data.accessToken);
          original.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(original);
        } catch {
          // fall through to logout
        }
      }

      clearSession();
      goToLogin();
    }

    return Promise.reject(error);
  }
);

export default api;
