package org.bytedeco.embeddedpython;

import org.bytedeco.javacpp.Loader;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Pip.
 * <p>
 * JavaCPP presets Python is installed to ~/.javacpp/cache folder.
 * This Pip class will install Python libraries to this folder.
 */
public class Pip {
    private static final String python = Loader.load(org.bytedeco.cpython.python.class);

    private Pip() {
    }

    /**
     * Install pip packages.
     *
     * @param packages The package names to install.
     * @return 0 on success.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the current thread is interrupted by another thread.
     */
    public static synchronized int install(String... packages) throws IOException, InterruptedException {
        return exec(concat(new String[]{python, "-m", "pip", "install"}, packages));
    }

    /**
     * Upgrade pip packages.
     *
     * @param packages The package names to upgrade.
     * @return 0 on success.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the current thread is interrupted by another thread.
     */
    public static synchronized int upgrade(String... packages) throws IOException, InterruptedException {
        return exec(concat(new String[]{python, "-m", "pip", "install", "--upgrade"}, packages));
    }

    /**
     * Uninstall pip packages.
     *
     * @param packages The package names to uninstall.
     * @return 0 on success.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the current thread is interrupted by another thread.
     */
    public static synchronized int uninstall(String... packages) throws IOException, InterruptedException {
        return exec(concat(new String[]{python, "-m", "pip", "uninstall", "-y"}, packages));
    }

    private static int exec(String[] commands) throws IOException, InterruptedException {
        return new ProcessBuilder(commands).inheritIO().start().waitFor();
    }

    private static String[] concat(String[] a, String[] b) {
        return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
    }
}
