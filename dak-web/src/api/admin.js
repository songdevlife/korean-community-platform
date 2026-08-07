'use client';

import apiClient from './client';

// Admin queues are worked through item by item, so a modest page size keeps
// each screen scannable and the sections below it reachable.
const QUEUE_PAGE_SIZE = 10;

export async function fetchPendingBusinesses(page = 0) {
  const response = await apiClient.get('/admin/businesses', {
    params: { status: 'PENDING', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function updateBusinessStatus(businessId, status) {
  const response = await apiClient.patch(`/admin/businesses/${businessId}/status`, { status });
  return response.data.data;
}

/**
 * Edits a published or draft business. Send only the fields being changed;
 * omitted fields are left as they are.
 */
export async function updateBusiness(businessId, changes) {
  const response = await apiClient.patch(`/admin/businesses/${businessId}`, changes);
  return response.data.data;
}

export async function fetchDraftUpdates(page = 0) {
  const response = await apiClient.get('/admin/australia-updates', {
    params: { status: 'DRAFT', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function fetchPublishedUpdates(page = 0) {
  const response = await apiClient.get('/admin/australia-updates', {
    params: { status: 'PUBLISHED', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function fetchArchivedUpdates(page = 0) {
  const response = await apiClient.get('/admin/australia-updates', {
    params: { status: 'ARCHIVED', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function updateUpdateStatus(updateId, status) {
  const response = await apiClient.patch(`/admin/australia-updates/${updateId}/status`, { status });
  return response.data.data;
}

/**
 * Updates the fields an admin must supply or rewrite before publishing.
 * Pass only the fields being changed; omitted ones are left as they are.
 */
export async function updateUpdateMetadata(updateId, changes) {
  const response = await apiClient.patch(
    `/admin/australia-updates/${updateId}/metadata`,
    changes
  );
  return response.data.data;
}

/**
 * Runs the RSS feeds once, now. Scheduled polling stays disabled while the
 * backend runs on a local machine - nothing polls while the machine is off, and
 * every restart would fire the schedule again - so this is the only way new
 * articles arrive.
 *
 * Slow by nature: each article costs one model call for the relevance decision
 * and the Korean draft, so a run over a few dozen articles takes minutes. The
 * caller is responsible for showing that it is still working.
 */
export async function triggerRssPoll() {
  const response = await apiClient.post('/admin/australia-updates/poll-now');
  return response.data.data;
}

/**
 * Edits a guide. Send only the fields being changed; omitted fields are left
 * as they are. Note that changing slug alters the public URL.
 */
export async function updateGuide(guideId, changes) {
  const response = await apiClient.patch(`/admin/guides/${guideId}`, changes);
  return response.data.data;
}

export async function updateGuideStatus(guideId, status) {
  const response = await apiClient.patch(`/admin/guides/${guideId}/status`, { status });
  return response.data.data;
}

export async function fetchDraftGuides(page = 0) {
  const response = await apiClient.get('/admin/guides', {
    params: { status: 'DRAFT', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function fetchPublishedGuides(page = 0) {
  const response = await apiClient.get('/admin/guides', {
    params: { status: 'PUBLISHED', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function fetchArchivedGuides(page = 0) {
  const response = await apiClient.get('/admin/guides', {
    params: { status: 'ARCHIVED', page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function fetchUsers(page = 0) {
  const response = await apiClient.get('/admin/users', {
    params: { page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}


/**
 * Creates a new guide. slug is optional but should be supplied in English —
 * a Korean title cannot generate one.
 */
export async function createGuide(payload) {
  const response = await apiClient.post('/admin/guides', payload);
  return response.data.data;
}

export async function fetchEventsByStatus(status, page = 0) {
  const response = await apiClient.get('/admin/events', {
    params: { status, page, pageSize: QUEUE_PAGE_SIZE },
  });
  return response.data.data;
}

export async function fetchEventById(eventId) {
  const response = await apiClient.get(`/admin/events/${eventId}`);
  return response.data.data;
}

export async function createEvent(payload) {
  const response = await apiClient.post('/admin/events', payload);
  return response.data.data;
}

/**
 * Edits an event. Send only the fields being changed; omitted fields are left
 * as they are. Status travels through here too rather than through a separate
 * endpoint, since publishing an event is usually the same act as finishing it.
 */
export async function updateEvent(eventId, changes) {
  const response = await apiClient.patch(`/admin/events/${eventId}`, changes);
  return response.data.data;
}

/**
 * Removes an event outright.
 *
 * Unlike updates and guides, which archive: an event entered with the wrong
 * date is not a record worth keeping, and a duplicate transcription is noise
 * rather than history. ARCHIVED remains available through updateEvent for
 * anything worth keeping out of sight but not deleting.
 */
export async function deleteEvent(eventId) {
  await apiClient.delete(`/admin/events/${eventId}`);
}

// --- Rentals ---

export async function fetchRentalsByStatus(status, page = 0) {
  const response = await apiClient.get('/admin/rentals', {
    params: { status, page },
  });
  return response.data.data;
}

export async function fetchRental(rentalId) {
  const response = await apiClient.get(`/admin/rentals/${rentalId}`);
  return response.data.data;
}

export async function createRental(payload) {
  const response = await apiClient.post('/admin/rentals', payload);
  return response.data.data;
}

export async function updateRental(rentalId, changes) {
  const response = await apiClient.patch(`/admin/rentals/${rentalId}`, changes);
  return response.data.data;
}

/**
 * Another twenty-one days from now. Separate from update because it is a
 * decision rather than an edit: an advertiser said they are still looking,
 * and nothing else about the listing changed.
 */
export async function extendRental(rentalId) {
  const response = await apiClient.post(`/admin/rentals/${rentalId}/extend`);
  return response.data.data;
}

export async function deleteRental(rentalId) {
  await apiClient.delete(`/admin/rentals/${rentalId}`);
}
