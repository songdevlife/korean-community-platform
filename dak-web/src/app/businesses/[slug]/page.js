import { notFound } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, BadgeCheck, Phone, Navigation, Globe, MapPin, Mail } from 'lucide-react';
import { getBusinessBySlug } from '@/api/server';
import PageShell from '@/components/PageShell';
import BusinessGallery from '@/components/BusinessGallery';
import CopyButton from '@/components/CopyButton';
import SaveButton from '@/components/SaveButton';
import BusinessAdminBar from '@/components/BusinessAdminBar';


export async function generateMetadata({ params }) {
  const { slug } = await params;
  const business = await getBusinessBySlug(slug);

  if (!business) return { title: 'Business not found' };

  const description =
    business.shortDescription || `${business.name} in ${business.suburb || 'Adelaide'}.`;

  return {
    title: business.name,
    description,
    alternates: { canonical: `/businesses/${business.slug}` },
    openGraph: {
      title: business.name,
      description,
      type: 'website',
      ...(business.images?.[0]?.imageUrl && { images: [business.images[0].imageUrl] }),
    },
  };
}

export default async function BusinessPage({ params }) {
  const { slug } = await params;
  const business = await getBusinessBySlug(slug);

  if (!business) notFound();

  const fullAddress = [business.addressLine, business.suburb].filter(Boolean).join(', ');
  const directionsUrl = fullAddress
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(fullAddress)}`
    : null;

  // Fields are omitted rather than emitted empty: a PostalAddress with a blank
  // streetAddress is worse than no address, since a search engine may surface it.
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'LocalBusiness',
    name: business.name,
    ...(business.shortDescription && { description: business.shortDescription }),
    ...(business.phone && { telephone: business.phone }),
    ...(business.email && { email: business.email }),
    ...(business.websiteUrl && { sameAs: [business.websiteUrl] }),
    ...(business.images?.length > 0 && {
      image: business.images.map((i) => i.imageUrl),
    }),
    ...((business.addressLine || business.suburb) && {
      address: {
        '@type': 'PostalAddress',
        addressCountry: 'AU',
        ...(business.addressLine && { streetAddress: business.addressLine }),
        ...(business.suburb && { addressLocality: business.suburb }),
        ...(business.state && { addressRegion: business.state }),
        ...(business.postcode && { postalCode: business.postcode }),
      },
    }),
    ...(business.latitude != null &&
      business.longitude != null && {
        geo: {
          '@type': 'GeoCoordinates',
          latitude: business.latitude,
          longitude: business.longitude,
        },
      }),
  };

  const actionClass =
    'flex flex-col items-center justify-center gap-1.5 py-3 rounded-xl border border-border-dark ' +
    'text-[13px] text-snow hover:bg-night transition-colors';

  return (
    <PageShell>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
      />

      <div className="flex items-center justify-between mb-5">
        <Link
          href="/directory"
          className="flex items-center gap-1.5 text-sm text-muted hover:text-snow transition-colors"
        >
          <ArrowLeft size={18} strokeWidth={1.75} />
          <span className="hidden sm:inline">Back</span>
        </Link>

        <div className="lg:hidden">
          <SaveButton resourceType="BUSINESS" resourceId={business.id} variant="icon" />
        </div>
      </div>

      <div className="flex flex-col lg:flex-row gap-6 lg:justify-center">
      <article className="w-full min-w-0 lg:max-w-[640px]">
          <BusinessGallery images={business.images ?? []} />

          <BusinessAdminBar business={business}>
            <div className="flex items-start gap-2 mb-1">
              <h1 className="text-2xl font-bold text-snow leading-tight">{business.name}</h1>
              {business.verified && (
                <BadgeCheck
                  size={20}
                  strokeWidth={2}
                  className="text-korea-blue shrink-0 mt-1"
                  aria-label="Verified business"
                />
              )}
            </div>

            {business.shortDescription && (
              <p className="text-sm text-muted mb-4">{business.shortDescription}</p>
            )}

            {business.categories?.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-5">
                {business.categories.map((category) => (
                  <span
                    key={category.id}
                    className="text-[11px] border border-border-dark px-2.5 py-0.5 rounded-full text-muted"
                  >
                    {category.name}
                  </span>
                ))}
              </div>
            )}

            {/* Primary actions, per the business detail wireframe. Each is a
                plain link, so they work without JavaScript. */}
            <div className="grid grid-cols-3 gap-2 mb-5">
              {business.phone ? (
                <a href={`tel:${business.phone}`} className={actionClass}>
                  <Phone size={18} strokeWidth={1.75} />
                  Call
                </a>
              ) : (
                <span className={`${actionClass} opacity-40 cursor-default`}>
                  <Phone size={18} strokeWidth={1.75} />
                  Call
                </span>
              )}

              {directionsUrl ? (
                <a href={directionsUrl} target="_blank" rel="noreferrer" className={actionClass}>
                  <Navigation size={18} strokeWidth={1.75} />
                  Directions
                </a>
              ) : (
                <span className={`${actionClass} opacity-40 cursor-default`}>
                  <Navigation size={18} strokeWidth={1.75} />
                  Directions
                </span>
              )}

              {business.websiteUrl ? (
                <a href={business.websiteUrl} target="_blank" rel="noreferrer" className={actionClass}>
                  <Globe size={18} strokeWidth={1.75} />
                  Website
                </a>
              ) : (
                <span className={`${actionClass} opacity-40 cursor-default`}>
                  <Globe size={18} strokeWidth={1.75} />
                  Website
                </span>
              )}
            </div>

            {business.description && (
              <p className="text-[15px] leading-relaxed text-snow whitespace-pre-line">
                {business.description}
              </p>
            )}
          </BusinessAdminBar>

          {/* Contact details stay outside the admin wrapper — they mirror fields
              in the form, and hiding them while editing removes the reference. */}
          <div className="lg:hidden mt-8 pt-5 border-t border-border-dark">
            ...
          </div>
        </article>

        <aside className="hidden lg:flex lg:flex-col w-72 shrink-0 gap-3">
          {(fullAddress || business.phone || business.email) && (
            <div className="rounded-xl border border-border-dark p-4 flex flex-col gap-3">
              <h2 className="text-sm font-semibold text-snow">Contact</h2>

              {fullAddress && (
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-start gap-2.5 text-[13px] text-snow min-w-0">
                    <MapPin size={15} strokeWidth={1.75} className="text-muted shrink-0 mt-0.5" />
                    {fullAddress}
                  </div>
                  <CopyButton value={fullAddress} label="Copy address" />
                </div>
              )}

              {business.phone && (
                <div className="flex items-start justify-between gap-2">
                  <a
                    href={`tel:${business.phone}`}
                    className="flex items-start gap-2.5 text-[13px] text-snow hover:text-white min-w-0"
                  >
                    <Phone size={15} strokeWidth={1.75} className="text-muted shrink-0 mt-0.5" />
                    {business.phone}
                  </a>
                  <CopyButton value={business.phone} label="Copy phone number" />
                </div>
              )}

              {business.email && (
                <div className="flex items-start justify-between gap-2">
                  <a
                    href={`mailto:${business.email}`}
                    className="flex items-start gap-2.5 text-[13px] text-korea-blue hover:underline break-all min-w-0"
                  >
                    <Mail size={15} strokeWidth={1.75} className="shrink-0 mt-0.5" />
                    {business.email}
                  </a>
                  <CopyButton value={business.email} label="Copy email address" />
                </div>
              )}
            </div>
          )}

          <SaveButton resourceType="BUSINESS" resourceId={business.id} />
        </aside>
      </div>
    </PageShell>
  );
}