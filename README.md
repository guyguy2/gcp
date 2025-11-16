# Developer Hub Platform

A GCP learning project that combines a developer portfolio, code snippet manager, and learning journal. Built using Java Spring Boot backend on GKE Autopilot, with React frontend hosted on GCS.

## Project Structure

This project follows a **deployment-focused organization** for better DevOps practices:

```
/
├── services/           # Application services
│   ├── api/           # Backend service (Java Spring Boot)
│   │   ├── src/      # Java source code
│   │   ├── pom.xml   # Maven dependencies
│   │   └── Dockerfile
│   └── web/           # Frontend service (React)
│       ├── src/      # React components
│       └── public/   # Static assets
│
├── deploy/            # Deployment configurations
│   ├── kubernetes/   # K8s manifests (deployment, service, ingress)
│   ├── gcp/         # GCP-specific configs (Config Connector)
│   └── ci-cd/       # GitHub Actions workflows (reference)
│
├── shared/           # Shared resources
│   ├── configs/     # Shared configurations
│   └── docs/        # Documentation
│       ├── PLAN.md
│       ├── QUICKSTART.md
│       └── progress.md
│
├── tools/           # Utilities and scripts
│   └── setup-gcp.sh
│
└── .github/workflows/  # GitHub Actions (actual)
```

## Quick Start

### Prerequisites
- GCP account with billing enabled
- `gcloud` CLI installed
- `kubectl` installed
- Java 17+ and Maven
- Node.js 18+

### Initial Setup

1. **Run the setup script:**
   ```bash
   ./tools/setup-gcp.sh
   ```

2. **Build and deploy backend:**
   ```bash
   cd services/api
   mvn clean package
   docker build -t us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest .
   docker push us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest
   kubectl apply -f ../../deploy/kubernetes/
   ```

3. **Build and deploy frontend:**
   ```bash
   cd services/web
   npm install
   npm run build
   gsutil -m rsync -r build gs://[BUCKET-NAME]-devhub-frontend
   ```

## Documentation

- **[CLAUDE.md](CLAUDE.md)** - Project guidance for Claude Code
- **[PLAN.md](shared/docs/PLAN.md)** - Detailed implementation plan
- **[QUICKSTART.md](shared/docs/QUICKSTART.md)** - Quick start guide
- **[progress.md](shared/docs/progress.md)** - Development progress tracking

## Architecture

### Services
- **API Service** (`services/api/`): Java Spring Boot REST API
  - Firestore for data persistence
  - GCS for file storage
  - Workload Identity for GCP auth

- **Web Service** (`services/web/`): React SPA
  - Hosted on GCS
  - Calls API service

### Deployment
- **GKE Autopilot**: Managed Kubernetes cluster (free tier eligible)
- **Workload Identity**: Secure GCP authentication without JSON keys
- **GitHub Actions**: Automated CI/CD pipelines

### Key Features
- Portfolio link management
- Code snippet repository
- Learning journal
- GCS file uploads
- Firestore data persistence

## Development

### Backend Development
```bash
cd services/api
mvn test                    # Run tests
mvn clean package          # Build JAR
kubectl logs -f deployment/devhub-api  # View logs
```

### Frontend Development
```bash
cd services/web
npm start                  # Dev server (localhost:3000)
npm run build             # Production build
```

### Kubernetes Operations
```bash
kubectl get pods          # Check pod status
kubectl get services      # View services
kubectl get ingress       # Check ingress
kubectl apply -f deploy/kubernetes/  # Deploy all K8s resources
```

## CI/CD

GitHub Actions workflows automatically deploy on push to `main`:

- **Backend Pipeline** (`.github/workflows/backend-deploy.yml`)
  - Builds with Maven
  - Creates Docker image
  - Pushes to Artifact Registry
  - Deploys to GKE

- **Frontend Pipeline** (`.github/workflows/frontend-deploy.yml`)
  - Builds React app
  - Uploads to GCS bucket
  - Configures caching headers

## Cost Optimization

- GKE Autopilot in free tier regions (us-central1, us-west1, us-east1)
- Resource limits: 512Mi-1Gi memory, 250m-500m CPU
- Budget alerts at $5 and $10
- Artifact Registry (not deprecated GCR)

## License

This is a learning project for educational purposes.
