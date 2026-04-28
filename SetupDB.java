import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SetupDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/";
        String user = "root";
        String pass = "12345";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            
         
            System.out.println("Dropping old database (if exists)...");
            stmt.executeUpdate("DROP DATABASE IF EXISTS jee_db");
            
            System.out.println("Creating fresh jee_db...");
            stmt.executeUpdate("CREATE DATABASE jee_db");
            stmt.executeUpdate("USE jee_db");
            
            // 2. Create Student Table
            System.out.println("Creating Student table...");
            stmt.executeUpdate("CREATE TABLE Student (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100)," +
                "category VARCHAR(20)," +
                "attempts INT," +
                "email VARCHAR(100)," +
                "registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // 3. Create Payment Table
            System.out.println("Creating Payment table...");
            stmt.executeUpdate("CREATE TABLE Payment (" +
                "payment_id INT AUTO_INCREMENT PRIMARY KEY," +
                "student_id INT," +
                "amount DECIMAL(10,2)," +
                "status VARCHAR(20)," +
                "payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE" +
            ")");
            
            // 4. Create Center Table
            System.out.println("Creating Center table...");
            stmt.executeUpdate("CREATE TABLE Center (" +
                "center_id INT AUTO_INCREMENT PRIMARY KEY," +
                "city_name VARCHAR(100)," +
                "total_seats INT," +
                "available_seats INT" +
            ")");
            
            // 5. Insert mock cities with limited seats
            System.out.println("Adding mock cities...");
            stmt.executeUpdate("INSERT INTO Center (city_name, total_seats, available_seats) VALUES " +
                "('New Delhi', 5, 5)," +      // Only 5 seats
                "('Mumbai', 3, 3)," +         // Only 3 seats
                "('Bangalore', 2, 2)," +      // Only 2 seats
                "('Chennai', 4, 4)," +        // Only 4 seats
                "('Kolkata', 1, 1)"           // Extremely limited, 1 seat!
            );
            
            // 6. Create Center Allocation Table
            System.out.println("Creating Center_Allocation table...");
            stmt.executeUpdate("CREATE TABLE Center_Allocation (" +
                "student_id INT PRIMARY KEY," +
                "center_id INT," +
                "FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE," +
                "FOREIGN KEY (center_id) REFERENCES Center(center_id) ON DELETE CASCADE" +
            ")");

            // 7. Create Result Table
            System.out.println("Creating Result table...");
            stmt.executeUpdate("CREATE TABLE Result (" +
                "student_id INT PRIMARY KEY," +
                "marks INT," +
                "exam_rank INT," +
                "FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE" +
            ")");

            // 8. Create Application Setting Status
            System.out.println("Creating App_Status table...");
            stmt.executeUpdate("CREATE TABLE App_Status (" +
                "id INT PRIMARY KEY," +
                "results_released BOOLEAN," +
                "counselling_started BOOLEAN," +
                "allotment_done BOOLEAN," +
                "current_round INT DEFAULT 0" +
            ")");
            stmt.executeUpdate("INSERT INTO App_Status (id, results_released, counselling_started, allotment_done, current_round) VALUES (1, false, false, false, 0)");

            // 8.5 Question Bank Table
            System.out.println("Creating Question_Bank table...");
            stmt.executeUpdate("CREATE TABLE Question_Bank (" +
                "q_id INT AUTO_INCREMENT PRIMARY KEY," +
                "question VARCHAR(500)," +
                "opt_a VARCHAR(200)," +
                "opt_b VARCHAR(200)," +
                "opt_c VARCHAR(200)," +
                "opt_d VARCHAR(200)," +
                "correct_opt CHAR(1)" +
            ")");
            
            // Insert default question bank
            System.out.println("Adding mock questions to the Question_Bank...");
            stmt.executeUpdate("INSERT INTO Question_Bank (question, opt_a, opt_b, opt_c, opt_d, correct_opt) VALUES " +
                "('What is the unit of Force?', 'Joule', 'Newton', 'Watt', 'Pascal', 'B')," +
                "('Chemical formula of Water?', 'H2O', 'CO2', 'O2', 'NaCl', 'A')," +
                "('Value of Pi (approx)?', '3.14', '9.81', '1.61', '2.71', 'A')," +
                "('Which element is a noble gas?', 'Oxygen', 'Nitrogen', 'Helium', 'Hydrogen', 'C')," +
                "('Speed of light in vacuum?', '3x10^8 m/s', '3x10^5 m/s', '1x10^8 m/s', 'None', 'A')"
            );

            // 9. Colleges Table
            System.out.println("Creating College table...");
            stmt.executeUpdate("CREATE TABLE College (" +
                "college_id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100)," +
                "type VARCHAR(20)," +
                "available_seats INT" +
            ")");
            // Colleges are now uploaded dynamically by Admin via CSV
            System.out.println("Adding mock colleges...");
            stmt.executeUpdate("INSERT INTO College (name, type, available_seats) VALUES ('IIT Bombay (CSE)', 'Govt', 2)");
            stmt.executeUpdate("INSERT INTO College (name, type, available_seats) VALUES ('IIT Bombay (EE)', 'Govt', 2)");
            stmt.executeUpdate("INSERT INTO College (name, type, available_seats) VALUES ('IIT Delhi (CSE)', 'Govt', 2)");
            stmt.executeUpdate("INSERT INTO College (name, type, available_seats) VALUES ('NIT Trichy (CSE)', 'Govt', 2)");
            stmt.executeUpdate("INSERT INTO College (name, type, available_seats) VALUES ('BITS Pilani (CSE)', 'Govt', 2)");

            // 10. Candidate Preferences for Counselling
            System.out.println("Creating Preference table...");
            stmt.executeUpdate("CREATE TABLE Preference (" +
                "pref_id INT AUTO_INCREMENT PRIMARY KEY," +
                "student_id INT," +
                "college_id INT," +
                "pref_order INT," +
                "FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE," +
                "FOREIGN KEY (college_id) REFERENCES College(college_id) ON DELETE CASCADE" +
            ")");

            // 11. Final Allotment
            System.out.println("Creating Allotment table...");
            stmt.executeUpdate("CREATE TABLE Allotment (" +
                "student_id INT PRIMARY KEY," +
                "college_id INT," +
                "status VARCHAR(20) DEFAULT 'PENDING'," +
                "round_allotted INT DEFAULT 1," +
                "FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE," +
                "FOREIGN KEY (college_id) REFERENCES College(college_id) ON DELETE CASCADE" +
            ")");

            // 12. Add Triggers (Audit logs and Data Validation)
            System.out.println("Creating triggers...");
            stmt.executeUpdate("CREATE TABLE Audit_Log (" +
                "log_id INT AUTO_INCREMENT PRIMARY KEY," +
                "action VARCHAR(255)," +
                "log_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

            // --- Audit Triggers ---
            stmt.executeUpdate("CREATE TRIGGER after_student_insert " +
                "AFTER INSERT ON Student FOR EACH ROW " +
                "INSERT INTO Audit_Log (action) VALUES (CONCAT('New student registered: ', NEW.name))"
            );

            stmt.executeUpdate("CREATE TRIGGER after_payment_insert " +
                "AFTER INSERT ON Payment FOR EACH ROW " +
                "INSERT INTO Audit_Log (action) VALUES (CONCAT('Payment of Rs.', NEW.amount, ' received from Student ID: ', NEW.student_id))"
            );

            stmt.executeUpdate("CREATE TRIGGER after_seat_allotment " +
                "AFTER INSERT ON Allotment FOR EACH ROW " +
                "INSERT INTO Audit_Log (action) VALUES (CONCAT('Student ID ', NEW.student_id, ' allotted seat at College ID ', NEW.college_id))"
            );

            // --- Formatting & Constraint Triggers ---
            // Validate Email on Insert
            stmt.executeUpdate("CREATE TRIGGER before_student_insert_email " +
                "BEFORE INSERT ON Student FOR EACH ROW " +
                "BEGIN " +
                "  IF NEW.email NOT LIKE '%_@__%.__%' THEN " +
                "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid email format! Email must contain @ and a domain.'; " +
                "  END IF; " +
                "END"
            );

            // Validate Email on Update
            stmt.executeUpdate("CREATE TRIGGER before_student_update_email " +
                "BEFORE UPDATE ON Student FOR EACH ROW " +
                "BEGIN " +
                "  IF NEW.email NOT LIKE '%_@__%.__%' THEN " +
                "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid email format! Email must contain @ and a domain.'; " +
                "  END IF; " +
                "END"
            );

            System.out.println("✅ Database reset successfully! All tables including Counselling and Triggers are ready.");

        } catch(Exception e) { 
            e.printStackTrace(); 
        }
    }
}
