package Java.Multithreading;

//About the class
/**
 * * * RunningFlagVolatileExample demonstrates the use of a volatile boolean
 * flag
 * to control the execution of a worker thread. The worker thread continuously
 * checks the
 * flag and prints "Working" until the flag is set to false by the main thread,
 * at which point it stops and prints "Worker stopped". This example illustrates
 * how the volatile keyword ensures visibility of changes to the flag across
 * threads.
 */
public class RunningFlagVolatileExample {
    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (running) {
                System.out.println("Working");
            }
            System.out.println("Worker stopped");
        });
        worker.start();
        running = false;
        Thread.sleep(1000);
        // This will be visible to the worker thread

        worker.join();
        System.out.println("Set running to false");
        Thread.sleep(2000);
    }
}
