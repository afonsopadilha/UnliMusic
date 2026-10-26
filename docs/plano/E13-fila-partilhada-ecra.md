# E13 · Fila partilhada no ecrã

> **Fase 3 — Ouvir juntos** · Dificuldade ●●●○○ · Depende de: E09, E11, E12 · Roadmap antigo: passo 5

## Em poucas palavras

Juntar o modo partilhado ao ecrã do mod. Em cada resultado de pesquisa há dois botões: **Tocar só para mim** e **Adicionar à fila de todos**. Um separador **Fila** mostra o que vem a seguir e quem o pôs.

## No fim vais ver

Tudo o que os comandos `/music` fazem também se faz no ecrã. A fila atualiza em tempo real para todos.

## O que se constrói

- Botões de pessoal/partilhado nos separadores Rádios e Jamendo (o partilhado só aparece se o servidor tiver o mod e o jogador tiver permissão).
- **Separador Fila**: lista, quem adicionou, duração, saltar/remover (com permissão), reordenar (opcional).
- Pacotes C2S de controlo (adicionar, saltar, remover), todos validados no servidor como na E12.
- Indicador no HUD de que a música é partilhada.

## Conceitos novos

- Manter a interface coerente com um estado que muda "por fora" (outro jogador mexe na fila enquanto tens o ecrã aberto).

## Passos sugeridos

1. Botão "Adicionar à fila de todos".
2. Separador Fila só de leitura, a atualizar com `QueueUpdate`.
3. Saltar/remover com permissão.
4. Indicador "partilhado" no HUD.

## Pronto quando

- [ ] Dois clientes: um adiciona, o outro vê na hora.
- [ ] Sem permissão, os botões de controlo não aparecem (e o servidor recusa na mesma).

## Armadilhas

- Fila grande → pacotes grandes: enviar só as próximas N faixas.

## APIs

- Reutiliza o que foi confirmado nas E08–E12.

## Para começar a sessão

> Vamos fazer a entrada E13 do plano: lê `docs/plano/E13-fila-partilhada-ecra.md`.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
