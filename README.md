# Forge Project Overview

## Project Description

Forge is a comprehensive auction platform built with Java, Spring Boot, and modern web technologies. It provides a real-time bidding system with a modular architecture separating the core auction engine from the platform application layer.

## Architecture Overview

The project follows a microservices-inspired modular architecture with two main components:

### 1. Forge Engine (`forge-engine`)
A pure Java library containing the core auction and bidding logic, independent of any web framework.

### 2. Forge Platform (`forge-platform`)
A Spring Boot web application that provides REST APIs, WebSocket support for real-time bidding, user authentication, and integrates with the Forge Engine.

## Technology Stack

- **Language**: Java 21
- **Build Tool**: Maven (Multi-module project)
- **Framework**: Spring Boot 3.2.5
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Authentication**: JWT (JSON Web Tokens)
- **Containerization**: Docker Compose
- **WebSockets**: Spring WebSocket for real-time bidding updates

## Project Structure

```
forge-parent/
├── pom.xml (Parent POM - manages dependencies and modules)
├── forge-engine/ (Core auction engine library)
│   ├── pom.xml
│   └── src/main/java/com/forge/engine/
│       ├── auction/ (Auction management logic)
│       ├── bidding/ (Bidding engine and bid book)
│       ├── event/ (Event-driven architecture components)
│       ├── model/ (Core domain models: Auction, Bid, Money, etc.)
│       ├── pricing/ (Pricing strategies and calculations)
│       ├── timing/ (Auction timing and scheduling)
│       └── tracker/ (Price tracking and analytics)
└── forge-platform/ (Spring Boot web application)
    ├── pom.xml
    └── src/main/java/com/forge/platform/
        ├── bridge/ (Integration layer with forge-engine)
        ├── config/ (Spring configuration classes)
        ├── controller/ (REST API endpoints)
        │   ├── AuctionController.java
        │   ├── AuthController.java
        │   ├── BidController.java
        │   ├── BiddingController.java
        │   ├── BiddingWebSocketController.java
        │   ├── EngineStateController.java
        │   ├── EngineTestController.java
        │   ├── HealthController.java
        │   ├── UserController.java
        │   └── WalletController.java
        ├── dto/ (Data Transfer Objects)
        ├── entity/ (JPA entities: User, Auction, Bid, Wallet)
        ├── enums/ (Enumerations)
        ├── exception/ (Custom exceptions)
        ├── repository/ (JPA repositories)
        ├── scheduler/ (Scheduled tasks)
        ├── security/ (Spring Security configuration)
        ├── service/ (Business logic services)
        └── websocket/ (WebSocket handlers)
```

## Key Components and Connections

### Database Schema
The platform uses PostgreSQL with the following main entities:
- **Users**: User accounts with wallet balances
- **Auctions**: Auction listings with title, description, starting price, end time, and status
- **Bids**: Bid records linking users to auctions
- **Wallets**: User wallet management (integrated into users table)

### Core Engine Integration
The Forge Platform integrates with the Forge Engine through:
- **Auction Management**: Uses engine's auction state machine and context
- **Bidding Logic**: Delegates bid validation and processing to the bidding engine
- **Event System**: Leverages event-driven architecture for auction events
- **Pricing**: Utilizes pricing strategies from the engine

### Real-Time Features
- **WebSocket Support**: Real-time bid updates via `BiddingWebSocketController`
- **Event Bus**: Internal event system for auction state changes
- **Caching**: Redis integration for performance optimization

### Security & Authentication
- **JWT Authentication**: Token-based auth with 24-hour expiration
- **Spring Security**: Comprehensive security configuration
- **PII Masking**: Sensitive data protection using pii-masker library

### API Endpoints
The platform exposes REST APIs for:
- User registration and authentication
- Auction creation and management
- Bid placement and retrieval
- Wallet operations
- Engine state monitoring
- Health checks

## Data Flow and Connections

1. **User Registration**: Users register via AuthController → stored in PostgreSQL
2. **Auction Creation**: Auctions created via AuctionController → persisted to database
3. **Bid Placement**: 
   - Bid submitted via REST API or WebSocket
   - Validated by BiddingEngine from forge-engine
   - Stored in database
   - Real-time updates sent via WebSocket
4. **Auction Processing**: 
   - Engine manages auction lifecycle using state machine
   - Events published through EventBus
   - Timers handled by scheduling components
5. **Caching**: Redis used for session data and performance optimization

## Configuration

### Application Profiles
- **dev**: Development profile with debug logging and local database
- **prod**: Production profile (configuration in application-prod.yml)

### Database Configuration
- PostgreSQL connection with JPA/Hibernate
- Flyway migrations (currently disabled, using Hibernate DDL auto-update)
- Connection pooling via HikariCP (Spring Boot default)

### External Services
- **PostgreSQL**: Data persistence on port 5432
- **Redis**: Caching and session storage on port 6379

## Development and Deployment

### Building the Project
```bash
mvn clean install
```

### Running Locally
1. Start infrastructure: `docker-compose up -d`
2. Run the application: `mvn spring-boot:run` in forge-platform directory
3. Access APIs at `http://localhost:8080`

### Key Dependencies
- **Spring Boot Starters**: Web, Data JPA, Security
- **JWT**: Authentication tokens
- **Lombok**: Code generation
- **SLF4J**: Logging
- **JUnit**: Testing

## Future Enhancements

Based on the architecture, potential areas for expansion:
- Microservices decomposition
- Advanced bidding strategies
- Auction analytics dashboard
- Mobile application support
- Integration with payment gateways
- Multi-tenant architecture

This modular design allows for easy extension and maintenance, with clear separation of concerns between the core auction logic and the web platform layer.