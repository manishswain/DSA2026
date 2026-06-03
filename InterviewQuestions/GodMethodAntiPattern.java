package InterviewQuestions;

import java.util.HashMap;
import java.util.Map;

public class GodMethodAntiPattern {
    public static void main(String[] args) {
        // Input parameters without proper checks
        int numTravellers = 3;
        String destination = "London";
        String tripType = "ROUND";

        FlightFareCalculator calculator = new FlightFareCalculator();
        // double fare = calculator.calculateFare(numTravellers, destination, tripType);
        // System.out.println("Total Fare: $" + fare);

        FareBreakDown breakDown = calculator.calculate(numTravellers, Destination.fromString(destination),
                TripType.valueOf(tripType.trim().toUpperCase()));
        System.out.println(breakDown);
    }
}

// ❌ BAD: Violates SRP, OCP, and Clean Code rules
class FlightFareCalculator {
    public double calculateFare(int numTravellers, String destination, String tripType) {
        double baseFare = 0;

        if (destination.equals("NYC")) {
            baseFare = 5000;
        } else if (destination.equals("LON")) {
            baseFare = 8000;
        } else if (destination.equals("DXB")) {
            baseFare = 3000;
        }

        if (tripType.equals("ROUND")) {
            baseFare = baseFare * 2;
        }

        return baseFare * numTravellers;
    }

    public FareBreakDown calculate(int numberOfTravellers, Destination destination, TripType tripType) {
        validateInputs(numberOfTravellers, destination, tripType);

        double farePerTraveller = tripType.applyMultiplier(destination.getBaseFare());
        double totalFare = farePerTraveller * numberOfTravellers;

        return new FareBreakDown(destination, tripType, numberOfTravellers, totalFare);

    }

    private void validateInputs(int numberOfTravellers, Destination destination, TripType tripType) {
        if (numberOfTravellers <= 0) {
            throw new IllegalArgumentException("Number of travellers must be at least 1.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination must not be null.");
        }
        if (tripType == null) {
            throw new IllegalArgumentException("Trip type must not be null.");
        }
    }
}

enum Destination {
    NEW_YORK("New York", 5000.0),
    LONDON("London", 3000.0),
    DUBAI("Dubai", 2000.0);

    private final String displayName;
    private final double baseFare;

    private static final Map<String, Destination> DISPLAY_NAME_MAP = new HashMap<>();

    static {
        for (Destination d : values()) {
            DISPLAY_NAME_MAP.put(
                    d.displayName.toLowerCase(), d);
        }
    }

    private Destination(String displayName, double baseFare) {
        this.displayName = displayName;
        this.baseFare = baseFare;
    }

    public static Destination fromString(String input) {
        Destination result = DISPLAY_NAME_MAP.get(
                input.trim()
                        .toLowerCase());
        if (result == null) {
            throw new IllegalArgumentException(
                    "Not a Valid Destination. Valid Values are " + DISPLAY_NAME_MAP.keySet());
        }
        return result;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseFare() {
        return baseFare;
    }

}

enum TripType {
    ONE_WAY(1.0),
    ROUND(2.0);

    private final double multiplier;

    TripType(double d) {
        this.multiplier = d;
    }

    public double applyMultiplier(double fare) {
        return fare * multiplier;
    }
}

class FareBreakDown {
    private final Destination destination;
    private final TripType tripType;
    private final int numberOfTravellers;
    private final double totalFare;

    public FareBreakDown(Destination destination, TripType tripType, int numberOfTravellers, double totalFare) {
        this.destination = destination;
        this.tripType = tripType;
        this.numberOfTravellers = numberOfTravellers;
        this.totalFare = totalFare;
    }

    public double getTotalFare() {
        return totalFare;
    }

    @Override
    public String toString() {
        return String.format(
                "Flight to %s | %s | Travellers: %d | Total Fare: ₹%.2f",
                destination.getDisplayName(),
                tripType,
                numberOfTravellers,
                totalFare);
    }

}
