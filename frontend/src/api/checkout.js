import apiClient from "./client.js";
import { MOCK_CHECKOUT_SUMMARY } from "../mocks/checkout.js";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

export async function fetchCheckoutSummary() {
  if (USE_MOCK) return MOCK_CHECKOUT_SUMMARY;
  try {
    const { data } = await apiClient.get("/checkout/summary");
    return data;
  } catch (error) {
    // 체크아웃 요약 API는 아직 백엔드에 구현되지 않아, 실패 시 목업 데이터로 안전하게 폴백한다.
    console.warn("[checkout] API 호출 실패, 목업 데이터로 대체합니다.", error);
    return MOCK_CHECKOUT_SUMMARY;
  }
}
