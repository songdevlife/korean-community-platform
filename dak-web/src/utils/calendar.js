/**
 * A Google Calendar link for an event.
 *
 * A URL rather than a library: the format is a documented query string, and
 * pulling in an ICS package to build one would be more dependency than the
 * feature is worth.
 *
 * An event is different from everything else on the site in that reading it is
 * not the point — turning up is. Someone who reads the page and means to go
 * has to remember, and this is the one moment they are in a position to make
 * that not their problem.
 */
export function googleCalendarUrl(event) {
    if (!event?.startsAt) return null;
  
    // Google wants UTC in basic ISO form: 20260806T174810Z.
    const stamp = (iso) => new Date(iso).toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
  
    // An event with no stated end is given an hour, because the parameter is
    // required and an hour is the least wrong guess for something that did not
    // say. The page still shows no end time, so nothing is being claimed.
    const end = event.endsAt
      ? stamp(event.endsAt)
      : stamp(new Date(new Date(event.startsAt).getTime() + 3600000).toISOString());
  
    const details = [
      event.description,
      event.sourceUrl ? `원문: ${event.sourceUrl}` : null,
      'via Discover Adelaide Korea',
    ].filter(Boolean).join('\n\n');
  
    const params = new URLSearchParams({
      action: 'TEMPLATE',
      text: event.title,
      dates: `${stamp(event.startsAt)}/${end}`,
      details,
    });
  
    const location = [event.venueName, event.venueAddress].filter(Boolean).join(', ');
    if (location) params.set('location', location);
  
    return `https://calendar.google.com/calendar/render?${params}`;
  }