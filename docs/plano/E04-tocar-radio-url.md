# E04 · Tocar uma rádio a partir de um URL fixo

> **Fase 1 — Ouvir sozinho** · Dificuldade ●●○○○ · Depende de: E03 · Roadmap antigo: passo 2

## Em poucas palavras

O mesmo que a E02, mas a origem é uma rádio online: um stream HTTP que nunca acaba. Prova que a parte de rede funciona e que lidamos bem com "não há duração nem seek".

## No fim vais ver

A tecla de teste toca uma rádio portuguesa a partir de um URL fixo (tirado à mão da Radio Browser). O HUD ainda não existe, mas o log mostra o nome da rádio.

## O que se constrói

- **Registar o `HttpAudioSourceManager`** na lavaplayer. Nunca `registerRemoteSources`.
- **`RadioSource implements MusicSource`**: sem seek, sem duração.
- **Reconexão**: se o stream cair, tentar de novo com esperas crescentes (1 s, 2 s, 4 s…) e desistir ao fim de N tentativas, com mensagem ao jogador.
- **Metadados ICY** ("StreamTitle", a música que está a dar na rádio): investigar se a lavaplayer os expõe. Se não, fica como extra.
- **Tipos de URL**: começar por MP3 e AAC diretos; depois playlists `.pls`/`.m3u`; HLS (`.m3u8`) só se a lavaplayer o suportar bem.
- **Timeouts** de ligação e leitura, e `User-Agent` próprio também nos pedidos de stream.
- **Pausa numa rádio** = ao retomar, volta ao ao vivo (não ao sítio onde parou).

## Conceitos novos

- **Streaming HTTP** e porque é que há sempre uns segundos de atraso (buffer).
- **ICY / Shoutcast**: o "protocolo" das rádios online, com metadados misturados no áudio.
- **Backoff exponencial**: esperar cada vez mais entre tentativas, para não martelar um servidor em baixo.

## Passos sugeridos

1. Registar o source manager HTTP e tocar um URL MP3 direto.
2. `RadioSource` integrada no `MusicPlayer` da E03.
3. Desligar a internet a meio → reconexão e mensagens.
4. Testar uma rádio AAC e uma playlist `.pls`.
5. Investigar os metadados ICY.

## Pronto quando

- [ ] Uma rádio toca 10 minutos ou mais sem cortes.
- [ ] Sem internet: mensagem de erro, sem crash, e volta a tentar.
- [ ] Parar/pausar/retomar funcionam como descrito.
- [ ] O jogo nunca congela enquanto a rádio liga.

## Armadilhas

- URLs que redirecionam; certificados HTTPS; rádios que demoram muito a começar.
- **HE-AAC (AAC+)**, comum em rádios: confirmar se a lavaplayer o descodifica.
- Rádios em 44,1 kHz e mono: o formato de saída é sempre o nosso, a lavaplayer converte.

## APIs

- **Por confirmar:** configuração de timeouts e `User-Agent` no `HttpAudioSourceManager` da 2.2.x; eventos da lavaplayer para erro/fim (`TrackExceptionEvent`, `TrackEndEvent`?); suporte a metadados ICY.

## Para começar a sessão

> Vamos fazer a entrada E04 do plano: lê `docs/plano/E04-tocar-radio-url.md`. Começamos com um URL MP3 direto de uma rádio portuguesa.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
