import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String CONNECTION_URL = "jdbc:sqlserver://cxp-sql-02.cxp.cwru.edu\\jrt108;"
                        + "database=Hotel;"
                        + "user=dbuser;"
                        + "password=csds341143sdsc;"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=15;";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_URL);
    }

    public static Object[][] getReservationData() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reservations")) {
            
            // Determine the number of rows and columns in ResultSet
            int rowCount = getRowCount(rs);
            int columnCount = rs.getMetaData().getColumnCount();
            Object[][] data = new Object[rowCount][columnCount];
    
            // Fill data array with ResultSet data
            int rowIndex = 0;
            while (rs.next()) {
                for (int i = 0; i < columnCount; i++) {
                    data[rowIndex][i] = rs.getObject(i + 1);
                }
                rowIndex++;
            }
            return data;
        } catch (SQLException e) {
            e.printStackTrace();
            return new Object[0][0]; // Return empty array on error
        }
    }
    
    public static int getRowCount(ResultSet rs) throws SQLException {
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
        }
        rs.beforeFirst(); // Reset cursor to the start
        return rowCount;
    }
}
