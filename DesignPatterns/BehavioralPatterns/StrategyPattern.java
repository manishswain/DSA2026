package DesignPatterns.BehavioralPatterns;

interface PaymentStrategy {
    void pay(double amount);
}

class CashPayment implements PaymentStrategy {

    @Override
    public void pay(double amount) {
        System.out.println("Paid via Cash" + amount);
    }
}

class PayPalPayment implements PaymentStrategy {

    @Override
    public void pay(double amount) {
        System.out.println("Paid via Paypal" + amount);
    }
}

// Context class
class PaymentProcessor {
    private PaymentStrategy paymentStrategy;

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void pay(double amount) {
        if (this.paymentStrategy == null) {
            System.out.println("No Payment Strategy");
        }
        this.paymentStrategy.pay(amount);
    }

}

public class StrategyPattern {
    public static void main(String[] args) {
        PaymentProcessor paymentProcessor = new PaymentProcessor();
        paymentProcessor.setPaymentStrategy(new CashPayment());
        paymentProcessor.pay(63.3);
        paymentProcessor.setPaymentStrategy(new PayPalPayment());
        paymentProcessor.pay(63.3);
    }
}