package Java.Multithreading;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Senior Java Developer interview examples for volatile and atomic classes.
 *
 * Important interview points:
 *
 * 1. volatile:
 * - Guarantees visibility across threads.
 * - Does not guarantee atomicity for compound operations.
 * - Good for flags like running, cancelled, initialized.
 *
 * 2. Atomic classes:
 * - Provide lock-free thread-safe operations using CAS.
 * - AtomicInteger/AtomicLong are useful for counters.
 * - AtomicBoolean is useful for one-time state transitions.
 * - LongAdder is often better than AtomicLong under very high contention.
 *
 * 3. synchronized:
 * - Provides both visibility and mutual exclusion.
 * - Needed when multiple variables must be updated atomically.
 */
public class VolatileAndAtomicExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Volatile and Atomic Examples ===\n");

        volatileVisibilityExample();
        volatileDoesNotGuaranteeAtomicityExample();
        atomicIntegerExample();
        atomicBooleanExample();
        longAdderVsAtomicLongExample();
        synchronizedStillMattersExample();

        System.out.println("\n=== Completed ===");
    }

    /**
     * Volatile visibility example.
     *
     * Without volatile, the worker thread may keep reading a stale cached value of
     * running.
     * With volatile, writes by one thread become visible to other threads.
     */
    private static void volatileVisibilityExample() throws InterruptedException {
        System.out.println("1. Volatile visibility example");
        System.out.println("------------------------------");

        VolatileFlagTask task = new VolatileFlagTask();
        Thread worker = new Thread(task, "volatile-worker");
        worker.start();

        Thread.sleep(1_000);
        task.stop();

        worker.join();
        System.out.println("Worker stopped gracefully.\n");
    }

    /**
     * Important interview trap:
     *
     * volatile count is visible, but count++ is not atomic.
     *
     * count++ is three operations:
     * 1. read count
     * 2. add 1
     * 3. write count
     *
     * Multiple threads can read the same value and lose updates.
     */
    private static void volatileDoesNotGuaranteeAtomicityExample() throws InterruptedException {
        System.out.println("2. Volatile does not guarantee atomicity");
        System.out.println("--------------------------------------");

        VolatileCounter counter = new VolatileCounter();
        int threadCount = 10;
        int operationsPerThread = 10_000;

        runConcurrentTask(threadCount, operationsPerThread, counter::increment);

        System.out.println("Expected count: " + (threadCount * operationsPerThread));
        System.out.println("Actual volatile count: " + counter.getCount());
        System.out.println("Lost updates are possible because volatile is visible but not atomic.\n");
    }

    /**
     * AtomicInteger example.
     *
     * AtomicInteger.incrementAndGet() is atomic.
     * It uses CAS internally, so it avoids locking in most cases.
     */
    private static void atomicIntegerExample() throws InterruptedException {
        System.out.println("3. AtomicInteger example");
        System.out.println("------------------------");

        AtomicInteger counter = new AtomicInteger();
        int threadCount = 10;
        int operationsPerThread = 10_000;

        runConcurrentTask(threadCount, operationsPerThread, counter::incrementAndGet);

        System.out.println("Expected count: " + (threadCount * operationsPerThread));
        System.out.println("Actual AtomicInteger count: " + counter.get());
        System.out.println("No lost updates because incrementAndGet is atomic.\n");
    }

    /**
     * AtomicBoolean example.
     *
     * compareAndSet is useful for one-time transitions.
     * Example: only one thread should initialize a resource, start a job, or
     * acquire a lock.
     */
    private static void atomicBooleanExample() throws InterruptedException {
        System.out.println("4. AtomicBoolean example");
        System.out.println("------------------------");

        AtomicBoolean started = new AtomicBoolean(false);
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            executor.submit(() -> {
                try {
                    boolean becameStarter = started.compareAndSet(false, true);

                    if (becameStarter) {
                        System.out.println("Thread " + threadNumber + " became the starter.");
                    } else {
                        System.out.println("Thread " + threadNumber + " saw that work was already started.");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("AtomicBoolean.compareAndSet ensured only one thread transitioned false -> true.\n");
    }

    /**
     * LongAdder example.
     *
     * LongAdder is optimized for high contention.
     * It maintains multiple internal cells and combines them when reading.
     *
     * Interview note:
     * - AtomicLong is usually fine for moderate contention.
     * - LongAdder can perform better when many threads update frequently.
     * - AtomicLong gives a single exact value more naturally.
     */
    private static void longAdderVsAtomicLongExample() throws InterruptedException {
        System.out.println("5. LongAdder vs AtomicLong example");
        System.out.println("----------------------------------");

        AtomicLong atomicLong = new AtomicLong();
        LongAdder longAdder = new LongAdder();
        int threadCount = 10;
        int operationsPerThread = 10_000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        atomicLong.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        longAdder.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Expected value: " + (threadCount * operationsPerThread));
        System.out.println("AtomicLong value: " + atomicLong.get());
        System.out.println("LongAdder value: " + longAdder.sum());
        System.out.println("LongAdder is preferred for very high update contention.\n");
    }

    /**
     * Synchronized still matters example.
     *
     * Volatile/Atomic classes do not make a group of operations atomic.
     * If multiple variables must be updated together, use synchronized or another
     * lock.
     */
    private static void synchronizedStillMattersExample() throws InterruptedException {
        System.out.println("6. Synchronized still matters example");
        System.out.println("-------------------------------------");

        Account account = new Account(1000);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    account.transferOut(100);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Expected balance: 600");
        System.out.println("Actual balance: " + account.getBalance());
        System.out.println("synchronized was used because debit + balance update must be atomic as a group.\n");
    }

    private static void runConcurrentTask(int threadCount, int operationsPerThread, Runnable task)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        task.run();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static final class VolatileFlagTask implements Runnable {
        private volatile boolean running = true;

        @Override
        public void run() {
            int count = 0;

            while (running) {
                count++;

                if (count % 200_000_000 == 0) {
                    System.out.println("Worker is still running...");
                }
            }

            System.out.println("Worker saw running = false and exited.");
        }

        public void stop() {
            running = false;
        }
    }

    private static final class VolatileCounter {
        private volatile int count;

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    private static final class Account {
        private volatile int balance;

        private Account(int balance) {
            this.balance = balance;
        }

        public synchronized void transferOut(int amount) {
            if (balance >= amount) {
                int currentBalance = balance;
                balance = currentBalance - amount;
            }
        }

        public int getBalance() {
            return balance;
        }
    }
}
