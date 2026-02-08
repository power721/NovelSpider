# GraalVM Native Build Troubleshooting

## Quick Check

Run the environment check script first:

```bash
./check-native-env.sh
```

## Common Issues and Solutions

### Issue: "native-image: command not found"

**Solution:**
```bash
# Install GraalVM
# Download from: https://www.graalvm.org/downloads/

# Set JAVA_HOME
export JAVA_HOME=/path/to/graalvm

# Install native-image component
gu install native-image
```

### Issue: "Error: Could not find option 'ProxyConfigurationFiles'"

**Status:** ✅ Fixed in latest pom.xml

The correct option is now `-H:DynamicProxyConfigurationFiles`. Update your pom.xml if you see this error.

### Issue: "Warning: The option '-H:+ReportUnsupportedElementsAtRuntime' is experimental"

**Status:** ✅ Fixed in latest pom.xml

Added `-H:+UnlockExperimentalVMOptions` to allow experimental options.

### Issue: Build fails with "Out of memory"

**Solution:**
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx4g"

# Or run with more memory directly
./mvnw -Pnative clean package -DskipTests -Xmx4g
```

**System Requirements:**
- Minimum 8GB RAM
- Minimum 5GB free disk space
- Recommended 16GB RAM for faster builds

### Issue: Build takes too long (>60 minutes)

**Normal times:**
- First build: 20-30 minutes
- Cached build: 12-18 minutes

**To speed up:**
1. Use Maven local repository caching
2. Close other applications
3. Use faster storage (SSD vs HDD)
4. Increase available RAM

### Issue: "ClassNotFoundException" or "NoSuchMethodError" at runtime

**Cause:** Missing reflection configuration

**Solution:** Add the class to `src/main/resources/META-INF/native-image/reflect-config.json`:

```json
{
  "name": "com.example.YourClass",
  "allDeclaredConstructors": true,
  "allPublicConstructors": true,
  "allDeclaredMethods": true,
  "allPublicMethods": true,
  "allDeclaredFields": true,
  "allPublicFields": true
}
```

### Issue: "Resource not found" at runtime

**Cause:** Missing resource configuration

**Solution:** Add the resource pattern to `src/main/resources/META-INF/native-image/resource-config.json`:

```json
{
  "resources": {
    "includes": [
      {
        "pattern": "your/resource/.*"
      }
    ]
  }
}
```

### Issue: Hibernate/JPA lazy loading fails

**Cause:** Missing proxy configuration

**Solution:** Add to `src/main/resources/META-INF/native-image/proxy-config.json`:

```json
[
  [
    "org.hibernate.proxy.HibernateProxy",
    "cn.har01d.novel.spider.Novel"
  ]
]
```

### Issue: Static web resources not served

**Cause:** Resources not embedded in native binary

**Solution:** Ensure frontend is built before native compilation:

```bash
# Build frontend first
cd web-ui && npm run build && cd ..

# Then build native
./mvnw -Pnative clean package
```

### Issue: MySQL driver not working in native image

**Cause:** MySQL connector needs additional configuration

**Solution:** Add to `reflect-config.json`:

```json
{
  "name": "com.mysql.cj.jdbc.Driver",
  "allDeclaredConstructors": true,
  "allPublicConstructors": true
}
```

## Warning Messages

### "Warning: Using a deprecated option"

These warnings come from Spring Boot's AOT processing and are harmless. They will be fixed in future Spring Boot versions.

**Examples:**
- `--report-unsupported-elements-at-runtime` - From Spring, safe to ignore
- `DynamicProxyConfigurationResources` - Proxy config deprecated but still works

## Debugging Native Build Issues

### Enable Verbose Output

```bash
./mvnw -Pnative clean package -DskipTests \
  -Dgraalvm.verbose=true \
  -Dgraalvm.debug.allocations=true
```

### Generate Tracer Agent Output

For automatically detecting missing configuration:

```bash
# Run with tracer agent (requires JVM mode)
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -jar target/NovelSpider-*.jar

# Exercise your application (make API calls, etc.)

# Then rebuild native image
./mvnw -Pnative clean package
```

### Check Native Image Configuration

```bash
# List all included resources
native-image --dump-config-file-output.txt target/NovelSpider

# View GraalVM version and options
native-image --help | grep -A 5 "Experimental"
```

## Performance Optimization

### Reduce Native Image Size

```xml
<!-- In pom.xml buildArgs -->
<arg>--strip-debug</arg>
<arg>--no-fallback</arg>
```

### Improve Startup Time

```xml
<!-- In pom.xml buildArgs -->
<arg>-H:+InlineAll</arg>
<arg>-H:-SpawnIsolates</arg>
```

### Reduce Memory Footprint

```xml
<!-- In pom.xml buildArgs -->
<arg>-H:+ReportUnusedElementsAtRuntime</arg>
<arg>--remove-unused-heap</arg>
```

## Platform-Specific Issues

### Linux
- ✅ Fully supported
- Use native Linux GraalVM

### macOS
- ✅ Fully supported
- May need Xcode command line tools

### Windows
- ⚠️ Requires WSL2 or use GitHub Actions
- Native Windows builds with WSL2

## Getting Help

1. Check GraalVM docs: https://www.graalvm.org/latest/reference-manual/native-image/
2. Spring Native guide: https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html
3. GitHub Issues: https://github.com/oracle/graalvm/issues
4. Stack Overflow: Tag with `graalvm` and `spring-native`

## Build Time Optimization Tips

1. **Use CI/CD**: Build in GitHub Actions (free for public repos)
2. **Cache Dependencies**: Maven cache is enabled in workflows
3. **Parallel Builds**: Not available for native (single-threaded)
4. **Incremental Builds**: Not well supported, always do clean builds

## Verification

After successful build, verify the native binary:

```bash
# Check binary is executable
file target/NovelSpider
# Expected: ELF 64-bit LSB executable, x86-64

# Check for static linking
ldd target/NovelSpider
# Should show: not a dynamic executable

# Run the binary
./target/NovelSpider
# Should start quickly (~100ms)
```

## Advanced: Custom Build Configuration

Create `src/main/resources/META-INF/native-image/cn.har01d/NovelSpider/native-image.properties`:

```properties
# Enable experimental options
Args=-H:+UnlockExperimentalVMOptions

# Add custom build args
Args=--enable-https
Args=--no-fallback
Args=-H:+ReportExceptionStackTraces

# Include configuration files
Args=-H:ResourceConfigurationFiles=src/main/resources/META-INF/native-image/resource-config.json
Args=-H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json
Args=-H:DynamicProxyConfigurationFiles=src/main/resources/META-INF/native-image/proxy-config.json
```

Note: This approach is redundant with the pom.xml configuration but can be useful for debugging.
