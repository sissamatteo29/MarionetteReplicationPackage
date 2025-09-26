
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
build-deploy-marionettist.sh
cd ..