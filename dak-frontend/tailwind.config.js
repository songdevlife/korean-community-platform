/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Light-theme tokens, retained for a possible future light mode
        cream: "#FDF8F3",
        ink: "#1A1A1A",

        // Accents — carried over from the logo artwork
        "adelaide-red": "#E63946",
        "korea-blue": "#2563EB",
        "soft-green": "#4CAF7D",
        "warm-orange": "#F0A868",

        // Dark theme. Slightly warm greys rather than neutral, so the
        // cream-toned logo sits on them without reading as cold.
        night: "#1C1C1A",        // page background
        surface: "#242422",      // cards, sidebar, raised areas
        "border-dark": "#3A3A37",// dividers, input borders
        snow: "#F2F2EF",         // primary text
        muted: "#A3A39D",        // secondary text
        faint: "#6F6F69",        // placeholders, disabled
      },

      keyframes: {
        // Pages fade up slightly on entry rather than snapping into place.
        // Small distance and short duration: enough to read as a transition,
        // not enough to delay someone who navigates quickly.
        pageEnter: {
          '0%':   { opacity: '0', transform: 'translateY(14px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },

      animation: {
        'page-enter': 'pageEnter 500ms ease-out',
      },
    },
  },
  plugins: [],
};