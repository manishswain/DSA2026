package LLD;

import java.util.*;

// Product class
class Product {
    private int productId;
    private String name;
    private double price;

    public Product(int productId, String name, double price) {
        this.productId = productId;
        this.name = name;
        this.price = price;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}

// Inventory class
class Inventory {
    private Map<Integer, Integer> stock; // productId -> quantity
    private Map<Integer, Product> products; // productId -> Product

    public Inventory() {
        this.stock = new HashMap<>();
        this.products = new HashMap<>();
    }

    // Add product to inventory
    public void addProduct(Product product, int quantity) {
        products.put(product.getProductId(), product);
        stock.put(product.getProductId(),
                stock.getOrDefault(product.getProductId(), 0) + quantity);
    }

    // Remove product from inventory
    public boolean removeProduct(int productId, int quantity) {
        if (!stock.containsKey(productId) || stock.get(productId) < quantity) {
            return false;
        }
        stock.put(productId, stock.get(productId) - quantity);
        if (stock.get(productId) == 0) {
            stock.remove(productId);
            products.remove(productId);
        }
        return true;
    }

    // Check stock availability
    public int getStock(int productId) {
        return stock.getOrDefault(productId, 0);
    }

    // Display all products
    public void displayInventory() {
        System.out.println("=== Inventory ===");
        for (int productId : products.keySet()) {
            Product p = products.get(productId);
            int qty = stock.get(productId);
            System.out.println(p.getProductId() + ". " + p.getName() +
                    " - Price: $" + p.getPrice() +
                    " - Stock: " + qty);
        }
    }
}

// Main class
public class InventoryManagmentLLD {
    public static void main(String[] args) {
        Inventory inventory = new Inventory();

        // Add products
        Product laptop = new Product(1, "Laptop", 999.99);
        Product mouse = new Product(2, "Mouse", 29.99);
        Product keyboard = new Product(3, "Keyboard", 79.99);

        inventory.addProduct(laptop, 10);
        inventory.addProduct(mouse, 50);
        inventory.addProduct(keyboard, 30);

        inventory.displayInventory();

        // Remove some products
        System.out.println("\n--- Selling 2 Laptops ---");
        inventory.removeProduct(1, 2);

        inventory.displayInventory();

        // Check stock
        System.out.println("\nMouse Stock: " + inventory.getStock(2));
    }
}
