package Java.Multithreading;

import java.util.ArrayList;
import java.util.List;

//About the class
/**
 * * SafeListManager demonstrates how to safely manage a shared list in a
 * multithreaded environment.
 * It uses synchronization to ensure that only one thread can modify the list at
 * a time, while allowing
 * expensive pre-processing to be done outside the synchronized block to improve
 * performance.
 */
public class SafeListManager {
    private final List<String> list = new ArrayList<>();

    public void addItem(String item) {
        // Do some expensive pre-processing outside the lock
        String processed = item.trim().toLowerCase();
        synchronized (SafeListManager.class) {
            list.add(processed);
        }
    }

    public String getItem(int index) {
        synchronized (SafeListManager.class) {
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
            return null;
        }
    }

    public static void main(String[] args) {
        // Test SafeListManager
        SafeListManager manager = new SafeListManager();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                manager.addItem(" Item " + i + " ");
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                manager.addItem(" Items " + i + " ");
            }
        });

        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 10; i++) {
            System.out.println(manager.getItem(i));
        }
    }
}