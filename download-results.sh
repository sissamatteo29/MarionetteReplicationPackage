#!/bin/bash

# =============================================================================
# Download Results and Generate Visualizations Script
# =============================================================================
# This script:
# 1. Discovers the marionette-control-plane endpoint
# 2. Fetches AB test results from /api/downloadresult
# 3. Creates a results folder with timestamp
# 4. Runs the Python visualization generator
# 5. Opens generated images with default viewer (cross-platform)
# =============================================================================

set -e  # Exit on any error

# Cleanup function
cleanup() {
    # Deactivate virtual environment if active
    if [[ -n "${VIRTUAL_ENV:-}" ]]; then
        deactivate 2>/dev/null || true
    fi
}

# Set trap for cleanup
trap cleanup EXIT

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[INFO]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" >&2
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" >&2
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Function to open file with default application (cross-platform)
open_file() {
    local file_path="$1"
    
    if [[ ! -f "$file_path" ]]; then
        warning "File not found: $file_path"
        return 1
    fi
    
    log "Opening: $(basename "$file_path")"
    
    case "$(uname -s)" in
        Darwin)
            # macOS
            open "$file_path"
            ;;
        Linux)
            # Linux
            xdg-open "$file_path" 2>/dev/null &
            ;;
        CYGWIN*|MINGW*|MSYS*)
            # Windows (Git Bash, Cygwin, etc.)
            start "$file_path"
            ;;
        *)
            warning "Unsupported operating system: $(uname -s)"
            warning "Please manually open: $file_path"
            return 1
            ;;
    esac
    
    return 0
}

# Function to discover marionette control plane endpoint
discover_marionette_endpoint() {
    log "🔍 Discovering Marionette Control Plane endpoint..."
    
    # Get the minikube IP
    local minikube_ip
    minikube_ip=$(minikube ip 2>/dev/null)
    if [[ -z "$minikube_ip" ]]; then
        error "Could not get minikube IP. Is minikube running?"
        return 1
    fi
    
    log "Minikube IP: $minikube_ip"
    
    local marionette_url=""
    
    # Method 1: Try to use minikube service command
    log "Trying minikube service discovery..."
    marionette_url=$(minikube service marionette-control-plane --namespace=outfit-app --url 2>/dev/null || true)
    
    # Method 2: If that fails, try direct NodePort access
    if [[ -z "$marionette_url" ]]; then
        log "Trying NodePort discovery..."
        local node_port
        node_port=$(kubectl get service marionette-control-plane -n outfit-app -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || true)
        if [[ -n "$node_port" ]]; then
            marionette_url="http://$minikube_ip:$node_port"
        fi
    fi
    
    # Method 3: Try ingress with nginx if available
    if [[ -z "$marionette_url" ]]; then
        log "Trying ingress discovery..."
        local ingress_ip
        ingress_ip=$(kubectl get ingress marionette-control-plane-ingress -n outfit-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)
        if [[ -n "$ingress_ip" ]]; then
            marionette_url="http://$ingress_ip/marionette"
        else
            # Fallback to minikube IP with ingress path
            marionette_url="http://$minikube_ip/marionette"
        fi
    fi
    
    # Final fallback
    if [[ -z "$marionette_url" ]]; then
        warning "Could not discover marionette control plane automatically"
        marionette_url="http://$minikube_ip:30081"  # Assume default NodePort
        warning "Using fallback URL: $marionette_url"
    else
        success "Discovered Marionette Control Plane: $marionette_url"
    fi
    
    # Return the URL - this should be the only output that gets captured
    echo "$marionette_url"
    return 0
}

# Function to test connection to marionette
test_marionette_connection() {
    local url="$1"
    
    log "🧪 Testing connection to marionette control plane..."
    
    if curl -f -s --connect-timeout 10 "$url/actuator/health" > /dev/null 2>&1; then
        success "Marionette control plane is responding (health check passed)"
        return 0
    elif curl -f -s --connect-timeout 10 "$url/" > /dev/null 2>&1; then
        success "Marionette control plane endpoint is accessible"
        return 0
    else
        error "Could not connect to marionette control plane at: $url"
        return 1
    fi
}

# Function to check if results are available
check_results_available() {
    local url="$1"
    
    log "🔍 Checking if test results are available..."
    
    local response
    response=$(curl -s -w "%{http_code}" "$url/api/downloadresult/available" 2>/dev/null)
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [[ "$http_code" -eq 200 ]]; then
        if [[ "$body" == "true" ]]; then
            success "Test results are available for download"
            return 0
        else
            warning "No test results available yet"
            return 1
        fi
    else
        error "Failed to check result availability (HTTP $http_code)"
        return 1
    fi
}

# Function to download test results
download_results() {
    local url="$1"
    local output_file="$2"
    
    log "📥 Downloading test results..."
    
    local response
    response=$(curl -s -w "%{http_code}" "$url/api/downloadresult" -o "$output_file" 2>/dev/null)
    local http_code="${response: -3}"
    
    if [[ "$http_code" -eq 200 ]]; then
        if [[ -f "$output_file" ]] && [[ -s "$output_file" ]]; then
            success "Test results downloaded successfully to: $output_file"
            return 0
        else
            error "Downloaded file is empty or doesn't exist"
            return 1
        fi
    else
        error "Failed to download results (HTTP $http_code)"
        return 1
    fi
}

# Function to setup Python environment for visualizations
setup_python_environment() {
    local venv_dir="05-result-visualiser/venv"
    
    log "🐍 Setting up Python environment..."
    
    # Check if virtual environment already exists
    if [[ -d "$venv_dir" ]]; then
        log "Virtual environment already exists"
    else
        log "Creating virtual environment..."
        if ! python3 -m venv "$venv_dir"; then
            error "Failed to create virtual environment"
            return 1
        fi
    fi
    
    # Activate virtual environment and install dependencies
    log "Installing/updating dependencies..."
    # shellcheck source=/dev/null
    source "$venv_dir/bin/activate"
    
    if ! pip install -q --upgrade pip; then
        error "Failed to upgrade pip"
        return 1
    fi
    
    if ! pip install -q -r "05-result-visualiser/requirements.txt"; then
        error "Failed to install visualization dependencies"
        return 1
    fi
    
    success "Python environment ready"
    return 0
}

# Function to generate visualizations
generate_visualizations() {
    local config_file="$1"
    local output_dir="$2"
    local venv_dir="05-result-visualiser/venv"
    
    log "🎨 Generating visualizations..."
    
    # Check if config file exists
    if [[ ! -f "$config_file" ]]; then
        error "Configuration file not found: $config_file"
        return 1
    fi
    
    # Validate JSON structure
    if ! jq empty "$config_file" 2>/dev/null; then
        error "Invalid JSON in configuration file: $config_file"
        return 1
    fi
    
    # Setup Python environment
    if ! setup_python_environment; then
        error "Failed to setup Python environment"
        return 1
    fi
    
    # Change to visualizer directory
    cd 05-result-visualiser || {
        error "Could not find visualizer directory"
        return 1
    }
    
    log "Running visualization generator..."
    log "Configuration file: $config_file"
    log "Output directory: $output_dir"
    
    # Get absolute paths for the Python script
    local abs_config_file
    local abs_output_dir
    abs_config_file=$(realpath "../$config_file")
    abs_output_dir=$(realpath "../$output_dir")
    
    # Activate virtual environment and run the Python script
    # shellcheck source=/dev/null
    source "venv/bin/activate"
    
    if ! python main.py "$abs_config_file" --output-dir "$abs_output_dir" --step all; then
        error "Python visualization script failed"
        cd ..
        return 1
    fi
    
    cd ..
    success "Visualizations generated successfully in: $output_dir"
    return 0
}

# Function to find and open generated images
open_generated_images() {
    local results_dir="$1"
    
    log "🖼️  Looking for generated images..."
    
    # Common image extensions
    local image_extensions=("*.png" "*.jpg" "*.jpeg" "*.gif" "*.bmp" "*.svg")
    local found_images=()
    
    # Find all image files in the results directory
    for ext in "${image_extensions[@]}"; do
        while IFS= read -r -d '' file; do
            found_images+=("$file")
        done < <(find "$results_dir" -name "$ext" -type f -print0 2>/dev/null)
    done
    
    if [[ ${#found_images[@]} -eq 0 ]]; then
        warning "No image files found in: $results_dir"
        return 1
    fi
    
    success "Found ${#found_images[@]} image(s) to open"
    
    # Open each image with a small delay
    for image in "${found_images[@]}"; do
        if open_file "$image"; then
            sleep 0.5  # Small delay between opening files
        fi
    done
    
    return 0
}

# Function to check system dependencies
check_dependencies() {
    log "🔍 Checking system dependencies..."
    
    local missing_deps=()
    
    # Check for required commands
    if ! command -v python3 >/dev/null 2>&1; then
        missing_deps+=("python3")
    fi
    
    if ! command -v pip >/dev/null 2>&1 && ! python3 -m pip --version >/dev/null 2>&1; then
        missing_deps+=("pip")
    fi
    
    if ! command -v jq >/dev/null 2>&1; then
        missing_deps+=("jq")
    fi
    
    if ! command -v curl >/dev/null 2>&1; then
        missing_deps+=("curl")
    fi
    
    if ! command -v kubectl >/dev/null 2>&1; then
        missing_deps+=("kubectl")
    fi
    
    if ! command -v minikube >/dev/null 2>&1; then
        missing_deps+=("minikube")
    fi
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        error "Missing required dependencies: ${missing_deps[*]}"
        error "Please install the missing dependencies and try again"
        return 1
    fi
    
    success "All system dependencies are available"
    return 0
}

# Main function
main() {
    log "🚀 Starting AB Test Results Download and Visualization"
    log "=================================================="
    
    # Check system dependencies
    if ! check_dependencies; then
        exit 1
    fi
    
    # Discover marionette endpoint
    local marionette_url
    if ! marionette_url=$(discover_marionette_endpoint); then
        error "Failed to discover marionette control plane endpoint"
        exit 1
    fi
    
    # Test connection
    if ! test_marionette_connection "$marionette_url"; then
        error "Cannot proceed without connection to marionette control plane"
        exit 1
    fi
    
    # Check if results are available
    if ! check_results_available "$marionette_url"; then
        error "No test results available. Make sure an AB test has been completed."
        exit 1
    fi
    
    # Create results directory with timestamp
    local timestamp
    timestamp=$(date +"%Y%m%d_%H%M%S")
    local results_dir="results/ab_test_$timestamp"
    
    log "📁 Creating results directory: $results_dir"
    mkdir -p "$results_dir"
    
    # Download results
    local results_file="$results_dir/test_results.json"
    if ! download_results "$marionette_url" "$results_file"; then
        error "Failed to download test results"
        exit 1
    fi
    
    # Generate visualizations
    if ! generate_visualizations "$results_file" "$results_dir"; then
        error "Failed to generate visualizations"
        exit 1
    fi
    
    # Open generated images
    if ! open_generated_images "$results_dir"; then
        warning "Could not open images automatically"
        log "Please manually check the results directory: $results_dir"
    fi
    
    success "✅ AB Test results processing completed!"
    success "📊 Results saved to: $results_dir"
    success "🎨 Visualizations have been generated and opened"
    
    # Show summary
    log ""
    log "📋 Summary:"
    log "  - Marionette endpoint: $marionette_url"
    log "  - Results file: $results_file"
    log "  - Visualizations: $results_dir"
    log ""
    log "You can re-open the images anytime by running:"
    log "  find $results_dir -name '*.png' -exec xdg-open {} \\;"
}

# Run main function
main "$@"