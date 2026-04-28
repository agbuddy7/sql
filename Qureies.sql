-- ==========================================
-- 1. DATABASE & TABLE CREATION (DDL)
-- ==========================================
DROP DATABASE IF EXISTS jee_db;
CREATE DATABASE jee_db;

CREATE TABLE Student (student_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), category VARCHAR(20), attempts INT, email VARCHAR(100));
CREATE TABLE Payment (payment_id INT AUTO_INCREMENT PRIMARY KEY, student_id INT, amount DECIMAL(10,2), status VARCHAR(20), FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE);
CREATE TABLE Center (center_id INT AUTO_INCREMENT PRIMARY KEY, city_name VARCHAR(100), total_seats INT, available_seats INT);
CREATE TABLE Center_Allocation (alloc_id INT AUTO_INCREMENT PRIMARY KEY, student_id INT, center_id INT, FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE, FOREIGN KEY (center_id) REFERENCES Center(center_id) ON DELETE CASCADE);
CREATE TABLE Result (student_id INT PRIMARY KEY, marks INT, exam_rank INT, FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE);
CREATE TABLE App_Status (id INT PRIMARY KEY, results_released BOOLEAN, counselling_started BOOLEAN, allotment_done BOOLEAN, current_round INT DEFAULT 0);
CREATE TABLE Question_Bank (question_id INT AUTO_INCREMENT PRIMARY KEY, question TEXT, opt_a VARCHAR(200), opt_b VARCHAR(200), opt_c VARCHAR(200), opt_d VARCHAR(200), correct_opt CHAR(1));
CREATE TABLE College (college_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), type VARCHAR(20), available_seats INT);
CREATE TABLE Preference (pref_id INT AUTO_INCREMENT PRIMARY KEY, student_id INT, college_id INT, pref_order INT, FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE, FOREIGN KEY (college_id) REFERENCES College(college_id) ON DELETE CASCADE);
CREATE TABLE Allotment (student_id INT PRIMARY KEY, college_id INT, status VARCHAR(20) DEFAULT 'PENDING', round_allotted INT DEFAULT 1, FOREIGN KEY (student_id) REFERENCES Student(student_id) ON DELETE CASCADE, FOREIGN KEY (college_id) REFERENCES College(college_id) ON DELETE CASCADE);
CREATE TABLE Audit_Log (log_id INT AUTO_INCREMENT PRIMARY KEY, action VARCHAR(255), timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

-- ==========================================
-- 2. TRIGGERS (DATA VALIDATION & AUDIT)
-- ==========================================
CREATE TRIGGER before_student_insert_email BEFORE INSERT ON Student FOR EACH ROW IF NEW.email NOT LIKE '%_@__%.__%' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid email format'; END IF;
CREATE TRIGGER after_student_insert AFTER INSERT ON Student FOR EACH ROW INSERT INTO Audit_Log (action) VALUES (CONCAT('New student registered: ', NEW.name));
CREATE TRIGGER after_payment_insert AFTER INSERT ON Payment FOR EACH ROW INSERT INTO Audit_Log (action) VALUES (CONCAT('Payment of Rs.', NEW.amount, ' received from Student ID: ', NEW.student_id));
CREATE TRIGGER after_seat_allotment AFTER INSERT ON Allotment FOR EACH ROW INSERT INTO Audit_Log (action) VALUES (CONCAT('Student ID ', NEW.student_id, ' allotted seat at College ID ', NEW.college_id));

-- ==========================================
-- 3. INITIAL DATA SEEDING (INSERTS)
-- ==========================================
INSERT INTO App_Status (id, results_released, counselling_started, allotment_done, current_round) VALUES (1, false, false, false, 0);
INSERT INTO Center (city_name, total_seats, available_seats) VALUES ('Delhi', 100, 100), ('Mumbai', 80, 80), ...
INSERT INTO Question_Bank (question, opt_a, opt_b, opt_c, opt_d, correct_opt) VALUES (?, ?, ?, ?, ?, ?);
INSERT INTO College (name, type, available_seats) VALUES ('IIT Bombay (CSE)', 'Govt', 2), ('IIT Delhi (CSE)', 'Govt', 2); -- (also supports dynamic inserts via ?)

-- ==========================================
-- 4. PHASE 1: REGISTRATION & EXAM (JeeDashboard)
-- ==========================================
INSERT INTO Student (name, category, attempts, email) VALUES (?, ?, ?, ?);
INSERT INTO Payment (student_id, amount, status) VALUES (?, ?, ?);
SELECT s.name, p.status FROM Student s LEFT JOIN Payment p ON s.student_id = p.student_id WHERE s.student_id = ? AND s.email = ?;
SELECT center_id, city_name, available_seats FROM Center WHERE available_seats > 0;
INSERT INTO Center_Allocation (student_id, center_id) VALUES (?, ?);
UPDATE Center SET available_seats = available_seats - 1 WHERE center_id = ? AND available_seats > 0;
SELECT * FROM Center_Allocation WHERE student_id = ?;
SELECT * FROM Result WHERE student_id = ?;
SELECT * FROM Question_Bank ORDER BY RAND() LIMIT 3;
INSERT INTO Result (student_id, marks) VALUES (?, ?);
SELECT results_released FROM App_Status WHERE id = 1;
SELECT marks, exam_rank FROM Result WHERE student_id = ?;

-- ==========================================
-- 5. ADMIN DASHBOARD OPERATIONS
-- ==========================================
-- Rank Calculation Loop Query:
UPDATE Result r SET exam_rank = (SELECT row_num FROM (SELECT student_id, RANK() OVER(ORDER BY marks DESC) as row_num FROM Result) as temp WHERE temp.student_id = r.student_id);

-- Flags & Master Data Deletion
UPDATE App_Status SET results_released = true WHERE id = 1;
UPDATE App_Status SET counselling_started = true WHERE id = 1;
UPDATE App_Status SET results_released = false, counselling_started = false, allotment_done = false WHERE id = 1;
TRUNCATE TABLE Allotment;
TRUNCATE TABLE Preference;
TRUNCATE TABLE Result;
TRUNCATE TABLE Center_Allocation;
TRUNCATE TABLE Payment;
TRUNCATE TABLE Audit_Log;
TRUNCATE TABLE Student;
UPDATE College SET available_seats = (SELECT original_seats FROM...);
SELECT COUNT(*) as count FROM Question_Bank;
SELECT COUNT(*) as count FROM College;

-- Main Admin Data Grid
SELECT s.student_id, s.name, s.email, s.category, s.attempts, 
       COALESCE(p.status, 'PENDING') as status, 
       COALESCE(p.amount, 0.00) as amount, 
       COALESCE(c.city_name, 'Not Selected') as center, 
       COALESCE(CAST(r.marks AS CHAR), 'N/A') as marks, 
       COALESCE(CAST(r.exam_rank AS CHAR), 'N/A') as exam_rank, 
       COALESCE(col.name, 'Not Allotted') as allotted_college,
       COALESCE(a.status, 'N/A') as allotment_status
FROM Student s 
LEFT JOIN Payment p ON s.student_id = p.student_id 
LEFT JOIN Center_Allocation ca ON s.student_id = ca.student_id 
LEFT JOIN Center c ON ca.center_id = c.center_id 
LEFT JOIN Result r ON s.student_id = r.student_id 
LEFT JOIN Allotment a ON s.student_id = a.student_id 
LEFT JOIN College col ON a.college_id = col.college_id 
ORDER BY s.student_id DESC;

-- Round Simulation Queries
SELECT current_round FROM App_Status WHERE id = 1;
SELECT s.student_id, r.exam_rank FROM Student s JOIN Result r ON s.student_id = r.student_id ORDER BY r.exam_rank ASC;
SELECT a.college_id, a.status, p.pref_order FROM Allotment a JOIN Preference p ON a.student_id = p.student_id AND a.college_id = p.college_id WHERE a.student_id = ?;
SELECT college_id, pref_order FROM Preference WHERE student_id = ? ORDER BY pref_order ASC;
SELECT available_seats FROM College WHERE college_id = ? FOR UPDATE;
UPDATE College SET available_seats = available_seats + 1 WHERE college_id = ?;
DELETE FROM Allotment WHERE student_id = ?;
INSERT INTO Allotment (student_id, college_id, status, round_allotted) VALUES (?, ?, 'PENDING', ?);
UPDATE College SET available_seats = available_seats - 1 WHERE college_id = ?;
UPDATE App_Status SET allotment_done = true, current_round = ? WHERE id = 1;

-- ==========================================
-- 6. PHASE 2: COUNSELLING & ALLOTMENT (CounsellingDashboard)
-- ==========================================
SELECT results_released, counselling_started, allotment_done FROM App_Status WHERE id = 1;
SELECT s.name, r.exam_rank, r.marks, (SELECT COUNT(*) FROM Result) as total_students FROM Student s JOIN Result r ON s.student_id = r.student_id WHERE s.student_id = ? AND s.email = ?;
SELECT college_id, name, type FROM College;
SELECT * FROM Preference WHERE student_id = ?;
INSERT INTO Preference (student_id, college_id, pref_order) VALUES (?, ?, ?);
SELECT a.status, c.name FROM Allotment a JOIN College c ON a.college_id = c.college_id WHERE student_id = ?;
UPDATE Allotment SET status = ? WHERE student_id = ?;