import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class JeeDashboard extends JFrame {

    // Database Credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/jee_db";
    private static final String USER = "root";
    private static final String PASS = "12345"; // Updated to match your first connection

    // UI Components
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    // State
    private int currentStudentId = -1;
    private DefaultComboBoxModel<String> centerModel = new DefaultComboBoxModel<>();

    public JeeDashboard() {
        setTitle("JEE Application Dashboard");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add the different screens (cards)
        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createRegistrationPanel(), "Registration");
        mainPanel.add(createPaymentPanel(), "Payment");
        mainPanel.add(createCenterSelectionPanel(), "CenterSelection");
        mainPanel.add(createStatusPanel(), "Status");
        mainPanel.add(createMockExamPanel(), "MockExam");

        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(new JLabel("Application ID:"));
        JTextField idField = new JTextField();
        panel.add(idField);

        panel.add(new JLabel("Email Address:"));
        JTextField emailField = new JTextField();
        panel.add(emailField);

        JButton loginBtn = new JButton("Login");
        JButton registerRedirectBtn = new JButton("New Registration");

        loginBtn.addActionListener(e -> {
            try {
                int studentId = Integer.parseInt(idField.getText());
                String email = emailField.getText();

                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                    String sql = "SELECT s.name, p.status FROM Student s LEFT JOIN Payment p ON s.student_id = p.student_id WHERE s.student_id = ? AND s.email = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, studentId);
                        pstmt.setString(2, email);
                        ResultSet rs = pstmt.executeQuery();

                        if (rs.next()) {
                            currentStudentId = studentId;
                            String paymentStatus = rs.getString("status");
                            
                            JOptionPane.showMessageDialog(this, "Welcome Back, " + rs.getString("name") + "!");
                            
                            if ("SUCCESS".equals(paymentStatus)) {
                                checkCenterAndProceed();
                            } else {
                                int choice = JOptionPane.showConfirmDialog(this, "Your payment is pending. Proceed to payment?", "Payment Required", JOptionPane.YES_NO_OPTION);
                                if (choice == JOptionPane.YES_OPTION) {
                                    cardLayout.show(mainPanel, "Payment");
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid ID or Email. Student not found!");
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Application ID must be a number!");
            }
        });

        registerRedirectBtn.addActionListener(e -> cardLayout.show(mainPanel, "Registration"));

        panel.add(loginBtn);
        panel.add(registerRedirectBtn);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel successLabel = new JLabel("✅ Application & Payment Completed Successfully!", SwingConstants.CENTER);
        successLabel.setFont(new Font("Arial", Font.BOLD, 14));
        successLabel.setForeground(new Color(0, 153, 0));

        JButton takeExamBtn = new JButton("Take Exam");
        takeExamBtn.setBackground(new Color(50, 150, 250));
        takeExamBtn.setForeground(Color.WHITE);

        JButton checkResultsBtn = new JButton("Check Results & Rank");

        JButton logoutBtn = new JButton("Logout");

        // Take Exam logic
        takeExamBtn.addActionListener(e -> {
            boolean alreadyTaken = false;
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Result WHERE student_id = ?")) {
                pstmt.setInt(1, currentStudentId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) alreadyTaken = true;
            } catch (SQLException ex) { ex.printStackTrace(); }

            if (alreadyTaken) {
                JOptionPane.showMessageDialog(this, "You have already completed the exam!");
            } else {
                mainPanel.add(createDynamicMockExamPanel(), "DynamicMockExam");
                cardLayout.show(mainPanel, "DynamicMockExam");
            }
        });

        // Results logic
        checkResultsBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 Statement stmt = conn.createStatement();
                 ResultSet rsStatus = stmt.executeQuery("SELECT results_released FROM App_Status WHERE id = 1")) {
                 
                if (rsStatus.next() && rsStatus.getBoolean("results_released")) {
                    try (PreparedStatement checkStmt = conn.prepareStatement("SELECT marks, exam_rank FROM Result WHERE student_id = ?")) {
                        checkStmt.setInt(1, currentStudentId);
                        ResultSet rsResult = checkStmt.executeQuery();
                        if (rsResult.next()) {
                            int marks = rsResult.getInt("marks");
                            int rank = rsResult.getInt("exam_rank");
                            JOptionPane.showMessageDialog(this, "YOUR RESULTS:\nMarks: " + marks + " / 12\nAll India Rank: " + rank, "Results Published!", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "You didn't take the exam.");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Results have not been released by the Admin yet.");
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        logoutBtn.addActionListener(e -> {
            currentStudentId = -1;
            cardLayout.show(mainPanel, "Login");
        });

        panel.add(successLabel);
        panel.add(takeExamBtn);
        panel.add(checkResultsBtn);
        panel.add(logoutBtn);

        return panel;
    }

    private JPanel createMockExamPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("JEE Mock Exam - Answer the following:"));
        panel.add(Box.createVerticalStrut(10));

        // Question 1
        panel.add(new JLabel("Q1: What is the unit of Force?"));
        JRadioButton q1A = new JRadioButton("Joule");
        JRadioButton q1B = new JRadioButton("Newton"); // Correct
        JRadioButton q1C = new JRadioButton("Watt");
        ButtonGroup bg1 = new ButtonGroup();
        bg1.add(q1A); bg1.add(q1B); bg1.add(q1C);
        panel.add(q1A); panel.add(q1B); panel.add(q1C);
        panel.add(Box.createVerticalStrut(10));

        // Question 2
        panel.add(new JLabel("Q2: Chemical formula of Water?"));
        JRadioButton q2A = new JRadioButton("H2O"); // Correct
        JRadioButton q2B = new JRadioButton("CO2");
        JRadioButton q2C = new JRadioButton("O2");
        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(q2A); bg2.add(q2B); bg2.add(q2C);
        panel.add(q2A); panel.add(q2B); panel.add(q2C);
        panel.add(Box.createVerticalStrut(10));

        // Question 3
        panel.add(new JLabel("Q3: Value of Pi (approx)?"));
        JRadioButton q3A = new JRadioButton("3.14"); // Correct
        JRadioButton q3B = new JRadioButton("2.71");
        JRadioButton q3C = new JRadioButton("9.81");
        ButtonGroup bg3 = new ButtonGroup();
        bg3.add(q3A); bg3.add(q3B); bg3.add(q3C);
        panel.add(q3A); panel.add(q3B); panel.add(q3C);
        panel.add(Box.createVerticalStrut(15));

        JButton submitExamBtn = new JButton("Submit Exam");
        submitExamBtn.addActionListener(e -> {
            int marks = 0;
            // Basic logic: +4 for correct, -1 for wrong, 0 for unattempted
            
            if (q1B.isSelected()) marks += 4;
            else if (q1A.isSelected() || q1C.isSelected()) marks -= 1;

            if (q2A.isSelected()) marks += 4;
            else if (q2B.isSelected() || q2C.isSelected()) marks -= 1;

            if (q3A.isSelected()) marks += 4;
            else if (q3B.isSelected() || q3C.isSelected()) marks -= 1;

            // Save to DB
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String sql = "INSERT INTO Result (student_id, marks) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, currentStudentId);
                    pstmt.setInt(2, marks);
                    pstmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Exam Submitted Successfully!");
                cardLayout.show(mainPanel, "Status");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error while saving exam: " + ex.getMessage());
            }
        });

        panel.add(submitExamBtn);
        return panel;
    }

    private JPanel createDynamicMockExamPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("JEE Mock Exam - Answer the following:"));
        panel.add(Box.createVerticalStrut(10));

        // Create an array or lists to store the correct answers and the button groups
        java.util.List<ButtonGroup> buttonGroups = new java.util.ArrayList<>();
        java.util.List<String> correctOptions = new java.util.ArrayList<>();
        java.util.List<java.util.AbstractMap.SimpleEntry<String, JRadioButton>> selectedOptions = new java.util.ArrayList<>();

        int qNumber = 1;
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             // Fetch 3 random questions from the Question Bank
             ResultSet rs = stmt.executeQuery("SELECT * FROM Question_Bank ORDER BY RAND() LIMIT 3")) {
            
            while (rs.next()) {
                String questionText = rs.getString("question");
                String optA = rs.getString("opt_a");
                String optB = rs.getString("opt_b");
                String optC = rs.getString("opt_c");
                String optD = rs.getString("opt_d");
                String correctOpt = rs.getString("correct_opt");

                panel.add(new JLabel("Q" + qNumber + ": " + questionText));
                
                JRadioButton rbA = new JRadioButton("A) " + optA);
                JRadioButton rbB = new JRadioButton("B) " + optB);
                JRadioButton rbC = new JRadioButton("C) " + optC);
                JRadioButton rbD = new JRadioButton("D) " + optD);
                
                // Keep track of the radio buttons mapped to their option letter
                java.util.Map<String, JRadioButton> mapping = new java.util.HashMap<>();
                mapping.put("A", rbA);
                mapping.put("B", rbB);
                mapping.put("C", rbC);
                mapping.put("D", rbD);
                
                ButtonGroup bg = new ButtonGroup();
                bg.add(rbA); bg.add(rbB); bg.add(rbC); bg.add(rbD);
                
                buttonGroups.add(bg);
                correctOptions.add(correctOpt);
                // We'll store this so we can check it later
                selectedOptions.add(new java.util.AbstractMap.SimpleEntry<>(correctOpt, null));
                
                // Need a final map for the loop scope
                final java.util.Map<String, JRadioButton> finalMapping = mapping;
                final int idx = qNumber - 1;
                
                // Add action listeners to save the selection
                rbA.addActionListener(e -> selectedOptions.set(idx, new java.util.AbstractMap.SimpleEntry<>(correctOptions.get(idx), rbA)));
                rbB.addActionListener(e -> selectedOptions.set(idx, new java.util.AbstractMap.SimpleEntry<>(correctOptions.get(idx), rbB)));
                rbC.addActionListener(e -> selectedOptions.set(idx, new java.util.AbstractMap.SimpleEntry<>(correctOptions.get(idx), rbC)));
                rbD.addActionListener(e -> selectedOptions.set(idx, new java.util.AbstractMap.SimpleEntry<>(correctOptions.get(idx), rbD)));
                
                panel.add(rbA);
                panel.add(rbB);
                panel.add(rbC);
                panel.add(rbD);
                panel.add(Box.createVerticalStrut(10));
                
                qNumber++;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            panel.add(new JLabel("Error loading questions."));
        }

        panel.add(Box.createVerticalStrut(15));
        JButton submitExamBtn = new JButton("Submit Exam");
        submitExamBtn.addActionListener(e -> {
            int marks = 0;
            // Iterate over selected options to calculate marks
            // +4 for correct, -1 for wrong
            for (java.util.AbstractMap.SimpleEntry<String, JRadioButton> entry : selectedOptions) {
                String correctAnswer = entry.getKey();
                JRadioButton chosenRb = entry.getValue();
                
                if (chosenRb != null) {
                    if (chosenRb.getText().startsWith(correctAnswer + ")")) {
                        marks += 4;
                    } else {
                        marks -= 1;
                    }
                }
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String sql = "INSERT INTO Result (student_id, marks) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, currentStudentId);
                    pstmt.setInt(2, marks);
                    pstmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Exam Submitted Successfully!");
                cardLayout.show(mainPanel, "Status");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error while saving exam: " + ex.getMessage());
            }
        });

        panel.add(submitExamBtn);
        return panel;
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Full Name:"));
        JTextField nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Category:"));
        String[] categories = {"GENERAL", "OBC", "SC", "ST"};
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        panel.add(categoryBox);

        panel.add(new JLabel("No. of Attempts:"));
        JTextField attemptsField = new JTextField();
        panel.add(attemptsField);

        panel.add(new JLabel("Email Address:"));
        JTextField emailField = new JTextField();
        panel.add(emailField);

        JButton registerBtn = new JButton("Register & Pay");
        
        registerBtn.addActionListener(e -> {
            String name = nameField.getText();
            String category = (String) categoryBox.getSelectedItem();
            int attempts;
            try {
                attempts = Integer.parseInt(attemptsField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Attempts must be a number!");
                return;
            }
            String email = emailField.getText();

            if (name.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields!");
                return;
            }

            // Database Insert
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String sql = "INSERT INTO Student (name, category, attempts, email) VALUES (?, ?, ?, ?)";
                // RETURN_GENERATED_KEYS is needed to get the auto-incremented student_id back
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, category);
                    pstmt.setInt(3, attempts);
                    pstmt.setString(4, email);
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            currentStudentId = rs.getInt(1);
                            JOptionPane.showMessageDialog(this, "Registration Successful! Student ID: " + currentStudentId);
                            // Switch to Payment Screen
                            cardLayout.show(mainPanel, "Payment");
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel()); // Empty spacer
        panel.add(registerBtn);

        return panel;
    }

    private JPanel createPaymentPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Registration Fee:"));
        panel.add(new JLabel("₹1000.00")); // Standard fee

        panel.add(new JLabel("Payment Method:"));
        String[] methods = {"Credit Card", "Debit Card", "UPI", "Net Banking"};
        JComboBox<String> methodBox = new JComboBox<>(methods);
        panel.add(methodBox);

        JButton payBtn = new JButton("Make Payment");
        
        payBtn.addActionListener(e -> {
            if (currentStudentId == -1) {
                JOptionPane.showMessageDialog(this, "No student registered!");
                return;
            }

            // Database Insert for Payment
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String sql = "INSERT INTO Payment (student_id, amount, status) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, currentStudentId);
                    pstmt.setDouble(2, 1000.00);
                    pstmt.setString(3, "SUCCESS"); // Simulating successful payment
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Payment Successful! Please select an exam center.");
                    checkCenterAndProceed();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel()); // Empty spacer
        panel.add(payBtn);

        return panel;
    }

    private void checkCenterAndProceed() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "SELECT * FROM Center_Allocation WHERE student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentStudentId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Already has a center
                    cardLayout.show(mainPanel, "Status");
                } else {
                    // Load centers and show selection
                    loadAvailableCenters();
                    cardLayout.show(mainPanel, "CenterSelection");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAvailableCenters() {
        centerModel.removeAllElements();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT center_id, city_name, available_seats FROM Center WHERE available_seats > 0")) {
            
            while (rs.next()) {
                centerModel.addElement(rs.getInt("center_id") + " - " + rs.getString("city_name") + " (" + rs.getInt("available_seats") + " seats)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private JPanel createCenterSelectionPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Select your preferred Exam Center:", SwingConstants.CENTER));
        
        JComboBox<String> centerBox = new JComboBox<>(centerModel);
        panel.add(centerBox);

        JButton confirmBtn = new JButton("Confirm Center");
        confirmBtn.addActionListener(e -> {
            String selected = (String) centerBox.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "No centers available!");
                return;
            }

            int centerId = Integer.parseInt(selected.split(" - ")[0]);

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                conn.setAutoCommit(false); // Transaction mode

                try {
                    // 1. Allocate Center
                    String insertSql = "INSERT INTO Center_Allocation (student_id, center_id) VALUES (?, ?)";
                    try (PreparedStatement checkStmt = conn.prepareStatement(insertSql)) {
                        checkStmt.setInt(1, currentStudentId);
                        checkStmt.setInt(2, centerId);
                        checkStmt.executeUpdate();
                    }

                    // 2. Reduce seats
                    String updateSql = "UPDATE Center SET available_seats = available_seats - 1 WHERE center_id = ? AND available_seats > 0";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, centerId);
                        int rowsAffected = updateStmt.executeUpdate();
                        if (rowsAffected == 0) {
                            throw new SQLException("Seats just filled up!");
                        }
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Center Confirmed Successfully!");
                    cardLayout.show(mainPanel, "Status");

                } catch (SQLException ex) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "Failed to allocate center: " + ex.getMessage());
                    loadAvailableCenters(); // Reload list
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error!");
            }
        });

        panel.add(confirmBtn);
        return panel;
    }

    public static void main(String[] args) {
        // Run GUI in the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new JeeDashboard().setVisible(true);
        });
    }
}
