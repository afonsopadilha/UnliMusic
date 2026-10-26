# E01 · Pôr a lavaplayer dentro do jar

> **Fase 0 — Fundações** · Dificuldade ●●●○○ · Depende de: E00 · Roadmap antigo: —

## Em poucas palavras

A lavaplayer é a biblioteca que sabe abrir MP3, rádios, etc. e transformá-los em som "cru". Os jogadores não a têm instalada, por isso tem de ir **dentro** do jar do mod. Esta entrada é só sobre o build: ter um jar que funcione num Minecraft a sério, não só no `runClient`.

## No fim vais ver

O mod arranca numa instância real (Modrinth App, Prism ou launcher oficial com Fabric) e o log mostra uma linha do género "lavaplayer pronta".

## O que se constrói

- **Dependência** `dev.arbjerg:lavaplayer` (2.2.7 era a mais recente no Maven Central em out/2026; confirmar).
- **`include` do Loom não é transitivo.** Incluir só a lavaplayer deixa de fora tudo aquilo de que ela depende. Dependências da 2.2.7 (segundo o pom):
  - compile: `lava-common`, `httpclient` 4.5.14, `jackson-core`, `jackson-databind`
  - runtime: `lavaplayer-natives`, `rhino-engine`, `slf4j-api`, `commons-io`, `jsoup`, `base64`, `org.json`, `annotations`
- Duas formas de resolver: listar cada uma com `include`, ou criar uma configuração Gradle própria e incluir tudo o que ela resolve. A segunda escala melhor; ver a doc do Loom.
- **Não duplicar o que o Minecraft já traz** (no cache do Gradle aparecem `slf4j`, `commons-io`, `gson` e `jackson`; confirmar quais são mesmo do jogo e excluir esses).
- **`rhino-engine` e `jsoup`** servem fontes que não vamos usar (YouTube e afins). Testar se dá para os excluir sem partir nada → jar mais pequeno.
- **Corrigir o `CLAUDE.md`**: fala num "descodificador AAC via JitPack", mas o pom da 2.2.7 não o tem. Confirmar como a 2.2.7 descodifica AAC (muitas rádios são AAC).
- **Smoke test**: no arranque do cliente, criar um `AudioPlayerManager`, registar só os source managers local e HTTP (nunca `registerRemoteSources`) e escrever no log.

## Conceitos novos

- **Dependências transitivas**: as dependências das tuas dependências.
- **Jar-in-jar**: o Fabric carrega jars que vêm dentro do jar do mod.
- **`runClient` vs jar real**: no `runClient` o Gradle mete tudo no classpath, por isso "funciona sempre". Os `ClassNotFoundException` só aparecem no jar real.
- **Bibliotecas nativas** (`.dll`, `.so`, `.dylib`): código compilado para cada sistema operativo.

## Passos sugeridos

1. Adicionar a dependência só com `implementation` → smoke test no `runClient`.
2. `./gradlew build` → abrir o jar (é um zip) e ver o que tem em `META-INF/jars`.
3. Testar o jar numa instância real → corrigir as classes em falta uma a uma.
4. Excluir duplicados e o que não é preciso; voltar a testar.
5. Anotar o tamanho final do jar.

## Pronto quando

- [ ] O jar arranca numa instância real sem `ClassNotFoundException`.
- [ ] Log com "lavaplayer pronta".
- [ ] Nenhum código da lavaplayer em `src/main`.
- [ ] `CLAUDE.md` corrigido sobre o AAC.

## Armadilhas

- **Natives**: a lavaplayer usa bibliotecas nativas para alguns formatos e para converter sample rates. Confirmar em que sistemas funcionam (Windows x64, Linux, macOS Intel e Apple Silicon). Um mac pode falhar onde o teu PC funciona.
- Versões diferentes da mesma biblioteca no jogo e no mod → erros estranhos em runtime.
- **Licenças**: a lavaplayer é Apache-2.0; ao distribuí-la dentro do jar é preciso guardar os avisos de licença (fica para a E20, mas anota já a lista).

## APIs

- **Por confirmar:** sintaxe do `include` e das configurações no Loom 1.18; nomes das classes da lavaplayer 2.2.x (`DefaultAudioPlayerManager`, `LocalAudioSourceManager`, `HttpAudioSourceManager`).

## Para começar a sessão

> Vamos fazer a entrada E01 do plano: lê `docs/plano/E01-lavaplayer-no-jar.md`. O objetivo é um jar que funcione numa instância real do Minecraft, não só no runClient.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
