# HabitO Backend - Quick Start Guide

## Overview
The HabitO backend is a Spring Boot application that provides a REST API for habit tracking integrated with Habitica.

## Requirements
- Java 17 or higher
- Maven 3.6+ (or use included Maven wrapper)
- MySQL (for production) or H2 (for development)

## Running the Application

### Development Mode (H2 Database)
```bash
cd habito
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- Application will run on: http://localhost:8080
- H2 Console available at: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: (leave empty)

### Production Mode (MySQL Database)
```bash
cd habito
./mvnw spring-boot:run
```
Make sure MySQL is running and configured with:
- Database: `habito`
- Username: `root`
- Password: `root123`

## API Endpoints

### Authentication
- **POST** `/api/auth/login`
  ```json
  {
    "habiticaUserId": "your-habitica-user-id",
    "habiticaApiToken": "your-habitica-api-token"
  }
  ```

### Stats & Data
- **GET** `/api/stats/user` - Get user stats from Habitica
- **GET** `/api/stats/tasks` - Get user tasks from Habitica

### Task Management
- **POST** `/api/stats/tasks/create` - Create a new task
- **PUT** `/api/stats/tasks/{taskId}` - Update a task
- **DELETE** `/api/stats/tasks/{taskId}` - Delete a task
- **POST** `/api/stats/tasks/{taskId}/complete` - Mark task as complete

## Testing
```bash
cd habito
./mvnw test
```

## Building
```bash
cd habito
./mvnw clean package
```

## Notes
- All endpoints except `/api/auth/login` require authentication (valid session)
- The application uses session-based authentication
- Static files are served from `/src/main/resources/static/`
- CORS is enabled for all origins during development