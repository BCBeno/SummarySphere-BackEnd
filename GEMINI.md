# SummarySphere-Backend - Project Instructions

This project is a Spring Boot-based microservice for AI-powered course summarization. It allows users to upload documents (PDF, Word, Text), extracts their content using Apache Tika, persists them in Azure Blob Storage, and generates summaries using Google's Gemini AI via Spring AI.

## Project Overview

- **Purpose:** Provide an API for students to upload materials and receive structured summaries.
- **Core Technologies:**
  - **Framework:** Spring Boot 4 (Java 21)
  - **AI Engine:** Spring AI with Google Gemini (Gemini 1.5 Flash)
  - **Persistence:** PostgreSQL (Database) & Azure Blob Storage (Document/Summary files)
  - **Security:** JWT-based stateless authentication via Spring Security
  - **File Processing:** Apache Tika (Text extraction)
  - **API Documentation:** SpringDoc OpenAPI (Swagger UI)

## Architecture

The project follows a standard Spring Boot layered architecture:
- `controllers/`: REST endpoints (Auth, Chat, Document, User).
- `services/`: Business logic, with implementations in `impl/`.
- `repositories/`: Spring Data JPA interfaces for PostgreSQL.
- `entities/`: JPA models (User, Document, DocumentSummary, ChatMessage).
- `config/`: Configuration for Security, Azure, Swagger, and Spring AI.
- `dtos/`: Data Transfer Objects for API requests/responses.

## Building and Running

### Prerequisites
- Java 21
- Maven
- PostgreSQL (or use Docker Compose)
- Azure Storage Account
- Google Gemini API Key

### Commands
- **Build:** `mvn clean install`
- **Run:** `mvn spring-boot:run`
- **Test:** `mvn test`
- **Docker Compose:** `docker compose up -d` (for local PostgreSQL)

### Environment Variables
The application expects the following variables (can be provided via a `.env` file):
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL connection details.
- `GOOGLE_API_KEY`: API key for Gemini.
- `JWT_SECRET`: Secret key for token signing.
- `AZURE_STORAGE_CONNECTION_STRING`: Connection string for Azure Blobs.
- `AZURE_STORAGE_CONTAINER_NAME`: Target container name.
- `MAIL_EMAIL`, `MAIL_PASS`: SMTP credentials (Gmail).
- `RESEND_API_KEY`: API key for Resend email service.

## Development Conventions

- **Security:** Most endpoints under `/api/**` require a valid JWT token. Public endpoints are `/api/auth/**` and `/api-docs/**`.
- **Async Processing:** Summarization is handled asynchronously via `@Async` in `GeminiServiceImpl`.
- **Data Mapping:** Uses `ModelMapper` for converting between Entities and DTOs.
- **Exception Handling:** Centralized in `GlobalExceptionHandler`.
- **Testing:**
  - JUnit 5 and Mockito for unit testing.
  - H2 in-memory database used for testing (`application-test.properties`).
- **File Storage:** Original files and extracted text are stored in Azure. Summaries are also stored as text blobs.

## API Documentation
Once running, the Swagger UI is available at:
`http://localhost:8080/swagger-ui/index.html`
