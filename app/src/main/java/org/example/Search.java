package org.example;

public class Search {

    long nodes;
    long cutoffs;

    void reset() {
        nodes = 0;
        cutoffs = 0;
    }

    int minimax(Node n, boolean isMax) {
        nodes++;
        if (n.isLeaf()) {
            return n.value;
        }
        int best = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Node c : n.children) {
            int v = minimax(c, !isMax);
            best = isMax ? Math.max(best, v) : Math.min(best, v);
        }
        n.value = best; // нужно, чтобы потом восстановить путь
        return best;
    }

    int alphaBeta(Node n, boolean isMax, int alpha, int beta) {
        nodes++;
        if (n.isLeaf()) {
            return n.value;
        }
        int last = n.children.length - 1;
        if (isMax) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i <= last; i++) {
                best = Math.max(
                    best,
                    alphaBeta(n.children[i], false, alpha, beta)
                );
                alpha = Math.max(alpha, best);
                if (alpha >= beta) {
                    if (i < last) {
                        cutoffs++; // считаем только отсечения, где остались непросмотренные потомки
                    }
                    break;
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i <= last; i++) {
                best = Math.min(
                    best,
                    alphaBeta(n.children[i], true, alpha, beta)
                );
                beta = Math.min(beta, best);
                if (alpha >= beta) {
                    if (i < last) {
                        cutoffs++;
                    }
                    break;
                }
            }
            return best;
        }
    }
}
