# FinGen: sistema de design

Fonte da verdade: `frontend/src/app.css` (tokens e componentes). Este arquivo explica as decisões.
Quando uma tela diverge daqui, a tela está errada.

## Ideia

Livro-caixa. Superfícies neutras com leve tom de verde, tinta escura, **um** acento (teal) e vermelho/âmbar
só para atraso e vencimento. Bordas finas no lugar de sombras. Números tabulares para as colunas de
dinheiro alinharem. A faixa do mês (dashboard) é o único elemento "memorável"; o resto é quieto.

## Tokens

| Papel | Claro | Escuro | Uso |
|---|---|---|---|
| `--bg` | `#F3F5F4` | `#0E1513` | fundo da página |
| `--surface` | `#FFFFFF` | `#141C19` | cartões, barra, campos |
| `--surface-2` / `--surface-h` | `#F7F8F7` / `#EEF1EF` | `#1A2420` / `#212D28` | blocos secundários, hover |
| `--border` / `--border-2` | `#DCE1DE` / `#C4CCC8` | `#26332E` / `#384942` | divisórias / bordas de campo |
| `--t1` `--t2` `--t3` | `#14201C` `#43524C` `#5B6A63` | `#E8EEEB` `#A5B2AC` `#8A9892` | tinta, apoio, terciário |
| `--brand` | `#0F766E` | `#2DB3A4` | ação primária, foco, progresso |
| `--green` `--amber` `--red` | `#13703A` `#9A4505` `#B91C1C` | `#4ADE80` `#FBBF24` `#F87171` | entrada, vencimento, atraso |

Contraste verificado (WCAG AA, 4,5:1) para `--t3` e semânticos sobre `--bg`, `--surface`, `--surface-2`.
No escuro, os tokens derivam do mesmo matiz do claro (não de outra família).

## Tipografia

- **DM Sans** (corpo, interface, números): variável, hospedada em `static/fonts/` (OFL-1.1).
- **Newsreader** (somente títulos de página e de seção): variável, idem.
- Sem caixa alta. Rótulos em frase, 13 px. Corpo 14–16 px. Mínimo 12 px.
- `font-variant-numeric: tabular-nums` no `body`.

## Forma

Raios: 6 px (campo, botão) · 10–14 px (cartão) · pílula só em avatar. Sem vidro, sem gradiente decorativo,
sem sombra em cartão (sombra só em camada flutuante: menu, diálogo).

## Componentes (`@layer components`)

`btn-primary` `btn-secondary` `btn-ghost` `btn-danger` · `card` · `field-control` `field-label` `field-hint`
`field-error` · `badge` `badge-{green,red,amber,blue,gray}` · `ledger` `ledger-row` · `meter` `meter-fill` ·
`notice` `notice-{ok,err}` · `empty-state` · `icon-btn` · `page-header` `page-title` `page-subtitle` ·
`month-hero` `month-track` `month-dot` `month-today` · `auth-*`.

## Regras de texto (pt-BR)

- Tratar por "você". Verbos nos botões: "Adicionar conta", "Salvar meta", "Marcar como paga".
- O mesmo nome para a mesma coisa em menu, título, botão e mensagem (ex.: **Contas recorrentes**).
- Erro: o que aconteceu + como corrigir. Nunca expor exceção, servidor ou "cron".
- Vazio: o que é, por que está vazio, como começar.
- Nada de números, depoimentos ou garantias sem lastro no código. Exemplos ilustrativos levam o rótulo
  "Exemplo ilustrativo".

## Acessibilidade (piso)

Foco visível (`:focus-visible`), alvos de toque de 44 px no mobile, `prefers-reduced-motion` respeitado,
um `<main>` por página, "Pular para o conteúdo", `aria-label` em controles só com ícone, `role="progressbar"`
com `aria-valuenow`. Verificado com axe-core (sem violações serious/critical/moderate nas telas principais).
