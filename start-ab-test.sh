
#!/bin/bash

# Parse command line arguments
DURATION_SECONDS=${1:-28800}  # Default to 8 hours (28800 seconds) if no argument provided

# Validate the duration parameter
if ! [[ "$DURATION_SECONDS" =~ ^[0-9]+$ ]]; then
    echo "Error: Duration must be a positive integer (seconds)"
    echo "Usage: $0 [duration_in_seconds]"
    echo "Example: $0 3600  # Run test for 1 hour"
    echo "Default: 28800 seconds (8 hours)"
    exit 1
fi

echo "🎯 Test duration set to: $DURATION_SECONDS seconds ($(($DURATION_SECONDS / 3600)) hours, $(($DURATION_SECONDS % 3600 / 60)) minutes)"

# Trap to handle script interruption
cleanup_on_exit() {
    echo "Script interrupted, cleaning up..."
    
    if [ ! -z "$PORT_FORWARD_PID" ] && kill -0 $PORT_FORWARD_PID 2>/dev/null; then
        echo "Stopping port-forward..."
        kill $PORT_FORWARD_PID 2>/dev/null || true
    fi
    
    # Kill any background curl processes
    pkill -f "curl.*start-ab-test" 2>/dev/null || true
    
    # Kill tmux session if it exists (this will also stop the user simulator)
    tmux kill-session -t marionette-monitoring 2>/dev/null || true
    
    exit 1
}

trap cleanup_on_exit SIGINT SIGTERM

# Get the minikube IP
MINIKUBE_IP=$(minikube ip)
echo "Minikube IP: $MINIKUBE_IP"

echo "🔍 Discovering service endpoints..."

# === DISCOVER MARIONETTE CONTROL PLANE URL (for AB test API) ===
echo "Finding Marionette Control Plane endpoint..."

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

# === DISCOVER OUTFIT APP URL (for user simulator) ===
echo "Finding Outfit App endpoint..."

# Method 1: Try to get outfit app ingress
OUTFIT_APP_URL=""

# Check if outfit-app ingress exists and has an IP
OUTFIT_INGRESS_IP=$(kubectl get ingress outfit-app-ingress -n outfit-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
if [ ! -z "$OUTFIT_INGRESS_IP" ]; then
    OUTFIT_APP_URL="http://$OUTFIT_INGRESS_IP"
    echo "✓ Found outfit app via ingress LoadBalancer IP: $OUTFIT_APP_URL"
else
    # Method 2: Try minikube IP with ingress (most common for minikube)
    OUTFIT_APP_URL="http://$MINIKUBE_IP"
    echo "✓ Using minikube IP for outfit app ingress: $OUTFIT_APP_URL"
fi

# Method 3: Fallback to direct service access if ingress fails
if [ -z "$OUTFIT_APP_URL" ]; then
    OUTFIT_SERVICE_URL=$(minikube service ui-service --namespace=outfit-app --url 2>/dev/null)
    if [ ! -z "$OUTFIT_SERVICE_URL" ]; then
        OUTFIT_APP_URL="$OUTFIT_SERVICE_URL"
        echo "✓ Using direct ui-service access: $OUTFIT_APP_URL"
    else
        # Last resort: try NodePort
        UI_NODE_PORT=$(kubectl get service ui-service -n outfit-app -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null)
        if [ ! -z "$UI_NODE_PORT" ]; then
            OUTFIT_APP_URL="http://$MINIKUBE_IP:$UI_NODE_PORT"
            echo "✓ Using ui-service NodePort: $OUTFIT_APP_URL"
        fi
    fi
fi

# === FINAL FALLBACK FOR MARIONETTE CONTROL PLANE ===
if [ -z "$MARIONETTE_URL" ] || [ "$MARIONETTE_URL" = "" ]; then
    echo "All methods failed for marionette control plane. Using port-forward as fallback..."
    kubectl port-forward svc/marionette-control-plane 8081:8080 -n outfit-app &
    PORT_FORWARD_PID=$!
    sleep 3
    MARIONETTE_URL="http://localhost:8081"
fi

# === FINAL FALLBACK FOR OUTFIT APP ===
if [ -z "$OUTFIT_APP_URL" ] || [ "$OUTFIT_APP_URL" = "" ]; then
    echo "⚠ Warning: Could not determine outfit app URL, using default"
    OUTFIT_APP_URL="http://$MINIKUBE_IP"
fi

echo "📋 DISCOVERED ENDPOINTS:"
echo "  🎛️  Marionette Control Plane: $MARIONETTE_URL"
echo "  🌐 Outfit App (User Simulator): $OUTFIT_APP_URL"

# Test connections before proceeding
echo "🧪 Testing connections..."

echo "Testing marionette control plane connection..."
if curl -f -s --connect-timeout 10 "$MARIONETTE_URL/actuator/health" > /dev/null 2>&1; then
    echo "✓ Marionette control plane connection successful"
elif curl -f -s --connect-timeout 10 "$MARIONETTE_URL/" > /dev/null 2>&1; then
    echo "✓ Marionette control plane is responding"
else
    echo "⚠ Warning: Could not verify marionette control plane connection to $MARIONETTE_URL"
fi

echo "Testing outfit app connection..."
if curl -f -s --connect-timeout 10 "$OUTFIT_APP_URL/" > /dev/null 2>&1; then
    echo "✓ Outfit app connection successful"
else
    echo "⚠ Warning: Could not verify outfit app connection to $OUTFIT_APP_URL"
fi

echo "Proceeding with discovered endpoints..."

# Send the web request to start the test (fire-and-forget)
echo "Starting AB test for $DURATION_SECONDS seconds..."

# Create a fire-and-forget curl request with very short timeout for initial response
echo "Sending AB test start request (fire-and-forget mode)..."

# Use nohup and background execution with timeout to prevent hanging
(
    timeout 10 curl -X POST "$MARIONETTE_URL/api/services/start-ab-test?durationSeconds=$DURATION_SECONDS" \
        --connect-timeout 5 \
        --max-time 10 \
        --silent \
        --show-error \
        > /tmp/ab_test_response.log 2>&1
    
    # Log the result but don't block the main script
    if [ $? -eq 0 ]; then
        echo "[$(date)] ✓ AB test request sent successfully" >> /tmp/ab_test_response.log
    elif [ $? -eq 124 ]; then
        echo "[$(date)] ✓ AB test request sent (timed out waiting for response - this is normal)" >> /tmp/ab_test_response.log
    else
        echo "[$(date)] ⚠ AB test request may have failed" >> /tmp/ab_test_response.log
    fi
) &

# Don't wait for the request, continue immediately
echo "✓ AB test request sent in background"
echo "Check /tmp/ab_test_response.log for request status"

# Setup tmux session for monitoring and user simulation
echo ""
echo "Setting up tmux monitoring and simulation session..."

# Check if tmux is installed
if ! command -v tmux &> /dev/null; then
    echo "⚠ tmux is not installed. Install it with: sudo apt install tmux"
    echo "Running user simulator in foreground instead..."
    cd 04-user-simulator
    CYCLE_DURATION=$((DURATION_SECONDS / 16))
    python3 user_simulator.py --base-url "$OUTFIT_APP_URL" --num-users 3 --cycle-duration $CYCLE_DURATION
    exit 0
fi

# Kill existing session if it exists
tmux kill-session -t marionette-monitoring 2>/dev/null || true

# Create new tmux session with 5 panes (4 for logs + 1 for user simulator)
tmux new-session -d -s marionette-monitoring -n monitoring

# Create a layout with 5 panes: 3 in top row, 2 in bottom row
# First split horizontally to create top and bottom sections
tmux split-window -v -t marionette-monitoring:monitoring

# Split top pane into 3 panes
tmux split-window -h -t marionette-monitoring:monitoring.0
tmux split-window -h -t marionette-monitoring:monitoring.1

# Split bottom pane into 2 panes  
tmux split-window -h -t marionette-monitoring:monitoring.3

# Give tmux a moment to set up the panes
sleep 1

# Now we have 5 panes arranged as:
# +------+------+------+
# |  0   |  1   |  2   |
# +------+------+------+
# |      3      |  4   |
# +-------------+------+

# Pane 0 (top-left): marionette-control-plane logs
tmux send-keys -t marionette-monitoring:monitoring.0 'echo "=== MARIONETTE CONTROL PLANE LOGS ===" && kubectl logs -f deployments/marionette-control-plane -n outfit-app' Enter

# Pane 1 (top-center): ui-service logs
tmux send-keys -t marionette-monitoring:monitoring.1 'echo "=== UI SERVICE LOGS ===" && kubectl logs -f deployments/ui-service -n outfit-app' Enter

# Pane 2 (top-right): imagestore-service logs
tmux send-keys -t marionette-monitoring:monitoring.2 'echo "=== IMAGESTORE SERVICE LOGS ===" && kubectl logs -f deployments/imagestore-service -n outfit-app' Enter

# Pane 3 (bottom-left): image-processor-service logs
tmux send-keys -t marionette-monitoring:monitoring.3 'echo "=== IMAGE PROCESSOR SERVICE LOGS ===" && kubectl logs -f deployments/image-processor-service -n outfit-app' Enter

# Pane 4 (bottom-right): user simulator
CYCLE_DURATION=$((DURATION_SECONDS / 16))
tmux send-keys -t marionette-monitoring:monitoring.4 "echo \"=== USER SIMULATOR ===\" && cd 04-user-simulator && python3 user_simulator.py --base-url $OUTFIT_APP_URL --num-users 3 --cycle-duration $CYCLE_DURATION" Enter

# Cleanup: kill port-forward if it was used
if [ ! -z "$PORT_FORWARD_PID" ]; then
    echo "Cleaning up port-forward process..."
    kill $PORT_FORWARD_PID 2>/dev/null || true
fi

echo "Script completed. Check /tmp/ab_test_response.log for AB test request status."

# Attach to the session
echo "✓ Tmux session created with 5 panes (4 for logs + 1 for user simulator)"
echo "Attaching to tmux session 'marionette-monitoring'..."
echo ""
echo "Tmux navigation:"
echo "  - Ctrl-b d         : detach from session"
echo "  - Ctrl-b arrow     : navigate between panes"
echo "  - Ctrl-b c         : create new window"
echo "  - Ctrl-b &         : kill current window"
echo "  - Ctrl-b x         : kill current pane"
echo ""
echo "To reattach later: tmux attach -t marionette-monitoring"
echo "To kill session:   tmux kill-session -t marionette-monitoring"

tmux attach-session -t marionette-monitoring 