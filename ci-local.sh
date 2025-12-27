#!/bin/bash
#
# Local CI Script
#
# This script runs the same checks as the GitHub Actions CI locally
# Usage: ./ci-local.sh

set -e

echo "=================================================="
echo "Flex-KT Local CI Pipeline"
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Make gradlew executable
chmod +x gradlew

echo -e "${BLUE}[1/4] Compiling code...${NC}"
./gradlew compileKotlin

echo -e "${BLUE}[2/4] Running unit tests...${NC}"
./gradlew test

echo -e "${BLUE}[3/4] Generating coverage report...${NC}"
./gradlew jacocoTestReport

echo -e "${BLUE}[4/4] Building library...${NC}"
./gradlew build

echo -e "${GREEN}=================================================="
echo "✓ All checks passed!"
echo "==================================================${NC}"

echo ""
echo "Coverage report generated at: lib/build/reports/jacoco/test/html/index.html"
echo "Test results at: lib/build/reports/tests/test/index.html"

