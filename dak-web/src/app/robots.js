/**
 * Next.js serves this at /robots.txt.
 *
 * The sitemap line is the point of the file: it is how a crawler that has not
 * been told about the site finds the list of everything on it.
 */
export default function robots() {
    return {
      rules: [
        {
          userAgent: '*',
          allow: '/',
          // Neither is worth indexing and both are reachable. Search results are
          // an unbounded set of near-duplicate pages; the admin queue is
          // authenticated but there is no reason to advertise it.
          disallow: ['/admin/', '/search'],
        },
      ],
      sitemap: 'https://discoveradelaidekorea.au/sitemap.xml',
    };
  }