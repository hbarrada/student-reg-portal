
import java.sql.*; // JDBC stuff.
import java.util.Properties;

public class PortalConnection {

    // --- read config from environment (with safe defaults)
    static final String DBNAME = getenvOr("DB_NAME", "portal");
    static final String DBHOST = getenvOr("DB_HOST", "localhost");
    static final String DBPORT = getenvOr("DB_PORT", "5432");
    static final String DBUSER = getenvOr("DB_USER", "postgres");
    static final String DBPASS = getenvOr("DB_PASS", "");

    static final String DATABASE = "jdbc:postgresql://" + DBHOST + ":" + DBPORT + "/" + DBNAME;

    private Connection conn;

    public PortalConnection() throws SQLException, ClassNotFoundException {
        this(DATABASE, DBUSER, DBPASS);
    }

    public PortalConnection(String db, String user, String pwd) throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pwd);
        this.conn = DriverManager.getConnection(db, props);
    }

    private static String getenvOr(String key, String fallback){
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    // Register a student on a course, returns a tiny JSON document (as a String)
    public String register(String student, String courseCode) {
      try {
          try (PreparedStatement ps = conn.prepareStatement(
                  "INSERT INTO Registrations VALUES (?, ?, 'registered')")) {
              ps.setString(1, student);
              ps.setString(2, courseCode);
              ps.executeUpdate();
              return "{\"success\":true}";
          }
      } catch (SQLException e) {
          return "{\"success\":false, \"error\":\"" + getError(e) + "\"}";
      }
  }

  public String unregister(String student, String courseCode) {
        String sql = "DELETE FROM Registrations WHERE student = ? AND course = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student);
            ps.setString(2, courseCode);
            int rows = ps.executeUpdate();
            if (rows > 0) return "{\"success\":true}";
            return "{\"success\":false, \"error\":\"Student is not registered or waiting for course\"}";
        } catch (SQLException e) {
            return "{\"success\":false, \"error\":\"" + getError(e) + "\"}";
        }
    }

    // Return a JSON document containing lots of information about a student, it should validate against the schema found in information_schema.json
public String getInfo(String student) throws SQLException {
    try(PreparedStatement st = conn.prepareStatement(
        "SELECT jsonb_build_object(" +
            "'student', bi.idnr, " +
            "'name', bi.name, " +
            "'login', bi.login, " +
            "'program', bi.program, " +
            "'branch', bi.branch, " +
            
            // finished courses array
            "'finished', (" +
                "SELECT COALESCE(jsonb_agg(jsonb_build_object(" +
                    "'course', fc.coursename, " +
                    "'code', fc.course, " +
                    "'credits', fc.credits, " +
                    "'grade', fc.grade" +
                ")), '[]'::jsonb) " +
                "FROM FinishedCourses fc " +
                "WHERE fc.student = bi.idnr" +
            "), " +
            
            // registered courses array
            "'registered', (" +
                "SELECT COALESCE(jsonb_agg(jsonb_build_object(" +
                    "'course', c.name, " +
                    "'code', r.course, " +
                    "'status', r.status, " +
                    "'position', wl.position" +
                ")), '[]'::jsonb) " +
                "FROM Registrations r " +
                "JOIN Courses c ON r.course = c.code " +
                "LEFT JOIN WaitingList wl ON r.student = wl.student AND r.course = wl.course " +
                "WHERE r.student = bi.idnr" +
            "), " +
            
            // additional required fields from PathToGraduation
            "'seminarCourses', (" +
                "SELECT COALESCE(ptg.seminarcourses, 0) " +
                "FROM PathToGraduation ptg " +
                "WHERE ptg.student = bi.idnr" +
            "), " +
            "'mathCredits', (" +
                "SELECT COALESCE(ptg.mathcredits, 0) " +
                "FROM PathToGraduation ptg " +
                "WHERE ptg.student = bi.idnr" +
            "), " +
            "'totalCredits', (" +
                "SELECT COALESCE(ptg.totalcredits, 0) " +
                "FROM PathToGraduation ptg " +
                "WHERE ptg.student = bi.idnr" +
            "), " +
            "'canGraduate', (" +
                "SELECT COALESCE(ptg.qualified, false) " +
                "FROM PathToGraduation ptg " +
                "WHERE ptg.student = bi.idnr" +
            ")" +
        ") AS jsondata " +
        "FROM BasicInformation bi " +
        "WHERE bi.idnr = ?")) {
        
        st.setString(1, student);
        ResultSet rs = st.executeQuery();
        
        if(rs.next())
            return rs.getString("jsondata");
        else
            return "{\"student\":\"does not exist :(\"}"; 
    }
}


    // This is a hack to turn an SQLException into a JSON string error message. No need to change.
    public static String getError(SQLException e){
       String message = e.getMessage();
       int ix = message.indexOf('\n');
       if (ix > 0) message = message.substring(0, ix);
       message = message.replace("\"","\\\"");
       return message;
    }
}