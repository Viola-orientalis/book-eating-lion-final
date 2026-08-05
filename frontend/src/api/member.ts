import { apiClient, unwrap } from './client.ts'
import { toMember, toSubscription } from './mappers.ts'
import type { ApiResponse, MemberResponse, SubscriptionResponse } from './types.ts'
import type { Member, Subscription } from '../types/member.ts'

// GET /api/members/me — 내 정보 조회 (JWT 인증 필요)
export async function getMyProfile(): Promise<Member> {
  return toMember(await unwrap(apiClient.get<ApiResponse<MemberResponse>>('/members/me')))
}

// GET /api/members/me/subscription — 구독 상태 조회 (JWT 인증 필요)
export async function getMySubscription(): Promise<Subscription> {
  try {
    return toSubscription(
      await unwrap(apiClient.get<ApiResponse<SubscriptionResponse | null>>('/members/me/subscription')),
    )
  } catch {
    // 네트워크 오류 등으로 구독 상태를 확인할 수 없으면 미구독으로 안전하게 폴백한다.
    return toSubscription(null)
  }
}
