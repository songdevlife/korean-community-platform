/**
 * Serves the backend's sitemap at the frontend's root.
 *
 * Crawlers look for /sitemap.xml on the domain the pages are on, which is this
 * one. The document itself is built from the database, so it stays where the
 * database is and this route passes it through — rather than a second generator
 * here that would drift out of step with the first.
 */
const API_URL = process.env.NEXT_PUBLIC_API_URL;

// The API base ends in /api/v1; the sitemap sits outside it, at the backend
// root, because it is a document rather than an API resource.
const SITEMAP_URL = API_URL
  ? `${API_URL.replace(/\/api\/v1\/?$/, '')}/sitemap.xml`
  : null;

// Regenerated at most once an hour. Publishing a guide should show up in the
// sitemap the same day; it does not need to show up the same second.
export const revalidate = 3600;

export async function GET() {
  if (!SITEMAP_URL) {
    return new Response('Sitemap unavailable.', { status: 503 });
  }

  try {
    const response = await fetch(SITEMAP_URL, { next: { revalidate } });

    if (!response.ok) {
      // 503 rather than passing the backend's status through: a crawler reading
      // a 404 here may drop the sitemap from its records, where a 503 tells it
      // to come back.
      return new Response('Sitemap unavailable.', { status: 503 });
    }

    return new Response(await response.text(), {
      headers: { 'Content-Type': 'application/xml' },
    });
  } catch {
    return new Response('Sitemap unavailable.', { status: 503 });
  }
}