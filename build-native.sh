#!/bin/bash
# Script to build GraalVM native image for NovelSpider
export JAVA_HOME=/opt/graalvm

echo "Building GraalVM native image for NovelSpider..."

# Check if JAVA_HOME is set to GraalVM
if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME is not set"
    echo "Please set JAVA_HOME to your GraalVM installation"
    echo "Example: export JAVA_HOME=/path/to/graalvm"
    exit 1
fi

# Check if native-image is installed
#if ! command -v native-image &> /dev/null; then
#    echo "Error: native-image command not found"
#    echo "Please install native-image: gu install native-image"
#    exit 1
#fi

# Build the native image
./mvnw -Pnative clean package -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✓ Native image built successfully!"
    echo "Executable location: target/NovelSpider"
    echo ""
    echo "To run the native executable:"
    echo "  ./target/NovelSpider"
    echo ""
    echo "Or using Docker:"
    echo "  docker build -f Dockerfile.native -t novelspider-native ."
    echo "  docker-compose -f docker-compose.native.yml up"
else
    echo "✗ Build failed!"
    exit 1
fi
