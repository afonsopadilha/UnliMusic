# E15 · Capas e imagens *(opcional)*

> **Fase 4 — Locais e definições** · Dificuldade ●●●○○ · Depende de: E05, E09 · Roadmap antigo: —

## Em poucas palavras

Mostrar a capa do álbum (Jamendo), o logótipo da rádio (Radio Browser) e a capa embutida nos ficheiros locais, no HUD e nas listas do ecrã.

## No fim vais ver

Uma pequena imagem quadrada ao lado do título no HUD e em cada linha dos resultados.

## O que se constrói

- **Download assíncrono** com limite de tamanho e de tempo.
- **Conversão para textura** do Minecraft (imagem → textura dinâmica registada), e libertar a textura quando já não é precisa.
- **Formatos**: PNG funciona; JPEG e outros podem precisar de conversão (confirmar). Favicons de rádios em ICO/SVG → ignorar e mostrar um ícone por defeito.
- **Cache** em memória (e talvez em disco).
- Capas dos ficheiros locais: depende da decisão D8 (biblioteca de tags).

## Conceitos novos

- **Texturas** no Minecraft: carregar, registar, desenhar, libertar.
- **Fugas de memória da placa gráfica**: uma textura nunca libertada fica lá para sempre.

## Passos sugeridos

1. Uma imagem fixa (dos recursos do mod) no HUD.
2. Download de uma capa Jamendo → textura → HUD.
3. Cache e libertação.
4. Listas do ecrã.
5. Logótipos das rádios e capas locais.

## Pronto quando

- [ ] Capas aparecem no HUD e nas listas.
- [ ] Rolar 200 resultados não faz disparar a memória.
- [ ] Imagens partidas mostram o ícone por defeito.

## Armadilhas

- Criar texturas fora da thread de render pode rebentar.
- Imagens enormes (alguns favicons têm megabytes).

## APIs

- **Por confirmar:** `NativeImage`, `DynamicTexture`, `TextureManager` na 26.3 e que formatos o `NativeImage` lê.

## Para começar a sessão

> Vamos fazer a entrada E15 do plano: lê `docs/plano/E15-capas-imagens.md`. Começamos por uma imagem fixa no HUD.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
