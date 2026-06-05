package DesignPatterns.StructuralPatterns;

// ============================================================
// SMART HOME SYSTEM - ADAPTER DESIGN PATTERN
// ============================================================
// Problem: We have a SmartHomeController (client) that expects
// a common SmartDevice interface. We have legacy/third-party
// devices (AC, SmartLight, CoffeeMachine) that have different
// incompatible APIs. We use Adapters to make them work with
// the SmartDevice interface.
// ============================================================

// ----- Target Interface (what client expects) -----
interface SmartDevice {
    void turnOn();

    void turnOff();

    String getStatus();

    String getDeviceType();
}

// ----- Adaptee 1: Legacy AC with different API -----
class LegacyAC {
    private boolean isOn = false;
    private int temperature = 24;

    public void powerOn() {
        isOn = true;
        System.out.println("Legacy AC is now ON. Temperature set to " + temperature + "°C");
    }

    public void powerOff() {
        isOn = false;
        System.out.println("Legacy AC is now OFF");
    }

    public void setTemperature(int temp) {
        this.temperature = temp;
        System.out.println("Legacy AC temperature set to " + temp + "°C");
    }

    public String fetchACState() {
        return isOn ? "AC Running at " + temperature + "°C" : "AC Off";
    }
}

// ----- Adaptee 2: Smart Light with incompatible API -----
class SmartLight {
    private boolean isOn = false;
    private int brightness = 50;

    public void activate() {
        isOn = true;
        System.out.println("Smart Light is now ON. Brightness: " + brightness + "%");
    }

    public void deactivate() {
        isOn = false;
        System.out.println("Smart Light is now OFF");
    }

    public void setBrightness(int level) {
        this.brightness = level;
        System.out.println("Smart Light brightness set to " + level + "%");
    }

    public String getLightState() {
        return isOn ? "Light On at " + brightness + "% brightness" : "Light Off";
    }
}

// ----- Adaptee 3: Coffee Machine with its own API -----
class CoffeeMachine {
    private boolean isBrewing = false;
    private boolean isOn = false;

    public void startBrewing() {
        isOn = true;
        isBrewing = true;
        System.out.println("Coffee Machine is now ON. Brewing your coffee...");
    }

    public void stopBrewing() {
        isBrewing = false;
        isOn = false;
        System.out.println("Coffee Machine is now OFF. Brewing stopped");
    }

    public String checkMachineState() {
        if (isBrewing)
            return "Coffee Machine Brewing";
        if (isOn)
            return "Coffee Machine On (Idle)";
        return "Coffee Machine Off";
    }
}

// ----- Adapter 1: Adapts LegacyAC to SmartDevice -----
class ACAdapter implements SmartDevice {
    private final LegacyAC legacyAC;

    public ACAdapter(LegacyAC legacyAC) {
        this.legacyAC = legacyAC;
    }

    @Override
    public void turnOn() {
        legacyAC.powerOn();
    }

    @Override
    public void turnOff() {
        legacyAC.powerOff();
    }

    @Override
    public String getStatus() {
        return legacyAC.fetchACState();
    }

    @Override
    public String getDeviceType() {
        return "Air Conditioner";
    }

    // Bonus: expose legacy-specific feature via adapter
    public void setTemperature(int temp) {
        legacyAC.setTemperature(temp);
    }
}

// ----- Adapter 2: Adapts SmartLight to SmartDevice -----
class SmartLightAdapter implements SmartDevice {
    private final SmartLight smartLight;

    public SmartLightAdapter(SmartLight smartLight) {
        this.smartLight = smartLight;
    }

    @Override
    public void turnOn() {
        smartLight.activate();
    }

    @Override
    public void turnOff() {
        smartLight.deactivate();
    }

    @Override
    public String getStatus() {
        return smartLight.getLightState();
    }

    @Override
    public String getDeviceType() {
        return "Smart Light";
    }

    public void setBrightness(int level) {
        smartLight.setBrightness(level);
    }
}

// ----- Adapter 3: Adapts CoffeeMachine to SmartDevice -----
class CoffeeMachineAdapter implements SmartDevice {
    private final CoffeeMachine coffeeMachine;

    public CoffeeMachineAdapter(CoffeeMachine coffeeMachine) {
        this.coffeeMachine = coffeeMachine;
    }

    @Override
    public void turnOn() {
        coffeeMachine.startBrewing();
    }

    @Override
    public void turnOff() {
        coffeeMachine.stopBrewing();
    }

    @Override
    public String getStatus() {
        return coffeeMachine.checkMachineState();
    }

    @Override
    public String getDeviceType() {
        return "Coffee Machine";
    }
}

// ----- Client: Smart Home Controller -----
class SmartHomeController {
    private final java.util.List<SmartDevice> devices = new java.util.ArrayList<>();

    public void addDevice(SmartDevice device) {
        devices.add(device);
        System.out.println("[Controller] Added device: " + device.getDeviceType());
    }

    public void turnAllOn() {
        System.out.println("\n[Controller] Turning all devices ON...");
        for (SmartDevice device : devices) {
            System.out.print("  " + device.getDeviceType() + " -> ");
            device.turnOn();
        }
    }

    public void turnAllOff() {
        System.out.println("\n[Controller] Turning all devices OFF...");
        for (SmartDevice device : devices) {
            System.out.print("  " + device.getDeviceType() + " -> ");
            device.turnOff();
        }
    }

    public void showAllStatus() {
        System.out.println("\n[Controller] Device Status Report:");
        for (SmartDevice device : devices) {
            System.out.println("  " + device.getDeviceType() + ": " + device.getStatus());
        }
    }
}

// ----- Demo / Main class -----
public class AdapterDesignPattern {
    public static void main(String[] args) {
        // Create legacy/third-party devices (incompatible APIs)
        LegacyAC legacyAC = new LegacyAC();
        SmartLight smartLight = new SmartLight();
        CoffeeMachine coffeeMachine = new CoffeeMachine();

        // Wrap them with adapters to fit the SmartDevice interface
        SmartDevice ac = new ACAdapter(legacyAC);
        SmartDevice light = new SmartLightAdapter(smartLight);
        SmartDevice coffee = new CoffeeMachineAdapter(coffeeMachine);

        // Client works only with SmartDevice interface
        SmartHomeController controller = new SmartHomeController();
        controller.addDevice(ac);
        controller.addDevice(light);
        controller.addDevice(coffee);

        controller.turnAllOn();

        // Use adapter-specific feature (e.g., set temperature / brightness)
        System.out.println();
        ((ACAdapter) ac).setTemperature(22);
        ((SmartLightAdapter) light).setBrightness(80);

        controller.showAllStatus();

        controller.turnAllOff();

        controller.showAllStatus();
    }
}
