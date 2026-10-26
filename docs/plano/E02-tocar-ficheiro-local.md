# E02 · Tocar um ficheiro local fixo (o pipeline)

> **Fase 1 — Ouvir sozinho** · Dificuldade ●●●●○ · Depende de: E01 · Roadmap antigo: passo 1

## Em poucas palavras

A peça mais importante do mod. Carregas numa tecla e ouves um MP3 do teu disco dentro do jogo, com o volume a obedecer ao slider "Jukebox/Blocos de notas". Se isto funciona, tudo o resto é "trocar a origem" do som.

```
ficheiro.mp3 ─► lavaplayer ─► PCM 16 bits LE, 48 kHz, estéreo ─► PcmAudioStream ─► MusicSoundInstance ─► motor de som do Minecraft
               (descodifica)                                      (nosso, AudioStream)  (nossa, FabricSoundInstance)   (OpenAL)
```

## No fim vais ver

Carregas na tecla de teste e a música toca do princípio ao fim; mexer no slider Jukebox muda o volume.

## O que se constrói

- **`assets/unlimusic/sounds.json`** com uma entrada (ex. `stream`) que aponta para `fabric-sound-api-v1:empty` com `"stream": true`. É o "som de mentira" que a Fabric API recomenda para sons com áudio próprio.
- **`LavaplayerEngine`** (cliente): `AudioPlayerManager` só com o source manager local (o HTTP entra na E04); formato de saída PCM 16 bits little-endian; carregamento assíncrono da faixa.
- **`PcmAudioStream implements AudioStream`**:
  - `getFormat()` → `javax.sound.sampled.AudioFormat(48000, 16, 2, true, false)` (com sinal, little-endian)
  - `read(int)` → devolve um `ByteBuffer` com o próximo bocado de PCM
  - `close()` → liberta tudo
- **`MusicSoundInstance extends AbstractSoundInstance`**, com override de `getAudioStream(...)` a devolver o `PcmAudioStream`. Categoria `SoundSource.RECORDS`, relativa ao jogador e sem atenuação (ouve-se igual em todo o lado).
- **Tecla temporária** "tocar teste" com um caminho fixo (num ficheiro de dev fora do Git, não no código).

## Conceitos novos

- **PCM**, sample rate, canais, **little vs big-endian**.
- O motor de som corre na sua própria thread e **puxa** dados chamando `read()`. Não somos nós que empurramos.
- **Stream vs som carregado**: os sons normais vão inteiros para memória; uma música longa tem de ir aos bocados.
- **`SoundSource`**: categoria de volume. `RECORDS` = slider Jukebox.

## Passos sugeridos

1. `sounds.json` + sound instance que toca o `empty` sem erro de "unknown sound" no log.
2. **`PcmAudioStream` que gera um tom** (onda seno, ex. 440 Hz). Prova o `AudioStream` **sem** lavaplayer: se o tom toca, o lado Minecraft está certo.
3. Ligar a lavaplayer: ficheiro → PCM → stream.
4. Confirmar que o slider RECORDS controla o volume.

## Pronto quando

- [ ] O tom de teste toca.
- [ ] Um MP3 local toca do princípio ao fim sem cortes.
- [ ] O slider Jukebox muda o volume.
- [ ] O jogo não congela ao carregar na tecla.

## Armadilhas

- `read()` é chamado pela thread de som. Se bloquear muito tempo há cortes; se devolver um buffer vazio, o Minecraft acha que a música acabou.
- **O OpenAL só posiciona som mono.** Estéreo toca sempre "na cabeça" do jogador: ótimo para música de fundo. O modo jukebox (E17) vai precisar de mono.
- Ficheiros a 44,1 kHz têm de ser convertidos para 48 kHz (a lavaplayer faz, possivelmente com natives; ver E01).
- Caminhos com acentos no Windows.

## APIs

- **Confirmado (26.3):** `FabricSoundInstance.getAudioStream(SoundBufferLibrary, Identifier, boolean)`, `FabricSoundInstance.EMPTY_SOUND`, `AudioStream` (`getFormat()`, `read(int)`, `close()`), `AbstractSoundInstance`, `SoundSource.RECORDS`.
- **Por confirmar:** como tocar a instância (`SoundManager.play`?); construtor de `AbstractSoundInstance`; na lavaplayer 2.2.x, se é melhor `AudioPlayerInputStream` ou ler frames com `player.provide()`; classe do formato PCM (ex. `Pcm16AudioDataFormat`).

## Para começar a sessão

> Vamos fazer a entrada E02 do plano: lê `docs/plano/E02-tocar-ficheiro-local.md`. Quero mesmo começar pelo tom de teste antes de ligar a lavaplayer, e que me expliques cada peça do caminho do som.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
