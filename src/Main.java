import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    final static SimpleDateFormat dateFormatter = new SimpleDateFormat("M/d/yy");

    private final Connection connection;
    private final Scanner stdin;

    Main(Connection connection, Scanner stdin) {
        this.connection = connection;
        this.stdin = stdin;
    }

    @FunctionalInterface interface MenuMethod {
        void run() throws SQLException;
    }

    static class MenuEntry {
        private final String description;
        private final MenuMethod code;
        MenuEntry(String description, MenuMethod code) {
            this.description = description;
            this.code = code;
        }
        String description() {
            return this.description;
        }
        MenuMethod code() {
            return this.code;
        }
    }

    final List<MenuEntry> mainMenu = List.of(
            new MenuEntry("Create Reservation - New Guest", this::createReservationNewGuest),
            new MenuEntry("Create Reservation - Existing Guest", this::createReservationExistingGuest),
            new MenuEntry("Check In Reservation", this::checkIn),
            new MenuEntry("Check Out Reservation", this::checkOut),
            new MenuEntry("Assign Room for Maintenance", this::assignRoomForMaintenance),
            new MenuEntry("Log Maintenance Completion", this::logMaintenanceCompletion),
            new MenuEntry("Remove Maintenance Assignment", this::removeMaintenanceAssignment),
            new MenuEntry("Update Staff Role", this::updateStaffRole),
            new MenuEntry("Get Room Price", this::getRoomPrice),
            new MenuEntry("Quit", this::quit)
    );

    void createReservationNewGuest() throws SQLException {
        System.out.print("Enter guest name: ");
        String guestName = stdin.nextLine();

        System.out.print("Enter guest payment method: ");
        String guestPaymentMethod = stdin.nextLine();

        CallableStatement createGuestStatement = connection.prepareCall("{call dbo.createGuest(?,?,?)}");
        createGuestStatement.setString(1, guestName);
        createGuestStatement.setString(2, guestPaymentMethod);
        createGuestStatement.registerOutParameter(3, Types.INTEGER);
        createGuestStatement.execute();
        int createdGuestId = createGuestStatement.getInt(3);

        createReservationInternal(createdGuestId);
    }

    void createReservationExistingGuest() throws SQLException {
        System.out.print("Enter part or all of guest name to search guests (blank to show all): ");
        String searchGuestName = stdin.nextLine();

        CallableStatement searchReservationsStatement = connection.prepareCall("{call dbo.searchGuests(?)}");
        searchReservationsStatement.setString(1, searchGuestName);
        ResultSet results = searchReservationsStatement.executeQuery();

        System.out.println("option | guest name");
        List<Integer> guestIdsInOrder = new ArrayList<>();
        int i = 0;
        while (results.next()) {
            int guestId = results.getInt("id");
            guestIdsInOrder.add(guestId);

            String guestName = results.getNString("name");
            System.out.println((i + 1) + " | " + guestName);

            i++;
        }

        System.out.print("Enter selection: ");
        int chosenIndex = stdin.nextInt() - 1;
        stdin.nextLine();
        int guestId = guestIdsInOrder.get(chosenIndex);

        createReservationInternal(guestId);
    }

    void createReservationInternal(int guestId) throws SQLException {
        System.out.print("Enter start date (e.g. 6/1/23): ");
        Date startDate;
        try {
            var parsedDate = dateFormatter.parse(stdin.nextLine());
            startDate = new Date(parsedDate.getTime());
        } catch (ParseException e) {
            System.out.println("invalid date format");
            return;
        }

        System.out.print("Enter end date (e.g. 6/14/23): ");
        Date endDate;
        try {
            var parsedDate = dateFormatter.parse(stdin.nextLine());
            endDate = new Date(parsedDate.getTime());
        } catch (ParseException e) {
            System.out.println("invalid date format");
            return;
        }

        CallableStatement createReservationStatement = connection.prepareCall("{call dbo.createReservation(?,?,?,?)}");
        createReservationStatement.setDate(1, startDate);
        createReservationStatement.setDate(2, endDate);
        createReservationStatement.setInt(3, guestId);
        createReservationStatement.registerOutParameter(4, Types.INTEGER);
        createReservationStatement.execute();

        connection.commit();
        System.out.println("Reservation created");
    }

    int searchReservationByGuestName(String status) throws SQLException {
        System.out.print("Enter part or all of guest name to search reservations (blank to show all): ");
        String searchGuestName = stdin.nextLine();

        CallableStatement searchReservationsStatement = connection.prepareCall("{call dbo.searchReservationsByGuestName(?,?)}");
        searchReservationsStatement.setString(1, searchGuestName);
        searchReservationsStatement.setString(2, status);
        ResultSet results = searchReservationsStatement.executeQuery();

        System.out.println("option | guest name | start date | end date");
        List<Integer> reservationIdsInOrder = new ArrayList<>();
        int i = 0;
        while (results.next()) {
            int reservationId = results.getInt("reservation_id");
            reservationIdsInOrder.add(reservationId);

            String guestName = results.getNString("name");
            String startDate = dateFormatter.format(results.getDate("start_date"));
            String endDate = dateFormatter.format(results.getDate("end_date"));
            System.out.println((i + 1) + " | " + guestName + " | " + startDate + " | " + endDate);

            i++;
        }

        System.out.print("Enter selection: ");
        int chosenIndex = stdin.nextInt() - 1;
        stdin.nextLine();
        return reservationIdsInOrder.get(chosenIndex);
    }

    void checkIn() throws SQLException {
        int reservationId = searchReservationByGuestName("reserved");

        CallableStatement checkInStatement = connection.prepareCall("{call dbo.updateReservationStatus(?,?)}");
        checkInStatement.setInt(1, reservationId);
        checkInStatement.setString(2, "checked_in");
        checkInStatement.execute();
        connection.commit();
        System.out.println("Reservation checked in");
    }

    void checkOut() throws SQLException {
        int reservationId = searchReservationByGuestName("checked_in");

        CallableStatement checkInStatement = connection.prepareCall("{call dbo.updateReservationStatus(?,?)}");
        checkInStatement.setInt(1, reservationId);
        checkInStatement.setString(2, "checked_out");
        checkInStatement.execute();
        connection.commit();
        System.out.println("Reservation checked out");
    }

    void deleteReservation() throws SQLException {
        int reservationId = searchReservationByGuestName("reserved");

        CallableStatement deleteReservationStatement = connection.prepareCall("{call dbo.deleteReservation(?)}");
        deleteReservationStatement.setInt(1, reservationId);
        deleteReservationStatement.execute();
        connection.commit();
        System.out.println("Reservation cancelled");
    }

    void changeReservation() throws SQLException {
        int reservationId = searchReservationByGuestName("reserved");

        System.out.print("Enter new start date (e.g. 6/1/23): ");
        Date startDate;
        try {
            var parsedDate = dateFormatter.parse(stdin.nextLine());
            startDate = new Date(parsedDate.getTime());
        } catch (ParseException e) {
            System.out.println("invalid date format");
            return;
        }

        System.out.print("Enter new end date (e.g. 6/14/23): ");
        Date endDate;
        try {
            var parsedDate = dateFormatter.parse(stdin.nextLine());
            endDate = new Date(parsedDate.getTime());
        } catch (ParseException e) {
            System.out.println("invalid date format");
            return;
        }
        System.out.print("Enter new room id: ")
        String roomId = stdin.nextLine();

        if(cheackRoomReservation(roomId, startDate, endDate)){
            CallableStatement changeReservationStatement = connection.prepareCall("{call dbo.deleteReservation(?)}");
            changeReservationStatement.setInt(1, reservationId);
            createReservationStatement.setDate(2, startDate);
            createReservationStatement.setDate(3, endDate);
            createReservationStatement.setString(4, roomId);
            changeReservationStatement.execute();
            connection.commit();
            System.out.println("Reservation changed to " + startDate + " to " + endDate + " in room " + roomId);
        } else {
            System.out.println("Room " + roomId + "cannot be booked during for that time please try a different room or different dates.")
        }
    }

    boolean checkRoomReservation(String roomId, Date start_date, Date end_date) throws SQLException{
        CallableStatement roomSearchStatement = conneciton.prepareCall("{call dbo.searchReservationByRoomId(?)}");
        roomSearchStatement.setString(1, roomId);
        ResultSet results = searchReservationsStatement.executeQuery();
        int i = 0;
        while (results.next()) {
            Date listStartDate = dateFormatter.format(results.getDate("start_date"));
            Date listEndDate = dateFormatter.format(results.getDate("end_date"));
            
            if((start_date.before(listEndDate) AND start_date.after(listStartDate)) OR (end_date.before(listEndDate) AND end_date.after(listStartDate)))
                return false;

            i++;
        }
        return true;
    }

    void updateStaffRole() throws SQLException {
        System.out.print("Enter Staff ID to update role: ");
        int staffId = Integer.parseInt(stdin.nextLine());

        System.out.print("Enter new role for the staff: ");
        String newRole = stdin.nextLine();

        DatabaseConnection.updateStaffRole(staffId, newRole);
        System.out.println("Staff role updated.");
    }

    void getRoomPrice() {
        try {
            System.out.print("Enter Room ID to get price: ");
            String roomId = stdin.nextLine();
    
            Long price = DatabaseConnection.getRoomPrice(roomId);
            if (price != null) {
                System.out.println("Price for room " + roomId + " is: " + price);
            } else {
                System.out.println("Room not found or price unavailable.");
            }
        } catch (SQLException e) {
            System.out.println("An error occurred: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }    

    void removeMaintenanceAssignment() throws SQLException {
        System.out.print("Enter Room ID to remove maintenance assignment: ");
        String roomId = stdin.nextLine();
    
        System.out.print("Enter Staff ID for the assignment: ");
        int staffId = Integer.parseInt(stdin.nextLine());
    
        DatabaseConnection.removeMaintenanceAssignment(roomId, staffId);
        System.out.println("Maintenance assignment removed.");
    }

    void assignRoomForMaintenance() throws SQLException {
        System.out.print("Enter Room ID: ");
        String roomId = stdin.nextLine();

        System.out.print("Enter Staff ID: ");
        int staffId = Integer.parseInt(stdin.nextLine());

        DatabaseConnection.assignRoomForMaintenance(roomId, staffId);
        System.out.println("Room assigned for maintenance.");
    }

    void logMaintenanceCompletion() throws SQLException {
        System.out.print("Enter Room ID: ");
        String roomId = stdin.nextLine();

        System.out.print("Enter Staff ID: ");
        int staffId = Integer.parseInt(stdin.nextLine());

        System.out.print("Enter Maintenance Date (YYYY-MM-DD): ");
        Date maintenanceDate = Date.valueOf(stdin.nextLine());

        System.out.print("Is the room in good condition? (true/false): ");
        boolean inGoodCondition = Boolean.parseBoolean(stdin.nextLine());

        DatabaseConnection.logMaintenanceCompletion(roomId, staffId, maintenanceDate, inGoodCondition);
        System.out.println("Maintenance completion logged.");
    }

    void quit() {
        System.exit(0);
    }

    void doMainMenu() throws SQLException {
        System.out.println("Select a use case by number:");
        for (int i = 0; i < mainMenu.size(); i++) {
            MenuEntry entry = mainMenu.get(i);
            int userFacingIndex = i + 1;
            System.out.println(userFacingIndex + " | " + entry.description());
        }
        System.out.print("Enter selection: ");
        int chosenIndex = stdin.nextInt() - 1;
        stdin.nextLine();
        var chosenEntry = mainMenu.get(chosenIndex);
        System.out.println(chosenEntry.description());
        chosenEntry.code().run();
    }


    public static void main(String[] args) {
        String connectionUrl =
                "jdbc:sqlserver://cxp-sql-02\\jrt108;"
                        + "database=Hotel;"
                        + "user=dbuser;"
                        + "password=csds341143sdsc;"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=15;";

        try (Connection connection = DriverManager.getConnection(connectionUrl))
        {
            connection.setAutoCommit(false);
            Main m = new Main(connection, new Scanner(System.in));
            System.out.println("Welcome to the Hotel Management System");
            while (true) {
                m.doMainMenu();
            }
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
