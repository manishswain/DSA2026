package DesignPatterns.CreationalPatterns;

public class AbstractFactoryPattern {
    public static void main(String[] args) {
        VehicleFactory hondaFactory = new HondaFactory();
        Vehicle hondaCar = hondaFactory.createVehicle("car");
        Vehicle hondaBike = hondaFactory.createVehicle("bike");
        hondaCar.drive();
        hondaBike.drive();

        VehicleFactory toyotaFactory = new ToyotaFactory();
        Vehicle toyotaCar = toyotaFactory.createVehicle("car");
        Vehicle toyotaBike = toyotaFactory.createVehicle("bike");
        toyotaCar.drive();
        toyotaBike.drive();
    }
}

interface VehicleFactory {
    Vehicle createVehicle(String type);
}

class HondaFactory implements VehicleFactory {

    @Override
    public Vehicle createVehicle(String type) {
        if (type.equalsIgnoreCase("car")) {
            return new HondaCar();
        } else if (type.equalsIgnoreCase("bike")) {
            return new HondaBike();
        }
        return null;
    }
}

class ToyotaFactory implements VehicleFactory {

    @Override
    public Vehicle createVehicle(String type) {
        if (type.equalsIgnoreCase("car")) {
            return new ToyotaCar();
        } else if (type.equalsIgnoreCase("bike")) {
            return new ToyotaBike();
        }
        return null;
    }
}

interface Vehicle {
    void drive();
}

class HondaCar implements Vehicle {
    @Override
    public void drive() {
        System.out.println("Driving a Honda Car");
    }
}

class HondaBike implements Vehicle {
    @Override
    public void drive() {
        System.out.println("Riding a Honda Bike");
    }
}

class ToyotaCar implements Vehicle {
    @Override
    public void drive() {
        System.out.println("Driving a Toyota Car");
    }
}

class ToyotaBike implements Vehicle {
    @Override
    public void drive() {
        System.out.println("Riding a Toyota Bike");
    }
}
