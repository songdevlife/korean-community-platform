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

/**
 * Creates a new guide. slug is optional but should be supplied in English —
 * a Korean title cannot generate one.
 */
export async function createGuide(payload) {
  const response = await apiClient.post('/admin/guides', payload);
  return response.data.data;
}