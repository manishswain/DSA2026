package DesignPrinciples;

//  Defination - The Dependency Inversion Principle states that high-level modules should not depend on low-level modules.
//  Both should depend on abstractions (e.g., interfaces).
//  Additionally, abstractions should not depend on details. Details (concrete implementations) should depend on abstractions.
//  This principle promotes loose coupling and enhances the flexibility and maintainability of the codebase.
public class DependecyInversionPrinciple {
    public static void main(String[] args) {
        // Book ride with credit card
        PaymentProcessor creditCardProcessor = new CreditCardProcessor();
        RideService rideService1 = new RideService(creditCardProcessor);
        rideService1.bookRide();

        // Book ride with PayPal
        PaymentProcessor payPalProcessor = new PayPalProcessor();
        RideService rideService2 = new RideService(payPalProcessor);
        rideService2.bookRide();
    }
}

// Interface for payment processing
interface PaymentProcessor {
    void processPayment();
}

// Credit card payment processor
class CreditCardProcessor implements PaymentProcessor {
    public void processPayment() {
        System.out.println("Processing payment with credit card");
    }
}

// PayPal payment processor
class PayPalProcessor implements PaymentProcessor {
    public void processPayment() {
        System.out.println("Processing payment with PayPal");
    }
}

// Ride service
class RideService {
    private PaymentProcessor paymentProcessor;

    public RideService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public void bookRide() {
        paymentProcessor.processPayment();
        System.out.println("Ride booked");
    }
}