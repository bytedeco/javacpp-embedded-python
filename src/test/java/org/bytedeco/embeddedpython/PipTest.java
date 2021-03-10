package org.bytedeco.embeddedpython;

import org.junit.Test;

import java.io.IOException;

public class PipTest {
    @Test
    public void testInstall() throws IOException, InterruptedException {
        Pip.install("pandas");
    }
}
