# E00 · Arrumar o projeto e primeira tecla

> **Fase 0 — Fundações** · Dificuldade ●○○○○ · Depende de: — · Roadmap antigo: passo 0

## Em poucas palavras

Limpar o que veio do template, alinhar a configuração e criar a primeira tecla do mod: carregas numa tecla e aparece uma mensagem no chat. É o "olá mundo" que prova que o mod carrega e que o código de cliente corre.

## No fim vais ver

Em *Opções → Controlos* aparece uma categoria **UnliMusic** com a tecla "Teste". Ao carregar nela, o chat mostra uma mensagem do mod.

## O que se constrói

- **`fabric.mod.json`**: descrição, autores e contactos ainda são os do template. A licença fica para a E20, mas anota-a como pendente.
- **Remover os mixins de exemplo** (`ExampleMixin`, `ExampleClientMixin` e as entradas nos `.mixins.json`). O mod não precisa de mixins por agora.
- **Versão do Minecraft (decisão D1)**: o `CLAUDE.md` diz 26.1.x, mas `gradle.properties` tem 26.3 e foi com ela que o `runClient` já arrancou. Escolher uma e corrigir a outra.
- **JDK 25**: o `javap` que está no PATH é do JDK 17. Confirmar que o Gradle e o IntelliJ usam o 25.
- **`.gitignore`**: acrescentar já o ficheiro de segredos que a E07 vai usar (ex. `unlimusic-server.json`), como pede o `CLAUDE.md`.
- **Organização de pacotes** (proposta, para todas as entradas seguirem a mesma):
  - `com.unlimusic` (`src/main`): `net/` (pacotes), `server/` (estado partilhado, comandos), `api/jamendo/`, `api/radiobrowser/`, `config/`
  - `com.unlimusic.client` (`src/client`): `audio/`, `hud/`, `screen/`, `input/` (teclas)
- **Tecla de teste**: registar um `KeyMapping` e, a cada tick do cliente, ver se foi carregada e escrever no chat.
- **Ficheiros de língua**: `assets/unlimusic/lang/en_us.json` e `pt_pt.json` com o nome da tecla e da categoria.

## Conceitos novos

- **Entrypoints** `main` vs `client` e porque o código está em duas pastas.
- **Tick do cliente**: o jogo "pensa" 20 vezes por segundo; é aí que se verificam as teclas.
- **`KeyMapping`**: tecla configurável pelo jogador (não é "ouvir a tecla M" diretamente).
- **Ficheiros de língua**: o texto nunca vai escrito no código, vai uma chave traduzível.
- **Mixin**: o que é, e porque não precisamos dele agora.

## Passos sugeridos

1. Limpar `fabric.mod.json` e remover os mixins de exemplo → `runClient` continua a arrancar.
2. Decidir D1 e corrigir o ficheiro que estiver errado.
3. Registar a tecla (ainda sem fazer nada) → aparece nos Controlos.
4. Ligar a tecla à mensagem no chat.
5. Traduções pt_pt / en_us.

## Pronto quando

- [ ] A tecla aparece nos Controlos com nome traduzido, numa categoria própria.
- [ ] Carregar uma vez = uma mensagem (não uma por tick enquanto está carregada).
- [ ] Mixins de exemplo removidos; `./gradlew build` passa.
- [ ] D1 decidida e `CLAUDE.md` coerente com `gradle.properties`.

## Armadilhas

- Usar `consumeClick()` num ciclo `while`, não `isDown()`, senão dispara em todos os ticks.
- Código de cliente (teclas, chat do jogador) nunca em `src/main`: num servidor dedicado essas classes não existem.
- Tutoriais antigos usam Yarn (`KeyBinding`, `MinecraftClient`…). Na 26.x são os nomes oficiais (`KeyMapping`, `Minecraft`…).

## APIs

- **Confirmado (26.3):** `KeyMappingHelper.registerKeyMapping(KeyMapping)` em `net.fabricmc.fabric.api.client.keymapping.v1`.
- **Por confirmar:** construtor de `KeyMapping` e como se cria a categoria (mudou nas versões recentes); `ClientTickEvents.END_CLIENT_TICK`; como mostrar uma mensagem ao jogador.

## Para começar a sessão

> Vamos fazer a entrada E00 do plano: lê `docs/plano/E00-arrumar-projeto.md`. Começa pela decisão D1 (versão do Minecraft) e depois avança passo a passo, testando com runClient.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
