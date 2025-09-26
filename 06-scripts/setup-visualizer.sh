#!/bin/bash

# =============================================================================
# Setup Visualizer Environment Script
# =============================================================================
# This script sets up the Python virtual environment for the result visualizer
# This is optional - the download-results.sh script will set it up automatically
# =============================================================================

set -e

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

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

main() {
    log "🐍 Setting up Result Visualizer Environment"
    log "==========================================="
    
    local venv_dir="05-result-visualiser/venv"
    
    # Check if directory exists
    if [[ ! -d "05-result-visualiser" ]]; then
        error "Visualizer directory not found. Make sure you're in the project root."
        exit 1
    fi
    
    # Remove existing environment if it exists
    if [[ -d "$venv_dir" ]]; then
        log "Removing existing virtual environment..."
        rm -rf "$venv_dir"
    fi
    
    # Create virtual environment
    log "Creating virtual environment..."
    if ! python3 -m venv "$venv_dir"; then
        error "Failed to create virtual environment"
        exit 1
    fi
    
    # Activate and install dependencies
    log "Installing dependencies..."
    # shellcheck source=/dev/null
    source "$venv_dir/bin/activate"
    
    pip install --upgrade pip
    pip install -r "05-result-visualiser/requirements.txt"
    
    success "Virtual environment setup complete!"
    log "Environment location: $venv_dir"
    log "To activate manually: source $venv_dir/bin/activate"
    log "The download-results.sh script will use this environment automatically."
}

main "$@"