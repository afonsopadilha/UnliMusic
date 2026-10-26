# E14 · Biblioteca de ficheiros locais

> **Fase 4 — Locais e definições** · Dificuldade ●●●○○ · Depende de: E03, E09 · Roadmap antigo: passo 6

## Em poucas palavras

Um separador **Locais** no ecrã: o mod lê uma pasta de músicas tua, mostra título e artista, e toca. Só para ti: ficheiros locais nunca são partilhados.

## No fim vais ver

A pasta de músicas do mod é criada sozinha. Um botão **Abrir pasta** (como nos resource packs) abre-a no explorador. A lista tem pesquisa, e há tocar, aleatório e repetir.

## O que se constrói

- **Pasta por defeito** (ex. dentro de `.minecraft/unlimusic/`) e caminho configurável na E16.
- **Sem seletor de pastas nativo "de graça"**: o Minecraft 26.3 não traz a biblioteca tinyfd (verificado no cache do Gradle). Solução: botão "Abrir pasta" + campo de texto para o caminho. Como extra, investigar os diálogos do SDL3 (`lwjgl-sdl` vem com o jogo).
- **Varrimento em segundo plano** das extensões que a lavaplayer toca (mp3, flac, wav, ogg, m4a…), com índice em cache para não reler tudo de cada vez, e deteção de ficheiros novos/apagados.
- **Tags**: título e artista (a lavaplayer dá alguns). Álbum e capa precisam de uma biblioteca de tags, ex. jaudiotagger (decisão D8: mais uma dependência no jar).
- **Fila pessoal**: tocar a pasta, aleatório, repetir um/todos, próxima/anterior.

## Conceitos novos

- **I/O de ficheiros** em segundo plano e caminhos multiplataforma (`Path`, não strings).
- **Índice em cache** e como saber se ficou desatualizado (data de modificação).

## Passos sugeridos

1. Criar a pasta e o botão "Abrir pasta".
2. Varrer e listar nomes de ficheiro.
3. Tags via lavaplayer e índice em cache.
4. Tocar a partir da lista; fila pessoal.
5. Decidir D8.

## Pronto quando

- [ ] 500 ficheiros indexam sem congelar o jogo.
- [ ] Nomes com acentos funcionam.
- [ ] Ficheiro apagado entretanto → erro tratado, não crash.
- [ ] Aleatório e repetir funcionam.

## Armadilhas

- Ler tags de centenas de ficheiros pode demorar: sempre fora da thread do jogo.
- Pastas de rede ou discos externos que desaparecem.

## APIs

- **Por confirmar:** como abrir uma pasta no explorador do sistema (`Util.getPlatform().openPath`?); informação de faixa que a lavaplayer devolve para ficheiros locais.

## Para começar a sessão

> Vamos fazer a entrada E14 do plano: lê `docs/plano/E14-ficheiros-locais.md`. Começamos pela pasta e pelo botão "Abrir pasta".

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
