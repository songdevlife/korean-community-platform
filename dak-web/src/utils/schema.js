const SITE_URL = 'https://discoveradelaidekorea.au';

// Absolute because metadataBase resolves relative paths for Next's metadata
// only — it does not touch JSON-LD. A relative path left in a schema block is
// one a crawler has no base to resolve against.
export const SITE_IMAGE = `${SITE_URL}/opengraph-image.png`;

// Named once here rather than inline in each page. Google's Article guidance
// wants a publisher logo, and a mismatched name across page types reads as two
// different publishers.
export const PUBLISHER = {
  '@type': 'Organization',
  name: 'DAK — Discover Adelaide Korea',
  url: SITE_URL,
  logo: {
    '@type': 'ImageObject',
    url: SITE_IMAGE,
  },
};

/**
 * trail: [{ name, path }] — path is site-relative, the last item is the page
 * itself. Absolute URLs are required; a relative item is ignored.
 */
export function buildBreadcrumb(trail) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: trail.map((item, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name: item.name,
      item: `${SITE_URL}${item.path}`,
    })),
  };
}