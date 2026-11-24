package biobuzz;

/**
 * Constantes do campo BIOBUZZ (manual TU10).
 * Unidades: polegadas, segundos, radianos.
 * Sistema de coordenadas igual ao do Pedro Pathing: origem no canto inferior esquerdo,
 * x para a direita, y para cima, heading 0 = +x, positivo = anti-horario.
 * Alianca VERMELHA a esquerda (x pequeno), AZUL a direita.
 *
 * Valores marcados com [EST] foram estimados das figuras do manual (+-2 in) ou sao
 * parametros de modelo que precisam ser calibrados com o campo real / CAD oficial.
 */
public final class Layout {
    private Layout() {}

    public static final double FIELD = 144.0;
    public static final double G = 386.09; // in/s^2

    // Elementos (9.8)
    public static final double POLLEN_R = 2.8 / 2, NECTAR_R = 3.6 / 2;
    public static final double POLLEN_WEIGHT = 1.0, NECTAR_WEIGHT = 1.8; // [EST] peso relativo para o impulso do tip

    // Tempos (10.4)
    public static final double AUTO_END = 30.0, TELEOP_START = 38.0, MATCH_END = 158.0;
    public static final double LAST_MINUTE = 98.0; // 60 s restantes: Flower e todo o Nectar liberados

    // HIVE (9.6) [EST] posicoes dos eixos lidas da Fig. 10-2
    public static final double RED_HIVE_X = 54.0, BLUE_HIVE_X = 90.0, HIVE_CENTER_Y = 72.0;
    public static final double CELL_SPACING = 18.8, CELL_OPEN_W = 20.0, CELL_OPEN_H = 14.0, CELL_DEPTH = 12.0;
    public static final double CELL_Z = 40.0;              // [EST] altura do centro da abertura da CELL voltada para cima
    public static final double FRAME_W = 49.46, FRAME_D = 38.95, FRAME_CX = 72.0, FRAME_CY = 72.0;
    // A HIVE so vira com pelo menos uma destas condicoes (elementos lancados na CELL voltada para cima desde o ultimo tip):
    //   8 Pollen, ou 5 Nectar, ou 3 Nectar + 3 Pollen
    public static final int TIP_POLLEN = 8, TIP_NECTAR = 5, TIP_MIX_NECTAR = 3, TIP_MIX_POLLEN = 3;
    // Os 3 Nectar ja posicionados na CELL no inicio contam para a condicao (confirmado pela equipe). Nao e final: os testes alternam.
    public static boolean TIP_COUNT_STAGED_NECTAR = true;

    // Anexo coletor de Pollen do AGGRON (2 servos): SUPOSICOES TEMPORARIAS ate confirmar o curso dos servos [EST]
    public static final double ATTACH_REACH = 4.43;        // in alem da frente do robo quando projetado (CAD: 558 mm estendido - 446 mm)
    public static final double ATTACH_WIDTH = 13.6;        // largura do garfo (CAD)
    public static final double ATTACH_SERVO_RATE = 2.0;    // extensao por segundo: curso completo em 0,5 s
    public static final double ATTACH_FULL = 0.9;          // a partir daqui conta como projetado
    public static final double ATTACH_TIP_TO_WALL = 4.9;   // a ponta precisa chegar a ate 4,9 in da parede da FLOWER
    public static final double ATTACH_MAX_MISMATCH = 0.25; // servos devem girar juntos (diferenca maxima entre as posicoes)
    public static final double ATTACH_DROP_INTERVAL = 0.4; // enquanto projetado e bem posicionado, derruba um Pollen a cada 0,4 s [EST]

    // FLOWER (9.7) [EST] offset da abertura em relacao a parede
    public static final double FLOWER_OPEN_Z = 21.5, FLOWER_MIDDLE_Z = 4.0, FLOWER_BASE_Z = 0.4;
    public static final double FLOWER_OFFSET = 3.0, FLOWER_CATCH_R = 2.2;

    // FLOWERS: {x, y na parede, normal x, normal y}; area de contato usada na regra G304.D [EST]
    public static final String[] FLOWER_NAME = {"Fundo", "Topo", "Direita", "Esquerda"};
    public static final double[][] FLOWER_WALL = {{96, 0, 0, 1}, {48, 144, 0, -1}, {144, 96, -1, 0}, {0, 48, 1, 0}};
    public static final double FLOWER_FOOT_HALF_W = 4.0, FLOWER_FOOT_DEPTH = 7.0;

    // Zonas {xmin, ymin, xmax, ymax} (9.3), lidas da Fig. 9-2 [EST]
    public static final double[] RED_LZ = {0, 94, 11, 117};
    public static final double[] BLUE_LZ = {133, 27, 144, 50};
    public static final double[] RED_GARDEN = {0, 0, 23, 2};
    public static final double[] BLUE_GARDEN = {121, 142, 144, 144};

    // Estrutura da HIVE para COLISAO (independente do desenho). O frame e um cavalete em A:
    // 2 triangulos nas extremidades da largura, cada um com uma barra de base no chao (comprimento = profundidade),
    // ligados por uma travessa no apice. So as barras de base impedem a passagem de um robo baixo. [EST] nas medidas
    public static final double HIVE_BASE_BAR_W = 2.0, HIVE_BASE_BAR_H = 2.0;
    public static final double HIVE_OVERHEAD_Z = 28.0;   // menor altura da parte movel (CELLs/travessa) acima do chao
    public static final double HIVE_OVERHEAD_TOP = 60.0;

    // Interacao ROBO x ELEMENTOS [EST]
    public static final double POLLEN_MASS_KG = 0.03, NECTAR_MASS_KG = 0.09; // so usadas na colisao com o robo
    public static final double ROBOT_BALL_E = 0.10;      // restituicao baixa: o robo empurra a bola

    // Dimensoes reais do AGGRON informadas pela equipe (mm). X = largura (eixo y do robo), Y = comprimento
    // (eixo x do robo, para frente), Z = altura.
    public static final double AGGRON_X_MM = 425, AGGRON_Y_MM = 446, AGGRON_Z_MM = 280;
    public static final double MM = 1.0 / 25.4;

    // Fisica das bolas [EST]
    public static final double ROLL_DECEL = 15.0, WALL_E = 0.5, FLOOR_E = 0.45, BALL_E = 0.5;
}
