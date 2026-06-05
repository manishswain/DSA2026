package DesignPatterns.BehavioralPatterns;

// State interface
interface TrafficLightState {
    void next(TrafficLight context);

    void previous(TrafficLight context);

    void displayLight();
}

// Concrete State: RedLight
class RedLight implements TrafficLightState {
    @Override
    public void next(TrafficLight context) {
        System.out.println("Red light completed. Transitioning to GREEN...");
        context.setState(new GreenLight());
    }

    @Override
    public void previous(TrafficLight context) {
        System.out.println("Cannot go previous from Red light");
    }

    @Override
    public void displayLight() {
        System.out.println("🔴 RED LIGHT - STOP! Do not cross the intersection.");
    }
}

// Concrete State: GreenLight
class GreenLight implements TrafficLightState {
    @Override
    public void next(TrafficLight context) {
        System.out.println("Green light completed. Transitioning to YELLOW...");
        context.setState(new YellowLight());
    }

    @Override
    public void previous(TrafficLight context) {
        System.out.println("Transitioning to RED light...");
        context.setState(new RedLight());
    }

    @Override
    public void displayLight() {
        System.out.println("🟢 GREEN LIGHT - GO! You can cross the intersection.");
    }
}

// Concrete State: YellowLight
class YellowLight implements TrafficLightState {
    @Override
    public void next(TrafficLight context) {
        System.out.println("Yellow light completed. Transitioning to RED...");
        context.setState(new RedLight());
    }

    @Override
    public void previous(TrafficLight context) {
        System.out.println("Transitioning to GREEN light...");
        context.setState(new GreenLight());
    }

    @Override
    public void displayLight() {
        System.out.println("🟡 YELLOW LIGHT - CAUTION! Prepare to stop.");
    }
}

// Context: TrafficLight
class TrafficLight {
    private TrafficLightState state;
    private String location;
    private int cycleCount = 0;

    public TrafficLight(String location) {
        this.location = location;
        this.state = new RedLight(); // Initial state
        System.out.println("Traffic Light initialized at: " + location);
    }

    public void setState(TrafficLightState state) {
        this.state = state;
    }

    public TrafficLightState getState() {
        return state;
    }

    public void changeLightNext() {
        System.out.println("\n--- " + location + " Cycle #" + (++cycleCount) + " ---");
        displayCurrentLight();
        System.out.println("Action: Changing to next state...");
        state.next(this);
        displayCurrentLight();
    }

    public void changeLightPrevious() {
        System.out.println("\n--- " + location + " (Reverse) ---");
        displayCurrentLight();
        System.out.println("Action: Changing to previous state...");
        state.previous(this);
        displayCurrentLight();
    }

    public void displayCurrentLight() {
        state.displayLight();
    }

    public String getLocation() {
        return location;
    }

    public int getCycleCount() {
        return cycleCount;
    }
}

// Demo class
public class StateDesignPattern {
    public static void main(String[] args) {
        // Create traffic lights at different locations
        TrafficLight mainIntersection = new TrafficLight("Main Street & 5th Avenue");
        TrafficLight downtownIntersection = new TrafficLight("Downtown & Central");

        System.out.println("=========================================");
        System.out.println("TRAFFIC LIGHT STATE DESIGN PATTERN DEMO");
        System.out.println("=========================================");

        // Main Intersection - Forward cycle
        System.out.println("\n========== MAIN INTERSECTION ==========");
        mainIntersection.displayCurrentLight();

        mainIntersection.changeLightNext();

        mainIntersection.changeLightNext();

        mainIntersection.changeLightNext();

        mainIntersection.changeLightNext();

        // Downtown Intersection - Forward cycle
        System.out.println("\n========== DOWNTOWN INTERSECTION ==========");
        downtownIntersection.displayCurrentLight();

        downtownIntersection.changeLightNext();
        downtownIntersection.changeLightNext();
        downtownIntersection.changeLightNext();

        // Reverse cycle demo
        System.out.println("\n========== REVERSE CYCLE DEMO ==========");
        System.out.println("Current state at Downtown:");
        downtownIntersection.displayCurrentLight();

        downtownIntersection.changeLightPrevious();
        downtownIntersection.changeLightPrevious();

        // Statistics
        System.out.println("\n========== STATISTICS ==========");
        System.out.println("Main Intersection completed " + mainIntersection.getCycleCount() + " cycles");
        System.out.println("Downtown Intersection completed " + downtownIntersection.getCycleCount() + " cycles");
    }
}
