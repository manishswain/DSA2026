package DesignPrinciples;

// Defination - The Open/Closed Principle states that software entities (classes, modules, functions, etc.)
// should be open for extension but closed for modification.
// This means that you should be able to add new functionality to a system without changing existing code, 
// which helps to prevent bugs and maintain stability in the software.    
public class OpenClosedPrinciple {
    public static void main(String[] args) {
        // Economy ride
        Ride economyRide = new Ride(new EconomyRide());
        System.out.println("Economy ride fare: $" + economyRide.calculateFare());

        // Premium ride
        Ride premiumRide = new Ride(new PremiumRide());
        System.out.println("Premium ride fare: $" + premiumRide.calculateFare());

        // Shared ride (new type added without modifying Ride class)
        Ride sharedRide = new Ride(new SharedRide());
        System.out.println("Shared ride fare: $" + sharedRide.calculateFare());
    }
}

// Interface for fare calculation
interface RideType {
    double calculateFare();
}

// Economy ride type
class EconomyRide implements RideType {
    public double calculateFare() {
        return 10.0;
    }
}

// Premium ride type
class PremiumRide implements RideType {
    public double calculateFare() {
        return 20.0;
    }
}

// Shared ride type (new addition)
class SharedRide implements RideType {
    public double calculateFare() {
        return 5.0;
    }
}

// Ride class
class Ride {
    private RideType rideType;

    public Ride(RideType rideType) {
        this.rideType = rideType;
    }

    public double calculateFare() {
        return rideType.calculateFare();
    }
}
