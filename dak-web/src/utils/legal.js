import fs from 'node:fs';
import path from 'node:path';

/**
 * Legal documents are files on disk rather than rows in the database. They are
 * revised by editing text, not through an admin screen, and they change perhaps
 * twice a year — so a table, an endpoint and a form would all be machinery
 * around something that a text file already does.
 *
 * Server-only: this reads the filesystem, so anything importing it must be a
 * server component.
 */
const DIR = path.join(process.cwd(), 'content', 'legal');

/**
 * Order is deliberate. Privacy Policy and Terms of Service lead because they
 * are the two a reader is sent here to find — the sign-up form will link
 * directly to both — and the rest follow in rough order of how often anyone
 * needs them.
 */
export const LEGAL_DOCUMENTS = [
  { slug: 'privacy-policy', title: 'Privacy Policy', blurb: 'What DAK collects, why, and what you can ask for.' },
  { slug: 'terms-of-service', title: 'Terms of Service', blurb: 'The terms you agree to by using DAK.' },
  { slug: 'community-guidelines', title: 'Community Guidelines', blurb: 'What is expected of people posting here.' },
  { slug: 'content-policy', title: 'Content Policy', blurb: 'What may and may not be published.' },
  { slug: 'moderation-policy', title: 'Moderation Policy', blurb: 'How content is reviewed and what happens when it is removed.' },
  { slug: 'business-listing-policy', title: 'Business Listing Policy', blurb: 'Rules for listing a business in the directory.' },
  { slug: 'ai-usage-policy', title: 'AI Usage Policy', blurb: 'Where DAK uses automated tools, and where it does not.' },
  { slug: 'accessibility-statement', title: 'Accessibility Statement', blurb: 'How DAK approaches accessibility.' },
  { slug: 'legal-notice', title: 'Legal Notice', blurb: 'Who operates this site.' },
];

export function findLegalDocument(slug) {
  return LEGAL_DOCUMENTS.find((d) => d.slug === slug) ?? null;
}

/**
 * Returns null for an unknown slug so the page can render a 404 rather than
 * throwing. The slug is checked against the list above before the filesystem is
 * touched — a path segment arriving from the URL should never reach path.join
 * unvalidated.
 */
export function readLegalDocument(slug) {
  if (!findLegalDocument(slug)) return null;

  try {
    return fs.readFileSync(path.join(DIR, `${slug}.md`), 'utf8');
  } catch {
    return null;
  }
}