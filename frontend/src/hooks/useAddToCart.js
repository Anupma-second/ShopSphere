import { useLocation, useNavigate } from "react-router-dom";
import api from "../api/axios";
import { isLoggedIn } from "../utils/auth";
import { notifyCartChanged } from "../utils/cart";
import { getErrorMessage } from "../utils/errors";

/**
 * Returns addToCart(productId, quantity) -> Promise<{ ok, text }>.
 * Logged-out shoppers are sent to /login and brought back here afterwards.
 */
export function useAddToCart() {
  const navigate = useNavigate();
  const location = useLocation();

  return async (productId, quantity = 1) => {
    if (!isLoggedIn()) {
      const here = location.pathname + location.search;
      navigate(`/login?next=${encodeURIComponent(here)}`);
      return { ok: false, text: "" };
    }
    try {
      await api.post("/cart-items/add", { productId, quantity });
      notifyCartChanged();
      return { ok: true, text: "Added to your cart." };
    } catch (err) {
      return { ok: false, text: getErrorMessage(err, "Could not add to cart.") };
    }
  };
}