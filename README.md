# SpringFlux Banking Integration

A multi-endpoint banking integration layer built with **Spring WebFlux** and **Resilience4j**, demonstrating reactive, non-blocking microservice patterns.

## Features

- **Reactive Endpoints** — Account balance, transaction history, and KYC verification
- **Non-blocking WebClient** — Fully reactive HTTP client for upstream banking API calls
- **Resilience Patterns** — Circuit breakers, retries with exponential backoff, and timeouts via Resilience4j
- **Centralized Error Handling** — Global exception handler with structured error responses
- **Observability** — Structured logging, Micrometer metrics, Prometheus endpoint, and Spring Boot Actuator health checks

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.2 / Spring WebFlux |
| Resilience | Resilience4j (circuit breaker, retry, time limiter) |
| Metrics | Micrometer + Prometheus |
| HTTP Client | WebClient (Reactor Netty) |
| Testing | JUnit 5, Reactor Test, MockWebServer |
| Build | Maven |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/banking/accounts/{accountId}/balance` | Get account balance |
| GET | `/api/v1/banking/accounts/{accountId}/transactions?limit=50` | Get transaction history |
| GET | `/api/v1/banking/customers/{customerId}/kyc` | Verify KYC status |

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+

### Build & Run

```bash
./mvnw clean install
./mvnw spring-boot:run
```

### Configuration

Set the upstream banking API URL via environment variable:

```bash
export BANKING_API_BASE_URL=http://your-banking-api:9090/api/v1
```

### Actuator & Metrics

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check with circuit breaker status |
| `/actuator/metrics` | Application metrics |
| `/actuator/prometheus` | Prometheus-compatible metrics |

## Project Structure

```
src/main/java/com/springflux/banking/
├── BankingApplication.java          # Application entry point
├── config/
│   ├── WebClientConfig.java         # WebClient with logging filters
│   └── MetricsConfig.java           # Micrometer common tags
├── controller/
│   └── BankingController.java       # Reactive REST endpoints
├── exception/
│   └── GlobalExceptionHandler.java  # Centralized error handling
├── model/
│   ├── AccountBalance.java          # Account balance DTO
│   ├── Transaction.java             # Transaction DTO
│   ├── KycVerification.java         # KYC verification DTO
│   └── ErrorResponse.java           # Error response DTO
└── service/
    └── BankingService.java          # Business logic with resilience patterns
``` 
