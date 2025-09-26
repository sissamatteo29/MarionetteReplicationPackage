# Tmux Layout Documentation

The `start-ab-test.sh` script now creates a tmux session with 5 panes arranged as follows:

```
+-------------------------+-------------------------+-------------------------+
|                         |                         |                         |
|   MARIONETTE CONTROL    |      UI SERVICE         |    IMAGESTORE SERVICE   |
|       PLANE LOGS        |        LOGS              |         LOGS            |
|       (Pane 0)          |      (Pane 1)           |       (Pane 2)          |
|                         |                         |                         |
+-------------------------+-------------------------+-------------------------+
|                                                   |                         |
|            IMAGE PROCESSOR SERVICE                |     USER SIMULATOR      |
|                    LOGS                           |      (Pane 4)           |
|                 (Pane 3)                          |                         |
|                                                   |                         |
+---------------------------------------------------+-------------------------+
```

## Navigation:
- **Ctrl-b + arrow keys**: Navigate between panes
- **Ctrl-b + d**: Detach from session  
- **Ctrl-b + x**: Kill current pane
- **Ctrl-b + &**: Kill entire session

## Reattaching:
- `tmux attach -t marionette-monitoring`
- `tmux kill-session -t marionette-monitoring` (to clean up)

## What runs in each pane:
1. **Pane 0**: `kubectl logs -f deployments/marionette-control-plane -n outfit-app`
2. **Pane 1**: `kubectl logs -f deployments/ui-service -n outfit-app`  
3. **Pane 2**: `kubectl logs -f deployments/imagestore-service -n outfit-app`
4. **Pane 3**: `kubectl logs -f deployments/image-processor-service -n outfit-app`
5. **Pane 4**: `python3 user_simulator.py --base-url <OUTFIT_APP_URL>` (from 04-user-simulator directory)

## Dual Endpoint Discovery:
The script now discovers **TWO** different endpoints:

### 🎛️ **Marionette Control Plane URL**
- Used for AB testing API calls (`/api/services/start-ab-test`)
- Discovery methods:
  1. `minikube service marionette-control-plane --url`
  2. NodePort access: `http://<minikube-ip>:<nodeport>`
  3. Ingress: `http://<ingress-ip>/marionette`
  4. Fallback: Port-forward to `localhost:8081`

### 🌐 **Outfit App URL** 
- Used by the user simulator to access the UI service
- Discovery methods:
  1. Ingress LoadBalancer: `http://<ingress-ip>/`
  2. Minikube IP: `http://<minikube-ip>/` (most common)
  3. Direct service: `minikube service ui-service --url`
  4. NodePort: `http://<minikube-ip>:<ui-nodeport>`

## Running user simulator manually:
```bash
cd 04-user-simulator
# Connect to outfit app via ingress
python3 user_simulator.py --base-url http://192.168.49.2
# Connect to specific service port
python3 user_simulator.py --base-url http://192.168.49.2:30080 --num-users 3
```

## Scripts Usage:
- `./start-ab-test.sh` - Auto-discovers both endpoints
- `./monitor-logs.sh [OUTFIT_APP_URL]` - Optionally specify outfit app URL

The user simulator is no longer blocking and connects to the correct outfit app endpoint!