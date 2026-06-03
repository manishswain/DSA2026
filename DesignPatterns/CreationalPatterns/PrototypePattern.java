package DesignPatterns.CreationalPatterns;

import java.util.HashMap;
import java.util.Map;

// Prototype Interface
// This example demonstrates the Prototype Design Pattern, which allows for cloning of objects to create new instances 
// without the need for direct instantiation. The Prototype interface defines a clone method that must be implemented by 
// concrete prototypes. The PrototypeRegistry class serves as a registry for storing and cloning prototypes. 
// The main method demonstrates three examples of using the Prototype pattern: direct cloning, configuration cloning,
//  and using a prototype registry.   
interface Prototype extends Cloneable {
    Prototype clone();
}

// Concrete Prototype 1 - Document
class Document implements Prototype {
    private String title;
    private String content;
    private String author;

    public Document(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    @Override
    public Document clone() {
        try {
            return (Document) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", author='" + author + '\'' +
                '}';
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

// Concrete Prototype 2 - Configuration
class Configuration implements Prototype {
    private String environment;
    private int port;
    private String database;

    public Configuration(String environment, int port, String database) {
        this.environment = environment;
        this.port = port;
        this.database = database;
    }

    @Override
    public Configuration clone() {
        try {
            return (Configuration) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "environment='" + environment + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                '}';
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}

// Prototype Registry (Registry Pattern combined with Prototype)
class PrototypeRegistry {
    private Map<String, Prototype> registry = new HashMap<>();

    public void register(String name, Prototype prototype) {
        this.registry.put(name, prototype);
    }

    public Prototype clone(String name) {
        Prototype prototype = this.registry.get(name);
        if (prototype != null) {
            return prototype.clone();
        }
        return null;
    }
}

// Main class demonstrating Prototype Pattern
public class PrototypePattern {
    public static void main(String[] args) {
        System.out.println("=== Prototype Pattern Example ===\n");

        // Example 1: Direct Cloning
        System.out.println("--- Example 1: Direct Cloning ---");
        Document originalDoc = new Document("Java Design Patterns", "Detailed explanation of patterns", "John Doe");
        System.out.println("Original Document: " + originalDoc);

        Document clonedDoc = originalDoc.clone();
        clonedDoc.setTitle("Modified Design Patterns");
        System.out.println("Cloned Document: " + clonedDoc);
        System.out.println("Original (unchanged): " + originalDoc);
        System.out.println();

        // Example 2: Configuration Cloning
        System.out.println("--- Example 2: Configuration Cloning ---");
        Configuration devConfig = new Configuration("development", 8080, "MySQL");
        System.out.println("Dev Config: " + devConfig);

        Configuration prodConfig = devConfig.clone();
        prodConfig.setEnvironment("production");
        System.out.println("Prod Config: " + prodConfig);
        System.out.println("Dev Config (unchanged): " + devConfig);
        System.out.println();

        // Example 3: Using Prototype Registry
        System.out.println("--- Example 3: Using Prototype Registry ---");
        PrototypeRegistry registry = new PrototypeRegistry();

        // Register prototypes
        registry.register("template_doc", new Document("Template", "Template content", "Admin"));
        registry.register("default_config", new Configuration("staging", 3000, "PostgreSQL"));

        // Clone from registry
        Document doc1 = (Document) registry.clone("template_doc");
        doc1.setTitle("New Document from Template");
        Document doc2 = (Document) registry.clone("template_doc");

        System.out.println("Document 1: " + doc1);
        System.out.println("Document 2: " + doc2);

        Configuration config1 = (Configuration) registry.clone("default_config");
        Configuration config2 = (Configuration) registry.clone("default_config");
        System.out.println("Config 1: " + config1);
        System.out.println("Config 2: " + config2);
    }
}