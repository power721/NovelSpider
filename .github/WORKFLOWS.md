# GitHub Actions Workflows Overview

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Push / PR                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ci.yml (Main Pipeline)                     │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  1. Test Job                                                ││
│  │     - Runs unit tests with MySQL                            ││
│  │     - Uploads test results                                  ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                  │
│                              ▼ (on success)                     │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  2. Build JAR Job                                           ││
│  │     - Builds frontend                                       ││
│  │     - Creates Spring Boot JAR                               ││
│  │     - Uploads artifact (7 days)                             ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                  │
│                              ▼ (main branch only)               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  3. Build Native Job                                        ││
│  │     - Compiles GraalVM native binary                        ││
│  │     - Compresses binary                                     ││
│  │     - Uploads artifact (30 days)                            ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    build-native.yml (Separate)                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Native Build Job                                           ││
│  │     - Same as ci.yml but separate                           ││
│  │     - Runs on push to main/tags                             ││
│  │     - Creates releases for tags                             ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Docker Build Job                                           ││
│  │     - Builds multi-platform Docker image                    ││
│  │     - Pushes to Docker Hub (if secrets configured)          ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    build-jar.yml (On Demand)                    │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Build & Release                                            ││
│  │     - Builds JAR with frontend                              ││
│  │     - Creates GitHub releases for tags                      ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## Workflow Comparison

| Workflow | Trigger | Artifacts | Release | Docker |
|----------|---------|-----------|---------|--------|
| `ci.yml` | Push/PR to all branches | JAR (7d), Native (30d) | ❌ | ❌ |
| `test.yml` | Push/PR to all branches | Test reports | ❌ | ❌ |
| `build-jar.yml` | Push/Tags/manual | JAR (30d) | ✅ Tags | ❌ |
| `build-native.yml` | Push to main/Tags/manual | Native (90d) | ✅ Tags | ✅ |

## When Each Workflow Runs

### ci.yml
- ✅ Every push to any branch
- ✅ Every pull request
- ✅ Manual trigger
- Purpose: Complete CI pipeline with tests and builds

### test.yml
- ✅ Every push to any branch
- ✅ Every pull request
- Purpose: Quick test feedback with detailed reports

### build-jar.yml
- ✅ Every push to any branch
- ✅ Every tag (creates release)
- ✅ Manual trigger
- Purpose: Build JAR artifacts and create releases

### build-native.yml
- ✅ Push to master/main only
- ✅ Every tag (creates release + Docker)
- ✅ Manual trigger
- Purpose: Build native binaries and Docker images

## Artifact Retention

### ci.yml
- JAR: 7 days
- Native: 30 days (main branch only)

### build-jar.yml
- JAR: 30 days
- Release: Permanent (on tags)

### build-native.yml
- Native: 90 days
- Docker: Permanent
- Release: Permanent (on tags)

## Build Times

| Workflow | Typical Time | Cache Hit |
|----------|--------------|------------|
| test.yml | 2-3 min | N/A |
| JAR build | 3-5 min | 1-2 min |
| Native build | 20-30 min | 12-18 min |
| Docker build | 15-25 min | 10-15 min |

## Resource Usage

### Test Job
- Runner: ubuntu-latest
- Memory: ~7 GB
- Services: MySQL container

### JAR Build
- Runner: ubuntu-latest
- Memory: ~3 GB
- Disk: ~500 MB

### Native Build
- Runner: ubuntu-latest
- Memory: ~8-10 GB
- Disk: ~2-3 GB (including GraalVM)

### Docker Build
- Runner: ubuntu-latest
- Memory: ~4 GB
- Cache: GitHub Actions cache

## Cost Estimation

GitHub Actions free tier includes:
- 2,000 minutes/month (public repos: unlimited)
- 500 MB storage

Estimated usage per release cycle:
- Tests: ~50 minutes
- JAR builds: ~30 minutes
- Native builds: ~90 minutes (2-3 builds)
- **Total**: ~170 minutes per cycle

For private repos, this uses ~8.5% of monthly allowance.

## Optimization Tips

1. **Use Caching**: Maven and Docker caches are enabled
2. **Conditional Jobs**: Native builds only on main branch
3. **Parallel Jobs**: Test and build can run in parallel
4. **Artifact Retention**: Adjust based on needs
5. **Manual Triggers**: Use workflow_dispatch for on-demand builds

## Migration Strategy

If you want to consolidate workflows:

### Option 1: Single Comprehensive Workflow
```yaml
# .github/workflows/comprehensive.yml
- Run tests always
- Build JAR always
- Build native on main
- Build Docker on tags
```

### Option 2: Separate Concerns (Current)
```yaml
# Keep workflows separate for:
# - Faster feedback (test.yml)
# - Flexibility (manual triggers)
# - Clear responsibilities
```

## Next Steps

1. **Update Badges**: Replace `YOUR_USERNAME` in README.md
2. **Configure Secrets**: Add Docker Hub credentials (optional)
3. **Test Workflows**: Push a commit to verify all workflows
4. **Monitor Builds**: Check Actions tab for workflow runs
5. **Adjust Retention**: Modify artifact retention as needed

## Support

For issues or questions:
- Check [`.github/README.md`](.github/README.md) for detailed docs
- See [`.github/DOCKER_SETUP.md`](.github/DOCKER_SETUP.md) for Docker setup
- Review workflow logs in Actions tab
- Open an issue for bugs or feature requests
