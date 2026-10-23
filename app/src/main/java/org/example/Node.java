package org.example;

import java.util.Random;

public class Node {

    int value;
    Node[] children;

    boolean isLeaf() {
        return children == null;
    }

    static Node build(int depth, int width, int min, int max, Random rnd) {
        Node n = new Node();
        if (depth == 0) {
            n.value = min + rnd.nextInt(max - min + 1);
            return n;
        }
        n.children = new Node[width];
        for (int i = 0; i < width; i++) {
            n.children[i] = build(depth - 1, width, min, max, rnd);
        }
        return n;
    }
}
