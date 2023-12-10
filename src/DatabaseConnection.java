import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Types;

public class DatabaseConnection {
    private static final String CONNECTION_URL = "######################;"
                        + "database=######;"
                        + "user=##########;"
                        + "password=#############;"
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

    public static Long getRoomPrice(String roomId) throws SQLException {
        Connection conn = null;
        CallableStatement cstmt = null;
        Long roomPrice = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Disable auto-commit
    
            String sql = "{call GetRoomPrice(?, ?)}";
            cstmt = conn.prepareCall(sql);
    
            cstmt.setString(1, roomId);
            cstmt.registerOutParameter(2, Types.BIGINT);
    
            cstmt.execute();
            roomPrice = cstmt.getLong(2);
    
            conn.commit(); // Commit the transaction
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Rollback in case of exception
            }
            throw e;
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }
            if (conn != null) {
                conn.setAutoCommit(true); // Re-enable auto-commit
                conn.close();
            }
        }
        return roomPrice;
    }    

    public static void updateStaffRole(int staffId, String newRole) throws SQLException {
        Connection conn = null;
        CallableStatement cstmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Disable auto-commit
    
            String sql = "{call UpdateStaffRole(?, ?)}";
            cstmt = conn.prepareCall(sql);
    
            cstmt.setInt(1, staffId);
            cstmt.setString(2, newRole);
    
            cstmt.executeUpdate();
            conn.commit(); // Commit the transaction after successful execution
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Roll back the transaction in case of an exception
            }
            throw e;
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }
            if (conn != null) {
                conn.setAutoCommit(true); // Re-enable auto-commit
                conn.close();
            }
        }
    }    

    public static void assignRoomForMaintenance(String roomId, int staffId) throws SQLException {
        Connection conn = null;
        CallableStatement cstmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Disable auto-commit
    
            String sql = "{call AssignRoomForMaintenance(?, ?)}";
            cstmt = conn.prepareCall(sql);
    
            cstmt.setString(1, roomId);
            cstmt.setInt(2, staffId);
    
            cstmt.executeUpdate();
            conn.commit(); // Commit the transaction after successful execution
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Roll back the transaction in case of an exception
            }
            throw e;
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }
            if (conn != null) {
                conn.setAutoCommit(true); // Re-enable auto-commit
                conn.close();
            }
        }
    }    

    public static void logMaintenanceCompletion(String roomId, int staffId, Date maintenanceDate, boolean inGoodCondition) throws SQLException {
        Connection conn = null;
        CallableStatement cstmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String sql = "{call LogMaintenance(?, ?, ?, ?)}";
            cstmt = conn.prepareCall(sql);
            cstmt.setString(1, roomId);
            cstmt.setInt(2, staffId);
            cstmt.setDate(3, maintenanceDate);
            cstmt.setBoolean(4, inGoodCondition);

            cstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public static void removeMaintenanceAssignment(String roomId, int staffId) throws SQLException {
        Connection conn = null;
        CallableStatement cstmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Disable auto-commit
    
            String sql = "{call RemoveMaintenanceAssignment(?, ?)}";
            cstmt = conn.prepareCall(sql);
            
            cstmt.setString(1, roomId);
            cstmt.setInt(2, staffId);
            
            cstmt.executeUpdate();
            conn.commit(); // Commit the transaction after successful execution
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Roll back the transaction in case of an exception
            }
            throw e;
        } finally {
            if (cstmt != null) {
                cstmt.close();
            }
            if (conn != null) {
                conn.setAutoCommit(true); // Re-enable auto-commit
                conn.close();
            }
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
