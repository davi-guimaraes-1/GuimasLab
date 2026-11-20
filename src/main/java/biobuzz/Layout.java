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
    public static final double TIP_THRESHOLD = 1.5;        // [EST] impulso acumulado para tombar (calibrar no campo real)
    public static final double TIP_SPEED_REF = 150.0;      // in/s
    public static final double TIP_DECAY = 0.5;            // impulso perdido por segundo

    // FLOWER (9.7) [EST] offset da abertura em relacao a parede
    public static final double FLOWER_OPEN_Z = 21.5, FLOWER_MIDDLE_Z = 4.0, FLOWER_BASE_Z = 0.4;
    public static final double FLOWER_OFFSET = 3.0, FLOWER_CATCH_R = 2.2;

    // Zonas {xmin, ymin, xmax, ymax} (9.3), lidas da Fig. 9-2 [EST]
    public static final double[] RED_LZ = {0, 94, 11, 117};
    public static final double[] BLUE_LZ = {133, 27, 144, 50};
    public static final double[] RED_GARDEN = {0, 0, 23, 2};
    public static final double[] BLUE_GARDEN = {121, 142, 144, 144};

    // Fisica das bolas [EST]
    public static final double ROLL_DECEL = 15.0, WALL_E = 0.5, FLOOR_E = 0.45, BALL_E = 0.5;
}
