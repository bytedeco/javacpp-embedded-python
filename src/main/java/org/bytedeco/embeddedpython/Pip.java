package org.bytedeco.embeddedpython;

import org.bytedeco.javacpp.Loader;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

public class Pip {
    private static final String python = Loader.load(org.bytedeco.cpython.python.class);

    public static synchronized int install(String... packages) throws IOException, InterruptedException {
        return exec(concat(new String[]{python, "-m", "pip", "install"}, packages));
    }

    public static synchronized int upgrade(String... packages) throws IOException, InterruptedException {
        return exec(concat(new String[]{python, "-m", "pip", "install", "--upgrade"}, packages));
    }

    public static synchronized int uninstall(String... packages) throws IOException, InterruptedException {
        return exec(concat(new String[]{python, "-m", "pip", "uninstall"}, packages));
    }

    private static int exec(String[] commands) throws IOException, InterruptedException {
        return new ProcessBuilder(commands).inheritIO().start().waitFor();
    }

    private static String[] concat(String[] a, String[] b) {
        return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
    }
}
