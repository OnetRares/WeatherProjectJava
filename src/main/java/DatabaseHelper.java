import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class DatabaseHelper {

    private static final String DB_URL = "your url";
    private static final String DB_USER = "your name";
    private static final String DB_PASSWORD = "your password";

    public static void createTables() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            //Create weather table
            String createWeatherTableSQL = "CREATE TABLE IF NOT EXISTS weather ("
                    + "id SERIAL PRIMARY KEY, "
                    + "location VARCHAR(255) NOT NULL, "
                    + "currentWeather VARCHAR(255), "
                    + "temperature DOUBLE PRECISION, "
                    + "latitude DOUBLE PRECISION, "
                    + "longitude DOUBLE PRECISION)";
            // Create forecast table
            String createForecastTableSQL = "CREATE TABLE IF NOT EXISTS forecast ("
                    + "id SERIAL PRIMARY KEY, "
                    + "weather_id INT, "
                    + "day VARCHAR(255), "
                    + "temperature INT, "
                    + "FOREIGN KEY (weather_id) REFERENCES weather(id) ON DELETE CASCADE)";

            try (Statement stmt = connection.createStatement()) {

                stmt.execute(createWeatherTableSQL);
                System.out.println("Weather table created.");

                stmt.execute(createForecastTableSQL);
                System.out.println("Forecast table created.");
            }
            // create User table
            String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS users ("
                    + "username VARCHAR(255) PRIMARY KEY, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(255) NOT NULL)";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createUsersTableSQL);
                System.out.println("Users table created.");
            }
        } catch (SQLException e) {
            System.err.println("Error at table create: " + e.getMessage());
        }
    }
    public static void loadWeatherDataIntoDatabase() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/weather_data.json"))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONArray jsonArray = new JSONArray(jsonBuilder.toString());

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String checkLocationSQL = "SELECT id FROM weather WHERE location = ?";
                String insertWeatherSQL = "INSERT INTO weather (location, currentWeather, temperature, latitude, longitude) "
                        + "VALUES (?, ?, ?, ?, ?)";
                String insertForecastSQL = "INSERT INTO forecast (weather_id, day, temperature) VALUES (?, ?, ?)";
                String checkForecastSQL = "SELECT COUNT(*) FROM forecast WHERE weather_id = ? AND day = ? AND temperature = ?";

                try (PreparedStatement checkStmt = connection.prepareStatement(checkLocationSQL);
                     PreparedStatement insertWeatherStmt = connection.prepareStatement(insertWeatherSQL, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement insertForecastStmt = connection.prepareStatement(insertForecastSQL);
                     PreparedStatement checkForecastStmt = connection.prepareStatement(checkForecastSQL)) {

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject weatherEntry = jsonArray.getJSONObject(i);

                        String location = weatherEntry.getString("location");
                        String currentWeather = weatherEntry.getString("currentWeather");
                        double temperature = weatherEntry.getDouble("temperature");
                        double latitude = weatherEntry.getDouble("latitude");
                        double longitude = weatherEntry.getDouble("longitude");

                        checkStmt.setString(1, location);
                        ResultSet rs = checkStmt.executeQuery();
                        int weatherId = -1;

                        if (rs.next()) {
                            weatherId = rs.getInt("id");
                        }

                        if (weatherId == -1) {
                            // Insert new weather data if it doesn't exist
                            insertWeatherStmt.setString(1, location);
                            insertWeatherStmt.setString(2, currentWeather);
                            insertWeatherStmt.setDouble(3, temperature);
                            insertWeatherStmt.setDouble(4, latitude);
                            insertWeatherStmt.setDouble(5, longitude);
                            insertWeatherStmt.executeUpdate();

                            ResultSet generatedKeys = insertWeatherStmt.getGeneratedKeys();
                            if (generatedKeys.next()) {
                                weatherId = generatedKeys.getInt(1);
                            }
                        }

                        // Process forecast data
                        JSONArray forecastArray = weatherEntry.getJSONArray("forecast");
                        for (int j = 0; j < forecastArray.length(); j++) {
                            JSONObject forecast = forecastArray.getJSONObject(j);
                            String day = forecast.getString("day");
                            int forecastTemperature = forecast.getInt("temperature");

                            // Check if this forecast already exists
                            checkForecastStmt.setInt(1, weatherId);
                            checkForecastStmt.setString(2, day);
                            checkForecastStmt.setInt(3, forecastTemperature);
                            ResultSet forecastRs = checkForecastStmt.executeQuery();

                            if (forecastRs.next() && forecastRs.getInt(1) == 0) {
                                // Insert new forecast if it doesn't exist
                                insertForecastStmt.setInt(1, weatherId);
                                insertForecastStmt.setString(2, day);
                                insertForecastStmt.setInt(3, forecastTemperature);
                                insertForecastStmt.executeUpdate();
                            }
                        }
                    }

                    System.out.println("Weather and forecast data has been processed.");
                }
            } catch (SQLException e) {
                System.err.println("Error inserting weather and forecast data: " + e.getMessage());
            }
        } catch (IOException | JSONException e) {
            System.err.println("Error reading or parsing JSON file: " + e.getMessage());
        }
    }


    public static void loadUsersFromTextFileToDatabase() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/users.txt"))) {
            String line;
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String insertUserSQL = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                String updateUserSQL = "UPDATE users SET password = ?, role = ? WHERE username = ?";

                try (PreparedStatement insertStmt = connection.prepareStatement(insertUserSQL);
                     PreparedStatement updateStmt = connection.prepareStatement(updateUserSQL)) {

                    while ((line = reader.readLine()) != null) {

                        if (line.trim().isEmpty() || line.startsWith("#")) continue;


                        String[] userFields = line.split(",");
                        if (userFields.length == 3) {
                            String username = userFields[0].trim();
                            String password = userFields[1].trim();
                            String role = userFields[2].trim();


                            String checkUserSQL = "SELECT COUNT(*) FROM users WHERE username = ?";
                            try (PreparedStatement checkStmt = connection.prepareStatement(checkUserSQL)) {
                                checkStmt.setString(1, username);
                                ResultSet rs = checkStmt.executeQuery();
                                if (rs.next() && rs.getInt(1) > 0) {

                                    updateStmt.setString(1, password);
                                    updateStmt.setString(2, role);
                                    updateStmt.setString(3, username);
                                    updateStmt.executeUpdate();
                                } else {

                                    insertStmt.setString(1, username);
                                    insertStmt.setString(2, password);
                                    insertStmt.setString(3, role);
                                    insertStmt.executeUpdate();
                                }
                            }
                        }
                    }
                    System.out.println("User data has been processed from the text file.");
                }
            } catch (SQLException e) {
                System.err.println("Error inserting or updating users: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error reading text file: " + e.getMessage());
        }
    }


}