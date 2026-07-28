import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

// react-markdown builds React elements from a syntax tree and never uses
// dangerouslySetInnerHTML. Raw HTML in the source is stripped by default, so no
// sanitiser is needed — rehype-raw is deliberately not enabled.
// remark-gfm is required for tables, which the guide content relies on.
const components = {
  h1: ({ children }) => (
    <h1 className="text-2xl font-bold text-snow mt-10 mb-4 first:mt-0">{children}</h1>
  ),
  h2: ({ children }) => (
    <h2 className="text-xl font-bold text-snow mt-10 mb-3 pb-2 border-b border-border-dark">
      {children}
    </h2>
  ),
  h3: ({ children }) => (
    <h3 className="text-lg font-semibold text-snow mt-6 mb-2">{children}</h3>
  ),
  p: ({ children }) => <p className="text-muted leading-7 mb-4">{children}</p>,
  ul: ({ children }) => (
    <ul className="list-disc pl-5 mb-4 space-y-1 text-muted">{children}</ul>
  ),
  ol: ({ children }) => (
    <ol className="list-decimal pl-5 mb-4 space-y-1 text-muted">{children}</ol>
  ),
  li: ({ children }) => <li className="leading-7">{children}</li>,
  strong: ({ children }) => (
    <strong className="font-semibold text-snow">{children}</strong>
  ),
  em: ({ children }) => <em className="italic">{children}</em>,
  blockquote: ({ children }) => (
    <blockquote className="border-l-2 border-adelaide-red pl-4 py-1 my-6 text-faint italic">
      {children}
    </blockquote>
  ),
  hr: () => <hr className="border-border-dark my-8" />,
  // Wrapped so a wide table scrolls rather than breaking the layout below the
  // 768px breakpoint defined in 06 §3.
  table: ({ children }) => (
    <div className="overflow-x-auto my-6">
      <table className="w-full text-sm border-collapse">{children}</table>
    </div>
  ),
  thead: ({ children }) => (
    <thead className="border-b border-border-dark">{children}</thead>
  ),
  th: ({ children }) => (
    <th className="text-left font-semibold text-snow px-3 py-2 whitespace-nowrap">
      {children}
    </th>
  ),
  td: ({ children }) => (
    <td className="px-3 py-2 text-muted border-t border-border-dark align-top">
      {children}
    </td>
  ),
  code: ({ children }) => (
    <code className="bg-surface px-1.5 py-0.5 rounded text-sm text-snow">{children}</code>
  ),
  a: ({ href, children }) => {
    const external = href?.startsWith('http');
    return (
        <a
        href={href}
        className="text-adelaide-red underline underline-offset-2 hover:opacity-80"
        target={external ? '_blank' : undefined}
        rel={external ? 'noopener noreferrer' : undefined}
      >
        {children}
      </a>
    );
  },
};

export default function Markdown({ children }) {
  if (!children) return null;
  return (
    <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
      {children}
    </ReactMarkdown>
  );
}