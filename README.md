<p align="center">
  <img src="WhatsApp Image 2026-05-08 at 1.00.34 AM" width="200" alt="FORGE Logo"/>
</p>

# FORGE — Real-Time Auction & Commerce Engine

> A high-performance concurrent bidding engine wrapped in a production-grade enterprise platform.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square)
![Tests](https://img.shields.io/badge/Tests-128%20passing-brightgreen?style=flat-square)
![Coverage](https://img.shields.io/badge/Coverage-80%25-brightgreen?style=flat-square)
![Benchmark](https://img.shields.io/badge/Throughput-400K%2B%20ops%2Fsec-blue?style=flat-square)

---

## What is FORGE?

FORGE is a real-time online auction platform where sellers list items, bidders compete in live auctions, and the system handles concurrent bid matching, settlement, and wallet management — processing hundreds of thousands of bids per second using lock-free data structures and Java 21 Virtual Threads.

---

## Architecture — Two Module Design

```
forge/
├── forge-engine/       # Pure Java 21 — zero Spring dependencies
│   ├── BiddingEngine   # ConcurrentSkipListMap + Virtual Threads
│   ├── PriceTracker    # AtomicReference CAS loop — lock-free
│   ├── BidBook         # O(log n) thread-safe ordered bid storage
│   ├── AuctionStateMachine  # ReentrantReadWriteLock state transitions
│   ├── EventBus        # BlockingQueue pub/sub — Producer-Consumer
│   └── SettlementCalculator # Fee + tax computation
│
└── forge-platform/     # Spring Boot 3.2 — wraps the engine
    ├── REST API        # JWT-secured endpoints
    ├── WebSocket       # STOMP real-time bid feed
    ├── PostgreSQL      # JPA + Hibernate 6 + Flyway migrations
    ├── Redis           # Distributed caching
    ├── Quartz          # DB-backed persistent job scheduling
    └── MinIO           # S3-compatible image storage
```

**Why two modules?** The engine is pure Java — no annotations, no DI framework. It can be embedded in any Java application. The platform wraps it with Spring Boot for REST, security, and persistence. ArchUnit enforces this separation automatically on every build.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads, Records, Sealed Classes) |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Cache | Redis 7 |
| Scheduler | Quartz (DB-backed, crash-safe) |
| File Storage | MinIO (S3-compatible) |
| Security | Spring Security 6 + JWT |
| Real-time | WebSocket + STOMP |
| Testing | JUnit 5 + Mockito + ArchUnit |
| Benchmarks | JMH |
| Coverage | JaCoCo (80%+) |
| Build | Maven multi-module |

---

## Performance

| Metric | Result |
|---|---|
| JMH Throughput | 400K–700K ops/sec (8 threads) |
| Concurrency Stress | 1000 virtual threads, 0 failures |
| Total Tests | 128 passing, 0 failures |
| JaCoCo Coverage | 80%+ |
| Architecture Rules | All passing (ArchUnit) |

---

## Design Patterns

| Pattern | Implementation |
|---|---|
| Strategy | `PricingStrategy` → English, Dutch, Sealed auction types |
| Observer | `EventBus` + `EngineEventListener` → WebSocket, DB bridge |
| State | `AuctionStateMachine` with validated transitions |
| Producer-Consumer | `LinkedBlockingQueue` in EventBus |
| CAS Loop | `PriceTracker` — lock-free price updates |
| Builder | Auction creation |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for PostgreSQL, Redis, MinIO)

---

## Quick Start

### 1. Start Infrastructure

```bash
# PostgreSQL
docker run -d --name postgres-forge \
  -e POSTGRES_DB=forge_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=sensei \
  -p 5432:5432 postgres:16

# Redis
docker run -d --name redis-forge \
  -p 6379:6379 redis:7-alpine

# MinIO
docker run -d --name minio-forge \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=sensei \
  -e MINIO_ROOT_PASSWORD=sensei123 \
  quay.io/minio/minio server /data --console-address ":9001"
```

### 2. Build

```bash
# Build engine first
cd forge-engine && mvn clean install

# Run platform
cd ../forge-platform && mvn spring-boot:run
```

### 3. Verify

```
API:       http://localhost:8080/api/v1/
Swagger:   http://localhost:8080/swagger-ui.html
MinIO UI:  http://localhost:9001
```

---

## API Reference

### Auth
```
POST /api/v1/auth/register   Register user
POST /api/v1/auth/login      Login → returns JWT
```

### Auctions
```
POST   /api/v1/auctions              Create auction
GET    /api/v1/auctions/active       List active auctions
GET    /api/v1/auctions/active/paged List with pagination (?page=0&size=20)
DELETE /api/v1/auctions/{id}         Delete auction
```

### Bidding
```
POST /api/v1/auctions/{id}/bid   Place bid (REST)
WS   /ws                          WebSocket STOMP endpoint
SUB  /topic/auctions/{id}         Subscribe to live bid feed
```

### Wallet
```
POST /api/v1/wallet/topup    Add funds
GET  /api/v1/wallet/balance  Get balance
```

### Images
```
POST /api/v1/images/upload   Upload auction image → returns MinIO URL
```

---

## Running Tests

```bash
# All engine tests (pure Java, no Docker needed)
cd forge-engine && mvn test

# All platform tests
cd forge-platform && mvn test

# Full test suite with coverage report
mvn clean verify
# View coverage: forge-platform/target/site/jacoco/index.html
```

---

## Concurrency Model

```
Bid Request (1000 concurrent)
       ↓
Virtual Thread Pool (Java 21)
       ↓
AuctionStateMachine.isActive() — READ LOCK
       ↓
PriceTracker.updatePrice() — CAS Loop (lock-free)
       ↓
BidBook.addBid() — ConcurrentSkipListMap O(log n)
       ↓
EventBus.publish() — LinkedBlockingQueue (non-blocking)
       ↓
WebSocket broadcast + DB persist (async)
```

---

## Project Structure

```
forge-engine/src/
├── main/java/com/forge/engine/
│   ├── bidding/        BiddingEngine, BidBook
│   ├── event/          EventBus, EngineEvent (sealed), BidPlacedEvent
│   ├── model/          Bid, BidKey, Money, AuctionStateMachine, BidResult
│   ├── pricing/        PricingStrategy, English/Dutch/SealedBidPricing
│   ├── settlement/     SettlementCalculator, FeeCalculator, SettlementResult
│   └── tracker/        PriceTracker (AtomicReference CAS)
└── test/java/com/forge/engine/
    ├── architecture/   EngineArchitectureTest (ArchUnit)
    ├── benchmark/      BiddingEngineBenchmark (JMH)
    ├── bidding/        BidBookTest, BiddingEngineConcurrencyTest
    ├── model/          MoneyTest, AuctionStateMachineTest
    └── settlement/     FeeCalculatorTest

forge-platform/src/
├── main/java/com/forge/platform/
│   ├── config/         Security, Redis, Quartz, MinIO, Engine wiring
│   ├── controller/     Auth, Auction, Bid, Wallet, Image, User
│   ├── entity/         User, Auction, Bid, Wallet
│   ├── service/        AuctionService, WalletService, AuthService, BidService
│   ├── repository/     JPA repositories with custom JPQL queries
│   ├── scheduler/      Quartz jobs (AuctionLifecycleJob)
│   ├── security/       JWT filter, token service
│   ├── bridge/         Engine ↔ Platform (DatabaseBridge, WebSocketBridge)
│   └── exception/      GlobalExceptionHandler
└── test/               128 tests — service, controller, repository, arch
```

---

## Also Published

**PII Masker Spring Boot Starter** — auto-masks Aadhaar/PAN numbers in logs

```xml
<dependency>
  <groupId>io.github.ketansinghrajput</groupId>
  <artifactId>pii-masker-spring-boot-starter</artifactId>
  <version>1.0.1</version>
</dependency>
```

[![Maven Central](https://img.shields.io/maven-central/v/io.github.ketansinghrajput/pii-masker-spring-boot-starter?style=flat-square)](https://central.sonatype.com/artifact/io.github.ketansinghrajput/pii-masker-spring-boot-starter)

---

## Author

**Ketansingh Rajput** — System Engineer Intern @ EdgeVerve Systems (Infosys)

B.E. Electronics & Telecommunication — SPPU 2025
