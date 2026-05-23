import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import * as aiService from '@/services/aiService';

export type ChatMessage = { id: string; role: 'user' | 'assistant'; content: string };

type AiState = {
  messages: ChatMessage[];
  recommendationText: string | null;
  chatStatus: 'idle' | 'loading' | 'failed';
  recStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
};

const initialState: AiState = {
  messages: [],
  recommendationText: null,
  chatStatus: 'idle',
  recStatus: 'idle',
  error: null,
};

export const fetchAiRecommendations = createAsyncThunk(
  'ai/recommendations',
  async (customerId: string) => {
    const res = await aiService.fetchRecommendations(customerId);
    return res.text;
  },
);

export const sendChatMessage = createAsyncThunk(
  'ai/chat',
  async ({ message, customerId }: { message: string; customerId?: string }) => {
    const res = await aiService.sendChat(message, customerId);
    return res.reply;
  },
);

const aiSlice = createSlice({
  name: 'ai',
  initialState,
  reducers: {
    pushUserMessage(state, a: PayloadAction<string>) {
      state.messages.push({
        id: crypto.randomUUID(),
        role: 'user',
        content: a.payload,
      });
    },
    clearChat(state) {
      state.messages = [];
      state.error = null;
    },
  },
  extraReducers: (b) => {
    b.addCase(fetchAiRecommendations.pending, (s) => {
      s.recStatus = 'loading';
      s.error = null;
    });
    b.addCase(fetchAiRecommendations.fulfilled, (s, a) => {
      s.recStatus = 'succeeded';
      s.recommendationText = a.payload;
    });
    b.addCase(fetchAiRecommendations.rejected, (s, e) => {
      s.recStatus = 'failed';
      s.error = e.error.message ?? 'Erro nas recomendações';
    });

    b.addCase(sendChatMessage.pending, (s) => {
      s.chatStatus = 'loading';
    });
    b.addCase(sendChatMessage.fulfilled, (s, a) => {
      s.chatStatus = 'idle';
      s.messages.push({
        id: crypto.randomUUID(),
        role: 'assistant',
        content: a.payload,
      });
    });
    b.addCase(sendChatMessage.rejected, (s, e) => {
      s.chatStatus = 'failed';
      s.error = e.error.message ?? 'Erro no chat';
    });
  },
});

export const { pushUserMessage, clearChat } = aiSlice.actions;
export default aiSlice.reducer;

export const selectAi = (s: { ai: AiState }) => s.ai;
