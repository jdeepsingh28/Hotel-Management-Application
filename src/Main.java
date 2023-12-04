import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
            new MenuEntry("Create Reservation", this::createReservation),
            new MenuEntry("Quit", this::quit)
    );

    void createReservation() throws SQLException {
        System.out.println("Create Reservation");

        System.out.print("Enter guest name: ");
        String guestName = stdin.nextLine();

        System.out.print("Enter guest credit card number: ");
        String guestPaymentMethod = stdin.nextLine();

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

        CallableStatement createGuestStatement = connection.prepareCall("{call dbo.createGuest(?,?,?)}");
        createGuestStatement.setString(1, guestName);
        createGuestStatement.setString(2, guestPaymentMethod);
        createGuestStatement.registerOutParameter(3, Types.INTEGER);
        createGuestStatement.execute();
        int createdGuestId = createGuestStatement.getInt(3);

        CallableStatement createReservationStatement = connection.prepareCall("{call dbo.createReservation(?,?,?,?)}");
        createReservationStatement.setDate(1, startDate);
        createReservationStatement.setDate(2, endDate);
        createReservationStatement.setInt(3, createdGuestId);
        createReservationStatement.registerOutParameter(4, Types.INTEGER);
        createReservationStatement.execute();

        connection.commit();
        System.out.println("Reservation created");
    }

    void checkIn() {

    }

    void quit() {
        System.out.println("Goodbye");
        System.exit(0);
    }

    void doMainMenu() throws SQLException {
        System.out.println("Select an option by number:");
        for (int i = 0; i < mainMenu.size(); i++) {
            MenuEntry entry = mainMenu.get(i);
            int userFacingIndex = i + 1;
            System.out.println(userFacingIndex + ".\t" + entry.description());
        }
        System.out.print("Enter selection: ");
        int chosenIndex = stdin.nextInt() - 1;
        stdin.nextLine();
        mainMenu.get(chosenIndex).code().run();
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
