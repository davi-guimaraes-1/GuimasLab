package biobuzz.sdk;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Carrega o TeamCode da equipe ja compilado (pasta de .class) e lista/instancia os OpModes. */
public final class TeamCodeLoader {
    private TeamCodeLoader() {}

    public static URLClassLoader loader(File dir) throws Exception {
        return new URLClassLoader(new URL[] {dir.toURI().toURL()}, TeamCodeLoader.class.getClassLoader());
    }

    /** Nomes completos das classes que sao OpMode concretos. */
    public static List<String> list(File dir) {
        List<String> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return out;
        try (java.util.stream.Stream<Path> s = Files.walk(dir.toPath())) {
            List<Path> files = s.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
            URLClassLoader cl = loader(dir);
            for (Path f : files) {
                String rel = dir.toPath().relativize(f).toString();
                String name = rel.substring(0, rel.length() - 6).replace(File.separatorChar, '.');
                if (name.contains("$")) continue;
                try {
                    Class<?> c = Class.forName(name, false, cl);
                    if (OpMode.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) out.add(name);
                } catch (Throwable ignored) { }
            }
        } catch (Exception e) {
            System.err.println("[sim] nao foi possivel listar OpModes: " + e);
        }
        java.util.Collections.sort(out);
        return out;
    }

    public static OpMode instantiate(File dir, String className) throws Exception {
        Class<?> c = Class.forName(className, true, loader(dir));
        return (OpMode) c.getDeclaredConstructor().newInstance();
    }
}
