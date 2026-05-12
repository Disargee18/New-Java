import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

abstract class Product {
    protected String id, name;
    protected double price;
    protected int stock;

    public Product(String id, String name, double price, int stock) {
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }

    public abstract void displayInfo();

    public String toDataString() {
        return getType() + "|" + id + "|" + name + "|" + price + "|" + stock;
    }

    public abstract String getType();
}

class FoodItem extends Product {
    public FoodItem(String id, String name, double price, int stock) {
        super(id, name, price, stock);
    }
    @Override
    public void displayInfo() {
        System.out.printf("[FOOD] %-15s | ID: %s | Price: %.2f | Stock: %d\n", name, id, price, stock);
    }
    @Override
    public String getType() { return "FOOD"; }
}

class GeneralItem extends Product {
    public GeneralItem(String id, String name, double price, int stock) {
        super(id, name, price, stock);
    }
    @Override
    public void displayInfo() {
        System.out.printf("[ITEM] %-15s | ID: %s | Price: %.2f | Stock: %d\n", name, id, price, stock);
    }
    @Override
    public String getType() { return "GENERAL"; }
}

interface Networkable {
    void connect(String host);
    void sync();
}

// --- REPLACE THE OLD NetworkManager BLOCK WITH THIS ---
class NetworkManager implements Networkable {
    private String host = "localhost";
    private int port = 5000;

    @Override
    public void connect(String host) { 
        this.host = host;
        System.out.println("Target host set to " + host); 
    }

    @Override
    public void sync() { 
        System.out.println("Initiating TCP Sync...");
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Send sync command to the server
            out.println("SYNC");
            
            // Read response from the server
            String response = in.readLine();
            System.out.println("Server Response: " + response);
            
        } catch (IOException e) {
            System.out.println("Sync Failed: Ensure Server.java is running on " + host + ":" + port);
        }
    }
}
// --- END OF REPLACEMENT ---

class User {
    String id, name, pin;
    boolean isManager;
    public User(String id, String name, String pin, boolean isManager) {
        this.id = id; this.name = name; this.pin = pin; this.isManager = isManager;
    }
}

public class POSSystem {
    static List<Product> products = new ArrayList<>();
    static List<User> users = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);
    static final String PROD_FILE = "products.txt", USER_FILE = "users.txt";

    public static void main(String[] args) {
        loadData();
        while (true) {
            User current = login();
            if (current != null) {
                if (current.isManager) managerMenu();
                else cashierMenu(current);
            }
        }
    }

    static User login() {
        System.out.print("\n--- LOGIN ---\nID: ");
        String id = sc.nextLine();
        System.out.print("PIN: ");
        String pin = sc.nextLine();
        for (User u : users) if (u.id.equals(id) && u.pin.equals(pin)) return u;
        System.out.println("Login Failed.");
        return null;
    }

    static void managerMenu() {
        while (true) {
            System.out.println("\n[1] Sale [2] Add Product [3] Network Sync [4] Logout");
            String choice = sc.nextLine();
            if (choice.equals("1")) newSale();
            else if (choice.equals("2")) addProduct();
            else if (choice.equals("3")) new NetworkManager().sync();
            else break;
        }
    }

    static void cashierMenu(User u) {
        while (true) {
            System.out.println("\n[1] Sale [2] Logout");
            if (sc.nextLine().equals("1")) newSale();
            else break;
        }
    }

    static void newSale() {
        double total = 0;
        List<String> cart = new ArrayList<>();
        while (true) {
            for (Product p : products) p.displayInfo();
            System.out.print("Enter ID (or 'DONE'): ");
            String id = sc.nextLine().toUpperCase();
            if (id.equals("DONE")) break;
            Product p = findProduct(id);
            if (p != null && p.stock > 0) {
                p.stock--;
                total += p.price;
                cart.add(p.name + " - " + p.price);
                saveProducts();
            }
        }
        if (total > 0) {
            System.out.println("\n--- RECEIPT ---\nTotal: " + total + "\nDate: " + LocalDateTime.now());
        }
    }

    static void addProduct() {
        System.out.print("Type (1: Food, 2: General): ");
        String type = sc.nextLine();
        System.out.print("ID: "); String id = sc.nextLine();
        System.out.print("Name: "); String name = sc.nextLine();
        System.out.print("Price: "); double price = Double.parseDouble(sc.nextLine());
        System.out.print("Stock: "); int stock = Integer.parseInt(sc.nextLine());
        products.add(type.equals("1") ? new FoodItem(id, name, price, stock) : new GeneralItem(id, name, price, stock));
        saveProducts();
    }

    static Product findProduct(String id) {
        for (Product p : products) if (p.id.equals(id)) return p;
        return null;
    }

    static void loadData() {
        try {
            BufferedReader pr = new BufferedReader(new FileReader(PROD_FILE));
            String l;
            while ((l = pr.readLine()) != null) {
                String[] d = l.split("\\|");
                if (d[0].equals("FOOD")) products.add(new FoodItem(d[1], d[2], Double.parseDouble(d[3]), Integer.parseInt(d[4])));
                else products.add(new GeneralItem(d[1], d[2], Double.parseDouble(d[3]), Integer.parseInt(d[4])));
            }
            pr.close();
            BufferedReader ur = new BufferedReader(new FileReader(USER_FILE));
            while ((l = ur.readLine()) != null) {
                String[] d = l.split("\\|");
                users.add(new User(d[0], d[1], d[2], Boolean.parseBoolean(d[3])));
            }
            ur.close();
        } catch (Exception e) { seed(); }
    }

    static void saveProducts() {
        try (PrintWriter out = new PrintWriter(new FileWriter(PROD_FILE))) {
            for (Product p : products) out.println(p.toDataString());
        } catch (Exception e) {}
    }

    static void seed() {
        users.add(new User("M001", "Admin", "1234", true));
        products.add(new FoodItem("P001", "Bread", 25.0, 50));
        saveProducts();
        try (PrintWriter out = new PrintWriter(new FileWriter(USER_FILE))) {
            for (User u : users) out.println(u.id + "|" + u.name + "|" + u.pin + "|" + u.isManager);
        } catch (Exception e) {}
    }
}