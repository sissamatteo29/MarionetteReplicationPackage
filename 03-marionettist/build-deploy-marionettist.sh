#!/bin/bash
# deploy.sh

eval $(minikube docker-env)

# Check for --no-frontend flag
SKIP_FRONTEND=false
for arg in "$@"; do
    if [[ $arg == "--no-frontend" ]]; then
        SKIP_FRONTEND=true
        break
    fi
done

# Build frontend only if --no-frontend flag is not passed
if [[ $SKIP_FRONTEND == false ]]; then
    echo "Building frontend"
    cd frontend && npm run build && cp -r build/* ../src/main/resources/static && cd ..
else
    echo "Skipping frontend build (--no-frontend flag detected)"
fi

echo "Building maven project"
mvn clean package

echo "Building Docker image..."
docker build -t marionette-control-plane:latest .

echo "== Printing docker images =="
docker image prune -f
docker images

# Deploy to minikube
kubectl apply -f deploy.yaml
kubectl rollout restart deployment/marionette-control-plane -n outfit-app