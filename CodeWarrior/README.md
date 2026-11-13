# Code Warrior: O Mestre do Debug

**Mecânica em 3 linhas:** o teclado do seu PC é o mapa. Você é um cursor ninja: segure uma tecla para ver a linha-fantasma e solte para dar um dash até ela.
Todo bug que estiver no caminho do dash é deletado (combo de até ×5). Sobreviva a 5 ondas e derrote o chefão "Div Desalinhada".

> ⚠️ **Aviso honesto:** este código foi escrito e conferido em um ambiente sem acesso ao Maven/libGDX, então **o jogo não foi compilado nem executado de verdade**.
> A lógica foi testada automaticamente (veja abaixo); gráficos, áudio e telas não puderam ser testados. Veja a seção "Pontos de risco".

---

## 1. Como rodar no Windows (passo a passo)

1. Instale o **JDK 17 ou mais novo** (recomendado: Temurin 21, https://adoptium.net). Marque a opção "Set JAVA_HOME" no instalador.
2. Descompacte o `.zip` numa pasta simples, por exemplo `C:\Jogos\CodeWarrior` (evite pastas sincronizadas com OneDrive).
3. Abra o **Prompt de Comando** (ou PowerShell) dentro dessa pasta: no Explorador, clique na barra de endereço, digite `cmd` e Enter.
4. Rode:

   ```
   gradlew.bat lwjgl3:run
   ```

   Na primeira vez o Gradle baixa tudo (libGDX, Gradle, talvez o JDK 21 automaticamente). Precisa de internet e pode demorar alguns minutos.
5. A janela do jogo abre. F11 alterna tela cheia.

Rodar os testes automáticos da lógica: `gradlew.bat core:testarLogica` (deve terminar com "0 falhas").

Referência oficial de setup: https://libgdx.com/wiki/start/setup

### Se aparecer erro
- **"JAVA_HOME is not set" / "java não é reconhecido"**: reinstale o JDK marcando JAVA_HOME e abra um novo Prompt.
- **Erro de toolchain / "Cannot find a Java installation ... languageVersion 21"**: instale o Temurin 21 e tente de novo.
- **Falha de download / timeout**: verifique a internet/antivírus/proxy e rode o comando de novo.
- **Janela abre e fecha / tela preta**: atualize o driver de vídeo. Copie as últimas linhas do erro e me envie.
- **Som estalando**: em `Lwjgl3Launcher.java` troque `setAudioConfig(16, 512, 3)` por `setAudioConfig(16, 1024, 3)`.

## 2. Regras e controles

- **Dash:** segure uma tecla (aparece a linha-fantasma com o nº de bugs atingidos) e solte. Toque rápido também funciona. Recarga de 0,4 s.
- **Mira assistida** (configurável): o dash faz um pequeno desvio para pegar um bug próximo da linha.
- **Ctrl+Z:** volta 2 s no tempo (recarga 20 s). **Ctrl+C → Ctrl+V** (em até 5 s): clone que ataca bugs por 8 s (recarga 25 s).
- **Enter:** terminal em câmera lenta (6 s). Digite `git push` (empurra e atordoa os bugs, recarga 30 s) ou `sudo rm -rf` (apaga todos os bugs e tira 6 PV do chefão, recarga 90 s).
- **Pares:** dash em `(`→`)` (teclas 9 e 0), `,`→`.` ou `[`→`]` em até 3 s explode tudo num raio.
- **Bugs:** *Syntax Error* (tranca teclas por 4 s; deletar libera), *Memory Leak* (deixa gosma que deixa o dash lento), *Infinite Loop* (precisa de 2 acertos em 2 s).
- **Chefão:** encostar no corpo dele dói; a explosão de par só o fere a cada 5 s, o clone a cada 1,5 s e o dash a cada 0,9 s (nada de ficar repetindo o mesmo truque).
- **Corações:** Fácil 7, Normal 5, Difícil 3 (+2 no modo facilitador). 1,5 s de invulnerabilidade após dano. Cura de +1 coração após as ondas 2 e 4.
- **Esc:** pausa (no tutorial, pula o tutorial). **F11:** tela cheia.
- Teclas **Ctrl**, **Enter**, Win, Alt, AltGr, Menu e as teclas `\` e `/` do ABNT2 não são destinos de dash (o libGDX não as reconhece).
- Configurações: som, música, intensidade dos efeitos, tela cheia, efeitos glitch, linhas de varredura, mira assistida, modo facilitador e dificuldade. Recordes são salvos por dificuldade.

## 3. Estrutura do código (`core/src/main/java/com/codewarrior`)

| Pasta | O que tem |
|---|---|
| `logica/` | Regras do jogo em Java puro (sem libGDX): `Simulacao`, `Estado`, `Teclado` (mapa ABNT2), inimigos, chefão, `TestesLogica`. |
| `efeitos/` | `Pincel` (desenho), partículas, tremor/câmera lenta (`Juice`), fundo animado, `PosProcessamento` (shaders: bloom, aberração, scanlines). |
| `ui/` | Fontes, textos, botões, HUD, mascote, arte do teclado, transições. |
| `jogador/`, `inimigos/` | Arte feita por código (nenhuma imagem externa). |
| `audio/` | `Sintetizador`: sons e música gerados em tempo real. |
| `telas/` | Menu, pergunta do tutorial, configurações, partida (`TelaJogo`), `Tutorial`, fim de jogo. |
| `dados/`, `teclado/` | Configurações/recordes salvos; ligação das teclas do libGDX com o mapa. |

A simulação (`logica`) não conhece gráficos: a tela lê `sim.e` e a lista de eventos para criar efeitos e sons.

## 4. Pontos de risco (o que pode dar errado na primeira execução)

1. **Nunca compilado com o libGDX real.** Foi conferido só contra uma simulação da API; podem existir erros de compilação pequenos (nome de método, importação).
2. **Gráficos, shaders e telas não testados visualmente.** Se os shaders falharem, o jogo roda sem pós-processamento.
3. **Áudio sintetizado:** possível atraso ou estalos conforme a placa de som (veja a dica acima).
4. **Fontes:** usa fontes do Windows (Consolas/Arial). Se não carregar, cai numa fonte padrão (acentos podem ficar feios).
5. **Teclado ABNT2:** as teclas `\` e `/` não têm código no libGDX e ficam apagadas. A posição das teclas segue o layout físico US do libGDX.
6. **Primeira execução** baixa o JDK 21 automaticamente (pode falhar sem internet).
7. **Telas com escala de 125%/150%:** mouse e tamanho foram tratados, mas não testados.
8. **Equilíbrio de dificuldade** não foi jogado; números podem pedir ajustes.
