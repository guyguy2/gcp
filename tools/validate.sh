#!/bin/bash

# Developer Hub Platform - Validation Script
# Validates code syntax, YAML files, Kubernetes manifests, and Docker configurations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track overall status
FAILED_CHECKS=0
TOTAL_CHECKS=0

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE}Developer Hub Validation Script${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Helper function to print section headers
print_section() {
    echo -e "\n${BLUE}[$1]${NC}"
    echo "-----------------------------------"
}

# Helper function to print results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
}

# Helper function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Helper function to print warnings
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Helper function to print info
print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Check for required tools
print_section "Checking Required Tools"

MISSING_TOOLS=0

if ! command_exists yamllint; then
    print_warning "yamllint not found (install: brew install yamllint)"
    MISSING_TOOLS=$((MISSING_TOOLS + 1))
else
    echo -e "${GREEN}✓ yamllint installed${NC}"
fi

if ! command_exists kubectl; then
    print_warning "kubectl not found (install: brew install kubectl)"
    MISSING_TOOLS=$((MISSING_TOOLS + 1))
else
    echo -e "${GREEN}✓ kubectl installed${NC}"
fi

if ! command_exists mvn; then
    print_warning "maven not found (install: brew install maven)"
    MISSING_TOOLS=$((MISSING_TOOLS + 1))
else
    echo -e "${GREEN}✓ maven installed${NC}"
fi

if ! command_exists hadolint; then
    print_warning "hadolint not found (install: brew install hadolint)"
    MISSING_TOOLS=$((MISSING_TOOLS + 1))
else
    echo -e "${GREEN}✓ hadolint installed${NC}"
fi

if ! command_exists actionlint; then
    print_warning "actionlint not found (install: brew install actionlint)"
    MISSING_TOOLS=$((MISSING_TOOLS + 1))
else
    echo -e "${GREEN}✓ actionlint installed${NC}"
fi

if [ $MISSING_TOOLS -gt 0 ]; then
    print_warning "$MISSING_TOOLS tools are missing. Some validations will be skipped."
    echo ""
fi

# YAML Syntax Validation
print_section "YAML Syntax Validation"

if command_exists yamllint; then
    if yamllint -f parsable "$PROJECT_ROOT" 2>&1 | grep -v "^$" > /tmp/yamllint_output.txt; then
        if [ -s /tmp/yamllint_output.txt ]; then
            cat /tmp/yamllint_output.txt
            print_result 1 "YAML syntax check (found issues)"
        else
            print_result 0 "YAML syntax check"
        fi
    else
        print_result 0 "YAML syntax check"
    fi
    rm -f /tmp/yamllint_output.txt
else
    print_info "Skipping YAML validation (yamllint not installed)"
fi

# Kubernetes Manifests Validation
print_section "Kubernetes Manifests Validation"

K8S_DIR="$PROJECT_ROOT/deploy/kubernetes"

# Check if kubeval is available for offline validation
if command_exists kubeval; then
    if [ -d "$K8S_DIR" ]; then
        for manifest in "$K8S_DIR"/*.yaml; do
            if [ -f "$manifest" ]; then
                filename=$(basename "$manifest")
                if kubeval "$manifest" > /dev/null 2>&1; then
                    print_result 0 "$filename"
                else
                    kubeval "$manifest" 2>&1
                    print_result 1 "$filename"
                fi
            fi
        done
    fi
elif command_exists kubectl && kubectl cluster-info > /dev/null 2>&1; then
    # kubectl is connected to cluster - use it for validation
    if [ -d "$K8S_DIR" ]; then
        for manifest in "$K8S_DIR"/*.yaml; do
            if [ -f "$manifest" ]; then
                filename=$(basename "$manifest")
                if kubectl apply --dry-run=client -f "$manifest" > /dev/null 2>&1; then
                    print_result 0 "$filename"
                else
                    kubectl apply --dry-run=client -f "$manifest" 2>&1 | tail -5
                    print_result 1 "$filename"
                fi
            fi
        done
    fi
else
    print_info "Skipping K8s validation (install kubeval for offline: brew install kubeval)"
    print_info "Or connect kubectl to cluster for online validation"
fi

# GitHub Actions Workflow Validation
print_section "GitHub Actions Workflow Validation"

if command_exists actionlint; then
    WORKFLOW_DIR="$PROJECT_ROOT/deploy/ci-cd"

    if [ -d "$WORKFLOW_DIR" ]; then
        if actionlint "$WORKFLOW_DIR"/*.yml > /tmp/actionlint_output.txt 2>&1; then
            print_result 0 "GitHub Actions workflows"
        else
            cat /tmp/actionlint_output.txt
            print_result 1 "GitHub Actions workflows"
        fi
        rm -f /tmp/actionlint_output.txt
    else
        print_warning "Workflow directory not found: $WORKFLOW_DIR"
    fi
else
    print_info "Skipping GitHub Actions validation (actionlint not installed)"
fi

# Dockerfile Validation
print_section "Dockerfile Validation"

if command_exists hadolint; then
    DOCKERFILE="$PROJECT_ROOT/services/api/Dockerfile"

    if [ -f "$DOCKERFILE" ]; then
        if hadolint "$DOCKERFILE" > /tmp/hadolint_output.txt 2>&1; then
            print_result 0 "Dockerfile lint"
        else
            cat /tmp/hadolint_output.txt
            print_result 1 "Dockerfile lint"
        fi
        rm -f /tmp/hadolint_output.txt
    else
        print_warning "Dockerfile not found: $DOCKERFILE"
    fi
else
    print_info "Skipping Dockerfile validation (hadolint not installed)"
fi

# Java/Maven Validation
print_section "Java/Maven Validation"

if command_exists mvn; then
    API_DIR="$PROJECT_ROOT/services/api"

    if [ -f "$API_DIR/pom.xml" ]; then
        cd "$API_DIR"

        # Validate pom.xml
        if mvn validate -q > /dev/null 2>&1; then
            print_result 0 "pom.xml validation"
        else
            mvn validate 2>&1 | tail -10
            print_result 1 "pom.xml validation"
        fi

        # Compile (without tests)
        print_info "Compiling Java code (this may take a moment)..."
        if mvn clean compile -q -DskipTests > /tmp/mvn_compile.log 2>&1; then
            print_result 0 "Java compilation"
        else
            tail -20 /tmp/mvn_compile.log
            print_result 1 "Java compilation"
        fi
        rm -f /tmp/mvn_compile.log

        # Check dependencies
        if mvn dependency:analyze -q > /tmp/mvn_deps.log 2>&1; then
            print_result 0 "Maven dependency analysis"
        else
            tail -20 /tmp/mvn_deps.log
            print_result 1 "Maven dependency analysis"
        fi
        rm -f /tmp/mvn_deps.log

        cd "$PROJECT_ROOT"
    else
        print_warning "pom.xml not found in $API_DIR"
    fi
else
    print_info "Skipping Maven validation (mvn not installed)"
fi

# Summary
print_section "Validation Summary"

PASSED_CHECKS=$((TOTAL_CHECKS - FAILED_CHECKS))

echo ""
echo "Total Checks: $TOTAL_CHECKS"
echo -e "${GREEN}Passed: $PASSED_CHECKS${NC}"

if [ $FAILED_CHECKS -gt 0 ]; then
    echo -e "${RED}Failed: $FAILED_CHECKS${NC}"
    echo ""
    echo -e "${RED}Validation completed with failures${NC}"
    exit 1
else
    echo -e "${RED}Failed: 0${NC}"
    echo ""
    echo -e "${GREEN}All validations passed!${NC}"
    exit 0
fi
