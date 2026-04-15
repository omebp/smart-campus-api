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

## 4. Report

### Part 1: Service Architecture & Setup

#### Question 1.1: JAX-RS resource lifecycle and shared state
By default, JAX-RS resources are request-scoped: the runtime creates a new resource instance for each incoming request. A resource behaves like a singleton only if it is explicitly registered as an object instance or annotated/configured as singleton.

In this project, resources are registered as classes in `SmartCampusApplication#getClasses()`, so Jersey applies the default per-request lifecycle. This means instance fields inside resource classes are not shared between requests, which reduces accidental thread-safety bugs.

However, the API intentionally keeps shared in-memory state in `DataStore`, which is a singleton. The `ConcurrentHashMap` collections (`rooms`, `sensors`, `sensorReadings`) make map-level operations thread-safe (for example `put`, `get`, `remove`). The important caveat is that nested mutable lists are plain `ArrayList` objects, so concurrent writes to the same list can still race. To prevent data loss in high-concurrency scenarios, list updates should be synchronized (or replaced with thread-safe list implementations) and multi-step read-modify-write sequences should be guarded.

#### Question 1.2: Why hypermedia (HATEOAS) is an advanced REST feature
Hypermedia makes responses self-descriptive by embedding navigation and action links. Instead of hardcoding endpoint flows from static documentation, clients follow links returned at runtime.

This is considered advanced RESTful design because it improves evolvability and reduces tight coupling. If endpoint structure changes, clients can continue working as long as link relations remain stable. In this API, discovery information and resource URLs already provide a foundation for this style; adding richer per-resource links (for example, from a room to related sensors and allowed next actions) would further strengthen HATEOAS compliance.

### Part 2: Room Management

#### Question 2.1: Returning room IDs only vs full room objects
Returning only IDs is bandwidth-efficient and keeps collection responses small, especially for large datasets. It is useful when clients only need identifiers and can fetch details lazily.

Returning full room objects reduces extra client round-trips and simplifies UI rendering, but increases payload size and parsing cost. In this implementation, `GET /rooms` returns full room objects (id, name, capacity, sensorIds), which favors client convenience for small-to-medium result sets. A common scalable pattern is to keep list responses lightweight and provide full detail through `GET /rooms/{roomId}`.

#### Question 2.2: DELETE idempotency in this implementation
Yes, the DELETE behavior is idempotent with respect to server state.

For `DELETE /rooms/{roomId}` in `RoomResource`:
1. If the room exists and has no sensors, the first call removes it and returns `204 No Content`.
2. If the same request is repeated, the room is already absent, so the API returns `404 Not Found`.
3. If the room still has linked sensors, the request returns `409 Conflict` (via `RoomNotEmptyExceptionMapper`) and state remains unchanged; repeating the same request keeps state unchanged.

Idempotency requires that repeating the same request does not continue changing server state after the first application. It does not require identical status codes on every repetition.

### Part 3: Sensor Operations & Linking

#### Question 3.1: Consequences of sending non-JSON content
POST methods in this API use `@Consumes(MediaType.APPLICATION_JSON)`. If a client sends `text/plain`, `application/xml`, or another unsupported media type, Jersey rejects the request before entering the resource method and responds with `415 Unsupported Media Type`.

If the `Content-Type` is JSON but the body is malformed, Jersey/Jackson typically produce a `400 Bad Request` parsing/mapping error. In both cases, business logic is not executed and in-memory state is not modified.

#### Question 3.2: `@QueryParam` filtering vs type in path
Using `@QueryParam("type")` for `/sensors?type=CO2` is generally superior for collection filtering because filters are optional, composable, and non-hierarchical. It is easy to extend with additional criteria like `status`, `roomId`, pagination, and sorting without creating many path variants.

A path like `/sensors/type/CO2` implies a fixed sub-collection hierarchy rather than an optional search criterion, and it scales poorly as filter combinations grow. Path segments are best for resource identity and hierarchy; query parameters are best for filtering/searching collections.

### Part 4: Deep Nesting with Sub-Resources

#### Question 4.1: Benefits of the sub-resource locator pattern
Sub-resource locators improve architecture by delegating nested concerns to dedicated classes. In this API, `SensorResource` routes `/{sensorId}/readings` to `SensorReadingResource`, which keeps sensor management logic separate from reading management logic.

This provides clear separation of responsibilities, smaller and easier-to-test classes, cleaner URI-to-code mapping, and better long-term maintainability. Without this pattern, one large controller would accumulate many nested endpoints and become harder to reason about, test, and evolve.

### Part 5: Advanced Error Handling & Logging

#### Question 5.1: Why 422 is more accurate than 404 for missing linked references
`404 Not Found` means the requested target resource/endpoint does not exist. In `POST /sensors`, the endpoint does exist, and the JSON can be syntactically valid, but the referenced `roomId` may not exist. That is a semantic validation failure inside an otherwise valid request, so `422 Unprocessable Entity` is more precise.

This API applies that rule through `LinkedResourceNotFoundExceptionMapper`, which returns `422` when a sensor payload references a non-existent room.

#### Question 5.2: Security risks of exposing Java stack traces
Exposing stack traces to external clients leaks internal implementation details, including package/class names, framework call chains, library versions, and sometimes filesystem paths or configuration hints. Attackers can use this intelligence to fingerprint the stack, identify known vulnerabilities, and craft targeted exploits.

The safer approach is to return sanitized error responses externally and log full exception details internally. This API follows that pattern through `GlobalExceptionMapper`, which logs server-side details but returns a generic `500` JSON message for unexpected failures.

#### Question 5.3: Why JAX-RS filters are better for cross-cutting logging
Logging is a cross-cutting concern. Implementing it in a JAX-RS filter (`LoggingFilter`) ensures every request/response is logged consistently in one place, with no duplication across resource methods.

Compared to manually adding `Logger.info()` in every endpoint, filters reduce boilerplate, prevent missed endpoints, simplify policy updates (format, redaction, correlation IDs), and keep resource classes focused on business behavior instead of infrastructure concerns.