# BIOBUZZ Sim (núcleo)

Simulador da arena, do robô e da pontuação da temporada FTC BIOBUZZ (manual TU10).
Java puro, sem dependências. Unidades: polegadas, segundos, radianos.
Coordenadas iguais às do Pedro Pathing: origem no canto inferior esquerdo, x para a direita,
y para cima, heading 0 = +x, anti-horário positivo. Alianças: vermelha à esquerda, azul à direita.

## Como rodar
Veja o passo a passo na conversa. Resumo:
1. Extraia o zip. Android Studio (ou IntelliJ): File > Open > escolha a pasta `biobuzz-sim` (a que tem o `build.gradle`).
2. Espere o Gradle sincronizar. Para ver a arena: abra `src/main/java/biobuzz/Viewer.java` e clique no triângulo verde ao lado de `main` (Run 'Viewer.main()'). Para os testes: o mesmo em `SelfTest.java`.
3. Sem IDE (JDK 17+ instalado): `javac -d out src/main/java/biobuzz/*.java` e depois `java -cp out biobuzz.SelfTest`.

## Controles do Viewer
W/S frente/trás, A/D esquerda/direita, Q/E girar, I intake, Z auto-mira na HIVE, ESPAÇO atirar,
J/L torreta, U/O potência do shooter, N Nectar (humano), Y pula o AUTO, R reinicia.

## O que já existe
- `Match`: fases (AUTO 30 s, transição 8 s, TELEOP 2 min), física das bolas, HIVES biestáveis,
  FLOWERS, humano entrando Nectar (um por tip, todo no último minuto), faltas, pontuação e RP.
- `Robot`: base mecanum com limite de aceleração, intake frontal (capacidade 4), torreta e shooter
  com spin-up. Todos os parâmetros são públicos (`maxForward`, `accel`, `shootElev`, `muzzleZ`...).
- `Ballistics.speedFor(...)`: velocidade de saída para acertar um ponto.
- Controle: `match.setController(robot, (m, r) -> { ... })`, chamado a 50 Hz.

## Premissas que precisam de calibração (marcadas [EST] em `Layout.java`)
- Posições da HIVE, FLOWERS, LOADING ZONES e GARDENS foram lidas das figuras (+-2 in). Trocar pelo CAD oficial.
- Altura/posição da abertura da CELL e a regra de tombar (`TIP_THRESHOLD`): o manual não diz quantos
  lançamentos tombam a HIVE. Calibrar no campo real.
- Pontos por elemento em CELL, FLOWER e GARDEN foram tratados como "por elemento".
- Bônus do Nectar mais baixo: uma vez por FLOWER. TIP creditado à aliança dona da HIVE.
- Ainda não modelado: colisão das bolas com a estrutura da HIVE, AprilTags, a maioria das regras de falta.

## Próximos passos
1. Parâmetros reais do robô (CAD + hardware).
2. Camada de hardware falso (HardwareMap, motores, servos, IMU) e localizer customizado para rodar o OpMode
   da equipe e o Pedro Pathing sobre este núcleo.
3. Visualização (arena 2D/3D) e replay de paths.
