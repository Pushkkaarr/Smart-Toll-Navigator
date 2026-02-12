# Smart Toll Navigator - Backend Service

A Spring Boot REST API service that determines toll plazas located between two Indian pincodes.

## Project Overview

This application provides an API to:
- Accept source and destination pincodes as input
- Calculate the route distance between the pincodes using geolocation services
- Return a list of toll plazas on the route with their details (name, latitude, longitude, distance from source)
- Cache results for improved performance
- Provide comprehensive error handling and validation

## Technology Stack

- **Framework**: Spring Boot 4.0.2
- **Language**: Java 21
- **Build Tool**: Maven

### Key Components

#### Controllers
- **TollPlazaController**: Handles API requests for toll plaza search and health checks

#### Services
- **TollPlazaService**: Main business logic for finding toll plazas between two pincodes
- **MappslIntegrationService**: Integration with Mappls API for routing and geocoding (with mock implementation)
- **TollPlazaCsvLoaderService**: Loads toll plaza data from CSV file at application startup

#### Repository
- **TollPlazaRepository**: In-memory repository for storing and querying toll plaza data

#### Utilities
- **GeoLocationUtil**: Geolocation calculations using Haversine formula
  - Distance calculation between two points
  - Route proximity detection
  - Bounding box generation

#### Models & DTOs
- **TollPlaza**: Domain model for toll plaza
- **TollPlazasRequestDTO**: API request DTO
- **TollPlazasResponseDTO**: API response DTO
- **RouteInfoDTO**: Route information in response
- **TollPlazaDTO**: Toll plaza details in response
- **ErrorResponseDTO**: Error response format

#### Exception Handling
- **CustomExceptions**: InvalidPincodeException, SamePincodeException, RouteNotAvailableException
- **GlobalExceptionHandler**: Centralized exception handling with proper HTTP status codes

## API Specification

### Find Toll Plazas Between Two Pincodes

**Endpoint**: `POST /api/v1/toll-plazas`

**Request Body**:
```json
{
  "sourcePincode": "110001",
  "destinationPincode": "560001"
}
```

**Success Response (200 OK)**:
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "560001",
    "distanceInKm": 2100.0
  },
  "tollPlazas": [
    {
      "name": "Toll Plaza 1",
      "latitude": 28.7041,
      "longitude": 77.1025,
      "distanceFromSource": 200.0
    },
    {
      "name": "Toll Plaza 2",
      "latitude": 19.076,
      "longitude": 72.8777,
      "distanceFromSource": 1400.0
    }
  ]
}
```

**Error Response (400 Bad Request) - Invalid Pincode**:
```json
{
  "error": "INVALID_PINCODE",
  "message": "Invalid source or destination pincode",
  "timestamp": 1631234567890
}
```

**Error Response (400 Bad Request) - Same Pincode**:
```json
{
  "error": "SAME_PINCODE",
  "message": "Source and destination pincodes cannot be the same",
  "timestamp": 1631234567890
}
```

**Error Response (200 OK) - No Toll Plazas on Route**:
```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "560001",
    "distanceInKm": 2100.0
  },
  "tollPlazas": []
}
```

### Health Check

**Endpoint**: `GET /api/v1/toll-plazas/health`

**Response**:
```json
{
  "status": "UP",
  "tollPlazaCount": 1250
}
```

## Setup Instructions

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher
- Git

### Installation & Running

1. **Clone/Navigate to the project**:
```bash
cd "backend/smart-toll"
```

2. **Build the project**:
```bash
mvn clean package
```

3. **Run the application**:
```bash
mvn spring-boot:run
```

Or run the JAR file directly:
```bash
java -jar target/smart-toll-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**


## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=TollPlazaServiceTest
```

### Run with Coverage Report
```bash
mvn test jacoco:report
```

### Test Coverage
- **TollPlazaService**: Tests for core business logic
  - Valid pincode scenarios
  - Invalid pincode validation
  - Same pincode validation
  - Toll plaza retrieval
  - Caching functionality

- **TollPlazaController**: REST API endpoint tests
  - Success scenarios
  - Error handling
  - Validation error responses
  - Health check endpoint

- **GeoLocationUtil**: Geolocation calculation tests
  - Distance calculations between known cities
  - Route proximity detection
  - Bounding box generation

### Testing with Postman

1. **Import the collection** (create a new request):

2. **Test: Find Toll Plazas**
   - Method: `POST`
   - URL: `http://localhost:8080/api/v1/toll-plazas`
   - Headers: `Content-Type: application/json`
   - Body:
   ```json
   {
     "sourcePincode": "560064",
     "destinationPincode": "411045"
   }
   ```

3. **Test: Health Check**
   - Method: `GET`
   - URL: `http://localhost:8080/api/v1/toll-plazas/health`

4. **Test: Invalid Pincode**
   - Method: `POST`
   - URL: `http://localhost:8080/api/v1/toll-plazas`
   - Body:
   ```json
   {
     "sourcePincode": "11001",
     "destinationPincode": "560001"
   }
   ```

5. **Test: Same Pincodes**
   - Method: `POST`
   - URL: `http://localhost:8080/api/v1/toll-plazas`
   - Body:
   ```json
   {
     "sourcePincode": "110001",
     "destinationPincode": "110001"
   }
   ```


## Data Source

- **Toll Plaza Data**: Loaded from `toll_plaza_india.csv` at application startup
- **Geolocation Data**: `all_india_pincodes_india_2025.csv`
- **Route Information**: Calculated using Haversine formula with 15% buffer for road distance

---

**Version**: 1.0.0  
**Last Updated**: February 2026
