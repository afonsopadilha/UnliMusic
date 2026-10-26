# E11 · Sincronização nos clientes

> **Fase 3 — Ouvir juntos** · Dificuldade ●●●●○ · Depende de: E10 · Roadmap antigo: passo 5

## Em poucas palavras

Fazer com que todos ouçam a mesma parte da música. Quem entra a meio começa no sítio certo; quem tem lag não fica muito atrás. Nas rádios basta "sintonizar" (é ao vivo, sem seek).

## No fim vais ver

Dois clientes lado a lado tocam a mesma faixa Jamendo com menos de ~1 segundo de diferença. Quem entra a meio apanha a música no ponto certo.

## O que se constrói

- **Cálculo da posição**: posição no pacote + tempo desde que o pacote chegou + latência estimada + tempo que a faixa demorou a carregar.
- **Seek na Jamendo** por HTTP: confirmar que o servidor de áudio da Jamendo aceita pedidos por partes (Range) e que a lavaplayer faz seek em MP3 por HTTP.
- **Batimento periódico** (ex. de 30 em 30 s) com a posição do servidor. Se o desvio passar um limite (ex. 1,5 s), corrigir.
- **Pessoal vs partilhado (decisão D5)**: recomendação = a partilhada pausa a pessoal; cada jogador pode "sair do partilhado" e silenciar só para si.
- Trocar de faixa muito depressa não pode deixar clientes presos na anterior.

## Conceitos novos

- **Cada PC tem o seu relógio**, e eles não batem certo.
- **Latência (ping)** e porque se estima metade dela.
- **Drift**: pequenas diferenças que se acumulam com o tempo.
- **Tolerância**: corrigir demasiadas vezes soa pior do que estar 300 ms atrasado.

## Passos sugeridos

1. Seek simples: tocar a partir da posição do pacote.
2. Compensar o tempo de carregamento.
3. Batimento e correção de desvio.
4. Pausa/retoma sincronizada.
5. Decisão D5 e opção "sair do partilhado".

## Pronto quando

- [ ] Dois clientes com diferença inferior a ~1 s.
- [ ] Entrar a meio de uma faixa começa no ponto certo.
- [ ] Pausar/retomar sincronizado.
- [ ] Trocar de faixa 10 vezes seguidas deixa todos na última.

## Armadilhas

- Testar no mesmo PC esconde problemas de latência: tentar com alguém noutra rede, se possível.
- Seek em MP3 por HTTP pode ser lento ou impreciso.

## APIs

- **Por confirmar:** seek na lavaplayer (`AudioTrack.setPosition`?); latência do jogador vista no cliente.

## Para começar a sessão

> Vamos fazer a entrada E11 do plano: lê `docs/plano/E11-sincronizacao.md`. Explica-me primeiro o cálculo da posição com um exemplo de números.

## Notas da sessão

*(preencher no fim: decisões tomadas, o que ficou por fazer)*
