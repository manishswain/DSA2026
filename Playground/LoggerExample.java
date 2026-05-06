package Playground;

import static java.lang.ScopedValue.where;

public class LoggerExample {

    // Define a ScopedValue for transaction ID
    static final ScopedValue<String> TRANSACTION_ID = ScopedValue.newInstance();

    public static void main(String[] args) {
        handleTransaction("TXN-001");
        handleTransaction("TXN-002");
    }

    static void handleTransaction(String id) {
        // Bind the transaction ID for this operation
        where(TRANSACTION_ID, id).run(() -> {
            execute();
            log("Transaction finished!");
        });
    }

    static void execute() {
        log("Executing operation...");
        storeData();
    }

    static void storeData() {
        log("Storing to database...");
    }

    static void log(String message) {
        // Access the bound transaction ID
        System.out.println("[" + TRANSACTION_ID.get() + "] " + message);
    }
}