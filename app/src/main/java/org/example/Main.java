package org.example;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

public class Main {

    static final int DEPTH = 6;
    static final int WIDTH = 2;
    static final int MIN = -20;
    static final int MAX = 20;
    static final int SEEDS = 20;

    static final int WARMUP_RUNS = 50_000;
    static final int RUNS_PER_BATCH = 20_000;
    static final int BATCHES = 20;

    static long sink; // чтобы JIT не выбросил вычисления

    // среднее время одного запуска в мс: BATCHES замеров по RUNS_PER_BATCH запусков, усредняем
    static double avgMs(
        Node root,
        boolean useAlphaBeta,
        int runs,
        int batches
    ) {
        Search s = new Search();
        double sum = 0;
        for (int b = 0; b < batches; b++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < runs; i++) {
                s.reset();
                sink += useAlphaBeta
                    ? s.alphaBeta(
                          root,
                          true,
                          Integer.MIN_VALUE,
                          Integer.MAX_VALUE
                      )
                    : s.minimax(root, true);
            }
            sum += (System.nanoTime() - t0) / 1e6 / runs;
        }
        return sum / batches;
    }

    static String path(Node root) {
        StringBuilder sb = new StringBuilder();
        Node n = root;
        while (!n.isLeaf()) {
            int next = 0;
            for (int i = 0; i < n.children.length; i++) {
                if (n.children[i].value == n.value) {
                    next = i;
                    break;
                }
            }
            sb.append(next == 0 ? "L " : "R ");
            n = n.children[next];
        }
        return sb.toString().trim() + " -> лист " + n.value;
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        System.setOut(
            new PrintStream(System.out, true, StandardCharsets.UTF_8)
        );

        Node warm = Node.build(DEPTH, WIDTH, MIN, MAX, new Random(1));
        avgMs(warm, false, WARMUP_RUNS, 1);
        avgMs(warm, true, WARMUP_RUNS, 1);

        long[] abNodes = new long[SEEDS];
        double[] mmTime = new double[SEEDS];
        double[] abTime = new double[SEEDS];
        long mmNodesCount = 0;
        String pathSeed1 = "";

        System.out.println(
            "| seed | Узлов (мини-макс) | Узлов (альфа-бета) | Отсечений | Время мини-макс, мс | Время альфа-бета, мс | Значение корня |"
        );
        System.out.println("|---|---|---|---|---|---|---|");

        for (int seed = 1; seed <= SEEDS; seed++) {
            Node root = Node.build(DEPTH, WIDTH, MIN, MAX, new Random(seed));
            Search s = new Search();

            int vMm = s.minimax(root, true);
            long nMm = s.nodes;
            if (seed == 1) {
                pathSeed1 = path(root);
            }

            s.reset();
            int vAb = s.alphaBeta(
                root,
                true,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            );
            long nAb = s.nodes;
            long cuts = s.cutoffs;

            if (vMm != vAb) {
                throw new IllegalStateException(
                    "Значения корня не совпали, seed=" + seed
                );
            }

            double tMm = avgMs(root, false, RUNS_PER_BATCH, BATCHES);
            double tAb = avgMs(root, true, RUNS_PER_BATCH, BATCHES);

            mmNodesCount = nMm;
            abNodes[seed - 1] = nAb;
            mmTime[seed - 1] = tMm;
            abTime[seed - 1] = tAb;

            System.out.printf(
                "| %d | %d | %d | %d | %.5f | %.5f | %d |%n",
                seed,
                nMm,
                nAb,
                cuts,
                tMm,
                tAb,
                vMm
            );
        }

        long minN = Long.MAX_VALUE,
            maxN = 0;
        double sumN = 0,
            sumMm = 0,
            sumAb = 0;
        for (int i = 0; i < SEEDS; i++) {
            minN = Math.min(minN, abNodes[i]);
            maxN = Math.max(maxN, abNodes[i]);
            sumN += abNodes[i];
            sumMm += mmTime[i];
            sumAb += abTime[i];
        }
        double meanN = sumN / SEEDS;
        double var = 0;
        for (long n : abNodes) {
            var += (n - meanN) * (n - meanN);
        }
        double std = Math.sqrt(var / SEEDS);

        System.out.println();
        System.out.println(
            "Итого по " +
                SEEDS +
                " деревьям (узлов у мини-макса всегда " +
                mmNodesCount +
                "):"
        );
        System.out.printf(
            "Узлов альфа-бета: min=%d, max=%d, среднее=%.2f, ст.откл.=%.2f%n",
            minN,
            maxN,
            meanN,
            std
        );
        System.out.printf(
            "Доля посещённых узлов: min=%.1f%%, max=%.1f%%, среднее=%.1f%%%n",
            (100.0 * minN) / mmNodesCount,
            (100.0 * maxN) / mmNodesCount,
            (100.0 * meanN) / mmNodesCount
        );
        System.out.printf(
            "Выигрыш по узлам: среднее=%.1f%% (лучший %.1f%%, худший %.1f%%)%n",
            100.0 - (100.0 * meanN) / mmNodesCount,
            100.0 - (100.0 * minN) / mmNodesCount,
            100.0 - (100.0 * maxN) / mmNodesCount
        );
        System.out.printf(
            "Среднее время: мини-макс=%.5f мс, альфа-бета=%.5f мс, выигрыш по времени=%.1f%%%n",
            sumMm / SEEDS,
            sumAb / SEEDS,
            100.0 - (100.0 * sumAb) / sumMm
        );

        System.out.println();
        System.out.println("Оптимальный путь для seed=1: " + pathSeed1);

        System.out.println();
        System.out.println(
            "Узлов у альфа-беты по seed (один # = 2 узла, всего у мини-макса " +
                mmNodesCount +
                "):"
        );
        for (int i = 0; i < SEEDS; i++) {
            System.out.printf(
                "seed %2d | %s %d%n",
                i + 1,
                "#".repeat((int) (abNodes[i] / 2)),
                abNodes[i]
            );
        }
    }
}
