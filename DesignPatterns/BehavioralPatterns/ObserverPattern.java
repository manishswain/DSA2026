package DesignPatterns.BehavioralPatterns;

import java.util.ArrayList;
import java.util.List;

// Observer interface
interface StockObserver {
    void update(String stockSymbol, double price);
}

// Concrete Observer 1: Email Notifier
class EmailNotifier implements StockObserver {
    private String email;

    public EmailNotifier(String email) {
        this.email = email;
    }

    @Override
    public void update(String stockSymbol, double price) {
        System.out.println("Sending email to " + email + ": Stock " + stockSymbol + " is now $" + price);
    }
}

// Concrete Observer 2: SMS Notifier
class SMSNotifier implements StockObserver {
    private String phoneNumber;

    public SMSNotifier(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void update(String stockSymbol, double price) {
        System.out.println("Sending SMS to " + phoneNumber + ": " + stockSymbol + " = $" + price);
    }
}

// Concrete Observer 3: Mobile App Notifier
class MobileAppNotifier implements StockObserver {
    private String userId;

    public MobileAppNotifier(String userId) {
        this.userId = userId;
    }

    @Override
    public void update(String stockSymbol, double price) {
        System.out.println("Notifying app user " + userId + ": " + stockSymbol + " updated to $" + price);
    }
}

// Subject/Observable class
class StockPriceManager {
    private String stockSymbol;
    private double price;
    private List<StockObserver> observers = new ArrayList<>();

    public StockPriceManager(String stockSymbol, double initialPrice) {
        this.stockSymbol = stockSymbol;
        this.price = initialPrice;
    }

    // Attach observer
    public void attach(StockObserver observer) {
        observers.add(observer);
        System.out.println("Observer attached");
    }

    // Detach observer
    public void detach(StockObserver observer) {
        observers.remove(observer);
        System.out.println("Observer detached");
    }

    // Notify all observers
    public void notifyObservers() {
        for (StockObserver observer : observers) {
            observer.update(stockSymbol, price);
        }
    }

    // Update price and notify observers
    public void setPrice(double newPrice) {
        if (newPrice != this.price) {
            this.price = newPrice;
            System.out.println("\n" + stockSymbol + " price changed to $" + newPrice);
            notifyObservers();
        }
    }

    public double getPrice() {
        return price;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }
}

// Demo class
public class ObserverPattern {
    public static void main(String[] args) {
        // Create stock price manager
        StockPriceManager apple = new StockPriceManager("AAPL", 150.00);

        // Create observers
        EmailNotifier emailNotifier = new EmailNotifier("investor@example.com");
        SMSNotifier smsNotifier = new SMSNotifier("+1-555-1234");
        MobileAppNotifier appNotifier = new MobileAppNotifier("user123");

        // Attach observers
        System.out.println("=== Attaching Observers ===");
        apple.attach(emailNotifier);
        apple.attach(smsNotifier);
        apple.attach(appNotifier);

        // Update price - all observers will be notified
        apple.setPrice(155.00);

        // Detach an observer
        System.out.println("\n=== Detaching SMS Notifier ===");
        apple.detach(smsNotifier);

        // Update price again - only email and app notifiers will be notified
        apple.setPrice(152.50);

        // Attach a new observer
        System.out.println("\n=== Adding new observer ===");
        EmailNotifier anotherEmail = new EmailNotifier("trader@example.com");
        apple.attach(anotherEmail);

        // Update price - new observers will also be notified
        apple.setPrice(158.00);

    }
}
