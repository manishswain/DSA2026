package DesignPatterns.CreationalPatterns;

public class BuilderPattern {
    public static void main(String[] args) {
        Car car = new Car.Builder()
                .make("Toyota")
                .model("Camry")
                .year(2020)
                .build();

        System.out.println(car);
    }
}

class Car {
    private String make;
    private String model;
    private int year;
    private String color; // Optional field

    private Car(Builder builder) {
        this.make = builder.make;
        this.model = builder.model;
        this.year = builder.year;
        this.color = builder.color;
    }

    // Getters
    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public String getColor() {
        return color;
    }

    public static class Builder {
        private String make;
        private String model;
        private int year;
        private String color; // Optional field

        public Builder make(String make) {
            this.make = make;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Car build() {
            return new Car(this);
        }
    }

    @Override
    public String toString() {
        return "Car{" +
                "make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", year=" + year +
                ", color='" + color + '\'' +
                '}';
    }
}
