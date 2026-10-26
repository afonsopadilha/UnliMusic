# Mod de música para Minecraft

Mod Fabric que permite ouvir música dentro do jogo, sozinho ou sincronizada entre todos os jogadores do mesmo mundo. Projeto pessoal; sou novo em modding, por isso explica as decisões e não apenas o código.

## Stack

- Minecraft Java 26.1.x, Fabric Loader + Fabric API, Java 25, Gradle (projeto gerado pelo template oficial do Fabric).
- A 26.1 já não é ofuscada: usa os nomes oficiais do código do Minecraft. **Não uses Yarn** nem exemplos/tutoriais de versões antigas.
- Antes de usar qualquer API do Minecraft ou do Fabric, confirma o nome e a assinatura na documentação do Fabric para a versão exata em `gradle.properties` (ou no código descompilado no IDE). Não adivinhes de memória.
- Mod ID: `TODO` (ver `fabric.mod.json`).

## Fontes de música (decisão fechada)

Só estas três, todas legais e sem conta/Premium para os jogadores:

1. **Ficheiros locais** do jogador.
2. **Jamendo** (música Creative Commons, API v3.0, precisa de client ID).
3. **Rádios online** via **Radio Browser** (sem chave; pesquisa por país, ex. `countrycode=PT`).

**Proibido:** YouTube, Spotify, Deezer ou qualquer extração/scraping de serviços comerciais. Na lavaplayer, regista apenas os source managers HTTP e local; nunca chames `AudioSourceManagers.registerRemoteSources(...)`.

## Modos

- **Partilhado** (servidor manda): Jamendo e rádios. O servidor guarda a fila/estado e envia aos clientes "faixa X a partir do instante T". Rádio = stream ao vivo, sem seek (todos sintonizam). Jamendo = sincroniza por ID da faixa + instante de início, cada cliente faz seek.
- **Pessoal** (só o próprio jogador): ficheiros locais, e também Jamendo/rádios.
- Ficheiros locais NÃO são partilháveis (só existem num PC). Futuro possível: pasta de músicas no servidor servida aos clientes.
- O mod tem de estar no servidor e em todos os clientes para o modo partilhado.

## Arquitetura de áudio

- Interface `MusicSource` (ou equivalente) com `play(faixa, posição)`, `pause()`, `stop()`; cada fonte implementa-a.
- Descodificação: **lavaplayer** (`dev.arbjerg:lavaplayer`), formato de saída PCM 16 bits little-endian (não Opus).
- PCM → `AudioStream` próprio → sound instance via Fabric API (`FabricSoundInstance` permite fornecer um `AudioStream`). Assim o volume segue um slider do jogo (Jukebox/Records) e há opção de som posicional tipo jukebox.
- Todo o trabalho lento (rede, descodificação, pedidos às APIs) corre fora da thread do jogo. Nunca bloquear em `ClientTickEvents`.

## Notas por fonte

- **Jamendo:** quota gratuita para apps não comerciais (~35 000 pedidos/mês) partilhada por todos os jogadores do client ID: fazer pesquisas no servidor e guardar em cache. Creditar sempre artista e licença no HUD. Uso não comercial.
- **Radio Browser:** enviar um `User-Agent` próprio nos pedidos.
- **Locais:** pasta escolhida nas definições; usar tags (título, artista, capa) para o HUD.

## Build

- A lavaplayer tem muitas dependências (HTTP client, descodificador AAC via JitPack, etc.). Todas têm de ir dentro do jar do mod com `include` do Loom. `ClassNotFoundException` em runtime = dependência em falta no jar.

## Segredos

- Client ID da Jamendo e qualquer outra chave ficam num ficheiro de configuração fora do Git (adicionar ao `.gitignore`).

## Roadmap

0. [ ] Keybind de teste que escreve no chat (confirmar se já funciona)
1. [ ] Tocar um ficheiro local fixo ao carregar numa tecla (prova o pipeline lavaplayer → AudioStream → sound instance)
2. [ ] Tocar uma rádio a partir de um URL fixo
3. [ ] HUD "a tocar agora" + keybinds pausar/parar
4. [ ] Ecrã de pesquisa (rádios por país, Jamendo)
5. [ ] Modo partilhado: pacotes cliente-servidor, comandos `/music`, sincronização
6. [ ] Navegador de ficheiros locais + ecrã de definições

## Forma de trabalhar

- Passos pequenos; correr o cliente (`./gradlew runClient`) e confirmar que funciona antes de avançar.
- Atualizar o roadmap acima quando um passo fica concluído.
