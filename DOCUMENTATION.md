# NovelSpider Documentation

## Overview

NovelSpider is a full-stack web scraping application designed to crawl, index, and manage Chinese novel metadata from external websites. It provides a web interface for searching, browsing, and filtering novels with real-time updates.

### Key Features

- **Automated Web Scraping**: Crawls novel information from external sources with scheduled updates
- **Cookie Management**: Maintains session cookies for authenticated scraping
- **Advanced Search**: Filter by title, author, category, and status
- **Real-time Updates**: Scheduled hourly updates to keep novel data fresh
- **Responsive UI**: Vue 3 + Element Plus interface with pagination and sorting
- **RESTful API**: Clean JSON API for integration
- **Rate Limiting**: Built-in delays to respect target server resources

## Technology Stack

### Backend

- **Framework**: Spring Boot 3.5.6
- **Language**: Kotlin 1.9.25
- **Database**: MySQL with Hibernate/JPA
- **HTML Parsing**: Jsoup 1.21.2
- **Build Tool**: Maven

### Frontend

- **Framework**: Vue 3.5.18
- **UI Library**: Element Plus 2.11.3
- **Build Tool**: Vite 7.0.6
- **HTTP Client**: Axios 1.12.2

## Project Structure

```
NovelSpider/
├── src/main/kotlin/cn/har01d/novel/spider/
│   ├── NovelSpiderApplication.kt    # Application entry point
│   ├── Novel.kt                      # JPA entity
│   ├── NovelRepository.kt            # Data access layer
│   ├── NovelService.kt               # Business logic
│   ├── NovelController.kt            # REST API endpoints
│   ├── SearchRequest.kt              # Search DTO
│   └── ScheduledTask.kt              # Scheduled crawler
├── src/main/resources/
│   ├── application.yaml              # Configuration
│   └── static/                       # Built frontend assets
├── web-ui/                           # Vue.js frontend
│   ├── src/
│   │   ├── App.vue                   # Main component
│   │   └── main.js                   # Entry point
│   ├── vite.config.js                # Vite configuration
│   └── package.json
├── pom.xml                           # Maven configuration
└── logs/                             # Application logs
```

## Installation & Setup

### Prerequisites

- Java 17 or higher
- Node.js 20.19.0+ or 22.12.0+
- MySQL 8.0+
- Maven (included as Maven wrapper)

### Database Setup

1. Create MySQL database:

```sql
CREATE DATABASE novels CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'novel'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON novels.* TO 'novel'@'localhost';
FLUSH PRIVILEGES;
```

2. Configure database connection in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/novels
    username: novel
    password: your_password
```

### Backend Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd NovelSpider
```

2. Build the project:
```bash
./mvnw clean install
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

The backend will start on `http://localhost:3000`

### Frontend Setup

1. Navigate to the web-ui directory:
```bash
cd web-ui
```

2. Install dependencies:
```bash
npm install
```

3. Development mode:
```bash
npm run dev
```

4. Production build:
```bash
npm run build
```

The build output is automatically placed in `src/main/resources/static` for serving by Spring Boot.

## Configuration

### Application Configuration (`application.yaml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/novels
    username: novel
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/ update tables

server:
  port: 3000

spider:
  base-url: http://www.999xiaoshuo.cc  # Target website
  timeout: 10000                       # HTTP timeout in ms
  max-pages: 10                        # Pages to crawl per run

logging:
  file:
    name: logs/app.log
  level:
    cn.har01d.novel.spider: INFO
```

### Environment Variables

You can override configuration using environment variables:

- `SPRING_DATASOURCE_URL`: Database URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `SPIDER_BASE_URL`: Target website URL
- `SPIDER_TIMEOUT`: HTTP timeout
- `SPIDER_MAX_PAGES`: Maximum pages to crawl

## API Reference

### Endpoints

#### 1. Start Crawling

```http
POST /api/novels/crawl?pages=5
```

**Query Parameters:**
- `pages` (optional): Number of pages to crawl (default: configured max-pages)

**Response:** `202 Accepted`

**Description:** Initiates asynchronous crawling of novel metadata. The operation runs in the background.

#### 2. Get Crawler Status

```http
GET /api/novels/status
```

**Response:**
```json
{
  "status": "ok"
}
```

**Description:** Health check endpoint to verify the service is running.

#### 3. Search Novels

```http
GET /api/novels/search?page=0&size=20
```

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `q` (optional): Search in novel title
- `author` (optional): Filter by author
- `status` (optional): Filter by status (连载/全本)
- `category` (optional): Filter by category
- `sort` (optional): Sort field (updatedAt, wordCount)
- `direction` (optional): Sort direction (ASC, DESC)

**Response:**
```json
{
  "content": [
    {
      "id": 12345,
      "title": "示例小说",
      "author": "作者名",
      "category": "玄幻",
      "status": "连载",
      "wordCount": 1000000,
      "description": "小说简介...",
      "novelUrl": "http://...",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-15T08:30:00"
    }
  ],
  "pageable": {...},
  "totalPages": 50,
  "totalElements": 1000,
  "size": 20,
  "number": 0
}
```

## Architecture

### Component Overview

#### NovelService.kt

Core service containing business logic for web scraping:

- **Configuration**: Manages base URL, timeout, and max pages
- **Cookie Management**: Handles set-cookie headers for session persistence
- **HTML Parsing**: Extracts novel metadata using Jsoup CSS selectors
- **Retry Logic**: Implements exponential backoff on failures
- **Rate Limiting**: 10s + random delay between page requests

Key Methods:
- `parseNovelInfo(Element)`: Parses HTML element to Novel object
- `getNovelList(page: Int)`: Fetches and parses novels from a page
- `crawlNovels(pages: Int)`: Async crawling with rate limiting
- `searchNovels(...)`: Searches database with filters

#### NovelController.kt

REST API controller with three endpoints:
- Crawl initiation
- Status check
- Novel search with pagination

#### ScheduledTask.kt

Scheduled component that triggers crawling every hour:
```kotlin
@Scheduled(cron = "0 0 * * * ?")
fun scheduledCrawl()
```

#### NovelRepository.kt

Spring Data JPA repository with custom JPQL search query supporting multiple optional filters.

### Data Flow

#### Crawling Process

1. **Trigger**: Manual API call or scheduled task
2. **Execution**: `NovelService.crawlNovels()` runs asynchronously
3. **Fetch**: HTTP request to target website with cookies
4. **Parse**: Jsoup extracts novel information from HTML
5. **Update**: Response cookies merged with existing cookie store
6. **Persist**: Novel data saved/updated in database
7. **Delay**: Wait before next page to respect server

#### Search Process

1. **Request**: User searches via web UI
2. **API Call**: Frontend calls `/api/novels/search`
3. **Query**: Repository executes JPQL query with filters
4. **Response**: Paginated results returned as JSON
5. **Display**: Vue component renders results in table

## Development

### Running Tests

```bash
./mvnw test
```

### Code Structure

#### Entity: Novel.kt

JPA entity with the following fields:
- `id`: Long (primary key, extracted from novel URL)
- `title`: String (255, required)
- `author`: String (100)
- `category`: String (50)
- `status`: String (20)
- `wordCount`: Long (actual words, displayed as 万)
- `description`: Text
- `novelUrl`: String (500)
- `createdAt`: LocalDateTime
- `updatedAt`: LocalDateTime

#### DTO: SearchRequest.kt

Data transfer object for search parameters with optional fields for title, author, status, and category filtering.

### Frontend Development

The Vue 3 application (`web-ui/src/App.vue`) provides:

- **Search Interface**: Title search with instant results
- **Filter Sidebar**: Author, status, and category filters
- **Data Table**: Sortable columns (word count, update time)
- **Pagination**: Page size selection and navigation
- **External Links**: Direct links to original novels

Key features:
- Reactive state management using Vue 3 Composition API
- Element Plus UI components
- Axios for HTTP requests
- Loading states and error handling

### Adding New Features

1. **New Search Filter**:
   - Add field to `SearchRequest.kt`
   - Update `NovelRepository.search()` JPQL query
   - Add UI control in `App.vue`

2. **New Scheduled Job**:
   - Create method in `ScheduledTask.kt`
   - Annotate with `@Scheduled(cron = "...")`

3. **Custom Scraping Logic**:
   - Modify `NovelService.parseNovelInfo()`
   - Add new CSS selectors for data extraction

## Deployment

### Production Build

1. Build frontend:
```bash
cd web-ui
npm run build
```

2. Build backend:
```bash
cd ..
./mvnw clean package
```

3. Run JAR:
```bash
java -jar target/NovelSpider-0.0.1.jar
```

### Docker Deployment

Create `Dockerfile`:

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/NovelSpider-0.0.1.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t novel-spider .
docker run -p 3000:3000 -e SPRING_DATASOURCE_URL=... novel-spider
```

### Systemd Service

Create `/etc/systemd/system/novel-spider.service`:

```ini
[Unit]
Description=Novel Spider Service
After=syslog.target network.target mysql.service

[Service]
Type=simple
User=novels
WorkingDirectory=/opt/novel-spider
ExecStart=/usr/bin/java -jar /opt/novel-spider/NovelSpider-0.0.1.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable novel-spider
sudo systemctl start novel-spider
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

**Error:** `HikariPool-1 - Exception during pool initialization`

**Solution:**
- Verify MySQL is running: `sudo systemctl status mysql`
- Check credentials in `application.yaml`
- Ensure database exists: `mysql -u root -p -e "SHOW DATABASES;"`

#### 2. Cookie Not Updating

**Log:** No "更新 cookie" debug message

**Solution:**
- Check server is returning set-cookie headers
- Verify response.headers("set-cookie") in code
- Add debug logging to inspect response headers

#### 3. Scraping Returns Empty Results

**Error:** Novel list is empty

**Solution:**
- Verify target website structure hasn't changed
- Check CSS selectors in `parseNovelInfo()`
- Inspect HTML manually: `curl -H "User-Agent: ..." <url>`
- Check logs for parsing errors

#### 4. Scheduled Task Not Running

**Symptom:** No automatic updates

**Solution:**
- Ensure `@EnableScheduling` is present in application class
- Verify cron expression: `0 0 * * * ?` (every hour)
- Check logs for scheduled task execution

#### 5. Frontend Build Fails

**Error:** Node version mismatch

**Solution:**
```bash
# Check required version
cat web-ui/package.json | grep engines

# Install correct Node.js version using nvm
nvm install 22
nvm use 22
npm install
```

### Debugging

Enable debug logging:

```yaml
logging:
  level:
    cn.har01d.novel.spider: DEBUG
    org.jsoup: DEBUG
```

View logs:
```bash
tail -f logs/app.log
```

### Performance Tuning

- **Increase max pages** for larger crawls: `SPIDER_MAX_PAGES=50`
- **Reduce timeout** on fast connections: `SPIDER_TIMEOUT=5000`
- **Adjust delay** between pages: Modify `Thread.sleep(10000 + ...)` in `crawlNovels()`
- **Database indexing**: Add indexes on frequently queried fields:

```sql
CREATE INDEX idx_novel_updated_at ON novel(updated_at DESC);
CREATE INDEX idx_novel_title ON novel(title);
CREATE INDEX idx_novel_author ON novel(author);
```

## Security Considerations

### Current Limitations

- Database credentials in plaintext configuration
- No API authentication
- No rate limiting on API endpoints
- External scraping may violate terms of service

### Recommendations

1. **Use environment variables** for sensitive data
2. **Add API authentication** (Spring Security, JWT)
3. **Implement rate limiting** on public endpoints
4. **Review target site's robots.txt** and ToS
5. **Add input validation** for search parameters
6. **Use connection pooling** for database

## License & Attribution

Most code is generated by AI using free ChatGPT.

## Support

For issues or questions:
1. Check logs in `logs/app.log`
2. Review this documentation
3. Verify configuration settings
4. Test API endpoints manually using curl or Postman
