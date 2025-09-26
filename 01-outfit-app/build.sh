#!/bin/bash
# Enhanced deploy.sh with smart building and Marionette integration

set -e

# Configuration
FORCE_REBUILD=${FORCE_REBUILD:-false}
SKIP_MARIONETTE=${SKIP_MARIONETTE:-false}
VERBOSE=${VERBOSE:-false}
BUILD_CACHE_DIR=".build-cache"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Logging functions
log() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠️${NC} $1"
}

error() {
    echo -e "${RED}❌${NC} $1"
    exit 1
}

verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${YELLOW}🔍${NC} $1"
    fi
}

# Help function
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Deploy outfit-app with Marionette instrumentation to Minikube

OPTIONS:
    --force-rebuild     Force rebuild of all components (ignore cache)
    --skip-marionette   Skip Marionette instrumentation step
    --clean             Clean all build artifacts and start fresh
    --verbose           Enable verbose output
    --help              Show this help message

EXAMPLES:
    $0                          # Smart incremental build
    $0 --force-rebuild          # Force rebuild everything
    $0 --skip-marionette        # Use pre-instrumented services
    $0 --clean --force-rebuild  # Complete clean rebuild

ENVIRONMENT VARIABLES:
    FORCE_REBUILD=true          # Same as --force-rebuild
    SKIP_MARIONETTE=true        # Same as --skip-marionette
    VERBOSE=true                # Same as --verbose
EOF
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --clean-transformed)
                clean_transformed_services
                exit 0
                ;;
            --show-diffs)
                for service in image-processor-service imagestore-service ui-service; do
                    show_transformation_diff "$service"
                done
                exit 0
                ;;
            --force-rebuild)
                FORCE_REBUILD=true
                shift
                ;;
            --skip-marionette)
                SKIP_MARIONETTE=true
                shift
                ;;
            --clean)
                clean_build_artifacts
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                error "Unknown option: $1. Use --help for usage information."
                ;;
        esac
    done
}

# Create build cache directory
init_build_cache() {
    mkdir -p "$BUILD_CACHE_DIR"
    verbose "Build cache directory: $BUILD_CACHE_DIR"
}

# Clean build artifacts
clean_build_artifacts() {
    log "Cleaning build artifacts..."
    
    # Clean Maven artifacts
    if [[ -d "common" ]]; then
        cd common && mvn clean && cd ..
    fi
    
    find services -name target -type d -exec rm -rf {} + 2>/dev/null || true
    
    # Clean Docker images
    docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "(image-processor-service|imagestore-service|ui-service)" | xargs -r docker rmi -f
    
    # Clean build cache
    rm -rf "$BUILD_CACHE_DIR"
    
    success "Build artifacts cleaned"
}

apply_marionette_transformation() {
    local service=$1
    local original_dir="services/${service}"
    local transformed_dir="services/${service}-marionette"
    local marionette_jar="../../../02-marionette-tool/marionette-tool-1.0.jar"  # Relative to service directory
    
    if [[ "$SKIP_MARIONETTE" == "true" ]]; then
        warning "Skipping Marionette transformation for $service"
        
        # If skipping, ensure we have a copy to build from
        if [[ ! -d "$transformed_dir" ]]; then
            log "Creating non-transformed copy for $service..."
            cp -r "$original_dir" "$transformed_dir"
        fi
        return 0
    fi
    
    log "Checking Marionette transformation for $service..."
    
    # Check if transformation is needed
    if needs_rebuild "marionette-${service}" "$original_dir" "$transformed_dir"; then
        log "Applying Marionette transformation to $service..."
        
        # Remove existing transformed version if it exists
        if [[ -d "$transformed_dir" ]]; then
            rm -rf "$transformed_dir"
            verbose "Removed existing transformed directory: $transformed_dir"
        fi
        
        # Verify original service directory exists
        if [[ ! -d "$original_dir" ]]; then
            error "Original service directory not found: $original_dir"
        fi
        
        # Verify Marionette jar exists (check from current directory before cd)
        if [[ ! -f "../02-marionette-tool/marionette-tool-1.0.jar" ]]; then
            error "Marionette tool not found: ../02-marionette-tool/marionette-tool-1.0.jar"
        fi
        
        # Change to the service directory and run Marionette tool
        verbose "Entering directory: $original_dir"
        cd "$original_dir"
        
        verbose "Running: java -jar $marionette_jar -o ../"
        if ! java -jar "$marionette_jar" -o ".."; then
            cd ../..  # Return to original directory
            error "Marionette transformation failed for $service"
        fi
        
        # Return to original directory
        cd ../..
        
        # Verify transformation was successful
        if [[ ! -d "$transformed_dir" ]]; then
            error "Transformed directory was not created: $transformed_dir"
        fi
        
        if [[ ! -d "$transformed_dir/src" ]]; then
            error "Transformed directory missing src/: $transformed_dir"
        fi
        
        # Transformation completed successfully
        success "Marionette transformation applied to $service"
        update_cache "marionette-${service}"
        
        # Store transformation metadata
        echo "$(date): Transformed from $original_dir using marionette-tool-1.0.jar" > "$transformed_dir/.marionette-metadata"
    else
        success "Marionette transformation for $service up to date"
    fi
}

needs_rebuild() {
    local component=$1
    local source_dir=$2
    local output_dir=$3  # Optional for transformation checks
    local cache_file="$BUILD_CACHE_DIR/${component}.timestamp"
    
    if [[ "$FORCE_REBUILD" == "true" ]]; then
        verbose "$component: Force rebuild requested"
        return 0
    fi
    
    # If output doesn't exist, definitely need to rebuild
    if [[ -n "$output_dir" ]] && [[ ! -d "$output_dir" ]]; then
        verbose "$component: Output directory missing"
        return 0
    fi
    
    if [[ ! -f "$cache_file" ]]; then
        verbose "$component: No cache found"
        return 0
    fi
    
    # Check if any source file is newer than cache
    if find "$source_dir" -newer "$cache_file" -type f | head -1 | grep -q .; then
        verbose "$component: Source files changed since last build"
        return 0
    fi
    
    # For Marionette transformations, also check if config changed
    if [[ "$component" == marionette-* ]]; then
        local service_name=${component#marionette-}
        local config_file="../02-marionette-framework/config/${service_name}-marionette.yaml"
        if [[ -f "$config_file" ]] && [[ "$config_file" -nt "$cache_file" ]]; then
            verbose "$component: Marionette configuration changed"
            return 0
        fi
    fi
    
    verbose "$component: Up to date"
    return 1
}

# Update build cache
update_cache() {
    local component=$1
    local cache_file="$BUILD_CACHE_DIR/${component}.timestamp"
    
    touch "$cache_file"
    verbose "$component: Cache updated"
}

# Check if Docker image exists and is recent
docker_image_exists() {
    local image_name=$1
    local cache_file="$BUILD_CACHE_DIR/${image_name//\//-}.docker"
    
    if [[ "$FORCE_REBUILD" == "true" ]]; then
        return 1  # force rebuild
    fi
    
    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${image_name}$"; then
        verbose "Docker image $image_name: Not found"
        return 1  # doesn't exist
    fi
    
    if [[ ! -f "$cache_file" ]]; then
        verbose "Docker image $image_name: No cache"
        return 1  # no cache
    fi
    
    verbose "Docker image $image_name: Exists and cached"
    return 0  # exists and cached
}

# Build common dependencies
build_common() {
    log "Checking common dependencies..."
    
    if needs_rebuild "common" "common/"; then
        log "Building common code..."
        cd common && mvn clean install && cd ..
        update_cache "common"
        success "Common code built"
    else
        success "Common code up to date"
    fi
}

# Apply Marionette instrumentation
apply_marionette_instrumentation() {
    local service=$1
    local original_dir="services/${service}"
    local instrumented_dir="services/${service}-marionette"
    
    if [[ "$SKIP_MARIONETTE" == "true" ]]; then
        warning "Skipping Marionette instrumentation for $service"
        return 0
    fi
    
    log "Checking Marionette instrumentation for $service..."
    
    if needs_rebuild "marionette-${service}" "$original_dir"; then
        log "Applying Marionette instrumentation to $service..."
        
        # Remove existing instrumented version
        rm -rf "$instrumented_dir"
        
        # Copy original service
        cp -r "$original_dir" "$instrumented_dir"
        
        # Apply Marionette transformation
        # TODO: Replace this with your actual Marionette tool invocation
        java -jar ../02-marionette-framework/marionette-core-1.0.jar \
            --input "$instrumented_dir" \
            --output "$instrumented_dir" \
            --config "../02-marionette-framework/config/${service}-config.yaml" \
            --in-place
        
        # Verify instrumentation was successful
        if grep -r "@MarionetteMethod" "$instrumented_dir/src/" > /dev/null; then
            success "Marionette instrumentation applied to $service"
            update_cache "marionette-${service}"
        else
            error "Marionette instrumentation failed for $service"
        fi
    else
        success "Marionette instrumentation for $service up to date"
    fi
}

# Build and package a service
build_transformed_service() {
    local service=$1
    local transformed_dir="services/${service}-marionette"
    
    if [[ ! -d "$transformed_dir" ]]; then
        error "Transformed directory not found: $transformed_dir. Run transformation first."
    fi
    
    log "Checking $service build..."
    
    if needs_rebuild "build-${service}" "$transformed_dir"; then
        log "Building transformed $service..."
        
        cd "$transformed_dir"
        
        # Maven build with error handling
        if ! mvn clean package -DskipTests; then
            error "Maven build failed for $service"
        fi
        
        # Verify JAR was created
        if ! ls target/*.jar >/dev/null 2>&1; then
            error "JAR file not found after build for $service"
        fi
        
        cd ../..
        update_cache "build-${service}"
        success "$service built successfully"
    else
        success "$service build up to date"
    fi
}


show_transformation_diff() {
    local service=$1
    local original_dir="services/${service}"
    local transformed_dir="services/${service}-marionette"
    
    if [[ ! -d "$original_dir" ]] || [[ ! -d "$transformed_dir" ]]; then
        warning "Cannot show diff for $service - missing directories"
        return
    fi
    
    echo
    log "Transformation summary for $service:"
    
    # Count Java files modified
    local modified_files=0
    while IFS= read -r -d '' file; do
        local rel_path=${file#$transformed_dir/}
        local original_file="$original_dir/$rel_path"
        
        if [[ -f "$original_file" ]] && ! diff -q "$file" "$original_file" > /dev/null 2>&1; then
            ((modified_files++))
        fi
    done < <(find "$transformed_dir" -name "*.java" -type f -print0)
    
    # Count new annotations
    local marionette_annotations=$(grep -r "@MarionetteMethod" "$transformed_dir" 2>/dev/null | wc -l)
    
    echo "  📝 Modified Java files: $modified_files"
    echo "  🎭 @MarionetteMethod annotations added: $marionette_annotations"
    
    if [[ -f "$transformed_dir/.marionette-metadata" ]]; then
        echo "  📅 $(cat "$transformed_dir/.marionette-metadata")"
    fi
}


clean_transformed_services() {
    log "Cleaning transformed services..."
    
    for service_dir in services/*-marionette; do
        if [[ -d "$service_dir" ]]; then
            local service_name=$(basename "$service_dir")
            log "Removing transformed service: $service_name"
            rm -rf "$service_dir"
        fi
    done
    
    success "Transformed services cleaned"
}



# Build Docker image
build_docker_image() {
    local service=$1
    local image_name="${service}:latest"
    local transformed_dir="services/${service}-marionette"
    
    if [[ ! -d "$transformed_dir" ]]; then
        error "Transformed directory not found: $transformed_dir"
    fi
    
    log "Checking Docker image for $service..."
    
    if ! docker_image_exists "$image_name" || needs_rebuild "docker-${service}" "$transformed_dir"; then
        log "Building Docker image: $image_name"
        
        cd "$transformed_dir"
        
        # Verify Dockerfile exists
        if [[ ! -f "Dockerfile" ]]; then
            error "Dockerfile not found in $transformed_dir"
        fi
        
        # Verify JAR exists
        if [[ ! -f target/*.jar ]]; then
            error "JAR file not found. Build the service first."
        fi
        
        # Build Docker image
        if ! docker build -t "$image_name" .; then
            error "Docker build failed for $service"
        fi
        
        cd ../..
        
        update_cache "docker-${service}"
        touch "$BUILD_CACHE_DIR/${image_name//\//-}.docker"
        
        success "Docker image $image_name built"
    else
        success "Docker image $image_name up to date"
    fi
}

# Setup Minikube Docker environment
setup_minikube() {
    log "Setting up Minikube Docker environment..."
    
    if ! minikube status > /dev/null 2>&1; then
        error "Minikube is not running. Please start it with: minikube start"
    fi
    
    eval $(minikube docker-env)
    success "Minikube Docker environment configured"
}

# Deploy services to Kubernetes
deploy_to_kubernetes() {
    log "Deploying to Kubernetes..."
    
    cd services/k8s
    
    # Setup ingress
    log "Setting up Ingress Controller..."
    if minikube addons list | grep "ingress" | grep -q "enabled"; then
        success "Ingress addon already enabled"
    else
        log "Enabling Ingress addon..."
        minikube addons enable ingress
        
        log "Waiting for Ingress Controller to be ready..."
        kubectl wait --namespace ingress-nginx \
            --for=condition=ready pod \
            --selector=app.kubernetes.io/component=controller \
            --timeout=300s
    fi
    
    # Apply Kubernetes manifests
    kubectl apply -f namespace.yaml
    kubectl apply -f imagestore-service.yaml
    kubectl apply -f image-processor-service.yaml
    kubectl apply -f ui-service.yaml
    kubectl apply -f outfit-app-ingress.yaml
    
    # Force restart to pick up new images
    kubectl rollout restart deployment/image-processor-service -n outfit-app
    kubectl rollout restart deployment/imagestore-service -n outfit-app
    kubectl rollout restart deployment/ui-service -n outfit-app
    
    # Deploy monitoring
    log "Deploying service monitor..."
    kubectl apply -f outfit-app-servicemonitor.yaml
    
    # Wait for deployments
    log "Waiting for deployments to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/image-processor-service -n outfit-app
    kubectl wait --for=condition=available --timeout=300s deployment/imagestore-service -n outfit-app
    kubectl wait --for=condition=available --timeout=300s deployment/ui-service -n outfit-app
    
    cd ../..
    success "Kubernetes deployment completed"
}

# Show deployment status
show_status() {
    echo
    log "Deployment Status:"
    kubectl get pods -n outfit-app
    echo
    
    log "Service Endpoints:"
    echo "🌐 Application: http://$(minikube ip)/outfit-app"
    echo "📊 Prometheus: kubectl port-forward -n monitoring svc/prometheus 9090:9090"
    echo "🎛️ Grafana: kubectl port-forward -n monitoring svc/grafana 3000:3000"
    echo
    
    log "Quick Commands:"
    echo "📋 View logs: kubectl logs -f deployment/ui-service -n outfit-app"
    echo "🔄 Restart service: kubectl rollout restart deployment/ui-service -n outfit-app"
    echo "🔍 Debug pod: kubectl exec -it deployment/ui-service -n outfit-app -- bash"
}

main() {
    local start_time=$(date +%s)
    
    log "Starting outfit-app deployment with Marionette transformations..."
    
    # Initialize
    init_build_cache
    setup_minikube
    
    # Build common dependencies first
    build_common
    
    # Services to process (must match directory names under services/)
    local services=("image-processor-service" "imagestore-service" "ui-service")
    
    # Phase 1: Apply Marionette transformations
    log "Phase 1: Applying Marionette transformations..."
    for service in "${services[@]}"; do
        apply_marionette_transformation "$service"
    done
    
    # Show transformation summary
    if [[ "$VERBOSE" == "true" ]]; then
        echo
        log "Transformation Summary:"
        for service in "${services[@]}"; do
            show_transformation_diff "$service"
        done
    fi
    
    # Phase 2: Build transformed services
    log "Phase 2: Building transformed services..."
    for service in "${services[@]}"; do
        build_transformed_service "$service"
    done
    
    # Phase 3: Build Docker images
    log "Phase 3: Building Docker images..."
    for service in "${services[@]}"; do
        build_docker_image "$service"
    done
    
    # Show what was built
    echo
    log "Docker images built:"
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}" | \
        grep -E "(REPOSITORY|$(IFS='|'; echo "${services[*]}"))"
    
    # Phase 4: Deploy to Kubernetes
    log "Phase 4: Deploying to Kubernetes..."
    deploy_to_kubernetes
    
    show_status
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    success "Deployment completed in ${duration}s"
}

# Script execution starts here
parse_args "$@"
main
