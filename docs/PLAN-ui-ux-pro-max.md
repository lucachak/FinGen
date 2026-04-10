# PLAN-ui-ux-pro-max

Objetivo: Elevar a UI/UX do **FinGen** ao nível "Pro Max" (Premium Wealth Management), refatorando o front-end legacy para **Tailwind CSS**, mantendo a stack core (Thymeleaf, Vanilla JS, HTMX) e aplicando princípios de Glassmorphism, tipografia elegante e responsividade perfeita.

## Bibliotecas e Stack Confirmadas
- **Framework CSS:** Tailwind CSS (via CDN configurado com tailwind.config no HEAD).
- **Tipografia:** `Noto Serif JP` (Headings H1-H3), `DM Sans` (UI e Body), `DM Mono` (Valores Financeiros, Tabelas, Gráficos).
- **Ícones:** Lucide Icons (via CDN `unpkg.com/lucide@latest` ou SVGs inline).
- **Animações & 3D:** GSAP (para micro-interações robustas) e Three.js (já previstos para cenários premium).
- **Tooling Web:** HTMX (páginas SPA-like).

## Paleta de Cores (Design System)
- **Primary / Brand:** Tons Esmeralda/Teal (`emerald-700` `#047857` a `teal-700` `#0F766E`).
- **Surface (Light Mode):** Off-White quente (`#F4F7F4` para o fundo da página, puro `#FFFFFF` para os cards).
- **Surface (Dark Mode):** Slate (`slate-900` `#0f172a`), com os cards em `slate-800` translúcido.
- **Glassmorphism:** Bordas e fundos em `rgba(255, 255, 255, 0.7)` com `backdrop-blur-md` (Light) e `rgba(15, 23, 42, 0.7)` com blur (Dark).

---

## Fases de Execução

### Phase 1: Design System (Master Layout)
- **Ação:** Injetar o CDN do Tailwind e o `tailwind.config` no `layout.html`.
- **Refatoração Inicial:** Substituir o `<style>` customizado global do Zen Serenity pelas utilitárias do Tailwind, mapeando as variáveis da paleta antiga para a nova `extend: { colors: {...} }`.

### Phase 2: Master Layout & Navigation
- **Ação:** Refatorar `.sidebar` e `.navbar` para o padrão utility-first.
- **Micro-interações:** Aplicar `transition-all duration-300 hover:bg-surface-muted`, `focus-visible:ring-2 focus-visible:ring-brand`.
- **Mobile:** Menu hambúrguer flutuante usando HTMX ou Vanilla JS toggle para offcanvas.

### Phase 3: Dashboards & Core Views
- **Ação:** Refatorar `home/index.html` e `wealth/setup.html` usando `grid grid-cols-1 md:grid-cols-3 gap-6`.
- **Tipografia nos Cards:** Valores financeiros renderizados com `font-mono tracking-tight text-brand font-semibold`.

### Phase 4: CRUDS e Formulários
- **Ação:** Mapear `.field-control` para utilitárias Tailwind (`w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-brand/50`).
- **Tabelas:** Classes `divide-y divide-slate-200` com visual minimalista.

### Phase 5: PWA & Performance
- **Ação:** Revisar as cores do `manifest.json` e otimizar chamadas CDN com `dns-prefetch`.

---

## Verification Plan

### Automated Tests
- N/A para estética UI. Lints (WCAG Contrast, validade do HTML via `ux_audit.py`) serão realizados após a refatoração.

### Manual Verification
- Inspecionar visualmente o Dashboard base com e sem a sidebar expandida.
- Validar se o Tailwind está sobrepondo de forma destrutiva o GSAP/ThreeJS ou se ambos coexistem harmoniosamente.
- O usuário avaliará a estética "Bank of America Private Bank / Zen Serenity" via Auto-Preview.
