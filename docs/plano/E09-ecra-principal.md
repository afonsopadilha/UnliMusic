# E09 · Ecrã principal: rádios e Jamendo

> **Fase 2 — Encontrar música** · Dificuldade ●●●●○ (provavelmente 2 sessões) · Depende de: E06, E08 · Roadmap antigo: passo 4

## Em poucas palavras

Uma janela do mod, aberta com uma tecla, com separadores **Rádios** e **Jamendo** (mais tarde **Locais**, E14, e **Fila**, E13). Escreves, pesquisas, vês a lista e carregas em "Tocar".

## No fim vais ver

O ecrã abre; pesquisa de rádios por país (PT por defeito) e por nome; pesquisa Jamendo; botão "Tocar"; indicador "a carregar…" e mensagens de erro.

## O que se constrói

- **`MusicScreen extends Screen`** com separadores.
- **Widgets**: caixa de texto, lista com scroll, botões.
- **Pesquisas assíncronas**: o ecrã nunca fica à espera. Mostra "a carregar…" e atualiza quando chega a resposta; respostas de pesquisas antigas são ignoradas.
- **Seletor de país** (a Radio Browser tem lista de países).
- **Mini-leitor** no fundo do ecrã: o que está a tocar + pausar/parar.
- **Favoritos de rádios** guardados localmente (opcional nesta entrada).
- **Divisão sugerida**: sessão 1 = base do ecrã + Rádios; sessão 2 = Jamendo + polimento.

## Conceitos novos

- **Ciclo de vida de um `Screen`**: `init`, render, tick, fechar.
- **Widgets** e layout que se adapta ao tamanho da janela e à GUI scale.
- **Foco do teclado** (escrever na caixa vs atalhos do ecrã).

## Passos sugeridos

1. Ecrã vazio que abre com uma tecla e fecha com ESC.
2. Separador Rádios: caixa de texto + lista + botão.
3. Ligar à pesquisa da E06 (assíncrona).
4. Separador Jamendo ligado à E08.
5. Mini-leitor e polimento.

## Pronto quando

- [ ] Tudo navegável com rato e teclado; ESC fecha.
- [ ] Redimensionar a janela não estraga o layout.
- [ ] 100 resultados com scroll sem lag.
- [ ] Erros de rede aparecem no ecrã, não só no log.

## Armadilhas

- A API de ecrãs e widgets mudou bastante entre versões: confirmar tudo nas fontes da 26.3.
- Texto que não cabe (nomes compridos) → cortar com "…".
- Abrir o ecrã não deve pausar a música (nem o jogo, em multijogador).

## APIs

- **Por confirmar:** `Screen`, `EditBox`, `Button`, listas com scroll (`ObjectSelectionList`?), como desenhar em ecrãs na 26.3 (o mesmo modelo de extração do HUD?).

## Para começar a sessão

> Vamos fazer a entrada E09 do plano: lê `docs/plano/E09-ecra-principal.md`. Nesta sessão fazemos só a base do ecrã e o separador Rádios.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
