# E17 · Som posicional tipo jukebox *(opcional)*

> **Fase 5 — Extras** · Dificuldade ●●●●○ · Depende de: E02, E11 · Roadmap antigo: —

## Em poucas palavras

Em vez de música "nos auscultadores", o som sai de um ponto do mundo: aproximas-te e ouves mais alto, afastas-te e deixas de ouvir, como uma jukebox.

## No fim vais ver

`/music aqui` (partilhado) põe a música a sair do bloco onde estás. No modo pessoal, uma opção faz o mesmo para ti.

## O que se constrói

- **Áudio mono**: o OpenAL só posiciona fontes mono. Converter estéreo → mono no `PcmAudioStream`, ou pedir à lavaplayer saída mono quando o modo posicional está ligado.
- **Sound instance com posição fixa**, atenuação linear e alcance configurável.
- **Partilhado**: o servidor guarda a posição; jogadores longe não ouvem e, idealmente, nem descarregam o stream (poupa rede).
- **Ideia para depois**: ligar a uma jukebox do jogo ou a um bloco "coluna" próprio (um bloco novo é muito mais trabalho: modelo, textura, registo).

## Conceitos novos

- **Áudio 3D**: atenuação, distância, mono vs estéreo.
- **Sound instances que mudam com o tempo** (tickable).

## Passos sugeridos

1. Tom de teste mono numa posição fixa → andar à volta e ouvir a diferença.
2. Conversão estéreo → mono do PCM da lavaplayer.
3. Opção no modo pessoal.
4. `/music aqui` no partilhado.

## Pronto quando

- [ ] Aproximar/afastar muda o volume e a direção do som.
- [ ] Longe do alcance não se ouve.
- [ ] Desligar a opção volta ao estéreo normal.

## Armadilhas

- Mudar de estéreo para mono a meio de uma faixa obriga a recriar o som.
- Dimensões diferentes (Nether/End): o som não deve atravessar.

## APIs

- **Por confirmar:** `SoundInstance.Attenuation`, sound instances tickable (`AbstractTickableSoundInstance`?), alcance do som.

## Para começar a sessão

> Vamos fazer a entrada E17 do plano: lê `docs/plano/E17-som-posicional.md`. Começamos pelo tom de teste mono numa posição fixa.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
