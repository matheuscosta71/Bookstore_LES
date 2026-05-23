import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export function AIResponse({ text }: { text: string }) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      // Security: ReactMarkdown does not render HTML by default; keep this explicit.
      skipHtml
      components={{
        p: ({ node, ...props }) => <p {...props} className="mb-2 last:mb-0" />,
        strong: ({ node, ...props }) => <strong {...props} className="font-semibold" />,
        em: ({ node, ...props }) => <em {...props} className="italic" />,
        ul: ({ node, ...props }) => <ul {...props} className="mb-2 list-disc pl-5" />,
        ol: ({ node, ...props }) => <ol {...props} className="mb-2 list-decimal pl-5" />,
        li: ({ node, ...props }) => <li {...props} className="mb-1" />,
        a: ({ node, ...props }) => {
          const href = (props.href ?? '') as string;
          const external = href.startsWith('http://') || href.startsWith('https://');
          return (
            <a
              {...props}
              className="text-brand underline underline-offset-2"
              target={external ? '_blank' : undefined}
              rel={external ? 'noreferrer' : undefined}
            />
          );
        },
      }}
    >
      {text}
    </ReactMarkdown>
  );
}

