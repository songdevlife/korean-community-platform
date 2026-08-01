import { AuthProvider } from '@/context/AuthContext';
import Layout from '@/components/Layout';
import './globals.css';

/**
 * Root layout. Wraps every route in the app.
 *
 * This file is a server component and must stay one. it renders the html and
 * body elements, which only the server can emit. The interactive parts sit
 * inside it: AuthProvider and Layout both carry 'use client' and take over in
 * the browser.
 *
 * The nav lives here rather than in each page because it is identical on all of
 * them. In the previous build every page imported Layout and wrapped itself;
 * that duplication disappears with file-based routing.
 */
export const metadata = {
  // Every page sets alternates.canonical as a path — '/guides', '/legal' and so
  // on. Without a base to resolve them against, Next emits those paths as-is,
  // and a relative canonical tells a crawler nothing it did not already know.
  // The value is the apex domain because www redirects to it, so this is the
  // address that should appear in results.
  metadataBase: new URL('https://discoveradelaidekorea.au'),
  // Korean, because the readers are. An English title on a Korean-language site
  // matches nothing a Korean speaker types, and title is the heaviest ranking
  // signal a page has. The English name stays after the divider so that anyone
  // searching for the brand by its full name still lands here.
  title: {
    default: '애들레이드를 더 쉽게, DAK | Discover Adelaide Korea',
    template: '%s | DAK',
  },
  description:
    '애들레이드 한인을 위한 지역 정보 플랫폼. 비즈니스, 생활 가이드, 최신 호주 소식을 제공합니다.',
  // KakaoTalk reads og:description and ignores the standard meta description,
  // so without this it prints its own English fallback under the title of every
  // shared link — which for this audience is the most common way anyone sees
  // DAK first. Deliberately no images key: opengraph-image.png is picked up by
  // file convention, and naming a path here would replace that, losing the
  // content hash that makes a replaced image re-scrape rather than serve stale.
  openGraph: {
    title: '애들레이드를 더 쉽게, DAK | Discover Adelaide Korea',
    description:
      '애들레이드 한인을 위한 지역 정보 플랫폼. 비즈니스, 생활 가이드, 최신 호주 소식을 제공합니다.',
    siteName: 'Discover Adelaide Korea',
    locale: 'ko_KR',
    type: 'website',
  },
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <head>
        {/* Opening the connection while the HTML is still parsing, rather than
            waiting until the CSS import is reached. Korean text renders in a
            fallback face until the subset arrives, so the swap is visible. */}
        <link rel="preconnect" href="https://cdn.jsdelivr.net" crossOrigin="anonymous" />
      </head>
      <body>
        <AuthProvider>
          <Layout>{children}</Layout>
        </AuthProvider>
      </body>
    </html>
  );
}