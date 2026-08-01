import { ExternalLink } from 'lucide-react';

/**
 * The sources behind an Australia Update, with licence attribution where the
 * publisher requires it.
 *
 * Rendered in two places on the detail page — once in the article for narrow
 * screens, once in the rail for wide ones. It lives here rather than being
 * written twice because the licence line is a condition of use: a copy that
 * falls behind the other is a breach, not a cosmetic difference.
 */
export default function SourceList({ sources }) {
  return (
    <ul className="flex flex-col gap-2.5">
      {sources.map((source) => (
        <li key={source.id}>
          <a
            href={source.sourceUrl}
            target="_blank"
            rel="noreferrer"
            className="flex items-start gap-2 text-[13px] text-korea-blue hover:underline"
          >
            <ExternalLink size={14} strokeWidth={1.75} className="shrink-0 mt-0.5" />
            {source.sourceTitle || source.sourceName}
          </a>

          {/* Who published it, not just what it was called. A recall issued by
              a regulator and a newspaper report of the same thing carry
              different weight, and the headline alone does not say which this
              is. Omitted when the title is missing, since the link already
              shows the organisation then. */}
          {source.sourceTitle && source.sourceName && (
            <span className="block text-[12px] text-faint pl-[22px] mt-0.5">
              {source.sourceName}
            </span>
          )}

          {/* Licence attribution, shown only where the source publishes under
              one. CC BY 4.0 asks for three things: credit, which the two lines
              above give; a link to the licence; and an indication that changes
              were made — required whether or not the result is an adaptation,
              which settles the question of whether a rewrite counts. The last
              sentence is not a licence condition but the reverse: the licence
              forbids implying endorsement, and a government source restated in
              Korean on a private site is exactly where a reader might infer
              one. Absent for ordinary copyright sources, where printing a
              licence line would be a false claim about the publisher. */}
          {source.licenceName && source.licenceUrl && (
            <span className="block text-[11px] text-faint pl-[22px] mt-1 leading-relaxed">
              원문{' '}
              <a
                href={source.licenceUrl}
                target="_blank"
                rel="license noreferrer"
                className="underline hover:text-muted"
              >
                {source.licenceName}
              </a>
              . DAK가 한국어로 재작성했으며, 원저작자의 보증을 의미하지 않습니다.
            </span>
          )}
        </li>
      ))}
    </ul>
  );
}