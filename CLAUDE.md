# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Backend (Spring Boot + Kotlin)

```bash
# Run the application (includes both backend and built frontend)
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=NovelServiceTest

# Build JAR
./mvnw clean package

# Skip tests during build
./mvnw clean package -DskipTests

# Build GraalVM Native Image (requires GraalVM)
./mvnw -Pnative clean package

# Build GraalVM Native Image skipping tests
./mvnw -Pnative clean package -DskipTests
```

### GraalVM Native Image

The application supports building a native executable using GraalVM for faster startup and lower memory footprint.

**Prerequisites** (for local build):
- Install GraalVM (version 21 or later)
- Set `JAVA_HOME` to GraalVM installation
- Install native-image builder: `gu install native-image`

**Building native image**:
```bash
# Step 1: Check your environment
./check-native-env.sh

# Step 2: Use the convenience script
./build-native.sh

# Option 2: Build manually
./mvnw -Pnative clean package

# Option 3: Build with Docker (no GraalVM required)
docker-compose -f docker-compose.native.yml build
docker-compose -f docker-compose.native.yml up

# The native executable will be created at:
# target/NovelSpider (Linux/macOS) or target/NovelSpider.exe (Windows)

# Run the native executable
./target/NovelSpider
```

**Benefits**:
- Instant startup (~100ms vs ~2s)
- Lower memory footprint (~50MB vs ~200MB)
- No JVM required for deployment

**Limitations**:
- Longer build time (~15-30 minutes for native build)
- Some Java features not supported (reflection, dynamic proxies, etc.)
- Static web resources must be embedded at build time

**Troubleshooting**:
See [NATIVE_BUILD_TROUBLESHOOTING.md](NATIVE_BUILD_TROUBLESHOOTING.md) for comprehensive troubleshooting guide including:
- Common build errors and solutions
- Debugging techniques
- Performance optimization
- Platform-specific issues

Quick reference:
```bash
# Check environment before building
./check-native-env.sh

# If build fails, check the GraalVM configuration files in:
# src/main/resources/META-INF/native-image/

# Common issues:
# 1. Missing reflection entries: Add to reflect-config.json
# 2. Missing resource entries: Add to resource-config.json
# 3. Hibernate proxy issues: Add to proxy-config.json
# 4. Kotlin reflection not working: Ensure kotlin-reflect dependency is included
```

### Frontend (Vue 3 + Vite)

```bash
cd web-ui

# Install dependencies
npm install

# Development server (with hot reload and API proxy)
npm run dev

# Production build (outputs to ../src/main/resources/static)
npm run build

# Preview production build
npm run preview
```

**Important**: Frontend build output goes directly into `src/main/resources/static`, which Spring Boot serves. Always rebuild the frontend after Vue component changes.

### Full Development Workflow

For development with both frontend and backend hot-reload:
1. Terminal 1: `cd web-ui && npm run dev` (runs on port 5173)
2. Terminal 2: `./mvnw spring-boot:run` (runs on port 3000)
3. Access frontend at http://localhost:5173 with API proxy to backend

For production-like testing:
1. `cd web-ui && npm run build`
2. `./mvnw spring-boot:run`
3. Access at http://localhost:3000 (serves built frontend)

## Architecture Overview

NovelSpider is a web scraping application that crawls Chinese novel metadata from external websites and provides a searchable web interface.

### Core Components

**NovelService** (src/main/kotlin/cn/har01d/novel/spider/NovelService.kt)
- Central business logic for web scraping
- Manages HTTP requests to external site with Jsoup
- Implements cookie-based session management (extracts set-cookie headers and updates internal cookie state)
- Parses HTML using CSS selectors in `parseNovelInfo()` method
- Handles retry logic with exponential backoff (3 attempts, 5s → 10s → 20s delays)
- Implements rate limiting: 10s + random(0-1000ms) delay between pages
- Async crawling via `@Async crawlNovels()` method
- Single global `working` flag prevents concurrent crawl operations

**NovelController** (src/main/kotlin/cn/har01d/novel/spider/NovelController.kt)
- Three REST endpoints:
  - `POST /api/novels/crawl?pages=N` - triggers async crawl (returns immediately)
  - `GET /api/novels/status` - health check
  - `GET /api/novels/search` - search with pagination and filters

**NovelRepository** (src/main/kotlin/cn/har01d/novel/spider/NovelRepository.kt)
- Custom JPQL query with 4 optional filters (query, author, status, category)
- Uses LIKE for title/author, exact match for status/category

**ScheduledTask** (src/main/kotlin/cn/har01d/novel/spider/ScheduledTask.kt)
- Runs `novelService.startCrawling()` every hour (cron: `0 0 * * * ?`)
- Calls `startCrawling()` which checks `working` flag before starting

### Data Flow

**Scraping Process**:
1. Triggered manually via API or automatically by scheduled task
2. `crawlNovels()` executes asynchronously via Spring's `@Async`
3. For each page: fetch HTML → parse novel items → extract set-cookie → merge with existing cookies → save to database → wait 10s+ → next page
4. Cookie management: parses current cookie string, merges with response headers, rebuilds cookie string for next request

**Frontend** (web-ui/src/App.vue):
- Single Vue 3 component (Composition API)
- Element Plus UI components (table, pagination, selects)
- Searches via `/api/novels/search` with page/sort/filter parameters
- Displays novels in sortable table with clickable links

### Configuration (application.yaml)

Key configuration:
- `spider.base-url`: Target website (default: http://www.999xiaoshuo.cc)
- `spider.timeout`: HTTP request timeout in ms (default: 10000)
- `spider.max-pages`: Maximum pages to crawl (default: 10)
- `server.port`: Application port (default: 3000)
- Database: MySQL on localhost:3306/novels with auto-DDL update

### Key Implementation Details

**Cookie Management** (NovelService.kt:132-155):
- Hardcoded initial cookie in `cookie` field
- After each HTTP request, extracts `set-cookie` headers
- Parses existing cookie string into mutable map
- Merges new cookies from response, overwriting existing values
- Rebuilds cookie string, filtering empty values
- Logs updated cookie at INFO level (changed from DEBUG per user preference)

**HTML Parsing** (NovelService.kt:41-111):
- CSS selectors target specific structure: `ul.flex li` with `h2` and `p.indent`
- Extracts: title, author, category, status, word count, update time, description
- Parses relative time expressions ("3天前", "刚刚", "2个月前") to LocalDateTime
- Converts word count from "万" (10,000s) to actual number
- Novel ID extracted from URL path (e.g., `/12345.html` → id=12345)

**Date Parsing Logic** (NovelService.kt:84-100):
- Supports relative time: "刚刚", "X分钟前", "X小时前", "X天前", "X个月前"
- Falls back to absolute date parsing with `LocalDate.parse()`

**Frontend Build Integration**:
- Vite build output directory: `../src/main/resources/static`
- Development server proxies `/api` requests to `http://127.0.0.1:3000`
- Production build bundles everything into Spring Boot static resources

### Common Modifications

**Changing CSS selectors**: Target website HTML structure may change. Update selectors in `parseNovelInfo()`:
- Title: `h2`
- Link: `a[href]`
- Category/Status: `span` with "/" separator
- Author: `i.fa-user-circle-o`
- Word count: `em.orange`
- Update time: `em.blue`
- Description: `p.indent`

**Adjusting crawl rate**: Modify delays in `crawlNovels()` method (line 169):
```kotlin
Thread.sleep(10000 + ThreadLocalRandom.current().nextInt(1000).toLong())
```

**Adding search filters**:
1. Add field to `SearchRequest.kt`
2. Update JPQL query in `NovelRepository.search()`
3. Add filter UI control in `web-ui/src/App.vue`

**Testing scraping logic**:
- Check logs: `tail -f logs/app.log`
- Manual HTML inspection: `curl -H "User-Agent: Mozilla..." -H "Cookie: ..." <url>`
- Verify CSS selectors still match target website structure

### Project Context

- Most code is AI-generated (as noted in README)
- No test coverage exists (test directory empty)
- No authentication on API endpoints
- Database credentials currently in plaintext (application.yaml:7)
- Logs written to `logs/app.log` with file rotation
