# Smart Campus Sensor & Room Management API

## 1. API Overview

This project is a JAX-RS RESTful API built using Jersey with an embedded Grizzly HTTP server.

Key characteristics:
- No Spring Framework or Spring Boot
- No database usage
- In-memory storage only using ConcurrentHashMap and ArrayList collections
- Java 11+
- Maven project
- Base URL: http://localhost:8080/api/v1

Main capabilities:
- API discovery endpoint
- Room management
- Sensor management
- Sensor reading sub-resources
- Custom exception mapping with JSON error bodies
- Request/response logging filter

Preloaded sample data includes:
- 3 rooms: LIB-301, LAB-101, HALL-01
- 4 sensors: TEMP-001, CO2-001, OCC-001, TEMP-002

## 2. Step-by-Step Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.8+

### Build
Run from the project root:

```bash
mvn clean package
```

### Run (Option A: Executable JAR)

```bash
java -jar target/smartcampus.jar
```

### Run (Option B: Maven Exec)

```bash
mvn exec:java
```

On startup, the server prints:

```text
Smart Campus API running at: http://localhost:8080/api/v1
```

## 3. Required curl Command Examples

### 1) GET all rooms

```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 2) POST a new room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
	-H "Content-Type: application/json" \
	-d "{\"name\":\"Engineering Seminar Room\",\"capacity\":120}"
```

### 3) POST a sensor with an invalid roomId (422)

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
	-H "Content-Type: application/json" \
	-d "{\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":420.0,\"roomId\":\"INVALID-ROOM\"}"
```

### 4) POST a reading to MAINTENANCE sensor OCC-001 (403)

```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
	-H "Content-Type: application/json" \
	-d "{\"value\":1.0}"
```

### 5) GET sensors filtered by type=CO2

```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```