# E05 · HUD "a tocar agora"

> **Fase 1 — Ouvir sozinho** · Dificuldade ●●●○○ · Depende de: E03 (a E04 ajuda a testar) · Roadmap antigo: passo 3 (HUD)

## Em poucas palavras

Uma caixinha no canto do ecrã que diz o que está a tocar: título, artista, uma barra de progresso (ou "● AO VIVO" nas rádios) e, na Jamendo, o crédito obrigatório (artista + licença Creative Commons).

## No fim vais ver

Título e artista no canto, barra a avançar, ícone de pausa quando está pausado. Títulos compridos deslizam em vez de serem cortados.

## O que se constrói

- **Elemento de HUD** registado com `HudElementRegistry`.
- **Atenção ao modelo de desenho novo**: na 26.x o HUD implementa `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`. Tutoriais com `DrawContext` ou `GuiGraphics.drawString` são de versões antigas e não servem.
- **Lê o estado do `MusicPlayer`** (E03). Nada de trabalho pesado aqui: isto corre a cada frame.
- **Crédito/licença**: o espaço já fica preparado, mesmo antes da Jamendo existir (E07).
- **Esconder**: com F1 e, mais tarde, por opção nas definições (E16). Posição configurável fica para a E16.
- Todo o texto via ficheiros de língua.

## Conceitos novos

- **Render loop**: desenhar 60+ vezes por segundo; qualquer coisa lenta aqui = lag.
- **Extração de render state**: o Minecraft separa "recolher o que desenhar" de "desenhar".
- **GUI scale**: as coordenadas do HUD são escaladas; tem de ficar bem em todas as escalas.

## Passos sugeridos

1. Elemento de HUD que escreve um texto fixo.
2. Ligar ao `MusicPlayer`: título e artista reais.
3. Barra de progresso (ficheiros) e "AO VIVO" (rádio).
4. Fundo semitransparente, ícone de pausa, texto a deslizar.
5. Esconder com F1.

## Pronto quando

- [ ] HUD correto para ficheiro local e para rádio.
- [ ] Não pisca nem treme.
- [ ] Não tapa a hotbar, a vida nem o chat nas GUI scales 1 a 4.
- [ ] F1 esconde.

## Armadilhas

- A API de desenho mudou muito nas últimas versões: confirmar tudo nas fontes da 26.3.
- Ordem dos elementos: decidir antes/depois de quê desenhar (`VanillaHudElements`).
- Fontes e caracteres especiais (acentos, japonês, emojis nos nomes das rádios).

## APIs

- **Confirmado (26.3):** `HudElementRegistry` (`addLast`, `attachElementAfter`…), `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)`, `VanillaHudElements`.
- **Por confirmar:** métodos de `GuiGraphicsExtractor` para texto, retângulos e texturas; como saber se o HUD está escondido (F1).

## Para começar a sessão

> Vamos fazer a entrada E05 do plano: lê `docs/plano/E05-hud-a-tocar-agora.md`. Antes de desenhar, mostra-me nas fontes da 26.3 como funciona o `GuiGraphicsExtractor`.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
