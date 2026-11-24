package app.lyra;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Concurrently {

    private Concurrently() {
    }

    public static List<Integer> run(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CyclicBarrier start = new CyclicBarrier(tasks.size());
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                results.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            return statuses;
        } finally {
            executor.shutdown();
        }
    }
}
