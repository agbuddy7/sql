import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class CounsellingDashboard extends JFrame {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/jee_db";
    private static final String USER = "root";
    private static final String PASS = "12345";

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private int currentStudentId = -1;

    private DefaultComboBoxModel<String> collegeModel1 = new DefaultComboBoxModel<>();
    private DefaultComboBoxModel<String> collegeModel2 = new DefaultComboBoxModel<>();
    private DefaultComboBoxModel<String> collegeModel3 = new DefaultComboBoxModel<>();

    private JLabel allotCollegeLabel = new JLabel("", SwingConstants.CENTER);
    private JButton freezeBtn = new JButton("FREEZE (Lock Seat)");
    private JButton floatBtn = new JButton("FLOAT (Try for better preference)");
    private JButton slideBtn = new JButton("SLIDE (Try for another branch in same college)");

    public CounsellingDashboard() {
        setTitle("JEE Counselling Portal");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createCounsellingPanel(), "Counselling");
        mainPanel.add(createAllotmentPanel(), "Allotment");
        mainPanel.add(createStatusPanel(), "Status");

        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(new JLabel("Application ID:"));
        JTextField idField = new JTextField();
        panel.add(idField);

        panel.add(new JLabel("Email Address:"));
        JTextField emailField = new JTextField();
        panel.add(emailField);

        JButton loginBtn = new JButton("Check Eligibility & Login");
        loginBtn.setBackground(new Color(50, 150, 200));
        loginBtn.setForeground(Color.WHITE);

        loginBtn.addActionListener(e -> {
            try {
                int studentId = Integer.parseInt(idField.getText());
                String email = emailField.getText();

                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                    // Check if results are out
                    Statement stmt = conn.createStatement();
                    ResultSet rsStatus = stmt.executeQuery("SELECT results_released, counselling_started, allotment_done FROM App_Status WHERE id = 1");
                    
                    if (rsStatus.next()) {
                        if (!rsStatus.getBoolean("results_released")) {
                            JOptionPane.showMessageDialog(this, "Results have not been released by the Admin yet.");
                            return;
                        }
                    }

                    // Look up student details, marks, rank
                    String sql = "SELECT s.name, r.exam_rank, r.marks, " +
                                 "(SELECT COUNT(*) FROM Result) as total_students " +
                                 "FROM Student s JOIN Result r ON s.student_id = r.student_id " +
                                 "WHERE s.student_id = ? AND s.email = ?";
                                 
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, studentId);
                        pstmt.setString(2, email);
                        ResultSet rs = pstmt.executeQuery();

                        if (rs.next()) {
                            int rank = rs.getInt("exam_rank");
                            int totalStudents = rs.getInt("total_students");
                            
                            // Calculate Top 10% cutoff
                            int cutoffRank = (int) Math.ceil(totalStudents * 0.10);
                            if (cutoffRank == 0) cutoffRank = 1; // Safety fallback

                            if (rank <= cutoffRank) {
                                currentStudentId = studentId;
                                JOptionPane.showMessageDialog(this, "Welcome " + rs.getString("name") + "!\nYour Rank: " + rank + 
                                    "\nStatus: QUALIFIED for Counselling!");
                                
                                // Check if already allotted
                                ResultSet rsAllot = stmt.executeQuery("SELECT a.status, c.name FROM Allotment a JOIN College c ON a.college_id = c.college_id WHERE student_id = " + currentStudentId);
                                if (rsAllot.next()) {
                                    String curStatus = rsAllot.getString("status");
                                    allotCollegeLabel.setText(rsAllot.getString("name") + " [" + curStatus + "]");

                                    if ("FREEZE".equals(curStatus)) {
                                        freezeBtn.setEnabled(false);
                                        floatBtn.setEnabled(false);
                                        slideBtn.setEnabled(false);
                                    } else {
                                        freezeBtn.setEnabled(true);
                                        floatBtn.setEnabled(true);
                                        slideBtn.setEnabled(true);
                                    }

                                    JOptionPane.showMessageDialog(this, "Seat allotted! Please select your preference.", "Allotment Result", JOptionPane.INFORMATION_MESSAGE);
                                    cardLayout.show(mainPanel, "Allotment");
                                    return;
                                }

                                ResultSet rsSettings = stmt.executeQuery("SELECT counselling_started, allotment_done FROM App_Status WHERE id = 1");
                                if (rsSettings.next()) {
                                    if (rsSettings.getBoolean("allotment_done")) {
                                        JOptionPane.showMessageDialog(this, "Allotment is done, but unfortunately you did not get a seat.");
                                        cardLayout.show(mainPanel, "Status");
                                    } else if (rsSettings.getBoolean("counselling_started")) {
                                        loadColleges();
                                        cardLayout.show(mainPanel, "Counselling");
                                    } else {
                                        JOptionPane.showMessageDialog(this, "Counselling hasn't started yet. Wait for Admin notification.");
                                        cardLayout.show(mainPanel, "Status");
                                    }
                                }
                            } else {
                                JOptionPane.showMessageDialog(this, "We are sorry.\nYour Rank is " + rank + ". Only the top 10% (" + cutoffRank + " ranks) are eligible.\nYou are NOT qualified for counselling.", "Not Qualified", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid ID or Email, OR you did not take the exam.");
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

        panel.add(new JLabel(""));
        panel.add(loginBtn);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Please wait for updates or check back later.", SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.CENTER);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            currentStudentId = -1;
            cardLayout.show(mainPanel, "Login");
        });
        panel.add(logoutBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAllotmentPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Counselling Allotment Result", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel);

        allotCollegeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        allotCollegeLabel.setForeground(new Color(0, 100, 0));
        panel.add(allotCollegeLabel);

        JLabel infoLabel = new JLabel("Please select an option for your seat:", SwingConstants.CENTER);
        panel.add(infoLabel);

        freezeBtn.setBackground(new Color(50, 200, 50));
        freezeBtn.setForeground(Color.WHITE);
        floatBtn.setBackground(new Color(50, 150, 200));
        floatBtn.setForeground(Color.WHITE);
        slideBtn.setBackground(new Color(200, 150, 50));
        slideBtn.setForeground(Color.WHITE);

        java.awt.event.ActionListener actionListener = e -> {
            String choice = "";
            if (e.getSource() == freezeBtn) choice = "FREEZE";
            else if (e.getSource() == floatBtn) choice = "FLOAT";
            else if (e.getSource() == slideBtn) choice = "SLIDE";

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE Allotment SET status = ? WHERE student_id = ?")) {
                pstmt.setString(1, choice);
                pstmt.setInt(2, currentStudentId);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Seat " + choice + " successfully registered!");
                cardLayout.show(mainPanel, "Status");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving choice.");
            }
        };

        freezeBtn.addActionListener(actionListener);
        floatBtn.addActionListener(actionListener);
        slideBtn.addActionListener(actionListener);

        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        btnPanel.add(freezeBtn);
        btnPanel.add(floatBtn);
        btnPanel.add(slideBtn);
        panel.add(btnPanel);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            currentStudentId = -1;
            cardLayout.show(mainPanel, "Login");
        });
        panel.add(logoutBtn);

        return panel;
    }

    private JPanel createCounsellingPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Select top 3 colleges in priority:", SwingConstants.CENTER));
        
        JComboBox<String> pref1 = new JComboBox<>(collegeModel1);
        JComboBox<String> pref2 = new JComboBox<>(collegeModel2);
        JComboBox<String> pref3 = new JComboBox<>(collegeModel3);
        
        panel.add(pref1);
        panel.add(pref2);
        panel.add(pref3);

        JButton savePrefBtn = new JButton("Save Preferences");
        savePrefBtn.addActionListener(e -> {
            String[] choices = { (String)pref1.getSelectedItem(), (String)pref2.getSelectedItem(), (String)pref3.getSelectedItem() };
            
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                conn.setAutoCommit(false);
                try {
                    PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM Preference WHERE student_id = ?");
                    checkStmt.setInt(1, currentStudentId);
                    if (checkStmt.executeQuery().next()) {
                        JOptionPane.showMessageDialog(this, "You have already submitted preferences.");
                        cardLayout.show(mainPanel, "Status");
                        return;
                    }
                    
                    String sql = "INSERT INTO Preference (student_id, college_id, pref_order) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        for (int i=0; i<3; i++) {
                            if (!choices[i].equals("None")) {
                                int qCollegeId = Integer.parseInt(choices[i].split("-")[0]);
                                pstmt.setInt(1, currentStudentId);
                                pstmt.setInt(2, qCollegeId);
                                pstmt.setInt(3, i+1);
                                pstmt.executeUpdate();
                            }
                        }
                    }
                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Preferences Saved Successfully!");
                    cardLayout.show(mainPanel, "Status");
                } catch (Exception ex) {
                    conn.rollback();
                    ex.printStackTrace();
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        panel.add(savePrefBtn);
        return panel;
    }

    private void loadColleges() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT college_id, name, type FROM College")) {
            
            collegeModel1.removeAllElements();
            collegeModel2.removeAllElements();
            collegeModel3.removeAllElements();

            collegeModel1.addElement("None");
            collegeModel2.addElement("None");
            collegeModel3.addElement("None");

            while (rs.next()) {
                String option = rs.getInt("college_id") + "-" + rs.getString("name") + " (" + rs.getString("type") + ")";
                collegeModel1.addElement(option);
                collegeModel2.addElement(option);
                collegeModel3.addElement(option);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CounsellingDashboard().setVisible(true));
    }
}