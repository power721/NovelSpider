#!/bin/bash
# Script to verify GraalVM native image build environment

echo "Checking GraalVM native image build environment..."
echo ""

# Check if JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    echo "❌ JAVA_HOME is not set"
    echo "   Please set JAVA_HOME to your GraalVM installation"
    echo "   Example: export JAVA_HOME=/opt/graalvm"
else
    echo "✅ JAVA_HOME is set: $JAVA_HOME"
fi

# Check if Java is from GraalVM
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    if [[ "$JAVA_VERSION" == *"GraalVM"* ]]; then
        echo "✅ GraalVM detected: $JAVA_VERSION"
    else
        echo "⚠️  Java is not from GraalVM: $JAVA_VERSION"
        echo "   Please install GraalVM: https://www.graalvm.org/downloads/"
    fi
else
    echo "❌ Java command not found"
fi

# Check if native-image is installed
if command -v native-image &> /dev/null; then
    NATIVE_VERSION=$(native-image --version 2>&1)
    echo "✅ native-image found: $NATIVE_VERSION"
else
    echo "❌ native-image command not found"
    echo "   Please install: gu install native-image"
fi

# Check if Maven wrapper exists
if [ -f "./mvnw" ]; then
    echo "✅ Maven wrapper found"
else
    echo "❌ Maven wrapper not found"
fi

# Check if configuration files exist
echo ""
echo "Checking GraalVM configuration files..."
CONFIG_DIR="src/main/resources/META-INF/native-image"

if [ -f "$CONFIG_DIR/reflect-config.json" ]; then
    echo "✅ reflect-config.json found"
else
    echo "⚠️  reflect-config.json not found"
fi

if [ -f "$CONFIG_DIR/resource-config.json" ]; then
    echo "✅ resource-config.json found"
else
    echo "⚠️  resource-config.json not found"
fi

if [ -f "$CONFIG_DIR/proxy-config.json" ]; then
    echo "✅ proxy-config.json found"
else
    echo "⚠️  proxy-config.json not found"
fi

# Check available memory
echo ""
TOTAL_MEM=$(free -h | awk '/^Mem:/ {print $2}')
AVAIL_MEM=$(free -h | awk '/^Mem:/ {print $7}')
echo "💾 Memory: $AVAIL_MEM available / $TOTAL_MEM total"

# Recommended minimum: 8GB
TOTAL_MEM_MB=$(free -m | awk '/^Mem:/ {print $2}')
if [ $TOTAL_MEM_MB -lt 8192 ]; then
    echo "⚠️  Less than 8GB RAM available. Native build may fail or be very slow."
else
    echo "✅ Sufficient memory for native build"
fi

# Check available disk space
echo ""
DISK_AVAIL=$(df -h . | tail -1 | awk '{print $4}')
DISK_AVAIL_MB=$(df -m . | tail -1 | awk '{print $4}')
echo "💽 Disk space: $DISK_AVAIL available"

# Recommended minimum: 5GB
if [ $DISK_AVAIL_MB -lt 5120 ]; then
    echo "⚠️  Less than 5GB disk space available. Native build may fail."
else
    echo "✅ Sufficient disk space for native build"
fi

echo ""
echo "Environment check complete!"
echo ""
echo "Next steps:"
echo "1. If all checks passed, run: ./build-native.sh"
echo "2. If checks failed, fix the issues above"
echo "3. For manual build: ./mvnw -Pnative clean package"
