# frontend

CSS do FinGen (Tailwind CSS 3). O resultado é `src/main/resources/static/css/app.css` e **é versionado**,
então o build Maven (e o deploy) não precisam de Node.

```bash
cd frontend
npm install        # uma vez
npm run build      # gera ../src/main/resources/static/css/app.css (minificado)
npm run watch      # recompila ao salvar templates ou src/app.css
```

- Tokens e componentes: `src/app.css`. Config do Tailwind: `tailwind.config.js` (lê os templates e os `.java`).
- Depois de mudar classes em templates, rode `npm run build` e commite o `app.css`.
- Fontes (DM Sans, Newsreader) estão em `src/main/resources/static/fonts/` (OFL-1.1). Para atualizar:
  `npm i -D @fontsource-variable/dm-sans @fontsource-variable/newsreader` e copie os `*-latin-wght-normal.woff2`.
- Decisões de design e regras de texto: `../design-system/fingen/MASTER.md`.
