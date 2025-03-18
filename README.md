# 🌦️ WeatherProject

## ✍️ Description
WeatherProject is a weather application developed in Java using Spring Boot. It retrieves real-time weather data from an external API and provides users with weather forecasts for different cities. The project follows the MVC architectural pattern to separate logic from the graphical interface.

## ⚙️ Features
- 🌡️ **Current Weather:** Displays temperature, humidity, and weather conditions.
- 📅 **Forecast:** Provides short-term weather forecasts.
- 🔎 **City Search:** Allows users to search for weather conditions in any city.
- ⚡ **REST API:** The backend serves weather data via REST endpoints.

## 🚀 How to Use
1. 📥 Clone this repository:
```bash
 git clone https://github.com/user/WeatherProject.git
```
2. 📌 Navigate to the project directory:
```bash
 cd WeatherProject
```
3. 🔨 Build the project using Maven:
```bash
 mvn clean install
```
4. ▶️ Run the application:
```bash
 mvn spring-boot:run
```
5. 🌍 Access the application at:
```
 http://localhost:8080
```

## 📁 Project Structure
- `src/main/java/WeatherClient.java` – Handles weather API requests.
- `src/main/java/WeatherServer.java` – Manages backend logic and REST endpoints.
- `src/main/java/DatabaseHelper.java` – Provides database interaction.
- `src/main/resources/application.properties` – Configuration file for the application.
- `pom.xml` – Maven dependencies and project settings.

## 📦 Dependencies
- Java 17+
- Spring Boot
- Maven
- Jackson (for JSON processing)
- PostGreSQL Database 

## 🛠️ Future Improvements
- 🌍 Multi-language support
- 📊 Graphical representation of weather trends
- 📌 Favorite locations feature
