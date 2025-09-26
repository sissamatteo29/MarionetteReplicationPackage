#!/bin/bash

# Standalone script to set up tmux monitoring for marionette services
# Usage: ./monitor-logs.sh [OUTFIT_APP_URL]
# Example: ./monitor-logs.sh http://192.168.49.2

echo "Setting up tmux monitoring session for marionette services..."

# Auto-discover outfit app URL if not provided
if [ -z "$1" ]; then
    echo "🔍 Auto-discovering outfit app endpoint..."
    MINIKUBE_IP=$(minikube ip 2>/dev/null)
    
    if [ ! -z "$MINIKUBE_IP" ]; then
        # Try ingress first
        OUTFIT_INGRESS_IP=$(kubectl get ingress outfit-app-ingress -n outfit-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
        if [ ! -z "$OUTFIT_INGRESS_IP" ]; then
            OUTFIT_APP_URL="http://$OUTFIT_INGRESS_IP"
            echo "✓ Found outfit app via ingress: $OUTFIT_APP_URL"
        else
            OUTFIT_APP_URL="http://$MINIKUBE_IP"
            echo "✓ Using minikube IP for outfit app: $OUTFIT_APP_URL"
        fi
    else
        OUTFIT_APP_URL="http://192.168.49.2"
        echo "⚠ Could not get minikube IP, using default: $OUTFIT_APP_URL"
    fi
else
    OUTFIT_APP_URL="$1"
    echo "Using provided outfit app URL: $OUTFIT_APP_URL"
fi

# Check if tmux is installed
if ! command -v tmux &> /dev/null; then
    echo "⚠ tmux is not installed. Install it with: sudo apt install tmux"
    exit 1
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
tmux send-keys -t marionette-monitoring:monitoring.4 "echo \"=== USER SIMULATOR ===\" && cd 04-user-simulator && python3 user_simulator.py --base-url $OUTFIT_APP_URL --num-users 3 --cycle-duration 1800" Enter

# Attach to the session
echo "✓ Tmux monitoring session created with 5 panes (4 for logs + 1 for user simulator)"
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
echo ""
echo "Usage: ./monitor-logs.sh [OUTFIT_APP_URL]"
echo "Example: ./monitor-logs.sh http://192.168.49.2"
echo "Note: If no URL provided, script will auto-discover the outfit app endpoint"

tmux attach-session -t marionette-monitoring