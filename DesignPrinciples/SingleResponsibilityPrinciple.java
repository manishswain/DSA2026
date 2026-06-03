package DesignPrinciples;

//Defination - The Single Responsibility Principle states that a class should have only one reason to change,
//  meaning it should have only one job or responsibility.
//This principle helps to create more maintainable and understandable code by ensuring that each class
//  has a clear and focused purpose. When a class has multiple responsibilities, it becomes more complex and harder to maintain,
//  as changes to one responsibility may affect the others. By adhering to the Single Responsibility Principle,
//  developers can create code that is easier to test, debug, and extend in the future.
public class SingleResponsibilityPrinciple {
    public static void main(String[] args) {
        // Create an order
        FoodOrder order = new FoodOrder();
        order.setOrderId("12345");
        order.setAmount(50.0);

        // Calculate total
        double total = order.calculateTotal();
        System.out.println("Order total: $" + total);

        // Send notification
        NotificationService notificationService = new NotificationService();
        notificationService.sendNotification("customer@example.com", "Your order is confirmed!");

        // Save to database
        OrderRepository repository = new OrderRepository();
        repository.saveToDatabase(order.getOrderId());
    }
}

// Class responsible for order details and calculation
class FoodOrder {
    private String orderId;
    private double amount;

    public double calculateTotal() {
        // Logic to calculate total
        return amount;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}

// Class responsible for sending notifications
class NotificationService {
    public void sendNotification(String customerEmail, String message) {
        System.out.println("Sending email to " + customerEmail + ": " + message);
    }
}

// Class responsible for database operations
class OrderRepository {
    public void saveToDatabase(String orderId) {
        System.out.println("Saving order " + orderId + " to database");
    }
}
