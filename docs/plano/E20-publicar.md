# E20 · Publicar

> **Fase 6 — Lançamento** · Dificuldade ●●○○○ · Depende de: E19 · Roadmap antigo: —

## Em poucas palavras

Pôr o mod disponível para outras pessoas: licença, créditos das bibliotecas, regras da Jamendo, página no Modrinth e versões.

## No fim vais ver

Uma página pública do mod com o jar para descarregar, e um processo repetível para lançar versões novas.

## O que se faz

- **Licença do mod (decisão D10)**: o template trouxe CC0; escolher conscientemente (MIT, Apache-2.0, LGPL…).
- **`THIRD_PARTY_NOTICES`** com as licenças de tudo o que vai dentro do jar (lavaplayer Apache-2.0 e as dependências da lista da E01).
- **Jamendo**:
  - termos de uso não comercial; confirmar se programas de recompensas de plataformas (ex. CurseForge) contam como uso comercial;
  - **decisão D2** fechada de vez: como funciona o client ID na versão pública.
- **Radio Browser**: `User-Agent` com o nome e a versão do mod (já feito na E06).
- **Página no Modrinth** (e/ou CurseForge): descrição, screenshots, aviso de que o modo partilhado precisa do mod no servidor e em todos os clientes, e que YouTube/Spotify não são suportados (de propósito).
- **Versões e changelog**; GitHub Actions a gerar o jar quando se cria uma tag (o `build.yml` do template já compila).
- **`fabric.mod.json`** final: contactos, página, issues.

## Conceitos novos

- **Licenças de software** e o que significa redistribuir bibliotecas.
- **Versionamento semântico** (1.0.0, 1.0.1, 1.1.0…).

## Pronto quando

- [ ] Licença escolhida e avisos de terceiros no jar.
- [ ] D2 resolvida e documentada no README.
- [ ] Página publicada e primeira versão descarregável.

## Para começar a sessão

> Vamos fazer a entrada E20 do plano: lê `docs/plano/E20-publicar.md`. Começamos pela licença (D10) e pelo client ID da Jamendo (D2).

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
