# GitHub Actions Workflows

This repository includes automated CI/CD workflows using GitHub Actions.

## Workflows

### 1. CI (`ci.yml`)
Runs on every push and pull request to main branches:
- Runs tests with MySQL service container
- Builds JAR artifact (if tests pass)
- Builds native image (on main/master branches only)
- Uploads artifacts for download

### 2. Test (`test.yml`)
Standalone test workflow that:
- Runs unit tests with MySQL service
- Publishes test results as PR comments
- Uploads test report artifacts

### 3. Build JAR (`build-jar.yml`)
Builds and publishes JAR artifacts:
- Builds complete JAR with embedded frontend
- Creates GitHub releases for tags
- Uploads JAR as artifact

### 4. Build Native (`build-native.yml`)
Builds GraalVM native images:
- Compiles native binary using GraalVM
- Builds and pushes Docker images
- Creates releases for tags with native binaries

## Required Secrets

For Docker image publishing (optional), configure these secrets in your repository:

1. Go to repository **Settings** → **Secrets and variables** → **Actions**
2. Add the following secrets:

| Secret | Description | Example |
|--------|-------------|---------|
| `DOCKER_USERNAME` | Docker Hub username | `yourusername` |
| `DOCKER_PASSWORD` | Docker Hub password or access token | `dckr_pat_...` |

## Artifacts

Artifacts are available for download from the Actions run page:

### JAR Artifacts
- **Name**: `novelspider-jar`
- **Contents**: `NovelSpider-*.jar`
- **Retention**: 7-30 days depending on workflow

### Native Binary Artifacts
- **Name**: `novelspider-native-linux-amd64`
- **Contents**:
  - `NovelSpider` - Native executable
  - `novelspider-linux-amd64.tar.gz` - Compressed binary
  - `novelspider-linux-amd64.sha256` - Checksum file
- **Retention**: 30-90 days depending on workflow

## Usage

### Manual Trigger

You can manually trigger workflows from the Actions tab:

1. Go to **Actions** tab
2. Select the workflow (e.g., "Build Native Image")
3. Click **Run workflow**
4. Select branch and click **Run workflow**

### Downloading Artifacts

1. Go to the Actions tab
2. Click on a completed workflow run
3. Scroll to **Artifacts** section
4. Click the artifact name to download

### Creating Releases

To create a release with artifacts:

```bash
# Tag your commit
git tag v1.0.0
git push origin v1.0.0

# The workflow will automatically:
# 1. Build JAR and native binary
# 2. Create GitHub release
# 3. Upload artifacts to release
```

## Build Times

- **JAR Build**: ~2-3 minutes
- **Native Build**: ~15-30 minutes (first run), ~10-15 minutes (cached)
- **Docker Build**: ~20-35 minutes (first run), ~15-20 minutes (cached)

## Status Badges

Add these badges to your README.md:

```markdown
[![CI](https://github.com/yourusername/NovelSpider/actions/workflows/ci.yml/badge.svg)](https://github.com/yourusername/NovelSpider/actions/workflows/ci.yml)
[![Build JAR](https://github.com/yourusername/NovelSpider/actions/workflows/build-jar.yml/badge.svg)](https://github.com/yourusername/NovelSpider/actions/workflows/build-jar.yml)
[![Build Native](https://github.com/yourusername/NovelSpider/actions/workflows/build-native.yml/badge.svg)](https://github.com/yourusername/NovelSpider/actions/workflows/build-native.yml)
```

## Troubleshooting

### Native Build Fails
- Check GraalVM version compatibility
- Increase timeout if needed (default: 60 minutes)
- Verify reflection/resource configs in `src/main/resources/META-INF/native-image/`

### Tests Fail in CI
- Check MySQL service connection
- Verify test database configuration
- Review test logs in artifacts

### Docker Push Fails
- Verify `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets
- Check Docker Hub account permissions
- Ensure repository exists on Docker Hub

## Contributing

When contributing:
1. Fork and create a feature branch
2. Make your changes
3. CI will run tests on your PR
4. After merge, native binary is built automatically
