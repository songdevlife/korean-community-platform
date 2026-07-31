import { AuthProvider } from '@/context/AuthContext';
import Layout from '@/components/Layout';
import './globals.css';

/**
 * Root layout. Wraps every route in the app.
 *
 * This file is a server component and must stay one — it renders the html and
 * body elements, which only the server can emit. The interactive parts sit
 * inside it: AuthProvider and Layout both carry 'use client' and take over in
 * the browser.
 *
 * The nav lives here rather than in each page because it is identical on all of
 * them. In the previous build every page imported Layout and wrapped itself;
 * that duplication disappears with file-based routing.
 */
export const metadata = {
  // Per-page metadata overrides this; the template supplies the suffix so a
  // page only has to name itself.
  title: {
    default: 'DAK — Discover Adelaide Korea',
    template: '%s | DAK',
  },
  description:
    'Korean-language local information for Adelaide: businesses, guides and updates for Korean speakers in South Australia.',
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