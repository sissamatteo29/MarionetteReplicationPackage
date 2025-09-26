# MARIONETTE: Automated A/B Testing Framework for Microservices

![Marionette Banner](https://img.shields.io/badge/Marionette-A%2FB%20Testing%20Framework-blue?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Kubernetes-orange?style=for-the-badge)

Marionette, a framework enabling fine-grained runtime behavioural configuration in microservice architectures.
It provides a non-intrusive approach to instrument microservices, allowing external control over behavioural decisions without requiring code changes or service restarts, as traditional deployment strategies. The framework is then employed to design an automated A/B/n testing pipeline that compares all possible system-level combinations of behaviours across the microservices composing the application.

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#️-architecture)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [Project Structure](#-project-structure)
- [Usage Guide](#-usage-guide)
- [Scripts Reference](#️-scripts-reference)
- [Results & Visualization](#-results--visualization)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)

## 🎯 Overview

Marionette enables fine-grained instrumentation of microservices to define internally multiple behavioural variants and allow externalised control over them. It also provides an automated A/B/n testing pipeline designed to explore all the system-level combinations of behaviours and compare them. It enables researchers and developers to:

- **Configure multiple behavioural variants** inside a microservice using XML-based configuration.
- **Control the behaviour** of single microservices through a centralised service offering a graphical interface.
- **Deploy and manage A/B/n tests** systematically exploring all system-level combinations of behaviours.
- **Simulate user load** with a customizable script.
- **Monitor real-time metrics** during test execution.
- **Generate comprehensive visualizations** of test results.
- **Compare performance** across different service configurations.

### Components

1. **Outfit App** (`01-outfit-app/`): Target microservice application for instrumentation of behaviuoral variants and A/B/n testing.
2. **Marionette Tool** (`02-marionette-tool/`): Code transformation engine.
3. **Control Plane** (`03-marionettist/`): Centralised control interface and test orchestration.
4. **User Simulator** (`04-user-simulator/`): User load simulation tool.
5. **Result Visualizer** (`05-result-visualiser/`): Automated chart generation from obtained results.
6. **Automation Scripts** (`06-scripts/`): Helper utilities and browser automation.

## 🔧 Prerequisites

### System Requirements

- **Operating System**: Linux, macOS, or Windows with WSL2
- **CPU**: 4+ cores recommended
- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 10GB free space

### Required Software

#### Core Dependencies

```bash
# Kubernetes and Container Runtime
minikube (v1.25+)
kubectl (v1.25+)
docker (v20.10+)

# Development Tools
java (JDK 17+)
maven (v3.8+)
node.js (v16+)
npm (v8+)

# System Tools
python3 (v3.8+)
pip3
curl
jq
git
```

#### Optional but Recommended

```bash
# For enhanced monitoring
helm (v3.10+)

# For development
git
vim/nano (text editor)
```

### Kubernetes Cluster Setup

The framework requires a Kubernetes cluster with specific addons:

#### Minikube Configuration
```bash
# Start minikube with sufficient resources
minikube start --cpus=4 --memory=8192 --disk-size=20g

# Enable required addons
minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable dashboard
```

#### Required Kubernetes Features
- **Ingress Controller**: NGINX ingress (enabled via minikube addon)
- **Metrics Server**: For resource monitoring
- **Persistent Volumes**: For data storage
- **Service Monitoring**: Prometheus operator (auto-installed)

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/sissamatteo29/MarionetteReplicationPackage.git
cd MarionetteReplicationPackage
```

### 2. Verify Prerequisites
```bash
# Check Kubernetes cluster
kubectl cluster-info

# Verify minikube addons
minikube addons list | grep -E "(ingress|metrics-server)"

# Check Java version
java -version

# Check Python version
python3 --version
```

### 3. Prepare the Environment
```bash
# Make scripts executable
chmod +x *.sh
chmod +x 06-scripts/*.sh

# Verify Docker environment (will be configured automatically)
minikube status
```

## 📁 Project Structure

```
marionette-replication-package/
│
├── 01-outfit-app/                   # Target microservice application
│   ├── common/                      # Shared libraries
│   ├── services/                    # Individual microservices
│   │   ├── ui-service/             # Web frontend service
│   │   ├── imagestore-service/     # Image storage service
│   │   ├── image-processor-service/ # Image processing service
│   │   ├── *-marionette/           # Auto-generated A/B test variants
│   │   └── k8s/                    # Kubernetes configurations
│   └── build-deploy-outfit-app.sh  # Service deployment script
│
├── 02-marionette-tool/              # Code transformation engine
│   └── marionette-tool-1.0.jar     # Pre-compiled transformation tool
│
├── 03-marionettist/                 # Control plane application
│   ├── src/                        # Java source code
│   ├── frontend/                   # React web interface
│   ├── build-deploy-marionettist.sh # Control plane deployment
│   └── marionette.xml              # A/B test configurations
│
├── 04-user-simulator/               # Load testing simulator
│   ├── user_simulator.py           # Main simulation script
│   ├── test-images/                # Sample test images
│   └── README.md                   # Simulator documentation
│
├── 05-result-visualiser/            # Results analysis and visualization
│   ├── main.py                     # Visualization generator
│   ├── requirements.txt            # Python dependencies
│   ├── venv/                       # Auto-created virtual environment
│   ├── output/                     # Generated charts and reports
│   └── *.py                        # Visualization modules
│
├── 06-scripts/                      # Automation utilities
│   ├── monitor-logs.sh             # Log monitoring setup
│   ├── open-browser.sh             # Cross-platform browser opener
│   └── setup-visualizer.sh         # Python environment setup
│
├── results/                         # Generated test results
│   └── ab_test_YYYYMMDD_HHMMSS/    # Timestamped result folders
│
├── build-deploy.sh                 # 🔧 Main deployment script
├── start-ab-test.sh                # 🚀 A/B test execution script
├── download-results.sh             # 📊 Results download and visualization
└── README.md                       # This documentation
```

## 📖 Usage Guide

The framework follows a simple three-step workflow:

### Step 1: Deploy the System
```bash
./build-deploy.sh
```

### Step 2: Run A/B Tests
```bash
./start-ab-test.sh
```

### Step 3: Download Results
```bash
./download-results.sh
```

### Detailed Workflow

1. **Initial Deployment**: Sets up all services and opens browser tabs
2. **Test Configuration**: Use the web interface to configure test parameters
3. **Load Testing**: Automated user simulation with real-time monitoring
4. **Results Analysis**: Automatic download and visualization generation

## 🛠️ Scripts Reference

### Core Scripts

#### `build-deploy.sh` 🔧
**Purpose**: Complete system deployment and setup

**What it does**:
- Configures Minikube Docker environment
- Builds all microservice variants using Marionette transformation tool
- Compiles and containerizes services (outfit-app + marionette control plane)
- Deploys services to Kubernetes with ingress configuration
- Sets up monitoring stack (Prometheus + Grafana)
- Discovers service endpoints automatically
- Opens browser tabs for both applications

**Usage**:
```bash
./build-deploy.sh
```

**Prerequisites**:
- Minikube running with ingress addon enabled
- Docker environment available
- Java 17+ and Maven installed

**Output**:
- All services deployed to `outfit-app` namespace
- Two browser tabs opened:
  - Marionette Control Plane: `http://<minikube-ip>:30081`
  - Outfit App UI: `http://<minikube-ip>`

---

#### `start-ab-test.sh` 🚀
**Purpose**: Execute A/B tests with automated monitoring

**What it does**:
- Discovers both Marionette Control Plane and Outfit App endpoints
- Tests connectivity to ensure services are ready
- Creates a tmux session with 5 monitoring panes:
  - **Pane 1**: UI Service logs
  - **Pane 2**: Image Store Service logs  
  - **Pane 3**: Image Processor Service logs
  - **Pane 4**: Marionette Control Plane logs
  - **Pane 5**: User Simulator (load testing)
- Sends HTTP requests to trigger test start and configuration changes
- Provides real-time monitoring during test execution

**Usage**:
```bash
./start-ab-test.sh
```

**Prerequisites**:
- Services deployed via `build-deploy.sh`
- tmux installed
- Both Marionette and Outfit App endpoints accessible

**Session Management**:
```bash
# Attach to existing session
tmux attach-session -t marionette-monitoring

# Kill session manually if needed
tmux kill-session -t marionette-monitoring
```

---

#### `download-results.sh` 📊
**Purpose**: Download test results and generate comprehensive visualizations

**What it does**:
- Automatically discovers Marionette Control Plane endpoint
- Checks system dependencies (Python, jq, curl, etc.)
- Downloads test results from `/api/downloadresult` endpoint
- Creates timestamped results directory
- **Automatically sets up Python virtual environment**
- Installs visualization dependencies (matplotlib, seaborn, numpy)
- Generates 6 different visualization types:
  1. **System Comparison**: Overall performance comparison
  2. **Radar Chart**: Multi-dimensional performance view
  3. **Individual Metrics**: Detailed metric-by-metric analysis
  4. **Service-Level Comparison**: Per-service performance breakdown
  5. **Aggregate Metrics**: Combined performance values
  6. **Configuration Overview**: Visual summary of test configurations
- Opens all generated images automatically
- Provides summary report with file locations

**Usage**:
```bash
# Basic usage (auto-discovers endpoint)
./download-results.sh
```

**Generated Files**:
```
results/ab_test_YYYYMMDD_HHMMSS/
├── test_results.json           # Raw test data
├── individual_metrics.png      # Performance by metric
├── system_comparison.png       # Overall comparison
├── aggregate_metrics.png       # Combined values
├── service_level_comparison.png # Per-service analysis
├── configuration_overview.png  # Configuration summary
└── radar_chart.png            # Multi-dimensional view
```

**Self-Contained Features**:
- Creates and manages its own Python virtual environment
- Automatically installs required dependencies
- Cross-platform image opening (Linux/macOS/Windows)
- Comprehensive error handling and recovery

### Utility Scripts

#### `06-scripts/monitor-logs.sh`
Sets up log monitoring in tmux panes for real-time debugging.

#### `06-scripts/open-browser.sh`
Cross-platform browser opening utility that works on Linux, macOS, and Windows.

#### `06-scripts/setup-visualizer.sh`
Manual Python environment setup script (optional, as `download-results.sh` handles this automatically).

### Service-Specific Scripts

#### `01-outfit-app/build-deploy-outfit-app.sh`
Builds and deploys the target microservice application with A/B test variants.

#### `03-marionettist/build-deploy-marionettist.sh`
Builds and deploys the Marionette control plane with React frontend.

## 📈 Results & Visualization

### Visualization Types

The framework generates six comprehensive visualization types:

1. **System Comparison** (`system_comparison.png`)
   - Bar charts comparing overall system performance
   - Shows all configurations side-by-side
   - Highlights best and worst performing configurations

2. **Radar Chart** (`radar_chart.png`)
   - Multi-dimensional performance comparison
   - All metrics normalized on same scale
   - Easy identification of performance trade-offs

3. **Individual Metrics** (`individual_metrics.png`)
   - Detailed analysis of each performance metric
   - Statistical summaries and rankings
   - Best/worst configuration identification

4. **Service-Level Comparison** (`service_level_comparison.png`)
   - Per-service performance breakdown
   - Identifies which services benefit from specific configurations
   - Tabular and visual representation

5. **Aggregate Metrics** (`aggregate_metrics.png`)
   - Combined performance values across all services
   - Shows system-wide impact of configuration changes
   - Clear ranking of configurations

6. **Configuration Overview** (`configuration_overview.png`)
   - Visual summary of what each configuration changes
   - Maps configuration names to actual service behaviors
   - Helps understand the relationship between configs and results

### Metrics Collected

- **Response Time**: P95 response time in seconds
- **Memory Usage**: JVM heap memory consumption in bytes
- **CPU Usage**: Process CPU utilization (0-1 ratio)

### Results Analysis

Each result set includes:
- **Raw JSON data**: Complete test results with timestamps
- **Performance rankings**: Configurations ranked by composite performance
- **Statistical analysis**: Best/worst values with performance deltas
- **Service-level insights**: Per-service performance breakdown

## 🔍 Troubleshooting

### Common Issues

#### Minikube Issues
```bash
# Minikube not running
minikube start --cpus=4 --memory=8192

# Ingress not working
minikube addons enable ingress
kubectl get pods -n ingress-nginx

# Check ingress status
kubectl get ingress -n outfit-app
```

#### Service Discovery Issues
```bash
# Check service status
kubectl get services -n outfit-app
kubectl get pods -n outfit-app

# Check ingress configuration
kubectl describe ingress outfit-app-ingress -n outfit-app

# Manual endpoint testing
curl -f http://$(minikube ip):30081/actuator/health
curl -f http://$(minikube ip)
```

#### Build Issues
```bash
# Java version issues
java -version  # Should be 17+
export JAVA_HOME=/path/to/java17

# Maven issues
mvn -version
which mvn

# Docker environment
eval $(minikube docker-env)
docker images
```

#### Python Environment Issues
```bash
# Python dependencies
python3 --version  # Should be 3.8+
pip3 --version

# Manual environment setup
cd 05-result-visualiser
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```