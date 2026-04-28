import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {
    
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/jee_db";
    private static final String USER = "root";
    private static final String PASS = "12345"; 

    private JTable table;
    private DefaultTableModel tableModel;

    public AdminDashboard() {
        setTitle("JEE Admin Panel - Live Registrations");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null); 


        tableModel = new DefaultTableModel(new String[]{
            "Student ID", "Full Name", "Email Address", "Category", "Attempts", "Payment Status", "Amount (₹)", "Exam Center", "Marks", "Rank", "Allotted College"
        }, 0);
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        JButton refreshBtn = new JButton(" Refresh Data");
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 14));
        refreshBtn.addActionListener(e -> loadData());

        JButton releaseResultsBtn = new JButton(" Release Results");
        releaseResultsBtn.setFont(new Font("Arial", Font.BOLD, 14));
        releaseResultsBtn.setBackground(new Color(200, 50, 50));
        releaseResultsBtn.setForeground(Color.WHITE);
        releaseResultsBtn.addActionListener(e -> releaseResults());

        JButton startCounsellingBtn = new JButton("> Start Counselling");
        startCounsellingBtn.setBackground(new Color(50, 150, 200));
        startCounsellingBtn.setForeground(Color.WHITE);
        startCounsellingBtn.addActionListener(e -> startCounselling());

        JButton runAllotmentBtn = new JButton("Run Allotment");
        runAllotmentBtn.setBackground(new Color(50, 200, 50));
        runAllotmentBtn.setForeground(Color.WHITE);
        runAllotmentBtn.addActionListener(e -> runAllotment());

        JButton uploadQuestionBtn = new JButton("Manage Q-Bank");
        uploadQuestionBtn.setBackground(new Color(150, 50, 200));
        uploadQuestionBtn.setForeground(Color.WHITE);
        uploadQuestionBtn.addActionListener(e -> openQuestionManager());

        JButton uploadCollegesBtn = new JButton("Manage Colleges");
        uploadCollegesBtn.setBackground(new Color(200, 100, 50));
        uploadCollegesBtn.setForeground(Color.WHITE);
        uploadCollegesBtn.addActionListener(e -> openCollegesManager());

        JButton resetBtn = new JButton("Master Reset");
        resetBtn.setBackground(new Color(220, 20, 60));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.addActionListener(e -> resetStudentData());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(refreshBtn);
        bottomPanel.add(uploadCollegesBtn);
        bottomPanel.add(uploadQuestionBtn);
        bottomPanel.add(releaseResultsBtn);
        bottomPanel.add(startCounsellingBtn);
        bottomPanel.add(runAllotmentBtn);
        bottomPanel.add(resetBtn);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Admin Overview of Registered Candidates"));
        
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadData();
    }

    private void releaseResults() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            
            // 1. Calculate ranks based on marks dynamically and update the table
            String rankSql = "UPDATE Result r " +
                             "JOIN (SELECT student_id, RANK() OVER(ORDER BY marks DESC) as rnk FROM Result) temp " +
                             "ON r.student_id = temp.student_id " +
                             "SET r.exam_rank = temp.rnk";
            stmt.executeUpdate(rankSql);

            // 2. Set boolean flag so students can see it
            stmt.executeUpdate("UPDATE App_Status SET results_released = true WHERE id = 1");
            
            JOptionPane.showMessageDialog(this, "Results & Ranks have been successfully released to candidates!");
            loadData(); // Refresh to show ranks

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error releasing results: " + ex.getMessage());
        }
    }

    private void resetStudentData() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "WARNING: This will permanently delete ALL registered students, payments, exam results, and seat allotments.\nColleges and Question Banks will be preserved.\nDo you want to continue?", 
            "Master Reset Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    
                    // Restore college seats based on existing allotments before deletion
                    stmt.executeUpdate("UPDATE College c " +
                                       "JOIN (SELECT college_id, COUNT(*) as c_count FROM Allotment GROUP BY college_id) a " +
                                       "ON c.college_id = a.college_id " +
                                       "SET c.available_seats = c.available_seats + a.c_count");
                    
                    // Restore center seats directly from total_seats
                    stmt.executeUpdate("UPDATE Center SET available_seats = total_seats");
                    
                    // Turn off foreign key checks temporarily to wipe data faster
                    stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
                    
                    // Delete all student-generated data
                    stmt.executeUpdate("TRUNCATE TABLE Allotment");
                    stmt.executeUpdate("TRUNCATE TABLE Preference");
                    stmt.executeUpdate("TRUNCATE TABLE Result");
                    stmt.executeUpdate("TRUNCATE TABLE Center_Allocation");
                    stmt.executeUpdate("TRUNCATE TABLE Payment");
                    stmt.executeUpdate("TRUNCATE TABLE Audit_Log"); 
                    stmt.executeUpdate("TRUNCATE TABLE Student");
                    
                    // Turn foreign key checks back on
                    stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
                    
                    // Reset application flags so exams/counselling can be retaken
                    stmt.executeUpdate("UPDATE App_Status SET results_released = false, counselling_started = false, allotment_done = false WHERE id = 1");
                    
                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Master Reset successful! The application is swept clean for new candidates.");
                    loadData();
                } catch (SQLException ex) {
                    conn.rollback();
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Reset failed: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Connection Error: " + ex.getMessage());
            }
        }
    }

    private void openQuestionManager() {
        JDialog dialog = new JDialog(this, "Manage Question Bank", true);
        dialog.setSize(450, 450);
        dialog.setLayout(new GridLayout(10, 2, 5, 5));
        
        dialog.add(new JLabel(" Question Text:"));
        JTextField txtQ = new JTextField();
        dialog.add(txtQ);
        
        dialog.add(new JLabel(" Option A:"));
        JTextField txtA = new JTextField();
        dialog.add(txtA);

        dialog.add(new JLabel(" Option B:"));
        JTextField txtB = new JTextField();
        dialog.add(txtB);

        dialog.add(new JLabel(" Option C:"));
        JTextField txtC = new JTextField();
        dialog.add(txtC);

        dialog.add(new JLabel(" Option D:"));
        JTextField txtD = new JTextField();
        dialog.add(txtD);
        
        dialog.add(new JLabel(" Correct Option (A/B/C/D):"));
        JComboBox<String> cmbCorrect = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        dialog.add(cmbCorrect);

        JButton btnSave = new JButton("Save Single Question");
        btnSave.setBackground(new Color(0, 150, 0));
        btnSave.setForeground(Color.WHITE);
        dialog.add(new JLabel(" — OR —")); 
        dialog.add(btnSave);

        JButton btnUploadCSV = new JButton("Upload CSV File");
        btnUploadCSV.setBackground(new Color(50, 100, 200));
        btnUploadCSV.setForeground(Color.WHITE);
        dialog.add(new JLabel(" Bulk Upload questions:"));
        dialog.add(btnUploadCSV);

        // Add a view all button
        JButton btnView = new JButton("View Library Size");
        dialog.add(new JLabel(" Data Status:"));
        dialog.add(btnView);

        btnSave.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Question_Bank (question, opt_a, opt_b, opt_c, opt_d, correct_opt) VALUES (?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, txtQ.getText());
                pstmt.setString(2, txtA.getText());
                pstmt.setString(3, txtB.getText());
                pstmt.setString(4, txtC.getText());
                pstmt.setString(5, txtD.getText());
                pstmt.setString(6, cmbCorrect.getSelectedItem().toString());
                pstmt.executeUpdate();
                
                JOptionPane.showMessageDialog(dialog, "Question successfully added to bank!");
                
                // Clear fields
                txtQ.setText(""); txtA.setText(""); txtB.setText(""); txtC.setText(""); txtD.setText("");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        btnUploadCSV.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a CSV file");
            // Basic CSV Filter
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
            
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                int successCount = 0;
                int failCount = 0;
                boolean isFirstLine = true;
                
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                     Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                     PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Question_Bank (question, opt_a, opt_b, opt_c, opt_d, correct_opt) VALUES (?, ?, ?, ?, ?, ?)")) {
                    
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            // simple split by comma, ignoring commas inside quotes
                            String[] data = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                            
                            // Check if it looks like a header (e.g. contains 'question') or has wrong column count
                            if (data.length < 6) {
                                failCount++;
                                continue;
                            }
                            
                            // strip quotes and trim 
                            for (int i=0; i<data.length; i++) {
                                data[i] = data[i].trim().replaceAll("^\"|\"$", "");
                            }

                            if (isFirstLine && data[0].toLowerCase().contains("question")) {
                                isFirstLine = false;
                                continue; // Skip header
                            }
                            isFirstLine = false;

                            pstmt.setString(1, data[0]); // Question
                            pstmt.setString(2, data[1]); // Opt A
                            pstmt.setString(3, data[2]); // Opt B
                            pstmt.setString(4, data[3]); // Opt C
                            pstmt.setString(5, data[4]); // Opt D
                            pstmt.setString(6, data[5].toUpperCase()); // Correct Option
                            pstmt.executeUpdate();
                            successCount++;

                        } catch (Exception parseEx) {
                            failCount++;
                        }
                    }
                    JOptionPane.showMessageDialog(dialog, "CSV Upload Complete!\nSuccessfully added: " + successCount + " questions.\nFailed/Skipped: " + failCount + " rows.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Error reading file or connecting to database: " + ex.getMessage(), "Upload Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnView.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM Question_Bank")) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(dialog, "Total Questions in Bank: " + rs.getInt("count"));
                }
            } catch(Exception ex) {}
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openCollegesManager() {
        JDialog dialog = new JDialog(this, "Manage Colleges", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));
        
        dialog.add(new JLabel(" College Name (e.g. IIT Bombay):"));
        JTextField txtName = new JTextField();
        dialog.add(txtName);
        
        dialog.add(new JLabel(" Type (IIT, NIT, IIIT):"));
        JTextField txtType = new JTextField();
        dialog.add(txtType);

        dialog.add(new JLabel(" Total Available Seats:"));
        JTextField txtSeats = new JTextField();
        dialog.add(txtSeats);

        JButton btnSave = new JButton("Save Single College");
        btnSave.setBackground(new Color(0, 150, 0));
        btnSave.setForeground(Color.WHITE);
        dialog.add(new JLabel(" — OR —")); 
        dialog.add(btnSave);

        JButton btnUploadCSV = new JButton("Upload CSV File");
        btnUploadCSV.setBackground(new Color(50, 100, 200));
        btnUploadCSV.setForeground(Color.WHITE);
        dialog.add(new JLabel(" Bulk Upload Colleges:"));
        dialog.add(btnUploadCSV);

        JButton btnView = new JButton("View College Count");
        dialog.add(new JLabel(" Data Status:"));
        dialog.add(btnView);

        btnSave.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO College (name, type, available_seats) VALUES (?, ?, ?)")) {
                pstmt.setString(1, txtName.getText());
                pstmt.setString(2, txtType.getText());
                pstmt.setInt(3, Integer.parseInt(txtSeats.getText()));
                pstmt.executeUpdate();
                
                JOptionPane.showMessageDialog(dialog, "College successfully added!");
                txtName.setText(""); txtType.setText(""); txtSeats.setText("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database Error: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Seats must be a number!");
            }
        });

        btnUploadCSV.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a Colleges CSV file");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
            
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                int successCount = 0;
                int failCount = 0;
                boolean isFirstLine = true;
                
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                     Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                     PreparedStatement pstmt = conn.prepareStatement("INSERT INTO College (name, type, available_seats) VALUES (?, ?, ?)")) {
                    
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            String[] data = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                            
                            if (data.length < 3) {
                                failCount++;
                                continue;
                            }
                            
                            for (int i=0; i<data.length; i++) {
                                data[i] = data[i].trim().replaceAll("^\"|\"$", "");
                            }

                            if (isFirstLine && (data[0].toLowerCase().contains("name") || data[0].toLowerCase().contains("college"))) {
                                isFirstLine = false;
                                continue; // Skip header
                            }
                            isFirstLine = false;

                            pstmt.setString(1, data[0]); // College Name
                            pstmt.setString(2, data[1]); // Type (IIT/NIT)
                            pstmt.setInt(3, Integer.parseInt(data[2])); // Available Seats
                            pstmt.executeUpdate();
                            successCount++;

                        } catch (Exception parseEx) {
                            failCount++;
                        }
                    }
                    JOptionPane.showMessageDialog(dialog, "CSV Upload Complete!\nSuccessfully added: " + successCount + " colleges.\nFailed/Skipped: " + failCount + " rows.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error reading file or connecting to database: " + ex.getMessage(), "Upload Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnView.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM College")) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(dialog, "Total Colleges Registered: " + rs.getInt("count"));
                }
            } catch(Exception ex) {}
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void loadData() {

        tableModel.setRowCount(0); 

            String sql = "SELECT s.student_id, s.name, s.email, s.category, s.attempts, " +
                     "COALESCE(p.status, 'PENDING') as status, " +
                     "COALESCE(p.amount, 0.00) as amount, " +
                     "COALESCE(c.city_name, 'Not Selected') as center, " +
                     "COALESCE(CAST(r.marks AS CHAR), 'N/A') as marks, " +
                     "COALESCE(CAST(r.exam_rank AS CHAR), 'N/A') as exam_rank, " +
                     "COALESCE(col.name, 'Not Allotted') as allotted_college " +
                     "FROM Student s " +
                     "LEFT JOIN Payment p ON s.student_id = p.student_id " +
                     "LEFT JOIN Center_Allocation ca ON s.student_id = ca.student_id " +
                     "LEFT JOIN Center c ON ca.center_id = c.center_id " +
                     "LEFT JOIN Result r ON s.student_id = r.student_id " +
                     "LEFT JOIN Allotment a ON s.student_id = a.student_id " +
                     "LEFT JOIN College col ON a.college_id = col.college_id " +
                     "ORDER BY s.student_id DESC"; 

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("category"),
                    rs.getInt("attempts"),
                    rs.getString("status"),
                    rs.getDouble("amount"),
                    rs.getString("center"),
                    rs.getString("marks"),
                    rs.getString("exam_rank"),
                    rs.getString("allotted_college")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void startCounselling() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate("UPDATE App_Status SET counselling_started = true WHERE id = 1");
            JOptionPane.showMessageDialog(this, "Counselling has been enabled for students!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void runAllotment() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            
            // Seat Allocation Algorithm based on Ranks + Preferences
            String sql = "SELECT s.student_id, r.exam_rank FROM Student s " +
                         "JOIN Result r ON s.student_id = r.student_id " +
                         "ORDER BY r.exam_rank ASC";

            conn.setAutoCommit(false);
            try {
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    
                    // Get preferences for this student
                    try (PreparedStatement prefStmt = conn.prepareStatement("SELECT college_id FROM Preference WHERE student_id = ? ORDER BY pref_order ASC")) {
                        prefStmt.setInt(1, studentId);
                        ResultSet prefRs = prefStmt.executeQuery();
                        
                        while (prefRs.next()) {
                            int collegeId = prefRs.getInt("college_id");
                            
                            // Check available seats in college
                            try (PreparedStatement seatStmt = conn.prepareStatement("SELECT available_seats FROM College WHERE college_id = ? FOR UPDATE")) {
                                seatStmt.setInt(1, collegeId);
                                ResultSet seatRs = seatStmt.executeQuery();
                                if (seatRs.next() && seatRs.getInt("available_seats") > 0) {
                                    // Allocate this college
                                    try (PreparedStatement allotStmt = conn.prepareStatement("INSERT INTO Allotment (student_id, college_id) VALUES (?, ?)")) {
                                        allotStmt.setInt(1, studentId);
                                        allotStmt.setInt(2, collegeId);
                                        allotStmt.executeUpdate();
                                    }
                                    // Drop available seat
                                    try (PreparedStatement dropSeatStmt = conn.prepareStatement("UPDATE College SET available_seats = available_seats - 1 WHERE college_id = ?")) {
                                        dropSeatStmt.setInt(1, collegeId);
                                        dropSeatStmt.executeUpdate();
                                    }
                                    break; // Break since allotted to stop checking more preferences
                                }
                            }
                        }
                    }
                }
                
                stmt.executeUpdate("UPDATE App_Status SET allotment_done = true WHERE id = 1");
                conn.commit();
                JOptionPane.showMessageDialog(this, "Allotment simulation ran successfully! Seats have been assigned based on merit & preference.");
                loadData(); // To refresh table (if college column is added)
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdminDashboard().setVisible(true);
        });
    }
}
