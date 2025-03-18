import java.io.*;
import java.net.*;
import java.util.*;

public class WeatherClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the weather server.");

            label:
            while (true) {
                System.out.println("\n=== Welcome to the Weather App ===");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1": {
                        // Login
                        String username = promptForInput(scanner, "Enter your username: ");
                        String password = promptForInput(scanner, "Enter your password: ");
                        out.println("LOGIN:" + username + ":" + password);

                        String response = in.readLine();
                        if (response == null || response.startsWith("ERROR")) {
                            System.out.println("Error: " + response);

                        } else {

                            String role = response.trim();

                            System.out.println("Successfully logged in as: " + role);
                            handleUserSession(scanner, in, out, role);
                        }
                        break;
                    }
                    case "2": {
                        // Register
                        String username = promptForInput(scanner, "Enter your desired username: ");
                        String password = promptForInput(scanner, "Enter your desired password: ");
                        String role = promptForInput(scanner, "Enter your role (admin/user): ");
                        out.println("REGISTER:" + username + ":" + password + ":" + role);

                        String response = in.readLine();
                        System.out.println(response);
                        break;
                    }
                    case "3":
                        System.out.println("Goodbye!");
                        break label;
                    default:
                        System.out.println("Invalid option. Please try again.");
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void provisionWeatherData(Scanner scanner, PrintWriter out, BufferedReader in) {
        System.out.print("Enter path to JSON file: ");
        String filePath = scanner.nextLine();

        //Send command to server
        out.println("PROVISION WEATHER DATA:" + filePath);

        try {

            String response = in.readLine();
            System.out.println(response);
        } catch (IOException e) {
            System.out.println("Error reading server response: " + e.getMessage());
        }
    }


    private static String promptForInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }


    private static void handleUserSession(Scanner scanner, BufferedReader in, PrintWriter out, String role) throws IOException {
        while (true) {
            if (role.equalsIgnoreCase("user")) {

                System.out.println("\n=== User Menu ===");
                System.out.println("1. Set location");
                System.out.println("2. Get weather");
                System.out.println("3. Logout");
                System.out.print("Enter your choice: ");

                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        String location = promptForInput(scanner, "Enter location (city name): ");
                        String latitude = promptForInput(scanner, "Enter latitude: ");
                        String longitude = promptForInput(scanner, "Enter longitude: ");
                        out.println("SET_LOCATION:" + location + ":" + latitude + ":" + longitude);
                        break;
                    case "2":
                        out.println("GET_WEATHER");
                        break;
                    case "3":
                        System.out.println("Logging out...");
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } else if (role.equalsIgnoreCase("admin")) {

                System.out.println("\n=== Admin Menu ===");
                System.out.println("1. Set location");
                System.out.println("2. Get weather");
                System.out.println("3. Provision weather data");
                System.out.println("4. Stop server");
                System.out.println("5. Logout");
                System.out.print("Enter your choice: ");

                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        String location = promptForInput(scanner, "Enter location (city name): ");
                        String latitude = promptForInput(scanner, "Enter latitude: ");
                        String longitude = promptForInput(scanner, "Enter longitude: ");
                        out.println("SET_LOCATION:" + location + ":" + latitude + ":" + longitude);
                        break;
                    case "2":
                        out.println("GET_WEATHER");
                        break;
                    case "3":
                        provisionWeatherData(scanner, out, in);
                        break;
                    case "4":
                        out.println("STOP");
                        System.out.println("Stopping server...");
                        return;
                    case "5":
                        System.out.println("Logging out...");
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } else {
                System.out.println("Invalid role received. Disconnecting...");
                return;
            }

          // Read response from server
            String response;
            while ((response = in.readLine()) != null && !response.isEmpty()) {
                System.out.println(response);
            }
        }
    }
}
