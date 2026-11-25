package biobuzz;

/** Configuracao escolhida na tela inicial. */
public final class SimConfig {
    public enum Mode { ROUND, CODE_TEST }

    public Mode mode = Mode.ROUND;
    public int robots = 1;                 // 1 (so o AGGRON) ou 4 (AGGRON + 3 robos com IA)
    public Alliance alliance = Alliance.RED;
    public double startX, startY, startH;  // pose inicial do AGGRON: centro (in) e heading (rad)
    public String teamCodeSrc = System.getProperty("biobuzz.teamcode", "");   // pasta TeamCode/src/main/java da equipe
    public java.io.File teamCodeClasses;   // TeamCode compilado
    public biobuzz.sdk.SdkParams.WheelLayout wheelLayout = biobuzz.sdk.SdkParams.WheelLayout.ATLAS_SIDES;
    public String opMode;                  // OpMode a executar (null = dirigir com o teclado)

    public SimConfig() { resetStart(); }

    /** Volta para a primeira posicao inicial legal da alianca. */
    public void resetStart() {
        double[] c = StartRules.candidates(alliance).get(0);
        startX = c[0]; startY = c[1]; startH = c[2];
    }
}
