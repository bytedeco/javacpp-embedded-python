package org.bytedeco.embeddedpython;

import java.util.Collections;

class TypeTreeBuilder {
    private final StringBuilder stringBuilder = new StringBuilder();
    int tab;

    public TypeTreeBuilder(int tab) {
        this.tab = tab;
    }

    void addType(String t) {
        stringBuilder.append(String.join("", Collections.nCopies(tab, "  "))).append(t).append("\n");
    }

    public String toString() {
        return stringBuilder.toString();
    }
}
