import apiClient from "./client.js";
import {
  MOCK_PROFILE,
  MOCK_FED_BOOKS,
  MOCK_READING_NOTES,
  MOCK_RAG_ANSWER,
  MOCK_ORDERS,
  MOCK_COUPON_STATE,
  MOCK_RETURN_REQUESTS,
  MOCK_RESTOCK_REQUESTS,
  MOCK_REVIEWS,
} from "../mocks/mypage.js";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

function mockDelay(value, ms = 400) {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

// 아직 백엔드에 구현되지 않은 API 호출용 안전 폴백.
// 404 등으로 실패해도 콘솔에 Uncaught AxiosError를 남기지 않고 목업 데이터로 대체한다.
async function withFallback(request, fallback) {
  try {
    const { data } = await request();
    return data;
  } catch (error) {
    console.warn("[mypage] API 호출 실패, 목업 데이터로 대체합니다.", error);
    return fallback;
  }
}

export async function fetchProfile() {
  if (USE_MOCK) return mockDelay(MOCK_PROFILE);
  return withFallback(() => apiClient.get("/members/me"), MOCK_PROFILE);
}

// 백엔드에 사자(Lion) 관련 API가 아직 없어 목업으로 폴백한다.
export async function fetchFedBooks() {
  if (USE_MOCK) return mockDelay(MOCK_FED_BOOKS);
  return withFallback(() => apiClient.get("/lion/feedable-books"), MOCK_FED_BOOKS);
}

export async function fetchReadingNotes() {
  if (USE_MOCK) return mockDelay(MOCK_READING_NOTES);
  return withFallback(() => apiClient.get("/lion/reading-notes"), MOCK_READING_NOTES);
}

export async function askLion(question) {
  if (USE_MOCK) return mockDelay(MOCK_RAG_ANSWER, 800);
  return withFallback(() => apiClient.post("/lion/rag/ask", { question }), MOCK_RAG_ANSWER);
}

// 주문 모듈은 아직 컨트롤러가 없어 목업으로 폴백한다.
export async function fetchOrders() {
  if (USE_MOCK) return mockDelay(MOCK_ORDERS);
  return withFallback(() => apiClient.get("/orders"), MOCK_ORDERS);
}

export async function fetchCoupons() {
  if (USE_MOCK) return mockDelay(MOCK_COUPON_STATE);
  return withFallback(() => apiClient.get("/members/me/coupons"), MOCK_COUPON_STATE);
}

export async function fetchReturnRequests() {
  if (USE_MOCK) return mockDelay(MOCK_RETURN_REQUESTS);
  return withFallback(() => apiClient.get("/members/me/returns"), MOCK_RETURN_REQUESTS);
}

export async function fetchRestockRequests() {
  if (USE_MOCK) return mockDelay(MOCK_RESTOCK_REQUESTS);
  return withFallback(() => apiClient.get("/members/me/restock-requests"), MOCK_RESTOCK_REQUESTS);
}

// 내 리뷰 목록. 책 단위 리뷰(GET /books/{bookId}/reviews)와는 다른, 회원 기준 조회다.
export async function fetchReviews() {
  if (USE_MOCK) return mockDelay(MOCK_REVIEWS);
  return withFallback(() => apiClient.get("/members/me/reviews"), MOCK_REVIEWS);
}
