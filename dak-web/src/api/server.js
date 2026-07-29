/**
 * Server-side reads of public content.
 *
 * Separate from client.js for one reason: this runs during the server render,
 * where there is no localStorage and no signed-in user. Anything fetched here
 * ends up in the HTML the server sends, which is what a crawler reads — so this
 * is the path that matters for every page meant to be found by search.
 *
 * Uses fetch rather than axios to get Next.js's caching, which axios bypasses.
 */
const API_URL = process.env.NEXT_PUBLIC_API_URL;

/**
 * @param {string} path      Endpoint path, e.g. '/guides/some-slug'
 * @param {object} [options]
 * @param {number} [options.revalidate]  Seconds to cache. Content that changes
 *   when an admin publishes needs a short window; a 404 needs none.
 */
async function getPublic(path, { revalidate = 60 } = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    next: { revalidate },
  });

  // 404 is an ordinary outcome here — an unknown slug — so it returns null and
  // the page decides what to render. Anything else is a real fault and throws.
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new Error(`API ${response.status} for ${path}`);
  }

  const body = await response.json();
  return body.data;
}

export async function getGuideBySlug(slug) {
  return getPublic(`/guides/${encodeURIComponent(slug)}`);
}

export async function getGuides({ categoryId, keyword, page = 0, pageSize = 20 } = {}) {
  const params = new URLSearchParams({ page, pageSize });
  if (categoryId) params.set('categoryId', categoryId);
  if (keyword) params.set('keyword', keyword);

  return getPublic(`/guides?${params}`);
}

export async function getGuideCategories() {
  // Categories change rarely, so they can be cached for longer.
  return getPublic('/guide-categories', { revalidate: 3600 });
}

export async function getUpdateById(id) {
    return getPublic(`/australia-updates/${encodeURIComponent(id)}`);
  }
  
  export async function getUpdateCategories() {
    // Categories change rarely, so they can be cached for longer.
    return getPublic('/update-categories', { revalidate: 3600 });
  }

  export async function getUpdates({ category, scope, keyword, sort, page = 0, pageSize = 20 } = {}) {
    const params = new URLSearchParams({ page, pageSize });
    if (category) params.set('category', category);
    if (scope) params.set('scope', scope);
    if (keyword) params.set('keyword', keyword);
    if (sort) params.set('sort', sort);
  
    return getPublic(`/australia-updates?${params}`);
  }

  export async function getBusinessBySlug(slug) {
    return getPublic(`/businesses/${encodeURIComponent(slug)}`);
  }
  
  export async function getBusinesses({ suburb, category, keyword, verified, sort, page = 0, pageSize = 20 } = {}) {
    const params = new URLSearchParams({ page, pageSize });
    if (suburb) params.set('suburb', suburb);
    if (category) params.set('category', category);
    if (keyword) params.set('keyword', keyword);
    if (verified) params.set('verified', 'true');
    if (sort) params.set('sort', sort);
  
    return getPublic(`/businesses?${params}`);
  }
  
  export async function getBusinessCategories() {
    // Categories change rarely, so they can be cached for longer.
    return getPublic('/business-categories', { revalidate: 3600 });
  }