package StreamPractice;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AStreamPractice {

    List<Employee> employees = Arrays.asList(
            new Employee("Alice", "IT", 80000, 28, "Female"),
            new Employee("Bob", "IT", 95000, 35, "Male"),
            new Employee("Charlie", "HR", 60000, 30, "Male"),
            new Employee("Diana", "HR", 72000, 26, "Female"),
            new Employee("Eve", "Finance", 110000, 40, "Female"),
            new Employee("Frank", "Finance", 98000, 33, "Male"),
            new Employee("Grace", "IT", 87000, 29, "Female"));

    public static void main(String[] args) {
        AStreamPractice practice = new AStreamPractice();

        // Q1 - All Employee whose name start with A
        var res = practice.employees.stream().filter(x -> x.getName().startsWith("A")).map(Employee::getName).toList();
        System.out.println(res);

        // Q2 - All Employees sorted alphabetically
        var res1 = practice.employees.stream().sorted(Comparator.comparing(Employee::getName)).map(Employee::getName)
                .toList();
        System.out.println(res1);

        // Q3 - Highest Paid Employee in each department
        var res2 = practice.employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment,
                        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary))));
        System.out.println(res2);
        res2.forEach((dept, emp) -> IO.println(dept + "->" + emp.get().getName()));

        // Q4 - Average Salary Per department
        var res3 = practice.employees.stream().collect(
                Collectors.groupingBy(Employee::getDepartment, Collectors.averagingDouble(Employee::getSalary)));

        System.out.println(res3);

        // Q5 - Count no of employees in each department
        var res4 = practice.employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

        System.out.println(res4);

        // Q6 Total Salary of all employees
        var res5 = practice.employees.stream().mapToDouble(Employee::getSalary).sum();
        System.out.println(res5);

        // 2nd Highest Salary of the Employee
        var res6 = practice.employees.stream().map(Employee::getSalary).distinct().sorted(Comparator.reverseOrder())
                .skip(1).findFirst();
        System.out.println(res6);

        // Partition employees into male and female
        var res7 = practice.employees.stream().collect(Collectors.partitioningBy(e -> e.getGender().equals("Female")));
        System.out.println(res7);

        // Find Average Age of Male and Female employees separetly
        var res8 = practice.employees.stream()
                .collect(Collectors.groupingBy(Employee::getGender, Collectors.averagingInt(Employee::getAge)));
        System.out.println(res8);

    }
}

class Employee {
    private String name;
    private String department;
    private double salary;
    private int age;
    private String gender;

    // constructor, getters, setters
    public Employee(String name, String department, double salary, int age, String gender) {
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.age = age;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "Employee [name=" + name + ", department=" + department + ", salary=" + salary + ", age=" + age
                + ", gender=" + gender + "]";
    }

}
