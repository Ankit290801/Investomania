# Investment Tracker - Backend

Spring Boot backend for the Investment Tracking and Portfolio Analysis application.

## Technology Stack

- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Build Tool**: Maven 3.8+
- **Database**: H2 (development), PostgreSQL 15+ (production)
- **Security**: Spring Security 6.x + JWT
- **ORM**: Spring Data JPA + Hibernate

## Project Structure

```
src/main/java/com/investment/tracker/
├── InvestmentTrackerApplication.java  # Main application class
├── config/                             # Configuration classes
│   ├── AppConfig.java                 # General app configuration
│   └── CorsConfig.java                # CORS configuration for frontend
├── security/                           # Security components
│   └── SecurityConstants.java         # Security constants
├── model/                              # Entity classes (JPA entities)
│   └── BaseEntity.java                # Base entity with common fields
├── repository/                         # JPA repositories
│   └── BaseRepository.java            # Base repository
├── service/                            # Business logic layer
│   └── BaseService.java               # Base service class
├── controller/                         # REST API controllers
│   └── BaseController.java            # Base controller
├── dto/                                # Data Transfer Objects
│   └── BaseDTO.java                   # Base DTO
├── scheduler/                          # Scheduled tasks
│   └── BaseScheduler.java             # Base scheduler
├── exception/                          # Custom exceptions
│   └── CustomException.java           # Exception placeholder
└── util/                               # Utility classes
    └── UtilityHelper.java             # Utility methods

src/main/resources/
├── application.yml                     # Base application config
├── application-dev.yml                 # Development profile
├── application-prod.yml                # Production profile
└── config/                             # External configuration files
    ├── api-config.yml                 # API configurations
    ├── tax-rules.yml                  # Country-specific tax rules
    ├── currency-config.yml            # Currency configurations
    └── business-rules.yml             # Business rules and validations
```

## Configuration

### Environment Variables

Create a `.env` file in the backend directory (use `.env.example` as template):

```env
ACTIVE_PROFILE=dev
JWT_SECRET=your-super-secret-jwt-key-min-256-bits-required
JWT_EXPIRATION=86400000
DB_URL=jdbc:postgresql://localhost:5432/investmentdb
DB_USERNAME=postgres
DB_PASSWORD=your-password
EXCHANGE_RATE_API_KEY=your-api-key
ALPHA_VANTAGE_API_KEY=your-api-key
```

### Profiles

- **dev**: Uses H2 in-memory database, debug logging, H2 console enabled at `/h2-console`
- **prod**: Uses PostgreSQL, info logging, production-ready settings

## Running the Application

### Prerequisites

- JDK 17 or higher
- Maven 3.8+

### Development Mode

```bash
# Using Maven
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Using Java
mvn clean package
java -jar target/investment-tracker-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### H2 Console (Development Only)

Access H2 database console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/investmentdb`
- Username: `sa`
- Password: (leave empty)

## API Documentation

### Base URL
- Development: `http://localhost:8080/api`
- Production: `https://your-domain.com/api`

### Endpoints (To be implemented in phases)

**Authentication** (`/api/auth`)
- `POST /register` - User registration
- `POST /login` - User login
- `POST /refresh` - Refresh JWT token

**Investments** (`/api/investments`)
- `GET /` - List all investments
- `GET /{id}` - Get investment details
- `POST /` - Create investment
- `PUT /{id}` - Update investment
- `DELETE /{id}` - Delete investment

**Transactions** (`/api/transactions`)
- `GET /` - List transactions
- `POST /investments/{investmentId}/transactions` - Add transaction
- `DELETE /{id}` - Delete transaction

**Analytics** (`/api/analytics`)
- `GET /networth` - Get net worth
- `GET /segments` - Get segment breakdown
- `GET /yoy-growth` - Year-over-year growth

**Expenses** (`/api/expenses`)
- `GET /` - List expenses
- `POST /` - Create expense
- `PUT /{id}` - Update expense
- `DELETE /{id}` - Delete expense

**Tax** (`/api/tax`)
- `GET /report` - Tax report
- `GET /projection` - Tax projection

**Data Import/Export** (`/api/data`)
- `POST /import/csv` - Import CSV
- `POST /import/excel` - Import Excel
- `POST /import/pdf` - Import PDF (Contract Notes)
- `GET /export/csv` - Export CSV
- `GET /export/excel` - Export Excel
- `GET /backup` - JSON backup
- `POST /restore` - JSON restore

## Dependencies

### Core Dependencies
- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- spring-boot-starter-validation

### Database
- h2 (runtime, development)
- postgresql (runtime, production)

### Security
- io.jsonwebtoken:jjwt-api:0.12.5
- io.jsonwebtoken:jjwt-impl:0.12.5
- io.jsonwebtoken:jjwt-jackson:0.12.5

### Utilities
- lombok (optional, compile-time)
- spring-boot-devtools (optional, development)

### Data Processing
- org.apache.pdfbox:pdfbox:3.0.2
- org.apache.poi:poi-ooxml:5.2.5
- com.opencsv:opencsv:5.9

### Testing
- spring-boot-starter-test
- spring-security-test

## Build

```bash
# Clean and build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run tests only
mvn test

# Clean install
mvn clean install
```

## Database Migrations

Database migrations will be handled by Liquibase (to be added in Phase 10).

For development, we use `ddl-auto: update` which auto-creates/updates schema.

For production, use `ddl-auto: validate` and manage schema with Liquibase migrations.

## Security

- JWT-based authentication
- BCrypt password encoding
- CORS configured for frontend origins
- Stateless session management
- Protected endpoints require Bearer token

## Configuration Externalization

All business rules, tax configurations, API settings, and currency configs are externalized in YAML files under `src/main/resources/config/`. This allows easy customization without code changes.

## Logging

- Development: DEBUG level for application packages
- Production: INFO level

Logs include:
- HTTP requests/responses
- Database queries (dev only)
- Security events
- Scheduled task execution

## Error Handling

Global exception handling will be implemented using `@ControllerAdvice` to return consistent error responses.

## Development Notes

- Use Lombok annotations to reduce boilerplate (`@Data`, `@Builder`, etc.)
- All entities should extend `BaseEntity` for common fields
- All DTOs should follow naming convention: `{Entity}DTO`, `{Entity}Request`, `{Entity}Response`
- Use constructor injection for dependencies
- Follow REST API best practices
- Validate all input using Bean Validation annotations

## Next Steps (Implementation Phases)

1. ✅ **Phase 1**: Core Infrastructure Setup (Current)
2. **Phase 2**: Authentication & Authorization (JWT, User management)
3. **Phase 3**: Investment Management Core (Entities, CRUD APIs)
4. **Phase 4**: Market Data Integration (Yahoo/Google Finance APIs)
5. **Phase 5**: Analytics & Dashboard (Calculations, Reports)
6. **Phase 6**: Expense Tracking (Expense entities, Asset mapping)
7. **Phase 7**: Tax Calculations (STCG/LTCG)
8. **Phase 8**: Data Import/Export (CSV, Excel, PDF, JSON)
9. **Phase 9**: Performance & UX Enhancements
10. **Phase 10**: Testing & Deployment Prep

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [JWT.io](https://jwt.io)

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: May 6, 2026
