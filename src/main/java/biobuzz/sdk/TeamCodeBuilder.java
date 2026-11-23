package biobuzz.sdk;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Compila o TeamCode da equipe (fontes .java, sem alteracoes) contra a API do SDK usada pelo simulador. */
public final class TeamCodeBuilder {
    private TeamCodeBuilder() {}

    public static final class Result {
        public File classesDir;
        public List<String> opModes = new ArrayList<>();
        public String error;
        public int files;
    }

    public static Result build(File srcDir) {
        Result r = new Result();
        try {
            if (srcDir == null || !srcDir.isDirectory()) { r.error = "Pasta invalida."; return r; }
            List<String> files = new ArrayList<>();
            try (java.util.stream.Stream<java.nio.file.Path> w = java.nio.file.Files.walk(srcDir.toPath())) {
                w.filter(x -> x.toString().endsWith(".java")).forEach(x -> files.add(x.toString()));
            }
            if (files.isEmpty()) { r.error = "Nenhum arquivo .java em " + srcDir; return r; }
            File out = java.nio.file.Files.createTempDirectory("teamcode").toFile();
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            if (jc == null) { r.error = "Compilador Java indisponivel (rode com um JDK)."; return r; }
            StringWriter log = new StringWriter();
            List<String> args = new ArrayList<>(Arrays.asList("-proc:none", "-nowarn", "-encoding", "UTF-8", "-source", "11", "-target", "11",
                "-cp", System.getProperty("java.class.path"), "-d", out.getAbsolutePath()));
            args.addAll(files);
            int rc = jc.run(null, null, new java.io.OutputStream() {
                @Override public void write(int b) { log.write(b); }
            }, args.toArray(new String[0]));
            r.files = files.size();
            if (rc != 0) { r.error = "Erro de compilacao:\n" + log; return r; }
            r.classesDir = out;
            r.opModes = TeamCodeLoader.list(out);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            r.error = sw.toString();
        }
        return r;
    }
}
