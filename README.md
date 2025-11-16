# Developer Hub Platform

A comprehensive GCP learning project combining a developer portfolio, code snippet manager, and learning journal. Built with Java Spring Boot on GKE Autopilot, React frontend on GCS, and optimized for Google Cloud Platform free tier.

## Project Overview

**What it does:**
- Portfolio management for showcasing projects and links
- Code snippet manager with tagging and categorization
- Learning journal for documenting your development journey
- Secure cloud-native architecture using GCP best practices

**Why this project:**
- Learn GKE Autopilot and Kubernetes
- Master Workload Identity for secure GCP authentication
- Practice Infrastructure as Code (K8s YAMLs, Config Connector, Terraform)
- Build a production-grade CI/CD pipeline
- Actually useful for daily development work

## Technology Stack

### Backend
- **Framework:** Java Spring Boot 3.2
- **Database:** Google Cloud Firestore (Native mode)
- **Storage:** Google Cloud Storage
- **Container:** Docker (multi-stage build)
- **Orchestration:** GKE Autopilot

### Frontend
- **Framework:** React 18
- **Hosting:** Google Cloud Storage (static site)
- **API Client:** Axios

### Infrastructure
- **Compute:** GKE Autopilot (free tier eligible)
- **Container Registry:** Artifact Registry
- **CI/CD:** GitHub Actions with Workload Identity Federation
- **Security:** Workload Identity (no JSON keys)
- **Monitoring:** Cloud Logging & Cloud Monitoring

## Architecture

```
┌─────────────────┐
│  GitHub Actions │  CI/CD Pipeline
└────────┬────────┘
         │
         v
┌─────────────────┐     ┌──────────────────┐
│ Artifact        │────>│ GKE Autopilot    │
│ Registry        │     │  ┌────────────┐  │
└─────────────────┘     │  │ Backend    │  │
                        │  │ Spring Boot│  │
                        │  └─────┬──────┘  │
                        └────────┼─────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    v                         v
            ┌───────────────┐        ┌──────────────┐
            │   Firestore   │        │     GCS      │
            │   (Database)  │        │   (Storage)  │
            └───────────────┘        └──────────────┘
                                             │
                    ┌────────────────────────┘
                    │
                    v
            ┌──────────────┐
            │   Frontend   │
            │    (React)   │
            └──────────────┘
```

## Project Structure

```
gcp/
├── backend/
│   ├── src/main/java/com/devhub/
│   │   ├── controller/           # REST API controllers
│   │   │   ├── PortfolioController.java
│   │   │   └── SnippetsController.java
│   │   ├── service/              # Business logic
│   │   │   ├── PortfolioService.java
│   │   │   ├── SnippetService.java
│   │   │   └── StorageService.java
│   │   ├── model/                # Data models
│   │   │   ├── PortfolioLink.java
│   │   │   ├── CodeSnippet.java
│   │   │   └── LearningNote.java
│   │   └── DevHubApplication.java
│   ├── k8s/                      # Kubernetes manifests
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── ingress.yaml
│   │   └── workload-identity.yaml
│   ├── Dockerfile                # Multi-stage Docker build
│   └── pom.xml                   # Maven dependencies
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Home.jsx
│   │   │   ├── Portfolio.jsx
│   │   │   └── SnippetManager.jsx
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
│
├── .github/workflows/            # CI/CD pipelines
│   ├── backend-deploy.yml
│   └── frontend-deploy.yml
│
├── PLAN.md                       # Detailed implementation plan
├── CLAUDE.md                     # AI assistant instructions
├── progress.md                   # Implementation progress tracker
└── README.md                     # This file
```

## Getting Started

### Prerequisites

1. **Google Cloud Platform account** with billing enabled
2. **gcloud CLI** installed and configured
3. **kubectl** installed
4. **Docker** installed (for local development)
5. **Java 17** and **Maven** (for backend development)
6. **Node.js 18+** (for frontend development)

### Phase 1: GCP Infrastructure Setup

#### 1.1 Create GCP Project

```bash
# Create project
gcloud projects create developer-hub-learning
gcloud config set project developer-hub-learning

# Link billing account (required)
gcloud billing accounts list
gcloud billing projects link developer-hub-learning \
  --billing-account=BILLING_ACCOUNT_ID

# Enable required APIs
gcloud services enable \
  container.googleapis.com \
  artifactregistry.googleapis.com \
  firestore.googleapis.com \
  storage-api.googleapis.com \
  cloudbuild.googleapis.com
```

#### 1.2 Create GCS Buckets

```bash
# Replace [YOUR-NAME] with something unique
BUCKET_PREFIX="your-name"

# Frontend bucket (public)
gsutil mb -l us-central1 gs://${BUCKET_PREFIX}-devhub-frontend
gsutil iam ch allUsers:objectViewer gs://${BUCKET_PREFIX}-devhub-frontend

# Storage bucket (private)
gsutil mb -l us-central1 gs://${BUCKET_PREFIX}-devhub-storage
```

#### 1.3 Setup Firestore

```bash
# Create Firestore database in Native mode
gcloud firestore databases create --location=us-central
```

#### 1.4 Create GKE Autopilot Cluster (CRITICAL)

```bash
# MUST use create-auto for free tier!
gcloud container clusters create-auto devhub-cluster \
    --region us-central1 \
    --project developer-hub-learning

# Get credentials
gcloud container clusters get-credentials devhub-cluster \
    --region us-central1
```

**Why Autopilot?**
- Standard GKE charges for control plane + nodes
- Autopilot = free cluster management + some free compute
- Perfect for learning and low-traffic apps

#### 1.5 Create Artifact Registry

```bash
gcloud artifacts repositories create devhub-repo \
    --repository-format=docker \
    --location=us-central1 \
    --description="Developer Hub containers"
```

### Phase 2: Setup Workload Identity

Workload Identity allows pods to authenticate as Google Service Accounts without JSON keys.

```bash
# 1. Create Google Service Account
gcloud iam service-accounts create devhub-gsa \
    --display-name="DevHub GKE Service Account"

# 2. Grant permissions to GSA
gcloud projects add-iam-policy-binding developer-hub-learning \
    --member="serviceAccount:devhub-gsa@developer-hub-learning.iam.gserviceaccount.com" \
    --role="roles/datastore.user"

gcloud projects add-iam-policy-binding developer-hub-learning \
    --member="serviceAccount:devhub-gsa@developer-hub-learning.iam.gserviceaccount.com" \
    --role="roles/storage.objectAdmin"

# 3. Create Kubernetes Service Account
kubectl create serviceaccount devhub-ksa -n default

# 4. Bind KSA to GSA (Workload Identity magic)
gcloud iam service-accounts add-iam-policy-binding \
    devhub-gsa@developer-hub-learning.iam.gserviceaccount.com \
    --role roles/iam.workloadIdentityUser \
    --member "serviceAccount:developer-hub-learning.svc.id.goog[default/devhub-ksa]"

# 5. Annotate KSA
kubectl annotate serviceaccount devhub-ksa \
    iam.gke.io/gcp-service-account=devhub-gsa@developer-hub-learning.iam.gserviceaccount.com
```

### Phase 3: Local Development

#### Backend (Java Spring Boot)

```bash
cd backend

# Build with Maven
mvn clean package

# Run locally (requires GCP credentials)
export GCP_PROJECT_ID=developer-hub-learning
java -jar target/devhub-api.jar

# Or use Maven
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

**API Endpoints:**
- `GET /api/portfolio` - Get all portfolio links
- `POST /api/portfolio` - Create new portfolio link
- `GET /api/snippets` - Get all code snippets
- `POST /api/snippets` - Create new snippet
- `GET /api/snippets/public` - Get public snippets only

#### Frontend (React)

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

The app will be available at `http://localhost:3000`

### Phase 4: Deploy to GKE

#### Build and Push Backend

```bash
cd backend

# Configure Docker for Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev

# Build Docker image
docker build -t us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest .

# Push to Artifact Registry
docker push us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest
```

#### Update Kubernetes Manifests

Edit `backend/k8s/deployment.yaml` and `backend/k8s/workload-identity.yaml`:
- Replace `PROJECT_ID` with `developer-hub-learning`

#### Deploy to Kubernetes

```bash
cd backend

# Apply Kubernetes manifests
kubectl apply -f k8s/workload-identity.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml

# Check deployment status
kubectl rollout status deployment/devhub-api

# Get ingress IP (may take a few minutes)
kubectl get ingress devhub-ingress
```

#### Deploy Frontend to GCS

```bash
cd frontend

# Build React app
npm run build

# Upload to GCS
gsutil -m rsync -r build gs://${BUCKET_PREFIX}-devhub-frontend

# Access at:
# http://storage.googleapis.com/${BUCKET_PREFIX}-devhub-frontend/index.html
```

### Phase 5: Setup CI/CD with GitHub Actions

#### Configure GitHub Secrets

Required secrets:
- `GCP_PROJECT_ID`: developer-hub-learning
- `GKE_CLUSTER`: devhub-cluster
- `GKE_REGION`: us-central1
- `ARTIFACT_REGISTRY`: us-central1-docker.pkg.dev
- `FRONTEND_BUCKET`: your-bucket-name-devhub-frontend
- `API_URL`: http://YOUR_INGRESS_IP/api

#### Setup Workload Identity Federation for GitHub

Follow: https://github.com/google-github-actions/auth#setup

This allows GitHub Actions to authenticate to GCP without service account keys.

## Cost Monitoring

**CRITICAL:** Set up budget alerts to avoid unexpected charges!

```bash
# Set budget alerts
gcloud billing budgets create \
    --billing-account=BILLING_ACCOUNT_ID \
    --display-name="Developer Hub Budget" \
    --budget-amount=10USD \
    --threshold-rule=percent=50 \
    --threshold-rule=percent=90 \
    --threshold-rule=percent=100
```

**Free Tier Limits:**
- GKE Autopilot: 1 cluster free (management)
- GCS: 5GB-months
- Firestore: 1 GiB storage + 50K reads, 20K writes, 20K deletes/day
- Cloud Build: 120 build-minutes/day

**Resource Limits in Deployment:**
- Memory: 512Mi-1Gi
- CPU: 250m-500m

## API Documentation

### Portfolio Endpoints

- **GET** `/api/portfolio` - Get all portfolio links
- **GET** `/api/portfolio/{id}` - Get specific link
- **GET** `/api/portfolio/category/{category}` - Get links by category
- **POST** `/api/portfolio` - Create new link
- **PUT** `/api/portfolio/{id}` - Update link
- **DELETE** `/api/portfolio/{id}` - Delete link

### Snippet Endpoints

- **GET** `/api/snippets` - Get all snippets
- **GET** `/api/snippets/{id}` - Get specific snippet
- **GET** `/api/snippets/public` - Get public snippets only
- **GET** `/api/snippets/language/{language}` - Get snippets by language
- **GET** `/api/snippets/tag/{tag}` - Get snippets by tag
- **POST** `/api/snippets` - Create new snippet
- **POST** `/api/snippets/upload` - Create snippet with file upload
- **PUT** `/api/snippets/{id}` - Update snippet
- **DELETE** `/api/snippets/{id}` - Delete snippet

## Testing

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test
```

## Troubleshooting

### Backend not starting
- Check logs: `kubectl logs -f deployment/devhub-api`
- Verify Workload Identity: `kubectl describe pod <pod-name>`
- Check Firestore and GCS permissions

### Ingress not getting external IP
- Wait 5-10 minutes for GCP to provision
- Check: `kubectl describe ingress devhub-ingress`

### API calls failing from frontend
- Update `REACT_APP_API_URL` environment variable
- Check CORS configuration in backend
- Verify ingress IP is correct

## Next Steps

1. **Phase 5:** Implement Config Connector for GitOps infrastructure management
2. **Phase 6:** Add Cloud Functions for automated backups
3. **Phase 7:** Deploy to Cloud Run for comparison
4. **Phase 8:** Enhanced frontend with authentication

See [PLAN.md](PLAN.md) for detailed roadmap.

## Learning Resources

- [GKE Autopilot Documentation](https://cloud.google.com/kubernetes-engine/docs/concepts/autopilot-overview)
- [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)
- [Spring Cloud GCP](https://spring.io/projects/spring-cloud-gcp)
- [GCP Free Tier](https://cloud.google.com/free)

## License

This is a learning project. Feel free to use and modify as needed.

## Contributing

This is a personal learning project, but suggestions and improvements are welcome!
