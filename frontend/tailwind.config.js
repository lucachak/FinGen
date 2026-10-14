/** @type {import('tailwindcss').Config} */
const token = (name) => `rgb(var(--${name}-rgb) / <alpha-value>)`;

module.exports = {
  darkMode: 'class',
  content: [
    '../src/main/resources/templates/**/*.html',
    '../src/main/resources/static/**/*.js',
    // classes montadas no Java (cores de categoria, status etc.)
    '../src/main/java/**/*.java',
  ],
  theme: {
    // Hierarquia de raios: campos e botões 6–8, cartões 10–14, pílulas só para badges/avatares.
    borderRadius: {
      none: '0',
      sm: '4px',
      DEFAULT: '6px',
      md: '6px',
      lg: '8px',
      xl: '10px',
      '2xl': '14px',
      '3xl': '16px',
      full: '9999px',
    },
    boxShadow: {
      none: 'none',
      sm: 'var(--sh-sm)',
      DEFAULT: 'var(--sh-sm)',
      md: 'var(--sh-md)',
      lg: 'var(--sh-lg)',
      xl: 'var(--sh-lg)',
      '2xl': 'var(--sh-lg)',
      inner: 'inset 0 1px 2px rgb(0 0 0 / 0.06)',
    },
    extend: {
      colors: {
        brand: {
          DEFAULT: token('brand'),
          h: token('brand-h'),
          soft: 'var(--brand-soft)',
        },
        zen: {
          bg: token('bg'),
          surface: token('surface'),
          surface2: token('surface-2'),
          surfaceH: token('surface-h'),
          border: token('border'),
          border2: token('border-2'),
          t1: token('t1'),
          t2: token('t2'),
          t3: token('t3'),
          inv: token('t-inv'),
        },
      },
      fontFamily: {
        sans: ['"DM Sans"', 'system-ui', 'sans-serif'],
        serif: ['Newsreader', 'Georgia', 'serif'],
        mono: ['"DM Mono"', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [],
};
