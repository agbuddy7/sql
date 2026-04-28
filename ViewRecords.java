import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ViewRecords {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/jee_db";
        String username = "root";
        String password = "12345";

        System.out.println("Fetching records from the database...\n");

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            // View Students
            System.out.println("--- STUDENT TABLE ---");
            ResultSet rsStudent = stmt.executeQuery("SELECT * FROM Student");
            while (rsStudent.next()) {
                System.out.printf("ID: %d | Name: %s | Category: %s | Attempts: %d | Email: %s%n",
                        rsStudent.getInt("student_id"),
                        rsStudent.getString("name"),
                        rsStudent.getString("category"),
                        rsStudent.getInt("attempts"),
                        rsStudent.getString("email"));
            }
            rsStudent.close();

            System.out.println("\n--- PAYMENT TABLE ---");
            // View Payments
            ResultSet rsPayment = stmt.executeQuery("SELECT * FROM Payment");
            while (rsPayment.next()) {
                System.out.printf("Payment ID: %d | Student ID: %d | Amount: %.2f | Status: %s%n",
                        rsPayment.getInt("payment_id"),
                        rsPayment.getInt("student_id"),
                        rsPayment.getDouble("amount"),
                        rsPayment.getString("status"));
            }
            rsPayment.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
