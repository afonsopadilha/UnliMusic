# E10 · Modo partilhado: estado no servidor e comandos `/music`

> **Fase 3 — Ouvir juntos** · Dificuldade ●●●●○ · Depende de: E08 · Roadmap antigo: passo 5

## Em poucas palavras

O servidor passa a ser "o DJ": guarda a fila, o que está a tocar e desde quando, e avisa todos os jogadores. Comandos `/music` para controlar. Só Jamendo e rádios (os ficheiros locais nunca são partilhados).

## No fim vais ver

`/music radio <nome>` e todos os jogadores ouvem a rádio. `/music jamendo <termo>`, `/music skip`, `/music stop`, `/music pause`, `/music fila`. A fila avança sozinha quando uma faixa acaba.

## O que se constrói

- **`SharedMusicState`** (servidor): faixa atual, instante de início (relógio do servidor), pausa (e a posição em que pausou), fila.
- **Avanço automático**: o servidor sabe a duração das faixas Jamendo e passa à seguinte quando a atual acaba. Usar o tick do servidor, ou um agendador que volta sempre à thread do servidor.
- **Pacotes S2C**: `SharedPlay(fonte, faixa, posiçãoMs, horaServidor)`, `SharedPause`, `SharedStop`, `QueueUpdate`.
- **Quem entra a meio** recebe logo o estado atual.
- **Comandos** com Brigadier, com sugestões (autocomplete) de resultados.
- **Âmbito (decisão D4)**: recomendação = servidor inteiro primeiro.
- Guardar a fila entre reinícios do servidor: opcional.

## Conceitos novos

- **Fonte da verdade**: o servidor decide, os clientes obedecem.
- **Relógio do servidor**: porque não usar o relógio de cada cliente.
- **Broadcast**: enviar o mesmo pacote a todos.

## Passos sugeridos

1. `SharedMusicState` + comando `/music radio` → pacote para todos → todos tocam.
2. `/music stop` e `/music pause`.
3. Jamendo e fila, com avanço automático.
4. Enviar o estado a quem entra.
5. Sugestões nos comandos.

## Pronto quando

- [ ] Com 2 clientes ligados ao `runServer`, os dois ouvem a mesma coisa.
- [ ] skip/stop/pause chegam a todos.
- [ ] A fila avança sozinha.
- [ ] Quem entra a meio começa a ouvir (a sincronização fina é a E11).

## Armadilhas

- Agendadores que correm noutra thread e mexem no estado ao mesmo tempo que um comando.
- Rádio ao vivo não tem fim: a fila só avança com skip.
- O que acontece à música partilhada quando o último jogador sai? (Parar, para não gastar quota.)

## APIs

- **Confirmado (26.3):** `CommandRegistrationCallback`, `PlayerLookup`, `ServerPlayNetworking`.
- **Por confirmar:** `ServerPlayConnectionEvents.JOIN`, `ServerTickEvents`, `SavedData` (se se guardar a fila), sugestões no Brigadier.

## Para começar a sessão

> Vamos fazer a entrada E10 do plano: lê `docs/plano/E10-partilhado-servidor.md`. Primeiro decidimos D4 e depois começamos por `/music radio` a tocar para todos.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
