import { useState, useRef, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { clearChat, pushUserMessage, sendChatMessage } from '@/features/ai/aiSlice';
import { ChatMessage } from '@/components/ChatMessage';
import { ChatInput } from '@/components/ChatInput';
import { ROUTES } from '@/constants/routes';

export function AiChatPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId);
  const { messages, chatStatus, error } = useAppSelector((s) => s.ai);
  const [input, setInput] = useState('');
  const bottom = useRef<HTMLDivElement>(null);
  const [searchParams] = useSearchParams();
  const prompt = searchParams.get('prompt') ?? '';
  const autoSentPromptRef = useRef<string | null>(null);

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!prompt) return;
    if (autoSentPromptRef.current === prompt) return;
    autoSentPromptRef.current = prompt;

    dispatch(clearChat());
    dispatch(pushUserMessage(prompt));
    dispatch(sendChatMessage({ message: prompt, customerId: customerId ?? undefined }));
    setInput('');
  }, [dispatch, prompt, customerId]);

  function send() {
    const t = input.trim();
    if (!t) return;
    dispatch(pushUserMessage(t));
    setInput('');
    dispatch(sendChatMessage({ message: t, customerId: customerId ?? undefined }));
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col px-4 py-8 md:h-[calc(100vh-8rem)] md:py-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold">Assistente de livros</h1>
        </div>
        <div className="flex shrink-0 items-center gap-4">
          <Link to={ROUTES.home} className="text-sm font-medium text-brand hover:underline">
            Continuar comprando →
          </Link>
          <button
            type="button"
            onClick={() => dispatch(clearChat())}
            className="text-sm text-ink-muted hover:text-brand"
          >
            Limpar conversa
          </button>
        </div>
      </div>

      <div className="mt-6 flex flex-1 min-h-0 flex-col overflow-hidden rounded-2xl border border-stone-200 bg-white shadow-card md:flex-row">
        <div className="flex flex-1 flex-col overflow-hidden">
          <div className="flex-1 space-y-4 overflow-y-auto p-4">
            {messages.length === 0 && (
              <p className="text-center text-sm text-ink-muted">
                Experimente: “Quero um livro leve sobre produtividade”
              </p>
            )}
            {messages.map((m) => (
              <ChatMessage key={m.id} role={m.role} content={m.content} />
            ))}
            {chatStatus === 'loading' && (
              <p className="text-sm text-ink-muted">Pensando…</p>
            )}
            {error && <p className="text-sm text-red-600">{error}</p>}
            <div ref={bottom} />
          </div>
          <ChatInput value={input} onChange={setInput} onSend={send} loading={chatStatus === 'loading'} />
        </div>
        <aside className="hidden w-48 flex-shrink-0 border-l border-stone-100 p-4 md:block">
          <p className="text-xs font-semibold uppercase text-ink-muted">Sugestões</p>
          <ul className="mt-3 space-y-2 text-sm text-brand">
            <li>Romance</li>
            <li>Tecnologia</li>
            <li>Negócios</li>
            <li>Ficção</li>
          </ul>
        </aside>
      </div>
    </div>
  );
}
