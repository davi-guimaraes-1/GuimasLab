# GuimasLab

GuimasLab: simulador da arena, do robô e da pontuação da temporada FTC BIOBUZZ (manual TU10), que roda o código da equipe.
Java puro, sem dependências. Unidades: polegadas, segundos, radianos.
Coordenadas iguais às do Pedro Pathing: origem no canto inferior esquerdo, x para a direita,
y para cima, heading 0 = +x, anti-horário positivo. Alianças: vermelha à esquerda, azul à direita.

## Como rodar
Veja o passo a passo na conversa. Resumo:
1. Extraia o zip. Android Studio (ou IntelliJ): File > Open > escolha a pasta `biobuzz-sim` (a que tem o `build.gradle`).
2. Espere o Gradle sincronizar. Para ver a arena: abra `src/main/java/biobuzz/Viewer.java` e clique no triângulo verde ao lado de `main` (Run 'Viewer.main()'). Para os testes: o mesmo em `SelfTest.java`.
3. Sem IDE (JDK 17+ instalado): `javac -d out src/main/java/biobuzz/*.java` e depois `java -cp out biobuzz.SelfTest`.

## Disposicao das rodas (v6)
O TeleOp da Atlas mistura o giro dando +T em `superiorEsquerdo` e `superiorDireito` e -T nos `inferior*`, e usa direcoes
REVERSE nos `superior*` e FORWARD nos `inferior*`. As duas coisas so fazem sentido se os motores `superior*` estiverem
no lado esquerdo do robo e os `inferior*` no direito. Esse e o padrao (`ATLAS_SIDES`): `superiorEsquerdo` = traseira
esquerda, `superiorDireito` = frente esquerda, `inferiorEsquerdo` = traseira direita, `inferiorDireito` = frente direita
(escolhido por ser o unico que faz o field centric do TeleOp funcionar). Com o layout padrao (superior = frente) o robo nao
gira. Mude em Menu > Codigo da equipe > Rodas.

## Testar o TeleOp passo a passo (v5)
1. Abra o projeto no Android Studio e rode `biobuzz.Viewer`.
2. Escolha Teste de codigo, o numero de robos e a alianca, e clique na arena para posicionar o AGGRON.
3. Em "Codigo da equipe" escolha `TeamCode/src/main/java` do projeto Atlas, clique em Compilar TeamCode e selecione `TeleOpAzul`.
4. Iniciar simulacao. O OpMode entra em INIT; ENTER (ou START do controle) da o START.
5. Controle: um controle ligado ao PC (Xbox, PlayStation e similares) e lido pelo LWJGL/GLFW (dependencia no `build.gradle`,
   baixada pelo Gradle) e tem prioridade sobre o teclado. Dois controles = gamepad1 e gamepad2. Se aparecer
   "biblioteca LWJGL ausente", o Gradle nao baixou os JARs e so o teclado funciona.
6. Anexo coletor: enquanto os servos estiverem projetados (os dois em ~1,0) e a ponta estiver sob a FLOWER, o Pollen e
   derrubado sozinho (um a cada 0,4 s), sem precisar recolher.

## Executar o codigo da equipe (v4)
O simulador roda o `TeamCode` da Atlas **sem alteracoes**: as fontes .java sao compiladas contra a API do FTC SDK 12.0,
Pedro Pathing 2.1.2 e FTC Dashboard 0.6.0 (`libs/ftc-stubs.jar`, gerado do dex do projeto) e executadas em tempo real
sobre um hardware virtual.
1. Menu > Teste de codigo > escolha a pasta `TeamCode/src/main/java` > Compilar TeamCode > escolha o OpMode > Iniciar.
2. ENTER = START do OpMode. O gamepad e o teclado: WASD stick esquerdo, Q/E stick direito, setas dpad, J K L I = A B X Y,
   U/O bumpers.
3. Hardware virtual (nomes iguais aos do robo): rodas `superiorEsquerdo/Direito` e `inferiorEsquerdo/Direito`,
   `shooter` (setVelocity em ticks/s), `intake`, `transfer` (potencia > 0 empurra o Pollen ao shooter com o shooter
   pronto), `torre` (potencia -> giro; encoder 720 ticks/volta), `servo` e `servo2` (anexo), `pinpoint`
   (origem na pose do resetPosAndIMU, X frente, Y esquerda, heading anti-horario).
4. Testes: `biobuzz.sdk.SdkSelfTest` (opcional: `-Dbiobuzz.teamcode=<TeamCode/src/main/java>` roda o TeleOpAzul original).
5. **Pedro Pathing real**: `libs/ftc-stubs.jar` so tem assinaturas. Para rodar OpModes que usam o Pedro (ex.: CalibragemReta)
   coloque os JARs reais (`com.pedropathing:*` 2.1.2 e o `classes.jar` dos AARs do SDK 12) em `libs/real/`; eles entram antes
   dos stubs. Sem eles a tela mostra "Pedro Pathing real nao esta no classpath".
6. Regenerar os stubs: `tools/dexapi.py`, `tools/prune.py` e `tools/shadowgen.py` (leem os .dex de
   `TeamCode/build/intermediates/dex/debug` do projeto da equipe).
Suposicoes (parametros em `SdkParams`): velocidades 75,17 e 59,33 in/s (Constants da Atlas); shooter 28 ticks/volta,
flywheel 4 in e eficiencia 0,5 [EST]; torre 240 gr/s a potencia 1 [EST]; potencia positiva = roda para frente
(as direcoes ja foram ajustadas pela equipe); servos 0 = levantado, 1 = projetado [EST].

## Tela inicial (v3)
Ao rodar `Viewer`, abre a configuracao antes da arena:
- Modo: Round (cronometro e pontuacao) ou Teste de codigo (ambiente livre, sem fases nem fim de partida).
- Robos: 1 (so o AGGRON) ou 4 (AGGRON + parceiro + 2 oponentes com IA, `SimpleAI`).
- Alianca: vermelha ou azul.
- Posicao inicial: clique na arena para posicionar, arraste (ou roda do mouse, ou Q/E) para girar.
  No Round a posicao e validada pela regra G304 (`StartRules`) e encosta sozinha na parede; no Teste de codigo e livre.
- O manual nao fixa pontos de partida: os robos com IA usam posicoes legais por G304 (`StartRules.candidates`).
- Na arena: M volta ao menu, R reinicia com a mesma configuracao.

## Regra para virar a HIVE (v3)
Conta os elementos lancados na CELL voltada para cima desde o ultimo tip. Vira com 8 Pollen, ou 5 Nectar, ou
3 Nectar + 3 Pollen (`Layout.TIP_*`, `Hive.shouldTip`). Os 3 Nectar posicionados no inicio nao contam por padrao
(`Layout.TIP_COUNT_STAGED_NECTAR`).

## Controles do Viewer
W/S frente/trás, A/D esquerda/direita, Q/E girar, I intake, Z auto-mira na HIVE, ESPAÇO atirar,
J/L torreta, U/O potência do shooter, N Nectar (humano), Y pula o AUTO, R reinicia,
C mostra a geometria de colisão da HIVE.

## O que já existe
- `Match`: fases (AUTO 30 s, transição 8 s, TELEOP 2 min), física das bolas, HIVES biestáveis,
  FLOWERS, humano entrando Nectar (um por tip, todo no último minuto), faltas, pontuação e RP.
- `Robot`: base mecanum com limite de aceleração, intake frontal (capacidade 4), torreta e shooter
  com spin-up. Todos os parâmetros são públicos (`maxForward`, `accel`, `shootElev`, `muzzleZ`...).
- `Ballistics.speedFor(...)`: velocidade de saída para acertar um ponto.
- Controle: `match.setController(robot, (m, r) -> { ... })`, chamado a 50 Hz.

## Colisões e intake (v2)
- `Match.collideBallsWithRobots()`: o robô (retângulo com altura) empurra Pollen e Nectar e sofre a reação
  (impulso com `Robot.mass` e `Ball.massKg`). A física elemento x elemento (`collideBalls`) não foi alterada.
  Bola prensada contra a parede empurra o robô de volta. A bola recém-lançada ignora o próprio robô até sair dele.
- `HiveGeometry`: colisão da HIVE separada do desenho. Só as 2 barras de base no chão bloqueiam um robô baixo;
  a parte móvel só bloqueia robôs mais altos que `Layout.HIVE_OVERHEAD_Z`.
- `Robot.canIntake(ball)`: o AGGRON coleta só Pollen (`intakesNectar = false`). Nectar é sólido e empurrável,
  inclusive no fundo da FLOWER.
- Dimensões do AGGRON (`Layout.AGGRON_*_MM`): X 425 mm = largura (eixo y do robô), Y 446 mm = comprimento (eixo x,
  frente), Z 280 mm = altura.

## Premissas que precisam de calibração (marcadas [EST] em `Layout.java`)
- Posições da HIVE, FLOWERS, LOADING ZONES e GARDENS foram lidas das figuras (+-2 in). Trocar pelo CAD oficial.
- Altura/posição da abertura da CELL e a regra de tombar (`TIP_THRESHOLD`): o manual não diz quantos
  lançamentos tombam a HIVE. Calibrar no campo real.
- Pontos por elemento em CELL, FLOWER e GARDEN foram tratados como "por elemento".
- Bônus do Nectar mais baixo: uma vez por FLOWER. TIP creditado à aliança dona da HIVE.
- Massas de Pollen/Nectar, restituição robô x bola, largura/altura das barras de base e `HIVE_OVERHEAD_Z` são estimativas
  (`Layout.java`). Massa do robô: 10,75 kg (`FollowerConstants.mass` da Atlas).
- As CELLs (20 in) em x=54 e x=90 invadem as barras da base (x~47 e ~97): a estimativa de x das HIVES provavelmente está errada em alguns in.
- Anexo coletor de Pollen do AGGRON (2 servos): ainda NAO implementado, aguardando esclarecimentos sobre o CAD.
- Ainda não modelado: colisão das bolas com a estrutura da HIVE, AprilTags, a maioria das regras de falta.

## Próximos passos
1. Parâmetros reais do robô (CAD + hardware).
2. Camada de hardware falso (HardwareMap, motores, servos, IMU) e localizer customizado para rodar o OpMode
   da equipe e o Pedro Pathing sobre este núcleo.
3. Visualização (arena 2D/3D) e replay de paths.
