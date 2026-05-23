import { cn } from '@/utils/cn';
import { AIResponse } from '@/components/AIResponse';

type Props = {
  role: 'user' | 'assistant';
  content: string;
};

export function ChatMessage({ role, content }: Props) {
  const isUser = role === 'user';
  return (
    <div className={cn('flex w-full', isUser ? 'justify-end' : 'justify-start')}>
      <div
        className={cn(
          'max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm',
          isUser ? 'bg-brand text-white' : 'border border-stone-200 bg-white text-ink',
        )}
      >
        {isUser ? content : <AIResponse text={content} />}
      </div>
    </div>
  );
}
