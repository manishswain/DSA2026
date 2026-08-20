package DesignPrinciples;

// Defination - The Interface Segregation Principle states that clients should not be forced to depend on interfaces they do not use.
// This principle encourages the creation of smaller, more specific interfaces rather than large
// ,general-purpose ones, which helps to reduce coupling and increase flexibility in the codebase.
public class InterfaceSegregationPrinciple {
    public static void main(String[] args) {
        // Customer actions
        Customer customer = new Customer();
        customer.placeOrder();
        customer.trackOrder();
        customer.cancelOrder();

        // Restaurant actions
        Restaurant restaurant = new Restaurant();
        restaurant.trackOrder();
        restaurant.cancelOrder();
    }
}

// Interface for placing orders
interface OrderPlacement {
    void placeOrder();
}

// Interface for tracking orders
interface OrderTracking {
    void trackOrder();
}

// Interface for canceling orders
interface OrderCancellation {
    void cancelOrder();
}

// Customer implements all interfaces
class Customer implements OrderPlacement, OrderTracking, OrderCancellation {
    public void placeOrder() {
        System.out.println("Customer placing order");
    }

    public void trackOrder() {
        System.out.println("Customer tracking order");
    }

    public void cancelOrder() {
        System.out.println("Customer canceling order");
    }
}

// Restaurant implements only relevant interfaces
class Restaurant implements OrderTracking, OrderCancellation {
    public void trackOrder() {
        System.out.println("Restaurant tracking order");
    }

    public void cancelOrder() {
        System.out.println("Restaurant canceling order");
    }
}
