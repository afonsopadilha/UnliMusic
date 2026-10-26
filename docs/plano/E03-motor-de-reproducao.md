# E03 · Motor de reprodução: pausar, parar e fim da faixa

> **Fase 1 — Ouvir sozinho** · Dificuldade ●●●○○ · Depende de: E02 · Roadmap antigo: passo 3 (teclas)

## Em poucas palavras

Transformar o "teste que toca um ficheiro" num leitor a sério: um único sítio no cliente que sabe o que está a tocar, e teclas para pausar, retomar e parar. Também se define a interface comum `MusicSource`, para que ficheiros locais, rádios e Jamendo se comportem da mesma maneira.

## No fim vais ver

Uma tecla pausa e retoma no mesmo sítio; outra para a música; sair do mundo para a música; quando a faixa acaba, nada fica "pendurado".

## O que se constrói

- **`TrackInfo`** (record): título, artista, duração (ou "ao vivo"), tipo de fonte, crédito/licença, URL da capa. Todos os ecrãs e o HUD vão usar isto.
- **`MusicSource`** (interface, como está no `CLAUDE.md`): `play(faixa, posição)`, `pause()`, `resume()`, `stop()`. Primeira implementação: `LocalFileSource` (o código da E02, arrumado). Rádio e Jamendo vêm depois.
- **`MusicPlayer`** (cliente, um só): estado (`PARADO`, `A_CARREGAR`, `A_TOCAR`, `EM_PAUSA`, `ERRO`), faixa atual, posição atual e eventos para quem quiser ouvir (o HUD da E05).
- **Pausa (decisão D6)**: pausar a lavaplayer mantendo o som do Minecraft vivo, ou parar o som e recriá-lo ao retomar com seek. Testar as duas e explicar o porquê da escolha.
- **Limpeza**: ao sair do servidor/mundo e ao fechar o jogo, parar tudo e libertar threads e streams.
- **Regra das threads**: rede e descodificação em threads próprias; tudo o que mexe no Minecraft volta à thread do jogo.
- **Teclas** pausar/retomar e parar (com traduções).

## Conceitos novos

- **Máquina de estados**: só certas transições são válidas (não se pausa o que está parado).
- **Thread-safety**: `volatile`, filas concorrentes, e porque é que dois sítios a mexer no mesmo objeto ao mesmo tempo dá bugs raros.
- **Padrão observer**: o HUD "subscreve" mudanças em vez de perguntar a cada frame.

## Passos sugeridos

1. Criar `TrackInfo`, `MusicSource` e mover o código da E02 para `LocalFileSource`.
2. `MusicPlayer` com estados e logs de cada transição.
3. Parar + limpeza ao sair do mundo.
4. Pausar/retomar (decisão D6).
5. Teclas.

## Pronto quando

- [ ] Pausar/retomar/parar 20 vezes seguidas sem erros no log.
- [ ] Tocar outra faixa a meio nunca deixa duas a tocar.
- [ ] Sair e voltar a entrar no mundo deixa tudo limpo.
- [ ] Nenhuma thread da lavaplayer fica viva depois de parar (ver no debugger).

## Armadilhas

- Em singleplayer, carregar em ESC pausa o jogo e o Minecraft pausa os sons. Ver o que isso faz ao nosso stream.
- O comando `/stopsound` e outras coisas do jogo podem parar a nossa sound instance sem nós sabermos → o `MusicPlayer` tem de detetar.
- Pausar uma rádio não faz sentido "no sítio": ao retomar, volta ao ao vivo (tratar na E04).

## APIs

- **Por confirmar:** `ClientPlayConnectionEvents.DISCONNECT`, `ClientLifecycleEvents.CLIENT_STOPPING`, como voltar à thread do jogo (`Minecraft.getInstance().execute(...)`?), como saber se uma sound instance ainda está ativa (`SoundManager.isActive`?).

## Para começar a sessão

> Vamos fazer a entrada E03 do plano: lê `docs/plano/E03-motor-de-reproducao.md`. Explica-me a máquina de estados antes de a escrever, e testa as duas opções de pausa (D6).

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
