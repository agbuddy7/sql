import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    public static void main(String[] args) {
        // Database credentials and URL
        // Format: jdbc:mysql://hostname:port/databaseName
        // If your database doesn't exist yet, you can remove the databaseName to connect to the server itself
        String url = "jdbc:mysql://localhost:3306/"; // Add your DB name here if needed, e.g. "localhost:3306/mydb"
        String username = "root"; // Default username for XAMPP/WAMP or fresh installations is usually "root"
        String password = "12345"; // Put your MySQL password here, it might be empty "" for root by default

        System.out.println("Attempting to connect to the database...");

        // Try-with-resources automatically closes the Connection, Statement, and ResultSet
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ Connected to the database successfully!");

            // Example queries
            try (Statement statement = connection.createStatement()) {
                
                // Example: Create a test database
                // statement.executeUpdate("CREATE DATABASE testdb;");
                // System.out.println("Created testdb");

                // Example: Execute a query like 'SHOW DATABASES;'
                System.out.println("\nDatabases in your server:");
                try (ResultSet resultSet = statement.executeQuery("SHOW DATABASES;")) {
                    while (resultSet.next()) {
                        System.out.println("- " + resultSet.getString(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(" Database connection failed!");
            e.printStackTrace();
        }
    }
}
