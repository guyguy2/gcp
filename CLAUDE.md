# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Developer Hub Platform - A GCP learning project that combines a developer portfolio, code snippet manager, and learning journal. Built using Java Spring Boot backend on GKE Autopilot, with React frontend hosted on GCS.

**Key architectural decisions:**
- GKE Autopilot (not Standard GKE) for free tier eligibility
- Workload Identity for secure GCP authentication (no JSON keys)
- Multi-stage deployment: K8s YAMLs, then Config Connector, optional Terraform
- Cloud Run comparison deployment for architectural learning
- Free tier optimized: single cluster in us-central1/us-west1/us-east1

## Common Commands

### GCP Setup
```bash
# Set project context
gcloud config set project developer-hub-learning

# Get GKE credentials
gcloud container clusters get-credentials devhub-cluster --region us-central1

# Configure Docker for Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev
```

### Backend Development (Java Spring Boot)
```bash
# Build with Maven
cd backend
mvn clean package

# Build Docker image
docker build -t us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest .

# Push to Artifact Registry
docker push us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest

# Deploy to GKE
kubectl apply -f k8s/
kubectl rollout status deployment/devhub-api

# View logs
kubectl logs -f deployment/devhub-api
```

### Testing
```bash
# Run Java tests
cd backend
mvn test

# Run specific test
mvn test -Dtest=ClassName#methodName
```

### Frontend Development (React)
```bash
cd frontend
npm install
npm start          # Development server
npm run build      # Production build

# Deploy to GCS
gsutil -m rsync -r build gs://[BUCKET-NAME]-devhub-frontend
```

### Kubernetes Operations
```bash
# Check pod status
kubectl get pods
kubectl describe pod <pod-name>

# View services and ingress
kubectl get services
kubectl get ingress

# Apply Config Connector resources
kubectl apply -f infra/

# Check Config Connector status
kubectl get storagebucket -n config-connector
```

## Architecture

### Service Layer
- **Backend**: Java Spring Boot REST API (port 8080)
  - Controllers: PortfolioController, SnippetsController
  - Services: FirestoreService, StorageService
  - Workload Identity integration via serviceAccountName in deployment.yaml
- **Frontend**: React SPA served from GCS bucket
- **Database**: Firestore Native mode with collections: portfolio/, snippets/, learningNotes/
- **Storage**: GCS buckets for frontend (public), configs/uploads (private)

### Security Model
Workload Identity binds Kubernetes Service Account (devhub-ksa) to Google Service Account (devhub-gsa@developer-hub-learning.iam.gserviceaccount.com). Pods authenticate automatically without JSON keys. Required roles: roles/datastore.user, roles/storage.objectAdmin.

### Infrastructure as Code Progression
1. Phase 1-2: Manual kubectl apply of k8s/*.yaml files
2. Phase 5: Config Connector manages GCP resources as K8s objects
3. Optional: Terraform for comparison/production scenarios

### CI/CD
GitHub Actions workflows deploy on push to main:
- backend-deploy.yml: Maven build, Docker build/push, kubectl apply
- frontend-deploy.yml: React build, GCS upload
- Uses Workload Identity Federation for GCP authentication (no service account keys)

## GCS Bucket Structure
```
devhub-storage/
├── configs/          # Application configurations
├── snippets/         # Uploaded code files
├── backups/          # Firestore exports (automated via Cloud Function)
└── assets/           # Images, diagrams
```

## Firestore Collections Schema
- **portfolio/**: {linkId}: title, url, order, category, icon
- **snippets/**: {snippetId}: title, code, language, tags[], createdAt, category, gcsFileUrl, isPublic
- **learningNotes/**: {noteId}: title, content, tags[], date, resources[]

## Cost Monitoring
GKE Autopilot cluster must stay in free-eligible regions (us-central1, us-west1, us-east1). Budget alerts configured at $5 and $10. Monitor with:
```bash
gcloud billing budgets list --billing-account=BILLING_ACCOUNT_ID
```

## Important Constraints
- **Always use GKE Autopilot** commands (create-auto), never Standard GKE
- **Artifact Registry** is used, not deprecated GCR
- **Workload Identity** is mandatory for all GCP API access
- Resource limits in deployment.yaml must stay within free tier: memory 512Mi-1Gi, cpu 250m-500m
