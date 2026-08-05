import apiClient from "./client.js";
import { MOCK_CART_ITEMS, MOCK_CART_BENEFITS } from "../mocks/cart.js";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// 장바구니 API는 아직 백엔드에 구현되지 않아, 실패 시 목업 데이터로 안전하게 폴백한다.
async function withFallback(request, fallback) {
  try {
    const { data } = await request();
    return data;
  } catch (error) {
    console.warn("[cart] API 호출 실패, 목업 데이터로 대체합니다.", error);
    return fallback;
  }
}

export async function fetchCartItems() {
  if (USE_MOCK) return MOCK_CART_ITEMS;
  return withFallback(() => apiClient.get("/cart/items"), MOCK_CART_ITEMS);
}

export async function fetchCartBenefits() {
  if (USE_MOCK) return MOCK_CART_BENEFITS;
  return withFallback(() => apiClient.get("/cart/benefits"), MOCK_CART_BENEFITS);
}
