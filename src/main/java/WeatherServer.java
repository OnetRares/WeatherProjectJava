import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class WeatherServer {
    private static final int PORT = 12345;
    private static final String DATA_FILE = "src/weather_data.json";
    private static final String USERS_FILE = "src/users.txt";
    private static final Map<String, LocationWeather> weatherData = new HashMap<>();
    private static final Map<String, User> users = loadUsers();
    private static boolean isRunning = true;

    public static void main(String[] args) {
        DatabaseHelper.createTables();
        loadWeatherData();
        DatabaseHelper.loadWeatherDataIntoDatabase();
        DatabaseHelper.loadUsersFromTextFileToDatabase();
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server is running on port " + PORT);
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(new ClientHandler(clientSocket)).start();
                    } catch (IOException e) {
                        if (!isRunning) {
                            System.out.println("Server has been stopped.");
                        } else {
                            System.err.println("Error accepting client: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
            } finally {
                System.out.println("Server has been shut down.");
            }
        });

        serverThread.start();
    }

    private static String provisionWeatherData(String filePath) {
        try {
            JSONArray existingData = getObjects();

            StringBuilder newJsonContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    newJsonContent.append(line);
                }
            }

            JSONArray newData = new JSONArray(newJsonContent.toString());

            // Validate and add new 
            for (int i = 0; i < newData.length(); i++) {
                JSONObject weatherEntry = newData.getJSONObject(i);

                // Validate
                if (!weatherEntry.has("location") || !weatherEntry.has("currentWeather") ||
                        !weatherEntry.has("temperature") || !weatherEntry.has("latitude") ||
                        !weatherEntry.has("longitude") || !weatherEntry.has("forecast")) {
                    throw new JSONException("Missing required fields in weather data.");
                }

                // Validate forecast
                JSONArray forecastArray = weatherEntry.getJSONArray("forecast");
                for (int j = 0; j < forecastArray.length(); j++) {
                    JSONObject forecast = forecastArray.getJSONObject(j);
                    if (!forecast.has("day") || !forecast.has("temperature")) {
                        throw new JSONException("Invalid forecast format.");
                    }
                }

                existingData.put(weatherEntry);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
                writer.write(existingData.toString(4));  
            }

            return "Weather data provisioned successfully.";  

        } catch (IOException | JSONException e) {
            return "Error provisioning weather data: " + e.getMessage(); 
        }
    }

    private static JSONArray getObjects() throws IOException {
        JSONArray existingData = new JSONArray();
        File weatherFile = new File(DATA_FILE);
        if (weatherFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(weatherFile))) {
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                existingData = new JSONArray(jsonContent.toString());
            }
        }
        return existingData;
    }


    private static Map<String, User> loadUsers() {
        Map<String, User> users = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    users.put(parts[0], new User(parts[0], parts[1], parts[2]));
                } else {
                    System.err.println("Invalid user data format: " + line);
                }
            }
            System.out.println("Loaded users: " + users.keySet()); // Debug
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        return users;
    }
    private static void loadWeatherData() {
        try (FileReader reader = new FileReader(DATA_FILE)) {
            StringBuilder jsonBuilder = new StringBuilder();
            int i;
            while ((i = reader.read()) != -1) {
                jsonBuilder.append((char) i);
            }

            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
            for (int j = 0; j < jsonArray.length(); j++) {
                JSONObject obj = jsonArray.getJSONObject(j);
                String location = obj.getString("location");
                String currentWeather = obj.getString("currentWeather");
                double temperature = obj.getDouble("temperature");
                double latitude = obj.getDouble("latitude");
                double longitude = obj.getDouble("longitude");
                List<Pair<String, Integer>> forecast = new ArrayList<>();

                JSONArray forecastArray = obj.getJSONArray("forecast");
                for (int k = 0; k < forecastArray.length(); k++) {
                    JSONObject forecastObj = forecastArray.getJSONObject(k);
                    String day = forecastObj.getString("day");
                    int temp = forecastObj.getInt("temperature");
                    forecast.add(new Pair<>(day, temp));
                }

                weatherData.put(location, new LocationWeather(currentWeather, temperature, forecast, latitude, longitude));
            }
        } catch (IOException | JSONException e) {
            System.err.println("Error loading weather data: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String clientLocation = "";
                double clientLat = 0.0;
                double clientLon = 0.0;
                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("REGISTER:")) {
                        System.out.println("Processing REGISTER command: " + input); // 

                        String[] parts = input.split(":", 4);
                        if (parts.length != 4) {
                            out.println("ERROR: Invalid register format. Expected: REGISTER:username:password:role");
                            continue;
                        }

                        String username = parts[1];
                        String password = parts[2];
                        String role = parts[3];

                        synchronized (users) {
                            if (users.containsKey(username)) {
                                out.println("ERROR: Username already exists.");
                            } else {
                                User newUser = new User(username, password, role);
                                users.put(username, newUser);
                                saveUsers(); 
                                out.println("SUCCESS: User registered.");
                            }
                        }
                    }else if (input.startsWith("LOGIN:")) {
                    //Login process
                        String[] parts = input.split(":", 3);
                        if (parts.length != 3) {
                            out.println("ERROR: Invalid login format. Expected: LOGIN:username:password");
                            continue;
                        }

                        String username = parts[1];
                        String password = parts[2];

                        Optional<User> userOpt = Optional.ofNullable(users.get(username));

                        userOpt.filter(user -> user.password().equals(password))
                                .filter(user -> user.role() != null && (user.role().equals("admin") || user.role().equals("user")))
                                .ifPresentOrElse(
                                        user -> out.println(user.role()),
                                        () -> out.println("ERROR: Invalid credentials or role.")
                                );
                    }
                        else if (input.startsWith("PROVISION WEATHER DATA:")) {
                            //Processing Provision command
                            String filePath = input.substring("PROVISION WEATHER DATA:".length()).trim(); 
                            String resultMessage = provisionWeatherData(filePath); 
                            out.println(resultMessage);
                            out.println();
                    } else if (input.equalsIgnoreCase("STOP")) {
                        synchronized (WeatherServer.class) {
                            isRunning = false;
                        }
                        out.println("SERVER_STOPPED");
                        break;
                    }
                    else if (input.startsWith("SET_LOCATION:")) {
                        String[] parts = input.split(":", 4);
                        clientLocation = parts[1];
                        clientLat = Double.parseDouble(parts[2]);
                        clientLon = Double.parseDouble(parts[3]);
                        out.println("Location updated to: " + clientLocation);
                        out.println();
                    } else if (input.equals("GET_WEATHER")) {
                        if (weatherData.containsKey(clientLocation)) {
                            LocationWeather weather = weatherData.get(clientLocation);
                            out.println("Current weather: " + weather.currentWeather);
                            out.println("Temperature: " + weather.temperature + "°C");
                            out.println("Forecast:");
                            for (Pair<String, Integer> pair : weather.forecast) {
                                out.println(" - " + pair.first() + ": " + pair.second() + "°C");
                            }
                            out.println();
                        }else {
                            double radius = 100.0;
                            String closestLocation = findClosestLocation(clientLocation, clientLat, clientLon, radius);
                            if (closestLocation != null) {
                                LocationWeather weather = weatherData.get(closestLocation);
                                out.println("Closest location: " + closestLocation);
                                out.println("Current weather: " + weather.currentWeather);
                                out.println("Temperature: " + weather.temperature);
                                out.println("Forecast:");
                                for (Pair<String, Integer> pair : weather.forecast) {
                                    out.println(" - " + pair.first() + ": " + pair.second() + "°C");
                                }
                                out.println();
                            } else {
                                out.println("No data available for this location or nearby.");
                            }
                        }
                    } else {
                        out.println("Unknown command.");
                        out.println();
                    }
                }
            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            }
        }
    }

    private static void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                writer.write(user.username() + "," + user.password() + "," + user.role());
                writer.newLine();
            }
            System.out.println("Users saved successfully to " + USERS_FILE); 
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2));
    }

    private static String findClosestLocation(String clientLocation, double clientLat, double clientLon, double radius) {
        double closestDistance = radius;
        String closestLocation = clientLocation;

        for (Map.Entry<String, LocationWeather> entry : weatherData.entrySet()) {
            LocationWeather locationWeather = entry.getValue();
            double distance = calculateDistance(clientLat, clientLon, locationWeather.latitude, locationWeather.longitude);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestLocation = entry.getKey();
            }
        }

        return closestLocation;
    }

    static class LocationWeather {
        String currentWeather;
        double temperature;
        List<Pair<String, Integer>> forecast;
        double latitude;
        double longitude;

        LocationWeather(String currentWeather, double temperature, List<Pair<String, Integer>> forecast, double latitude, double longitude) {
            this.currentWeather = currentWeather;
            this.temperature = temperature;
            this.forecast = forecast;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    record Pair<F, S>(F first, S second) {
    }

    record User(String username, String password, String role) {

        @Override
        public int hashCode() {
            return Objects.hash(username, password, role);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            User user = (User) obj;
            return username.equals(user.username) && password.equals(user.password) && role.equals(user.role);
        }
    }
}
