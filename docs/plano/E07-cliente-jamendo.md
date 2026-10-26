# E07 · Cliente da Jamendo no servidor, com cache

> **Fase 2 — Encontrar música** · Dificuldade ●●●○○ · Depende de: E00 (para tocar os resultados: E04) · Roadmap antigo: passo 4

## Em poucas palavras

Pesquisar música Creative Commons na Jamendo. A chave (client ID) e as pesquisas ficam no **servidor**, com cache, porque a quota gratuita (~35 000 pedidos/mês) é partilhada por todos os jogadores que usam esse client ID.

## No fim vais ver

`/music jamendo procurar lofi` lista faixas com artista, duração e licença. É um comando de servidor, mas funciona em singleplayer porque há um servidor integrado.

## O que se constrói

- **Client ID (passo manual teu)**: registar uma aplicação no portal de developers da Jamendo.
- **Configuração do servidor fora do Git**: `config/unlimusic-server.json` com `jamendoClientId`. Em dev vive em `run/config/` (já ignorado), mas o nome do ficheiro também vai para o `.gitignore`.
- **`JamendoClient` (`src/main`)**: pesquisa de faixas por texto e por tags, ordenação e limite. Campos que interessam: id, nome, artista, duração, URL do áudio, imagem do álbum, URL da licença, link da faixa.
- **Cache**: em memória com validade (ex. 1 h) e, se fizer falta, em disco. Contador de pedidos do mês com aviso perto do limite.
- **Sem client ID → Jamendo desativada** com mensagem clara. O mod tem de funcionar sem ela.
- **Comando de servidor de teste.**
- **Licenças CC**: guardar o tipo (BY, BY-SA, BY-NC…) para o crédito no HUD.

## Conceitos novos

- **Segredos fora do Git** e porque é que um client ID num repositório público é um problema.
- **Cache com TTL** e quotas de API.
- **Servidor integrado vs dedicado**.
- **Licenças Creative Commons** e o que obrigam a mostrar.

## Passos sugeridos

1. Ler a documentação da API v3.0 e os termos de uso.
2. Ficheiro de configuração do servidor (ler, criar com valores por defeito se não existir).
3. `JamendoClient` com uma pesquisa → log.
4. Cache e contador de pedidos.
5. Comando `/music jamendo procurar`.

## Pronto quando

- [ ] Pesquisa funciona em singleplayer.
- [ ] A mesma pesquisa repetida não gasta pedido (log diz "cache").
- [ ] Sem client ID, o jogo arranca e explica o que falta.
- [ ] O client ID nunca aparece no Git, no chat nem nos logs.

## Armadilhas

- O URL do áudio pode trazer o client ID lá dentro → nunca o mostrar no chat ou nos logs.
- Erros da API podem vir dentro do JSON com código 200: ler o cabeçalho da resposta.
- **Decisão D2** (client ID numa versão pública): não precisa de ficar resolvida já, mas a configuração deve permitir as várias opções.

## APIs

- **Confirmado (26.3):** `CommandRegistrationCallback`.
- **Por confirmar:** endpoints e parâmetros da Jamendo API v3.0 (`/tracks`, `search`, `include`, `audioformat`…); limites exatos da quota; onde fica a pasta de configuração (`FabricLoader.getInstance().getConfigDir()`?).

## Para começar a sessão

> Vamos fazer a entrada E07 do plano: lê `docs/plano/E07-cliente-jamendo.md`. Já tenho (ou vou criar agora) um client ID da Jamendo. Começa pela configuração fora do Git.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
