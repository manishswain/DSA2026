package DesignPrinciples;

// Defination - The Liskov Substitution Principle states that objects of a superclass should be replaceable 
// with objects of a subclass without affecting the correctness of the program.
// This principle ensures that a subclass can stand in for its superclass without causing errors or unexpected behavior, 
// which promotes code reusability and maintainability.
public class LiskovSubstitutionPrinciple {
    public static void main(String[] args) {
        PaymentProcessor1 processor = new PaymentProcessor1();

        // Credit card payment
        Payment creditCard = new CreditCardPayment("TXN123");
        processor.process(creditCard);

        // Cash payment
        Payment cash = new CashPayment();
        processor.process(cash);
    }
}

// Interface for payment processing
interface Payment {
    void processPayment();
}

// Credit card payment
class CreditCardPayment implements Payment {
    private String transactionId;

    public CreditCardPayment(String transactionId) {
        this.transactionId = transactionId;
    }

    public void processPayment() {
        System.out.println("Processing credit card payment with transaction ID: " + transactionId);
    }
}

// Cash payment
class CashPayment implements Payment {
    public void processPayment() {
        System.out.println("Processing cash payment");
    }
}

// Payment processor
class PaymentProcessor1 {
    public void process(Payment payment) {
        payment.processPayment();
    }
}
