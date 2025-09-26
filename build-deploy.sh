
log "Setting up Minikube Docker environment..."

if ! minikube status > /dev/null 2>&1; then
    error "Minikube is not running. Please start it with: minikube start"
fi

eval $(minikube docker-env)
success "Minikube Docker environment configured"

cd 01-outfit-app
./build-deploy-outfit-app.sh
cd ..

cd 03-marionettist
./build-deploy-marionettist.sh
cd ..

sleep 5

echo "🔍 Discovering Marionette Control Plane endpoint..."

# Get the minikube IP
MINIKUBE_IP=$(minikube ip)
echo "Minikube IP: $MINIKUBE_IP"

# Discover Marionette Control Plane URL using same logic as start-ab-test.sh
MARIONETTE_URL=""

# Method 1: Try to use minikube service command
MARIONETTE_URL=$(minikube service marionette-control-plane --namespace=outfit-app --url 2>/dev/null)

# Method 2: If that fails, try direct NodePort access
if [ -z "$MARIONETTE_URL" ] || [ "$MARIONETTE_URL" = "" ]; then
    NODE_PORT=$(kubectl get service marionette-control-plane -n outfit-app -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null)
    if [ ! -z "$NODE_PORT" ]; then
        MARIONETTE_URL="http://$MINIKUBE_IP:$NODE_PORT"
    fi
fi

# Method 3: Try ingress with nginx if available
if [ -z "$MARIONETTE_URL" ] || [ "$MARIONETTE_URL" = "" ]; then
    # Check if nginx ingress is available
    INGRESS_IP=$(kubectl get ingress marionette-control-plane-ingress -n outfit-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
    if [ ! -z "$INGRESS_IP" ]; then
        MARIONETTE_URL="http://$INGRESS_IP/marionette"
    else
        # Fallback to minikube IP with ingress path
        MARIONETTE_URL="http://$MINIKUBE_IP/marionette"
    fi
fi

# Method 4: Final fallback - construct URL manually
if [ -z "$MARIONETTE_URL" ] || [ "$MARIONETTE_URL" = "" ]; then
    echo "⚠ Warning: Could not discover marionette control plane automatically"
    MARIONETTE_URL="http://$MINIKUBE_IP:30081"  # Assume default NodePort
    echo "Using fallback URL: $MARIONETTE_URL"
else
    echo "✓ Discovered Marionette Control Plane: $MARIONETTE_URL"
fi

# Test the connection briefly
echo "🧪 Testing connection to marionette control plane..."
if curl -f -s --connect-timeout 5 "$MARIONETTE_URL/actuator/health" > /dev/null 2>&1; then
    echo "✅ Marionette control plane is responding"
elif curl -f -s --connect-timeout 5 "$MARIONETTE_URL/" > /dev/null 2>&1; then
    echo "✅ Marionette control plane endpoint is accessible"
else
    echo "⚠ Warning: Could not verify connection to $MARIONETTE_URL"
    echo "Browser will still be opened - the service might be starting up"
fi

echo "🔍 Discovering Outfit App (UI Service) endpoint..."

# Discover Outfit App URL - UI service is ClusterIP so it's only accessible via ingress
OUTFIT_APP_URL=""

# Method 1: Check if ingress is working (preferred for UI service)
if curl -f -s --connect-timeout 3 "http://$MINIKUBE_IP" > /dev/null 2>&1; then
    OUTFIT_APP_URL="http://$MINIKUBE_IP"
    echo "✓ Ingress is working, using minikube IP"
else
    echo "⚠ Ingress not responding, checking alternatives..."
fi

# Method 2: Try to get from ingress status if available
if [ -z "$OUTFIT_APP_URL" ] || [ "$OUTFIT_APP_URL" = "" ]; then
    INGRESS_IP=$(kubectl get ingress outfit-app-ingress -n outfit-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
    if [ ! -z "$INGRESS_IP" ] && [ "$INGRESS_IP" != "" ]; then
        OUTFIT_APP_URL="http://$INGRESS_IP"
        echo "✓ Found ingress IP: $INGRESS_IP"
    fi
fi

# Method 3: Try port-forward as fallback (for development)
if [ -z "$OUTFIT_APP_URL" ] || [ "$OUTFIT_APP_URL" = "" ]; then
    echo "⚠ No ingress available, using port-forward approach"
    OUTFIT_APP_URL="http://localhost:8080"
    echo "🔧 You may need to run: kubectl port-forward svc/ui-service 8080:8080 -n outfit-app"
fi

# Final fallback to minikube IP (most common case)
if [ -z "$OUTFIT_APP_URL" ] || [ "$OUTFIT_APP_URL" = "" ]; then
    echo "⚠ Warning: Using fallback to minikube IP"
    OUTFIT_APP_URL="http://$MINIKUBE_IP"
fi

echo "✓ Discovered Outfit App: $OUTFIT_APP_URL"

# Test the outfit app connection
echo "🧪 Testing connection to outfit app..."
if curl -f -s --connect-timeout 5 "$OUTFIT_APP_URL" > /dev/null 2>&1; then
    echo "✅ Outfit app is responding"
elif curl -f -s --connect-timeout 5 "$OUTFIT_APP_URL/health" > /dev/null 2>&1; then
    echo "✅ Outfit app endpoint is accessible"
else
    echo "⚠ Warning: Could not verify connection to $OUTFIT_APP_URL"
    echo "Browser will still be opened - the service might be starting up"
fi

echo "🌐 Opening browser tabs..."
echo "  📊 Marionette Control Plane: $MARIONETTE_URL"
echo "  🎨 Outfit App: $OUTFIT_APP_URL"

# Ensure we call the script from the correct directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Open both URLs in separate tabs
"$SCRIPT_DIR/06-scripts/open-browser.sh" "$MARIONETTE_URL"
sleep 1  # Small delay to ensure first tab opens properly
"$SCRIPT_DIR/06-scripts/open-browser.sh" "$OUTFIT_APP_URL" 