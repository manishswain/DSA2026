package DesignPatterns.StructuralPatterns;

/**
 * Decorator Design Pattern - Coffee Shop Example
 *
 * Structure:
 * - Component : Beverage (abstract)
 * - ConcreteComponent: Espresso, Cappuccino
 * - Decorator : BeverageDecorator (abstract)
 * - ConcreteDecorator: Milk, Vanilla, Chocolate
 */

// ---------- Component ----------
abstract class Beverage {
    protected String description = "Unknown Beverage";

    public String getDescription() {
        return description;
    }

    public abstract double cost();
}

// ---------- Concrete Components ----------
class Espresso extends Beverage {
    public Espresso() {
        description = "Espresso";
    }

    @Override
    public double cost() {
        return 2.00;
    }
}

class Cappuccino extends Beverage {
    public Cappuccino() {
        description = "Cappuccino";
    }

    @Override
    public double cost() {
        return 3.50;
    }
}

// ---------- Decorator ----------
abstract class BeverageDecorator extends Beverage {
    protected Beverage beverage;

    public abstract String getDescription();
}

// ---------- Concrete Decorators ----------
class Milk extends BeverageDecorator {
    public Milk(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Milk";
    }

    @Override
    public double cost() {
        return beverage.cost() + 0.50;
    }
}

class Vanilla extends BeverageDecorator {
    public Vanilla(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Vanilla";
    }

    @Override
    public double cost() {
        return beverage.cost() + 0.75;
    }
}

class Chocolate extends BeverageDecorator {
    public Chocolate(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Chocolate";
    }

    @Override
    public double cost() {
        return beverage.cost() + 1.00;
    }
}

// ---------- Client / Demo ----------
public class DecoratorDesignPattern {
    public static void main(String[] args) {
        // 1. Plain Espresso
        Beverage espresso = new Espresso();
        printOrder(espresso);

        // 2. Espresso + Milk
        Beverage espresso2 = new Espresso();
        espresso2 = new Milk(espresso2);
        printOrder(espresso2);

        // 3. Cappuccino + Vanilla + Chocolate
        Beverage fancyCappuccino = new Chocolate(new Vanilla(new Cappuccino()));
        printOrder(fancyCappuccino);

        // 4. Espresso + double Milk + Chocolate
        Beverage doubleMilkMocha = new Chocolate(new Milk(new Milk(new Espresso())));
        printOrder(doubleMilkMocha);

        // 5. Cappuccino + all three decorators
        Beverage deluxeCappuccino = new Chocolate(new Vanilla(new Milk(new Cappuccino())));
        printOrder(deluxeCappuccino);
    }

    private static void printOrder(Beverage beverage) {
        System.out.printf("%-50s $%.2f%n", beverage.getDescription(), beverage.cost());
    }
}
