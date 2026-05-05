package Java.Multithreading;

public class LongRunningTask implements Runnable {

    private String cmd;

    public LongRunningTask(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void run() {
        IO.println("Starting Task..." + cmd + " on thread " + Thread.currentThread().getName());
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        IO.println("Ending Task " + cmd + " on thread " + Thread.currentThread().getName());
    }
}
