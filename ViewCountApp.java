import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewCountApp {

    public static void main(String[] args) throws InterruptedException {
        runNaiveTest();    // lock YO'Q versiya
        System.out.println();
        runLockedTest();   // lock bilan versiya
    }

    private static void runNaiveTest() throws InterruptedException {
        FakeDatabase db = new FakeDatabase();
        FakeCache cache = new FakeCache();
        ViewCountService service = new ViewCountService(db, cache);

        int threadCount = 20;
        int loopPerThread = 500;
        int expectedTotal = threadCount * loopPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < loopPerThread; i++) {
                        service.increaseViewCount(); // lock yo'q
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        long end = System.currentTimeMillis();

        int dbValue = service.getDbValue();
        Integer cacheValue = service.getCacheValue();

        System.out.println("===== Naive Version Result (LOCK YO'Q) =====");
        System.out.println("Expected total calls: " + expectedTotal);
        System.out.println("DB viewCount        : " + dbValue);
        System.out.println("Cache viewCount     : " + cacheValue);
        System.out.println("Time(ms)            : " + (end - start));
    }

    private static void runLockedTest() throws InterruptedException {
        FakeDatabase db = new FakeDatabase();
        FakeCache cache = new FakeCache();
        ViewCountService service = new ViewCountService(db, cache);

        int threadCount = 50;
        int loopPerThread = 1000;
        int expectedTotal = threadCount * loopPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < loopPerThread; i++) {
                        service.increaseViewCountWithLock(); // lock bilan
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        long end = System.currentTimeMillis();

        int dbValue = service.getDbValue();
        Integer cacheValue = service.getCacheValue();

        System.out.println("===== Locked Version Result (LOCK BOR) =====");
        System.out.println("Expected total calls: " + expectedTotal);
        System.out.println("DB viewCount        : " + dbValue);
        System.out.println("Cache viewCount     : " + cacheValue);
        System.out.println("Time(ms)            : " + (end - start));
    }
}
