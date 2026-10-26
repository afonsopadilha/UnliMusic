# E06 · Pesquisa de rádios (Radio Browser)

> **Fase 2 — Encontrar música** · Dificuldade ●●○○○ · Depende de: E04 · Roadmap antigo: passo 4

## Em poucas palavras

Ligar à Radio Browser, uma base de dados aberta de rádios de todo o mundo, sem chave. Pesquisar por país (ex. PT), nome ou género, e tocar a escolhida. Ainda sem ecrã bonito: testa-se com um comando de chat.

## No fim vais ver

`/musica radios PT` (comando de cliente) lista no chat as rádios mais ouvidas de Portugal. Clicar numa toca-a.

## O que se constrói

- **`RadioBrowserClient` em `src/main`**: o servidor também vai precisar dele para validar rádios no modo partilhado (E12).
- **Escolha de servidor**: a API tem vários espelhos. Escolher um a partir da lista oficial e trocar de espelho se falhar.
- **`User-Agent: UnliMusic/<versão>`** em todos os pedidos (pedido pela API e obrigatório pelo `CLAUDE.md`).
- **Pesquisa** por `countrycode`, nome e tag, ordenada por popularidade, escondendo estações partidas, com limite de resultados.
- **Filtrar** codecs que a lavaplayer toca; usar o URL "resolvido" da estação.
- **Contar o clique** quando o jogador toca uma rádio (a API pede isto para manter a popularidade certa).
- **HTTP assíncrono** com `java.net.http.HttpClient` e parsing com Gson (já vem com o Minecraft).
- **Comando de cliente** com resultados clicáveis.
- Cache curta das pesquisas (minutos), para não repetir pedidos.

## Conceitos novos

- **APIs REST e JSON**; converter JSON em records Java com Gson.
- **Brigadier**: como se definem comandos e argumentos.
- **Componentes de texto clicáveis** no chat.
- **`CompletableFuture`**: pedir algo à rede sem bloquear o jogo.

## Passos sugeridos

1. Ler a documentação oficial da API (api.radio-browser.info) e escolher os endpoints.
2. Cliente HTTP + parsing para `RadioStation` (record) → teste com log.
3. Comando `/musica radios <país>`.
4. Resultados clicáveis que tocam a rádio (via `RadioSource` da E04).
5. Pesquisa por nome e por género; contagem de cliques.

## Pronto quando

- [ ] Pesquisa por país, por nome e por género.
- [ ] Clicar num resultado toca a rádio.
- [ ] Sem internet → mensagem amigável.
- [ ] O jogo nunca congela durante a pesquisa.

## Armadilhas

- Endpoints e parâmetros exatos: confirmar na documentação, não de memória.
- Estações marcadas como OK mas que não tocam → tratar o erro na `RadioSource`, não aqui.
- Nomes com emojis ou caracteres que a fonte do Minecraft não tem.

## APIs

- **Confirmado (26.3):** `ClientCommandRegistrationCallback`, `ClientCommands`, `FabricClientCommandSource`.
- **Por confirmar:** endpoints da Radio Browser (`/json/stations/search`, `/json/url/{uuid}`, lista de servidores); como criar texto clicável que corre um comando na 26.3.

## Para começar a sessão

> Vamos fazer a entrada E06 do plano: lê `docs/plano/E06-pesquisa-radios.md`. Começa por ler a documentação oficial da Radio Browser e mostra-me os endpoints que vamos usar.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
