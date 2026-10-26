# E19 · Polimento: traduções, mensagens e erros

> **Fase 6 — Lançamento** · Dificuldade ●●○○○ · Depende de: E18 · Roadmap antigo: —

## Em poucas palavras

Pôr o mod com cara de "acabado": todo o texto traduzido, erros explicados ao jogador em vez de escondidos no log, e um README para quem o vai instalar.

## No fim vais ver

Jogo em inglês e em português sem nenhuma chave por traduzir; avisos claros quando algo falha ("Não foi possível ligar à rádio"); README com instalação e uso.

## O que se faz

- **Todo o texto** em `assets/unlimusic/lang/` (pt_pt, en_us); procurar strings escritas diretamente no código.
- **Mensagens de erro amigáveis**: toasts ou mensagens no chat em vez de stacktraces; um tipo de erro → uma mensagem.
- **Logs** com níveis certos (nada de spam em `INFO`).
- **README** para jogadores: o que faz, como instalar, modo partilhado (mod no servidor e em todos os clientes), fontes de música, créditos.
- **Ícone** do mod (o atual é o do template).
- Rever o `CLAUDE.md` para refletir o que o projeto é agora.

## Conceitos novos

- **Toasts** (as notificações no canto do ecrã).
- **Chaves de tradução com argumentos** (`%s`).

## Pronto quando

- [ ] Nenhuma chave por traduzir em pt_pt e en_us.
- [ ] Os cenários de falha da E18 mostram mensagens compreensíveis.
- [ ] README e ícone novos.

## APIs

- **Por confirmar:** API de toasts na 26.3.

## Para começar a sessão

> Vamos fazer a entrada E19 do plano: lê `docs/plano/E19-polimento.md`. Começa por procurar texto escrito diretamente no código.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
