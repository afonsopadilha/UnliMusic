# E16 · Definições e configuração

> **Fase 4 — Locais e definições** · Dificuldade ●●○○○ · Depende de: E05, E14 · Roadmap antigo: passo 6

## Em poucas palavras

Um ecrã de definições e os ficheiros onde elas ficam guardadas: pasta de músicas, HUD (mostrar, posição, tamanho), país por defeito das rádios, aceitar música partilhada, baixar a música do próprio Minecraft enquanto tocas a tua.

## No fim vais ver

Botão "Definições" no ecrã do mod (e, se tiveres o Mod Menu, um botão "Configurar" na lista de mods). Mudar uma opção tem efeito na hora e fica guardada.

## O que se constrói

- **`config/unlimusic-client.json`** (cliente) e **`config/unlimusic-server.json`** (servidor, já existe desde a E07) com Gson; valores por defeito; tolerar ficheiros antigos ou estragados.
- **Ecrã próprio vs biblioteca (decisão D7)**: recomendação = ecrã próprio simples, para os jogadores não terem de instalar mais mods.
- **Integração opcional com o Mod Menu** (dependência opcional, só ativa se estiver instalado).
- **Silenciar a música do Minecraft** enquanto a nossa toca: investigar o `MusicManager`. Pode exigir o primeiro mixin a sério do projeto, bom momento para aprender.
- Opções reunidas das entradas anteriores: HUD (E05), pasta (E14), país (E06), aceitar partilhado (E11), modo jukebox (E17, se existir).

## Conceitos novos

- **Serialização de configuração** e compatibilidade entre versões.
- **Dependências opcionais** de mods.
- **Mixins**, se for preciso para a música do jogo.

## Passos sugeridos

1. Classe de configuração + ficheiro, com valores por defeito.
2. Ligar as opções que já existem no código.
3. Ecrã de definições.
4. Mod Menu (opcional).
5. Silenciar a música do jogo.

## Pronto quando

- [ ] Opções guardadas e lidas ao arrancar.
- [ ] Apagar o ficheiro → recriado com valores por defeito.
- [ ] Ficheiro estragado → aviso no log, valores por defeito, sem crash.

## Armadilhas

- Gravar o ficheiro a cada tecla escrita → gravar ao fechar o ecrã.

## APIs

- **Por confirmar:** pasta de configuração (`FabricLoader.getInstance().getConfigDir()`?); entrypoint do Mod Menu para a versão compatível com a 26.3; `MusicManager`.

## Para começar a sessão

> Vamos fazer a entrada E16 do plano: lê `docs/plano/E16-definicoes.md`. Primeiro decidimos D7.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
