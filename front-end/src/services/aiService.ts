import { api } from './api';

export type RecommendationResponse = { text: string };
export type ChatResponse = { reply: string };

export async function fetchRecommendations(customerId: string): Promise<RecommendationResponse> {
  const { data } = await api.post<RecommendationResponse>(
    `/ai/recommendations/${customerId}`,
  );
  return data;
}

export async function sendChat(
  message: string,
  customerId?: string,
  history?: Array<{ role: 'user' | 'assistant'; content: string }>,
): Promise<ChatResponse> {
  const { data } = await api.post<ChatResponse>('/ai/chat', { message, customerId, history });
  return data;
}
