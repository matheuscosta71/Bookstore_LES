/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: { DEFAULT: '#0f172a', muted: '#475569', subtle: '#94a3b8' },
        paper: { DEFAULT: '#fafaf9', accent: '#f5f5f4' },
        brand: {
          DEFAULT: '#1e3a5f',
          light: '#2d4a6f',
          soft: '#e8eef5',
        },
        accent: { DEFAULT: '#c45c26', hover: '#a84d1f' },
      },
      fontFamily: {
        sans: ['DM Sans', 'system-ui', 'sans-serif'],
        display: ['Fraunces', 'Georgia', 'serif'],
      },
      boxShadow: {
        card: '0 1px 3px rgba(15, 23, 42, 0.06), 0 4px 12px rgba(15, 23, 42, 0.04)',
        lift: '0 8px 24px rgba(15, 23, 42, 0.08)',
      },
    },
  },
  plugins: [],
};
