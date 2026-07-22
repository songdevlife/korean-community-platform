/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        cream: "#FDF8F3",
        ink: "#1A1A1A",
        "adelaide-red": "#E63946",
        "korea-blue": "#2563EB",
        "soft-green": "#4CAF7D",
        "warm-orange": "#F0A868",
      },
    },
  },
  plugins: [],
};