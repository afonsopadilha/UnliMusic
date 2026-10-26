# E08 · Rede: pacotes entre cliente e servidor

> **Fase 2 — Encontrar música** · Dificuldade ●●●○○ · Depende de: E04, E07 · Roadmap antigo: passo 5

## Em poucas palavras

Ensinar o cliente e o servidor a falarem um com o outro com mensagens próprias ("pacotes"). É a base da pesquisa Jamendo no ecrã e de todo o modo partilhado.

## No fim vais ver

O cliente pede uma pesquisa Jamendo ao servidor, recebe os resultados e consegue tocar uma faixa só para si (modo pessoal). Testado em singleplayer e num servidor dedicado local.

## O que se constrói

- **Payloads** (records) com o seu `StreamCodec`, registados em `src/main` com `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()`.
- **Handlers** com `ServerPlayNetworking` (no servidor) e `ClientPlayNetworking` (no cliente).
- **Primeiros pacotes**:
  - C2S `JamendoSearch(pedidoId, termo)` → S2C `JamendoResults(pedidoId, faixas)`
  - versão do protocolo, para cliente e servidor saberem se se entendem
- **Servidor sem o mod**: o cliente deteta que o servidor não fala o nosso protocolo e desativa o que precisa dele (decisão D9: Jamendo precisa do mod no servidor).
- **`JamendoSource implements MusicSource`** (cliente): toca o URL de áudio por HTTP, com seek (vai ser preciso na E11).

## Conceitos novos

- **C2S vs S2C**, serialização e o que é um codec.
- **Ids de pedido**: casar cada resposta com a pergunta certa (e ignorar respostas antigas).
- **Thread de rede**: os handlers não correm na thread do jogo.
- **Nunca confiar no cliente**: introdução ao que a E12 aprofunda.

## Passos sugeridos

1. Um pacote "ping" C2S → S2C com log nos dois lados.
2. Versão do protocolo e deteção de servidor sem o mod.
3. Pesquisa Jamendo por pacote.
4. `JamendoSource` e tocar um resultado.
5. Testar com `runServer` + `runClient`.

## Pronto quando

- [ ] Pesquisa via pacote em singleplayer e em servidor dedicado local.
- [ ] Servidor sem o mod → o cliente não rebenta e mostra um aviso.
- [ ] Strings e listas nos pacotes têm tamanho máximo.
- [ ] Uma faixa Jamendo toca no modo pessoal.

## Armadilhas

- Registar os payloads em `src/main` (os dois lados precisam de os conhecer), mas os handlers de cliente em `src/client`.
- Um cliente sem o mod a entrar num servidor com o mod: confirmar que o servidor não lhe envia pacotes que ele não conhece.

## APIs

- **Confirmado (26.3):** `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()`, `ServerPlayNetworking`, `ClientPlayNetworking`, `PacketSender`, `PlayerLookup`.
- **Por confirmar:** interface do payload (`CustomPacketPayload`?) e `StreamCodec` na 26.3; como saber se o servidor aceita um canal (`ClientPlayNetworking.canSend`?).

## Para começar a sessão

> Vamos fazer a entrada E08 do plano: lê `docs/plano/E08-rede-pacotes.md`. Começa por um pacote "ping" e explica-me cada peça (payload, codec, registo, handler).

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
