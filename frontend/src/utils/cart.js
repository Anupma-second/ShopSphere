// Lets any page tell the header "the cart changed, refresh the count".
export const CART_EVENT = "cart-updated";

export const notifyCartChanged = () => window.dispatchEvent(new Event(CART_EVENT));