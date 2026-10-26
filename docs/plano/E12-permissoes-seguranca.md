# E12 · Permissões, segurança e limites

> **Fase 3 — Ouvir juntos** · Dificuldade ●●●○○ · Depende de: E10 · Roadmap antigo: passo 5

## Em poucas palavras

Num servidor com outras pessoas, nem todos devem poder mudar a música de todos. E um cliente modificado não pode obrigar os PCs dos outros a abrir links quaisquer nem esgotar a quota da Jamendo.

## No fim vais ver

Um jogador sem permissão recebe "sem permissão". Pedidos a mais são travados. Nada do que um cliente envia chega aos outros sem passar pelo servidor.

## O que se constrói

- **Quem controla (decisão D3)**: nível de OP configurável, todos, ou lista de jogadores. Votação para saltar fica como ideia.
- **Rádios no partilhado**: o cliente envia só o UUID da estação; o servidor confirma na Radio Browser e usa o URL que *ela* devolve. **Nunca** reencaminhar um URL escrito por um jogador.
- **Jamendo**: o cliente envia o id da faixa; o servidor resolve o URL.
- **Limites**: pesquisas por jogador por minuto; tamanho máximo de texto e listas nos pacotes; tamanho da fila por jogador.
- **Validação** de tudo o que chega por pacote.
- **Log** de quem mudou o quê.

## Conceitos novos

- **Nunca confiar no cliente**: qualquer pacote pode ter sido inventado.
- **Rate limiting**.
- **Permissões** no Minecraft (níveis de OP).

## Passos sugeridos

1. Verificação de permissão nos comandos e nos pacotes de controlo.
2. Rádio por UUID, resolvida pelo servidor.
3. Limites de pesquisas e de fila.
4. Validação de tamanhos em todos os payloads.
5. Configuração no `unlimusic-server.json`.

## Pronto quando

- [ ] Sem permissão → mensagem, e o estado não muda.
- [ ] Um pacote com um URL inventado é ignorado (testar com um comando de debug).
- [ ] Spam de pesquisas é travado sem afetar os outros jogadores.

## Armadilhas

- Verificar permissões só no ecrã do cliente não serve: tem de ser no servidor.
- Em singleplayer o jogador é "dono": não bloquear o próprio.

## APIs

- **Por confirmar:** como verificar o nível de permissão de um `CommandSourceStack` e de um `ServerPlayer` na 26.3 (a API de permissões mudou nas versões recentes).

## Para começar a sessão

> Vamos fazer a entrada E12 do plano: lê `docs/plano/E12-permissoes-seguranca.md`. Primeiro decidimos D3; depois tratamos da regra "o servidor nunca reencaminha URLs de jogadores".

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
