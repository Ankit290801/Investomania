# Investment Tracking & Portfolio Analysis Application - Project Context

**Date Created**: May 1, 2026
**Status**: Planning Phase

---

## Project Overview

Multi-user web application for investment tracking and portfolio analysis with comprehensive features including transaction tracking, market data integration, analytics, expense management, and tax calculations.

---

## Requirements Summary

### Core Features

1. **Entry Forms for Investment Types**
   - Equity (public & private)
   - Bonds
   - Recurring Deposits (RD)
   - Fixed Deposits (FD)
   - National Pension System (NPS)
   - Public Provident Fund (PPF)
   - Real Estate
   - Cryptocurrency
   - Cash in banks
   - Salary income
   - Each form contains type-specific attributes

2. **Dashboard & Analytics**
   - Net worth display (multi-currency)
   - Segment details: equity, safe assets, illiquid assets
   - Details of investments in each segment
   - Segmental growth and its impact on net portfolio
   - YoY (Year-over-Year) growth of net worth
   - Visual charts and graphs

3. **Expense Management**
   - Expenses as a separate segment
   - Percentage of expenses covered by investment assets
   - Option to map expenses to one or more investment assets
   - Coverage analysis

4. **Additional Features** (Clarified Requirements)
   - Multi-user with login/registration and authentication
   - Full transaction history tracking (buy/sell/dividend/interest)
   - Market data integration:
     - Manual entry for private equity
     - Auto-fetch for listed equities via Yahoo Finance / Google Finance APIs
     - Support for Indian markets (NSE/BSE) and foreign markets (NYSE/NASDAQ/etc.)
   - Tax calculations (STCG/LTCG) with country-specific rules
   - Multi-currency support
   - Data import/export:
     - CSV import/export
     - Excel import/export
     - PDF imports for Contract Notes (broker statements)
     - JSON backup/restore

---

## Technology Stack (Finalized)

### Frontend
- **Framework**: React 18+
- **Build Tool**: Vite 4+
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **UI Framework**: Material-UI (MUI) v5
- **Charts**: Recharts v2
- **State Management**: Context API for auth and global state
- **Form Handling**: React Hook Form + Yup
- **Utilities**: date-fns, Axios interceptors
- **Testing**: Jest + React Testing Library (post-MVP)

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Build Tool**: Maven 3.8+
- **Security**: Spring Security 6.x + JWT (io.jsonwebtoken:jjwt)
- **Database**: Spring Data JPA + Hibernate
- **Scheduler**: Spring @Scheduled for market data updates
- **Dependencies**: 
  - Spring Web (REST API)
  - Spring Security
  - Spring Data JPA
  - H2 Database (development)
  - PostgreSQL Driver (production)
  - Bean Validation (JSR-380)
  - Lombok
  - Jackson (JSON)
  - Apache PDFBox (PDF parsing)
  - Apache POI (Excel)
  - OpenCSV (CSV processing)
- **Testing**: JUnit 5 + Mockito + Spring Boot Test (post-MVP)
- **Database Migration**: Liquibase (add post-MVP)

### Database
- **Development**: H2 (embedded, file-based)
- **Production**: PostgreSQL 15+

### External APIs
- Yahoo Finance API (primary for market data)
- Google Finance API (fallback)
- ExchangeRate-API.io (currency conversion, 1500 free requests/month)
- Alpha Vantage API (optional paid fallback)

---

## Database Schema Design

### Core Entities

#### Users
- id (Primary Key)
- username
- email (unique)
- password_hash
- created_at
- updated_at

#### Investments
- id (Primary Key)
- user_id (Foreign Key → Users)
- type (ENUM: EQUITY, BOND, FD, RD, NPS, PPF, REAL_ESTATE, CRYPTO, CASH)
- name
- currency
- created_at
- updated_at

**Type-Specific Attributes** (using inheritance or JSON fields):
- **Equity**: symbol, quantity, avg_price, market (NSE/BSE/NYSE/NASDAQ)
- **FD/RD**: principal, interest_rate, tenure_months, maturity_date, bank_name
- **NPS/PPF**: contribution_frequency, total_contributed, account_number
- **Bonds**: face_value, coupon_rate, maturity_date, credit_rating, issuer

#### Transactions
- id (Primary Key)
- investment_id (Foreign Key → Investments)
- type (ENUM: BUY, SELL, DIVIDEND, INTEREST, CONTRIBUTION)
- quantity (nullable for non-equity)
- price_per_unit (nullable for non-equity)
- amount
- transaction_date
- currency
- notes
- created_at

#### MarketPrices
- id (Primary Key)
- symbol
- market (NSE/BSE/NYSE/NASDAQ)
- price
- currency
- last_updated

#### Expenses
- id (Primary Key)
- user_id (Foreign Key → Users)
- category (housing, food, transport, healthcare, entertainment, etc.)
- amount
- currency
- expense_date
- description
- created_at

#### ExpenseAssetMapping
- id (Primary Key)
- expense_id (Foreign Key → Expenses)
- investment_id (Foreign Key → Investments)
- percentage (sum across all mappings for an expense = 100%)

#### TaxProfiles
- id (Primary Key)
- user_id (Foreign Key → Users)
- country
- holding_period_short_days (e.g., 365 for India equity)
- holding_period_long_days
- short_term_tax_rate
- long_term_tax_rate
- created_at
- updated_at

#### CurrencyRates (for forex conversion)
- id (Primary Key)
- from_currency
- to_currency
- rate
- last_updated

---

## Implementation Plan (10 Phases, 52 Steps)

### Phase 1: Core Infrastructure Setup
1. Initialize Spring Boot project with dependencies
2. Initialize React project with libraries
3. Setup project structure (backend & frontend folders)
4. Configure H2 database connection and JPA properties
5. Setup CORS configuration for local development

### Phase 2: Authentication & Authorization
6. Implement User entity and repository
7. Create JWT utility classes (token generation, validation, parsing)
8. Implement Spring Security config with JWT filter
9. Create auth REST endpoints: /api/auth/register, /api/auth/login, /api/auth/refresh
10. Create React auth context, login/register pages, protected route wrapper
11. Implement token storage (localStorage/sessionStorage) and auto-refresh logic

### Phase 3: Investment Management Core
12. Create investment type enum and Investment entity with discriminator/inheritance
13. Create Transaction entity with relationship to Investment
14. Implement REST APIs: CRUD for investments, transaction logging
15. Create React forms for each investment type with type-specific fields
16. Build transaction history UI with filters (date range, type, investment)
17. Implement multi-currency support: currency field in entities, conversion rate table, display logic

### Phase 4: Market Data Integration
18. Create MarketPrice entity and repository
19. Implement Yahoo Finance API integration service for listed equities
20. Implement Google Finance API fallback service
21. Create scheduled job (Spring @Scheduled) to auto-fetch prices daily
22. Build manual price update UI for private equities
23. Handle foreign markets (NYSE, NASDAQ, etc.) with proper symbol formats

### Phase 5: Analytics & Dashboard
24. Create service layer for calculations:
    - Net worth: sum of all investment current values across currencies
    - Segment totals: group by type
    - YoY growth: compare net worth year-over-year
    - Segmental growth impact: each segment's contribution to total growth
25. Build dashboard UI with cards/widgets:
    - Net worth display with currency selector
    - Segment breakdown pie chart
    - YoY growth line chart
    - Top performing investments table
    - Segmental growth contribution bar chart
26. Add filters: date range, currency view, segment selection

### Phase 6: Expense Tracking
27. Create Expense entity and ExpenseAssetMapping entity
28. Implement expense CRUD REST APIs
29. Build expense entry form with categories
30. Create expense-to-asset mapping UI:
    - Select expense
    - Choose one or more investments to map
    - Set percentage distribution (sum = 100%)
31. Calculate coverage metrics: total expenses vs investment income
32. Build expense dashboard: monthly trends, category breakdown, coverage percentage

### Phase 7: Tax Calculations
33. Create TaxProfile entity for different countries/regions
34. Implement capital gains calculation service:
    - STCG for holdings < threshold
    - LTCG for holdings >= threshold
    - Different rates for equity vs other assets
    - Country-specific rules
35. Build tax report page showing realized/unrealized gains by year, investment type
36. Add tax projection feature: "if I sell now" calculator

### Phase 8: Data Import/Export
37. Implement CSV/Excel export for investments, transactions, expenses
38. Build CSV/Excel import parser with validation
39. Create PDF parser for Contract Notes using Apache PDFBox:
    - Extract transaction details (date, symbol, quantity, price)
    - Support common broker formats (Zerodha, Groww, Upstox, etc.)
    - Manual review UI before import
40. Implement JSON backup/restore:
    - Export all user data as JSON
    - Import/restore from JSON backup
    - Version compatibility handling

### Phase 9: Performance & UX Enhancements
41. Add loading states, skeleton screens, error boundaries
42. Implement pagination for transaction lists, investment grids
43. Add search/filter capabilities across all lists
44. Create data visualization improvements (tooltips, drill-downs, legends)
45. Implement responsive design for mobile/tablet
46. Add caching for market data, calculation results

### Phase 10: Testing & Deployment Prep
47. Write unit tests for critical services (calculation logic, JWT, API parsers)
48. Write integration tests for REST endpoints
49. Create production database migration scripts (H2 → PostgreSQL/MySQL)
50. Setup environment configurations (dev, staging, prod)
51. Create Docker containers for frontend and backend
52. Setup deployment pipeline (CI/CD optional)

---

## Project Structure

### Backend (Spring Boot)
```
src/main/java/com/investment/tracker/
├── InvestmentTrackerApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── CorsConfig.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── model/
│   ├── User.java
│   ├── Investment.java (base class)
│   ├── EquityInvestment.java
│   ├── BondInvestment.java
│   ├── FDInvestment.java
│   ├── Transaction.java
│   ├── TransactionType.java (enum)
│   ├── MarketPrice.java
│   ├── Expense.java
│   ├── ExpenseAssetMapping.java
│   ├── TaxProfile.java
│   └── CurrencyRate.java
├── repository/
│   ├── UserRepository.java
│   ├── InvestmentRepository.java
│   ├── TransactionRepository.java
│   ├── MarketPriceRepository.java
│   ├── ExpenseRepository.java
│   ├── TaxProfileRepository.java
│   └── CurrencyRateRepository.java
├── service/
│   ├── AuthService.java
│   ├── InvestmentService.java
│   ├── TransactionService.java
│   ├── MarketDataService.java
│   ├── AnalyticsService.java
│   ├── ExpenseService.java
│   ├── TaxCalculationService.java
│   └── ImportExportService.java
├── controller/
│   ├── AuthController.java
│   ├── InvestmentController.java
│   ├── TransactionController.java
│   ├── AnalyticsController.java
│   ├── ExpenseController.java
│   ├── TaxController.java
│   └── DataController.java
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── JwtResponse.java
│   ├── InvestmentDTO.java
│   ├── TransactionDTO.java
│   └── AnalyticsDTO.java
└── scheduler/
    └── MarketDataScheduler.java
```

### Frontend (React)
```
src/
├── App.jsx
├── main.jsx
├── contexts/
│   └── AuthContext.jsx
├── components/
│   ├── auth/
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   └── ProtectedRoute.jsx
│   ├── investments/
│   │   ├── InvestmentForm.jsx
│   │   ├── EquityForm.jsx
│   │   ├── BondForm.jsx
│   │   ├── FDForm.jsx
│   │   ├── RDForm.jsx
│   │   ├── NPSForm.jsx
│   │   ├── PPFForm.jsx
│   │   ├── InvestmentList.jsx
│   │   └── TransactionHistory.jsx
│   ├── dashboard/
│   │   ├── Dashboard.jsx
│   │   ├── NetWorthCard.jsx
│   │   ├── SegmentChart.jsx
│   │   ├── YoYGrowthChart.jsx
│   │   └── SegmentalImpactChart.jsx
│   ├── expenses/
│   │   ├── ExpenseForm.jsx
│   │   ├── ExpenseList.jsx
│   │   ├── AssetMappingForm.jsx
│   │   └── CoverageDisplay.jsx
│   ├── tax/
│   │   ├── TaxReport.jsx
│   │   └── TaxProjection.jsx
│   └── common/
│       ├── Navbar.jsx
│       ├── Sidebar.jsx
│       └── LoadingSpinner.jsx
├── pages/
│   ├── DashboardPage.jsx
│   ├── InvestmentsPage.jsx
│   ├── ExpensesPage.jsx
│   ├── TaxPage.jsx
│   └── SettingsPage.jsx
├── services/
│   ├── api.js
│   ├── authService.js
│   ├── investmentService.js
│   ├── expenseService.js
│   ├── analyticsService.js
│   └── taxService.js
├── hooks/
│   ├── useAuth.js
│   ├── useInvestments.js
│   └── useAnalytics.js
└── utils/
    ├── currencyConverter.js
    ├── dateFormatter.js
    └── validators.js
```

---

## Verification Steps

### Backend Testing
1. **Authentication**: Test registration, login, JWT token generation via Postman/Insomnia
2. **Investment CRUD**: Create each investment type, verify database entries
3. **Transaction logging**: Add buy/sell/dividend transactions, check calculations
4. **Market data**: Verify scheduled job runs, prices updated in database
5. **Analytics API**: Test net worth calculation, segment totals, YoY growth endpoints
6. **Tax calculations**: Create test transactions with different holding periods, verify STCG/LTCG
7. **Import/Export**: Test CSV import, PDF parsing, JSON backup

### Frontend Testing
1. **Auth flow**: Complete registration → login → protected route access → logout
2. **Investment forms**: Fill each investment type form, submit, verify in list
3. **Dashboard**: Check all widgets display correct data, charts render properly
4. **Expenses**: Create expenses, map to assets, verify coverage percentage calculation
5. **Responsive design**: Test on desktop, tablet, mobile viewport sizes
6. **Multi-currency**: Switch base currency, verify all amounts convert correctly

### Integration Testing
1. **E2E scenario**: Register → add investments → add transactions → view dashboard → add expenses → map to assets → view tax report → export CSV
2. **Market data sync**: Add listed equity → wait for scheduled job → verify price updated → dashboard reflects new value
3. **PDF import**: Upload sample Contract Note → verify transactions extracted → review and confirm → check in transaction history

---

## Key Decisions & Rationale

### Architecture Decisions
- **JWT over sessions**: Stateless authentication better for REST APIs, scales horizontally
- **H2 for dev, PostgreSQL for prod**: H2 enables fast setup, PostgreSQL provides robust production support
- **Spring Boot over Node.js**: Better for complex financial calculations, strong typing, mature ecosystem
- **React Query recommended**: Provides excellent caching, automatic refetching, and optimistic updates

### Data Model Decisions
- **Transaction-based tracking**: Store every buy/sell/dividend instead of just current balances for complete audit trail and accurate tax calculations
- **Inheritance for investment types**: Base Investment class with type-specific subclasses using JPA inheritance (Single Table or Joined strategy)
- **Multi-currency at entity level**: Each transaction stores its original currency, conversion happens at display time
- **Expense-asset mapping with percentages**: Allows flexible distribution (e.g., rent paid 50% from FD interest, 50% from dividend income)

### Scope Boundaries
**Included in v1.0**:
- All features mentioned in requirements
- Multi-user authentication
- Transaction history tracking
- Market data auto-fetch (listed equities)
- Tax calculations (STCG/LTCG)
- Expense tracking and asset mapping
- Import/Export (CSV, Excel, PDF, JSON)
- Multi-currency support

**Excluded from v1.0** (potential future enhancements):
- Automated bank sync via Plaid or similar APIs
- Goal-based planning (retirement calculators, education planning)
- Asset allocation recommendations and rebalancing suggestions
- Social features (portfolio sharing, forums)
- Native mobile apps (iOS/Android) - v1.0 is web-only but mobile-responsive
- Robo-advisor features
- Portfolio comparison with benchmarks

---

## Technical Considerations & Decisions Needed

### 1. Market Data Reliability
**Challenge**: Yahoo Finance API is unofficial and can break without notice.

**Options**:
- **Option A**: Rely on Yahoo Finance + manual update fallback (already in step 22)
- **Option B**: Add paid alternative like Alpha Vantage (free tier: 500 calls/day) or Polygon.io
- **Option C**: Use multiple free APIs with fallback chain: Yahoo → Google → Manual

**Recommendation**: Start with Option A, add Option B if reliability becomes an issue.

### 2. PDF Parsing Complexity
**Challenge**: Contract Note formats vary wildly across brokers (Zerodha, Groww, Upstox, HDFC Securities, ICICI Direct, etc.).

**Options**:
- **Option A**: Template-based parser for 2-3 popular brokers, manual entry fallback
- **Option B**: AI/ML-based parser (OCR + NLP) - complex, expensive
- **Option C**: Manual entry only, skip PDF parsing

**Recommendation**: Option A - Start with Zerodha and Groww (most popular in India), add more broker templates over time. Always provide manual review UI before confirming imports.

### 3. Production Database
**Challenge**: H2 is not suitable for production use.

**Options**:
- **Option A**: Start with H2, migrate to PostgreSQL before production deployment
- **Option B**: Use PostgreSQL from day 1 for dev/prod parity
- **Option C**: Use MySQL instead of PostgreSQL

**Recommendation**: Option A for faster development start, but plan migration early (Phase 10, step 49). PostgreSQL preferred over MySQL for better JSON support and advanced features.

### 4. Currency Conversion Rates
**Challenge**: Need reliable, up-to-date forex rates for multi-currency support.

**Options**:
- **Option A**: Free forex API like exchangerate-api.io (free tier: 1500 requests/month)
- **Option B**: Paid forex API like fixer.io or currencylayer.com
- **Option C**: Manual rate entry by user
- **Option D**: Hybrid: auto-fetch daily via API + allow manual override

**Recommendation**: Option D - Auto-fetch daily via free API (exchangerate-api.io), store in CurrencyRate table, allow user manual override for specific conversions.

### 5. Deployment Target
**Challenge**: Where will the application be hosted?

**Options**:
- **Option A**: Docker Compose on VPS (DigitalOcean, Linode) - $10-20/month, simple setup
- **Option B**: Cloud platform (AWS/GCP/Azure) - scalable, more complex, potentially higher cost
- **Option C**: Local deployment only - simplest, no hosting cost, no internet dependency

**Recommendation**: Depends on use case. For personal use: Option C or A. For public/commercial: Option B.

### 6. Charting Library
**Challenge**: Material-UI doesn't include charting components.

**Options**:
- **Option A**: Chart.js - Simple, lightweight, canvas-based, imperative API
- **Option B**: Recharts - React-native, declarative, composable, good for complex visualizations
- **Option C**: ApexCharts - Feature-rich, beautiful defaults, extensive configuration

**Recommendation**: Option B (Recharts) - Best React integration, declarative syntax, easy customization, good documentation.

---

## Configuration Externalization Strategy

**Principle**: All environment-specific settings, API configurations, business rules, and constants must be externalized to configuration files. Never hardcode these in source code.

### Configuration Files Structure

#### Backend Configuration (Spring Boot)

**Location**: `src/main/resources/`

1. **application.yml** (base configuration)
   ```yaml
   spring:
     application:
       name: investment-tracker
     profiles:
       active: ${ACTIVE_PROFILE:dev}
   
   server:
     port: 8080
   
   jwt:
     secret: ${JWT_SECRET}
     expiration: ${JWT_EXPIRATION:86400000}  # 24 hours in milliseconds
   
   app:
     config:
       tax-rules-file: classpath:config/tax-rules.yml
       api-config-file: classpath:config/api-config.yml
       currency-config-file: classpath:config/currency-config.yml
   ```

2. **application-dev.yml** (development environment)
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:file:./data/investmentdb
       driver-class-name: org.h2.Driver
       username: sa
       password:
     h2:
       console:
         enabled: true
     jpa:
       show-sql: true
       hibernate:
         ddl-auto: update
   
   logging:
     level:
       com.investment.tracker: DEBUG
   ```

3. **application-prod.yml** (production environment)
   ```yaml
   spring:
     datasource:
       url: ${DB_URL}
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
       driver-class-name: org.postgresql.Driver
     jpa:
       show-sql: false
       hibernate:
         ddl-auto: validate
   
   logging:
     level:
       com.investment.tracker: INFO
   ```

4. **config/api-config.yml** (external API configurations)
   ```yaml
   apis:
     yahoo-finance:
       enabled: true
       base-url: https://query1.finance.yahoo.com/v8/finance
       timeout-seconds: 10
       retry-attempts: 3
       rate-limit-per-minute: 100
     
     google-finance:
       enabled: true
       fallback: true
       base-url: https://www.google.com/finance
       timeout-seconds: 10
     
     exchange-rate:
       provider: exchangerate-api
       base-url: https://api.exchangerate-api.com/v4
       api-key: ${EXCHANGE_RATE_API_KEY}
       update-frequency-hours: 24
       fallback-enabled: true
     
     alpha-vantage:
       enabled: false
       api-key: ${ALPHA_VANTAGE_API_KEY}
       base-url: https://www.alphavantage.co/query
   ```

5. **config/tax-rules.yml** (country-specific tax rules)
   ```yaml
   tax-rules:
     - country: INDIA
       equity:
         short-term:
           holding-period-days: 365
           tax-rate: 0.15  # 15%
         long-term:
           holding-period-days: 366
           tax-rate: 0.10  # 10%
           exemption-limit: 100000  # ₹1 lakh
       
       debt:
         short-term:
           holding-period-days: 1095  # 3 years
           tax-rate: slab-rate  # As per income tax slab
         long-term:
           holding-period-days: 1096
           tax-rate: 0.20  # 20% with indexation
       
       real-estate:
         short-term:
           holding-period-days: 730  # 2 years
           tax-rate: slab-rate
         long-term:
           holding-period-days: 731
           tax-rate: 0.20
     
     - country: USA
       equity:
         short-term:
           holding-period-days: 365
           tax-rate: slab-rate  # As per income tax bracket
         long-term:
           holding-period-days: 366
           tax-rate: 0.15  # 0%, 15%, or 20% based on income
       
       real-estate:
         short-term:
           holding-period-days: 365
           tax-rate: slab-rate
         long-term:
           holding-period-days: 366
           tax-rate: 0.15
   ```

6. **config/currency-config.yml** (currency and locale settings)
   ```yaml
   currencies:
     supported:
       - code: INR
         symbol: ₹
         name: Indian Rupee
         locale: en-IN
         decimal-places: 2
       
       - code: USD
         symbol: $
         name: US Dollar
         locale: en-US
         decimal-places: 2
       
       - code: EUR
         symbol: €
         name: Euro
         locale: en-EU
         decimal-places: 2
       
       - code: GBP
         symbol: £
         name: British Pound
         locale: en-GB
         decimal-places: 2
     
     default-currency: INR
     
     conversion:
       auto-update: true
       update-time: "00:00:00"  # Daily at midnight
       cache-duration-hours: 24
       fallback-to-manual: true
   ```

7. **config/business-rules.yml** (business logic configurations)
   ```yaml
   business-rules:
     investments:
       equity:
         min-quantity: 1
         max-quantity: 1000000
         markets:
           - NSE
           - BSE
           - NYSE
           - NASDAQ
       
       fixed-deposit:
         min-amount: 1000
         max-tenure-months: 120
         min-interest-rate: 0.01
         max-interest-rate: 0.15
       
       recurring-deposit:
         min-monthly-contribution: 100
         max-tenure-months: 120
     
     expenses:
       categories:
         - HOUSING
         - FOOD
         - TRANSPORT
         - HEALTHCARE
         - ENTERTAINMENT
         - EDUCATION
         - UTILITIES
         - INSURANCE
         - OTHER
       
       mapping:
         min-percentage: 0.01
         max-percentage: 100
         total-must-equal: 100
     
     analytics:
       dashboard:
         default-date-range-days: 365
         max-date-range-days: 3650  # 10 years
       
       segments:
         equity:
           - EQUITY
         safe-assets:
           - FD
           - RD
           - BOND
           - PPF
         illiquid-assets:
           - REAL_ESTATE
           - NPS
   ```

#### Frontend Configuration

**Location**: Root directory

1. **.env.development**
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   VITE_APP_NAME=Investment Tracker
   VITE_ENABLE_DEBUG=true
   ```

2. **.env.production**
   ```env
   VITE_API_BASE_URL=https://api.yourdomain.com/api
   VITE_APP_NAME=Investment Tracker
   VITE_ENABLE_DEBUG=false
   ```

3. **src/config/constants.js** (frontend constants, can load from backend API)
   ```javascript
   export const TAX_RULES = null;  // Load from backend API
   export const CURRENCY_CONFIG = null;  // Load from backend API
   export const BUSINESS_RULES = null;  // Load from backend API
   
   export const CHART_COLORS = {
     equity: '#4caf50',
     bonds: '#2196f3',
     safeAssets: '#ff9800',
     illiquidAssets: '#9c27b0',
   };
   
   export const DATE_FORMATS = {
     display: 'MMM dd, yyyy',
     api: 'yyyy-MM-dd',
     full: 'MMM dd, yyyy HH:mm:ss',
   };
   ```

### Configuration Loading Strategy

**Backend**:
1. Use `@Value` or `@ConfigurationProperties` to inject config values
2. Create configuration POJOs for complex configurations (TaxRulesConfig, ApiConfig, CurrencyConfig)
3. Validate configurations on startup using `@Validated` and `@PostConstruct`
4. Provide REST endpoints to expose business rules to frontend: `/api/config/tax-rules`, `/api/config/currencies`

**Frontend**:
1. Load environment variables via `import.meta.env.VITE_*`
2. Fetch business rules from backend API on app initialization
3. Store in React Context or global state
4. Avoid hardcoding any business logic in components

### Environment Variables (Sensitive Data)

**Never commit to Git. Store in .env files (gitignored) or environment.**

```env
# Backend (.env or system environment)
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
DB_URL=jdbc:postgresql://localhost:5432/investmentdb
DB_USERNAME=postgres
DB_PASSWORD=your-db-password
EXCHANGE_RATE_API_KEY=your-api-key
ALPHA_VANTAGE_API_KEY=your-api-key

# Frontend (.env files)
VITE_API_BASE_URL=http://localhost:8080/api
```

### Benefits of This Approach

1. **Easy Updates**: Change tax rates, API endpoints without code changes
2. **Environment Flexibility**: Same code runs in dev/staging/prod with different configs
3. **Multi-Country Support**: Add new country tax rules by editing YAML
4. **Security**: Sensitive data in environment variables, never in code
5. **Testability**: Mock configurations in tests easily
6. **Maintainability**: Business rules in one place, easy to audit
7. **Client Customization**: Different deployments can have different rules

---

## Next Steps

Once you're ready to begin implementation:

1. **Phase 1 Start**: Initialize Spring Boot and React projects
2. **Setup development environment**: 
   - Install JDK 17+
   - Install Node.js 18+
   - Install IDE (IntelliJ IDEA / VS Code)
   - Install Postman for API testing
4. **Version control**: Initialize Git repository, setup .gitignore
5. **Begin Phase 1 implementation**

---

## Questions & Clarifications Needed

Before starting implementation, consider:

1. **Team size**: Solo developer or team? Affects timeline and scope prioritization.
2. **Timeline expectations**: MVP in weeks or months? Determines which phases to prioritize.
3. **Primary user base**: Personal use, family, friends, or public SaaS? Affects scalability decisions.
4. **Budget**: Any constraints on paid services (APIs, hosting)? Affects free vs paid tool choices.
5. **Compliance needs**: Any regulatory requirements (data privacy, financial regulations)? May require GDPR, SOC2, etc.

---

## Reference Materials

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Spring Security JWT**: https://jwt.io/introduction
- **React Documentation**: https://react.dev
- **Recharts**: https://recharts.org
- **Yahoo Finance API (unofficial)**: Multiple wrappers available (yfinance for Python, yahoo-finance for Node.js)
- **Exchange Rate API**: https://www.exchangerate-api.com
- **Apache PDFBox**: https://pdfbox.apache.org

---

**Last Updated**: May 1, 2026
**Project Status**: Planning Complete, Ready for Implementation
Tech Stack Finalized, Ready for Implementation

---

## Finalized Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI Framework | Material-UI v5 | Component-rich, professional look, good documentation |
| Backend Build Tool | Maven | Traditional, XML-based, widely used, excellent IDE support |
| Frontend Build Tool | Vite | Faster builds, better dev experience, modern tooling |
| Data Fetching | Axios | Simple, straightforward, sufficient for MVP |
| Production Database | PostgreSQL 15+ | Better JSON support, advanced features, robust |
| Database Migrations | Liquibase (post-MVP) | Database versioning, add after core features stable |
| Testing Strategy | Mid-way after MVP | Focus on implementation first, add tests when MVP ready |
| Frontend Testing | Jest + RTL (post-MVP) | Standard React testing tools, add after MVP |
| Configuration | Externalized YAML/ENV | All API configs, tax rules, business logic in config files |