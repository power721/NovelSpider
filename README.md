# NovelSpider

A full-stack web scraping application for crawling and indexing Chinese novel metadata.

[![CI](https://github.com/YOUR_USERNAME/NovelSpider/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/NovelSpider/actions/workflows/ci.yml)
[![Build JAR](https://github.com/YOUR_USERNAME/NovelSpider/actions/workflows/build-jar.yml/badge.svg)](https://github.com/YOUR_USERNAME/NovelSpider/actions/workflows/build-jar.yml)
[![Build Native](https://github.com/YOUR_USERNAME/NovelSpider/actions/workflows/build-native.yml/badge.svg)](https://github.com/YOUR_USERNAME/NovelSpider/actions/workflows/build-native.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-purple.svg)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-green.svg)](https://spring.io/projects/spring-boot)
[![GraalVM](https://img.shields.io/badge/GraalVM-Native%20Image-blueviolet.svg)](https://www.graalvm.org/latest/reference-manual/native-image/)

## Features

- Automated web scraping with scheduled hourly updates
- Cookie-based session management
- Advanced search and filtering (title, author, category, status)
- RESTful API with pagination
- Vue 3 + Element Plus web interface
- MySQL database with JPA/Hibernate
- **GraalVM native image support** for instant startup and low memory footprint

## Quick Start

### Prerequisites

- Java 17+
- Node.js 20.19+ or 22.12+
- MySQL 8.0+

### Setup

```bash
# Create database
mysql -u root -p
CREATE DATABASE novels CHARACTER SET utf8mb4;
CREATE USER 'novel'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON novels.* TO 'novel'@'localhost';

# Run backend
./mvnw spring-boot:run

# Build frontend (in another terminal)
cd web-ui
npm install
npm run build
```

Access the application at http://localhost:3000

### GraalVM Native Image (Optional)

For production deployments with instant startup and lower memory usage:

**Option 1: Build locally with GraalVM**
```bash
# Check your environment first
./check-native-env.sh

# Install GraalVM and native-image builder
# See: https://www.graalvm.org/latest/reference-manual/native-image/
export JAVA_HOME=/path/to/graalvm
gu install native-image

# Build using the convenience script
./build-native.sh

# Or build manually
./mvnw -Pnative clean package

# Run native executable
./target/NovelSpider
```

> **Note:** See [NATIVE_BUILD_TROUBLESHOOTING.md](NATIVE_BUILD_TROUBLESHOOTING.md) if you encounter build issues.

**Option 2: Build with Docker (recommended)**
```bash
# Build and run with Docker Compose
docker-compose -f docker-compose.native.yml up

# Or build manually
docker build -f Dockerfile.native -t novelspider-native .
docker run -p 3000:3000 -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/novels novelspider-native
```

**Benefits**: ~100ms startup time, reduced memory usage (~50MB vs ~200MB), no JVM required

## CI/CD

This project uses GitHub Actions for automated builds and testing:

### Automated Workflows

- **CI Pipeline**: Runs tests and builds on every push/PR
- **JAR Builds**: Automatic JAR artifact generation
- **Native Builds**: GraalVM native binary compilation (main branch)
- **Docker Images**: Multi-stage Docker builds with caching

### Artifacts

Build artifacts are automatically uploaded to GitHub Actions:

- **JAR**: `NovelSpider-*.jar` - Standard Spring Boot executable
- **Native**: `NovelSpider` - GraalVM native binary
- **Compressed**: `novelspider-linux-amd64.tar.gz` - Compressed native binary

### Releases

Tagged commits automatically create GitHub releases with artifacts:

```bash
git tag v1.0.0
git push origin v1.0.0
```

See [`.github/README.md`](.github/README.md) for detailed CI/CD documentation.

## Documentation

See [DOCUMENTATION.md](DOCUMENTATION.md) for comprehensive documentation including:

- Architecture overview
- API reference
- Configuration guide
- Development instructions
- Deployment guide
- Troubleshooting

## Project Structure

```
NovelSpider/
├── .github/workflows/        # CI/CD workflows
├── src/main/kotlin/          # Spring Boot backend
├── src/main/resources/       # Configuration & static assets
├── web-ui/                   # Vue 3 frontend
├── pom.xml                   # Maven configuration
├── Dockerfile.native         # GraalVM native Dockerfile
├── docker-compose.native.yml # Native Docker Compose config
├── build-native.sh           # Native build convenience script
└── DOCUMENTATION.md          # Full documentation
```

## API Endpoints

- `POST /api/novels/crawl?pages=5` - Start crawling
- `GET /api/novels/status` - Health check
- `GET /api/novels/search` - Search novels with filters

## Technology Stack

- **Backend**: Spring Boot 3.5.6, Kotlin 1.9.25, MySQL
- **Frontend**: Vue 3, Element Plus, Vite
- **Scraping**: Jsoup 1.21.2

---

**Note**: Most code is generated by AI using free ChatGPT.