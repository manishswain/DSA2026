package Playground;

import java.util.ArrayList;
import java.util.List;

public final class Employee {
    private final int id;
    private final String name;
    private final List<String> skills;

    public Employee(int id, String name, List<String> skills) {
        this.id = id;
        this.name = name;
        this.skills = new ArrayList<>(skills); // defensive copy
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getSkills() {
        return new ArrayList<>(skills); // defensive copy
    }

    public static void main(String[] args) {
        List<String> skills = new ArrayList<>();
        skills.add("Java");
        skills.add("Python");

        Employee employee = new Employee(1, "John Doe", skills);

        // Attempt to modify the original skills list
        skills.add("C++");

        // Attempt to modify the skills list obtained from the getter
        List<String> employeeSkills = employee.getSkills();
        employeeSkills.add("JavaScript");

        // Print the employee's skills to verify immutability
        System.out.println("Employee Skills: " + employee.getSkills());
    }
}
