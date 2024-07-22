import java.sql.*;
import java.util.Scanner;

public class UniDB {
    
    public static void main(String[] args) {
        String URL = args[0];
        String USERNAME = args[1];
        String PASSWORD = args[2];

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            System.out.println("Connected to database");

            Scanner input = new Scanner(System.in);
            while (true) {
                System.out.println();
                System.out.println("Select an operation:");
                System.out.println("1. Search students by name");
                System.out.println("2. Search students by year");
                System.out.println("3. Search for students with a GPA equal to or above a given threshold");
                System.out.println("4. Search for students with a GPA equal to or below a given threshold");
                System.out.println("5. Report number of students and average GPA for a given department");
                System.out.println("6. Report number of students currently taking a given class and distribution of grades for those who've taken it");
                System.out.println("7. Execute arbitrary SQL query");
                System.out.println("8. Exit");

                int choice = input.nextInt();
                input.nextLine(); // consume the newline character

                switch (choice) {
                    case 1:
                        searchByName(conn, input);
                        break;
                    case 2:
                        searchByYear(conn, input);
                        break;
                    case 3:
                        searchByGPAGreaterThan(conn, input);
                        break;
                    case 4:
                        searchByGPALessThan(conn, input);
                        break;
                    case 5:
                        String departmentName = input.nextLine();
                        reportDepartmentStats(conn, input, departmentName);
                        break;
                    case 6:
                        reportClassStats(conn, input);
                        break;
                    case 7:
                        executeQuery(conn, input);
                        break;
                    case 8:
                        System.out.println("Exiting");
                        return;
                    default:
                        System.out.println("Invalid choice");
                        break;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
    }

    private static void searchByName(Connection conn, Scanner input) throws SQLException {
        System.out.print("Enter search string: ");
        String search = input.nextLine();
        String query = "SELECT * FROM Students WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, "%" + search.toLowerCase() + "%");
            stmt.setString(2, "%" + search.toLowerCase() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String id = rs.getString("id");
                    System.out.println(firstName + " " + lastName + " (" + id + ")");
                }
            }
        }
    }


    private static void searchByYear(Connection conn, Scanner input) throws SQLException {
        System.out.print("Enter year (Fr, So, Ju, Sr): ");
        String year = input.nextLine();
        String query = "SELECT s.first_name, SUM(credits) as total_credits FROM Students s, HasTaken ht INNER JOIN Classes c ON ht.name = c.name GROUP BY s.first_name, ht.sid HAVING total_credits >= ? AND total_credits <= ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            int lowerBound, upperBound;

            if (year.equals("Fr")) {

                lowerBound = 0;

                upperBound = 29;

            } else if (year.equals("So")) {

                lowerBound = 30;

                upperBound = 59;

            } else if (year.equals("Ju")) {

                lowerBound = 60;

                upperBound = 89;

            } else {

                lowerBound = 90;

                upperBound = 120;

            }

            stmt.setInt(1, lowerBound);

            stmt.setInt(2, upperBound);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                String firstName = rs.getString("first_name");


                int totalCredits = rs.getInt("total_credits");

                System.out.println("Name: " + firstName);
                System.out.println("Credits: " + totalCredits);


            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

    }



    public static void searchByGPAGreaterThan(Connection conn, Scanner input)  throws SQLException {
        System.out.print("Enter GPA threshold: ");
        double threshold = input.nextDouble();
        String query = "SELECT s.id, s.first_name, s.last_name, SUM(g.points*c.credits)/SUM(c.credits) AS gpa, SUM(c.credits) AS credits FROM Students s, IsTaking it, Classes c, HasTaken ht, (SELECT 'A' AS letter, 4 AS points UNION ALL SELECT 'B' AS letter, 3 AS points UNION ALL SELECT 'C' AS letter, 2 AS points UNION ALL SELECT 'D' AS letter, 1 AS points UNION ALL SELECT 'F' AS letter, 0 AS points) g WHERE s.id=it.sid AND it.name=c.name AND s.id=ht.sid AND ht.grade=g.letter GROUP BY s.id HAVING gpa >= ?";


        try {
            // Create a prepared statement for the query and set the threshold parameter.
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setDouble(1, threshold);
            // Execute the query and get the result set.
            ResultSet rs = ps.executeQuery();
            // Print out the results.
            while (rs.next()) {
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String id = rs.getString("id");
                double gpa = rs.getDouble("GPA");
                System.out.println(firstName + " " + lastName + " (" + id + ") - GPA: " + gpa);
            }
        } catch (SQLException e) {
            // If there is an error executing the query, print the stack trace.
            e.printStackTrace();
        }
    }

    private static void searchByGPALessThan(Connection conn, Scanner input) throws SQLException {
        System.out.print("Enter GPA threshold: ");
        double threshold = input.nextDouble();
        input.nextLine(); // consume the newline character

        String query = "SELECT s.id, s.first_name, s.last_name, SUM(g.points*c.credits)/SUM(c.credits) AS gpa, SUM(c.credits) AS credits FROM Students s, IsTaking it, Classes c, HasTaken ht, (SELECT 'A' AS letter, 4 AS points UNION ALL SELECT 'B' AS letter, 3 AS points UNION ALL SELECT 'C' AS letter, 2 AS points UNION ALL SELECT 'D' AS letter, 1 AS points UNION ALL SELECT 'F' AS letter, 0 AS points) g WHERE s.id=it.sid AND it.name=c.name AND s.id=ht.sid AND ht.grade=g.letter GROUP BY s.id HAVING gpa <= ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String id = rs.getString("id");
                    double gpa = rs.getDouble("GPA");
                    System.out.println(firstName + " " + lastName + " (" + id + ") - GPA: " + gpa);
                }
            }
        }
    }

    public static void reportDepartmentStats(Connection conn, Scanner input, String departmentName) throws SQLException {
        System.out.print("Enter department code: ");
        String department = input.nextLine();
        try {
            Statement stmt = conn.createStatement();

            // count number of students in department (including majors and minors)
            String countStudentsQuery = "SELECT COUNT(DISTINCT sid) FROM (" +
                    "SELECT sid FROM majors WHERE dname = '" + departmentName + "' " +
                    "UNION " +
                    "SELECT sid FROM minors WHERE dname = '" + departmentName + "') AS department_students";
            ResultSet countStudentsResult = stmt.executeQuery(countStudentsQuery);
            countStudentsResult.next();
            int numStudents = countStudentsResult.getInt(1);

            // calculate average GPA of students in department
            String gpaQuery = "SELECT SUM(points * credits) / SUM(credits) FROM (SELECT grade, credits, CASE WHEN grade = 'A' THEN 4 " +
                    "WHEN grade = 'B' THEN 3 WHEN grade = 'C' THEN 2 WHEN grade = 'D' THEN 1 ELSE 0 END AS points " +
                    "FROM hasTaken NATURAL JOIN classes NATURAL JOIN isTaking NATURAL JOIN majors WHERE dname = '" + departmentName + "') AS grade_points";
            ResultSet gpaResult = stmt.executeQuery(gpaQuery);
            gpaResult.next();
            double avgGPA = gpaResult.getDouble(1);

            // print results
            System.out.println("Department: " + departmentName);
            System.out.println("Number of students: " + numStudents);
            System.out.println("Average GPA: " + avgGPA);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private static void reportClassStats(Connection conn, Scanner input) throws SQLException {
        String name = new String();
        System.out.print("Enter class code: ");
        String classCode = input.nextLine();
        String query = "SELECT COUNT(DISTINCT it.sid, ht.sid) as num_students, COUNT(CASE WHEN grade = 'A' THEN 1 ELSE NULL END) as num_As, COUNT(CASE WHEN grade = 'B' THEN 1 ELSE NULL END) as num_Bs, COUNT(CASE WHEN grade = 'C' THEN 1 ELSE NULL END) as num_Cs, COUNT(CASE WHEN grade = 'D' THEN 1 ELSE NULL END) as num_Ds, COUNT(CASE WHEN grade = 'F' THEN 1 ELSE NULL END) as num_Fs FROM IsTaking it INNER JOIN HasTaken ht ON it.sid = ht.sid AND it.name = ht.name WHERE it.name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, classCode);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                int numStudents = rs.getInt("num_students");

                int numAs = rs.getInt("num_As");

                int numBs = rs.getInt("num_Bs");

                int numCs = rs.getInt("num_Cs");

                int numDs = rs.getInt("num_Ds");

                int numFs = rs.getInt("num_Fs");
                System.out.println("Number of students currently enrolled: " + numStudents);
                System.out.println("Grades of previous enrollees:");
                System.out.println("Number of A's: " + numAs);
                System.out.println("Number of B's: " + numBs);
                System.out.println("Number of C's: " + numCs);
                System.out.println("Number of D's: " + numDs);
                System.out.println("Number of F's: " + numFs);

            }

        } catch (SQLException e) {
            System.out.println("Error executing query: " + e.getMessage());
        }
    }

    private static void executeQuery(Connection conn, Scanner input) throws SQLException {
        System.out.print("Enter SQL query: ");
        String query = input.nextLine();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            boolean isResultSet = stmt.execute();
            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(meta.getColumnLabel(i) + "\t");
                    }
                    System.out.println();
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.print(rs.getString(i) + "\t");
                        }
                        System.out.println();
                    }
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                System.out.println(updateCount + " rows affected");
            }
        }
    }
}