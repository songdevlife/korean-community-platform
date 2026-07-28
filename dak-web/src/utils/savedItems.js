import { Store, Newspaper, BookOpen } from 'lucide-react';

/**
 * Shared by Favourites and the dashboard preview. Both render the same saved
 * rows, and a link or icon that differs between them reads as a bug.
 */

export function resourceLink(item) {
  if (item.resourceType === 'BUSINESS') return `/businesses/${item.slugOrId}`;
  if (item.resourceType === 'AUSTRALIA_UPDATE') return `/australia-updates/${item.slugOrId}`;
  if (item.resourceType === 'GUIDE') return `/guides/${item.slugOrId}`;
  return '#';
}

export function resourceIcon(resourceType) {
  if (resourceType === 'BUSINESS') return Store;
  if (resourceType === 'GUIDE') return BookOpen;
  return Newspaper;
}