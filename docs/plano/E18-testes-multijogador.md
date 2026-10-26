# E18 · Testes a sério: multijogador, servidor dedicado, outros sistemas

> **Fase 6 — Lançamento** · Dificuldade ●●○○○ · Depende de: tudo o que se quer lançar · Roadmap antigo: —

## Em poucas palavras

Garantir que o mod funciona fora do teu PC e fora do `runClient`: com vários jogadores, num servidor dedicado com o jar real, e noutros sistemas operativos.

## No fim vais ver

Uma lista de verificação preenchida, com o que foi testado onde, e os bugs encontrados corrigidos ou anotados.

## O que se faz

- **`runServer` + 2 clientes** com nomes diferentes (configurar duas run configurations com `--username`).
- **Servidor dedicado com o jar real**: não pode carregar classes de cliente nem da lavaplayer. É o crash clássico de mods novos.
- **Jar real numa instância real** (Prism, Modrinth App) em Windows; Linux (o GitHub Actions já compila em Ubuntu); macOS por causa das natives, se houver quem teste.
- **Cenários de falha**: sem internet, rádio morta, quota Jamendo esgotada, servidor sem o mod, cliente sem o mod a entrar num servidor com o mod.
- **Desempenho**: memória e número de threads depois de 1 h a tocar; trocar de faixa 50 vezes.
- **Testes unitários** (JUnit) para a lógica pura: cálculo da posição (E11), fila (E10), parsing de JSON (E06/E07), cache.

## Conceitos novos

- **Testes unitários vs testes no jogo**: o que dá para testar sem abrir o Minecraft.
- **Separação cliente/servidor** vista do lado do servidor dedicado.

## Pronto quando

- [ ] Lista de cenários preenchida.
- [ ] Servidor dedicado arranca com o jar real e sem erros.
- [ ] Nenhuma fuga de threads ou memória depois de 1 h.
- [ ] Testes unitários no `./gradlew build` (e por isso no CI).

## Armadilhas

- "No meu PC funciona": o runClient esconde dependências em falta (ver E01).

## Para começar a sessão

> Vamos fazer a entrada E18 do plano: lê `docs/plano/E18-testes-multijogador.md`. Começa por montar a lista de cenários e as run configurations de 2 clientes + servidor.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
