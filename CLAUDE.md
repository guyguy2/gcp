# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Developer Hub Platform - A GCP learning project that combines a developer portfolio, code snippet manager, and learning journal. Built using Java Spring Boot backend on GKE Autopilot, with React frontend hosted on GCS.

**Implementation Status:**
- Phases 1-4: COMPLETE (Backend, K8s, Workload Identity, CI/CD)
- Phase 5-8: DOCUMENTED, NOT IMPLEMENTED (Config Connector, Cloud Functions, Cloud Run, advanced features)
- Backend: Production-ready Spring Boot API with full CRUD operations
- Frontend: React components complete, package.json setup pending
- Testing: Test suites not yet implemented

**Key architectural decisions:**
- GKE Autopilot (not Standard GKE) for free tier eligibility
- Workload Identity for secure GCP authentication (no JSON keys)
- Multi-stage deployment: K8s YAMLs, then Config Connector, optional Terraform
- Cloud Run comparison deployment for architectural learning
- Free tier optimized: single cluster in us-central1/us-west1/us-east1

## Project Structure (Deployment-Focused)

```
/
├── services/
│   ├── api/             # Backend service (Java Spring Boot)
│   └── web/             # Frontend service (React)
├── deploy/
│   ├── kubernetes/      # K8s manifests (deployment, service, ingress)
│   ├── gcp/            # GCP-specific configs (future: Config Connector)
│   └── ci-cd/          # GitHub Actions workflows (reference copies)
├── shared/
│   ├── configs/        # Shared configurations
│   └── docs/           # Documentation (README, PLAN, guides)
└── tools/              # Scripts and utilities (setup-gcp.sh)
```

**Note:** GitHub Actions workflows are stored in both `.github/workflows/` (required by GitHub) and `deploy/ci-cd/` (organizational reference).

## Key File Locations

### Backend (Java Spring Boot)
- **Main Application**: `services/api/src/main/java/com/devhub/DevHubApplication.java`
- **Controllers**: `services/api/src/main/java/com/devhub/controller/`
  - `PortfolioController.java` - Portfolio CRUD endpoints
  - `SnippetsController.java` - Snippet CRUD + file upload
- **Services**: `services/api/src/main/java/com/devhub/service/`
  - `PortfolioService.java` - Firestore operations for portfolio
  - `SnippetService.java` - Firestore operations for snippets
  - `StorageService.java` - GCS file operations
- **Models**: `services/api/src/main/java/com/devhub/model/`
  - `PortfolioLink.java`, `CodeSnippet.java`, `LearningNote.java`
- **Configuration**: `services/api/src/main/resources/application.properties`
- **Build**: `services/api/pom.xml`, `services/api/Dockerfile`

### Frontend (React)
- **Entry Point**: `services/web/src/index.js`
- **Main App**: `services/web/src/App.js`
- **Components**: `services/web/src/components/`
  - `Home.jsx` - Landing page
  - `Portfolio.jsx` - Portfolio management UI
  - `SnippetManager.jsx` - Snippet management UI
- **Styling**: `services/web/src/App.css`, `services/web/src/index.css`
- **Missing**: `services/web/package.json` (needs creation)

### Deployment
- **Kubernetes**: `deploy/kubernetes/`
  - `deployment.yaml` - Pod spec with Workload Identity
  - `service.yaml` - ClusterIP service definition
  - `ingress.yaml` - GCE load balancer configuration
  - `workload-identity.yaml` - Service account binding
- **CI/CD**: `.github/workflows/` (active) and `deploy/ci-cd/` (reference)
  - `backend-deploy.yml` - Backend pipeline
  - `frontend-deploy.yml` - Frontend pipeline
- **GCP Resources**: `deploy/gcp/` (reserved for Config Connector, currently empty)

### Documentation
- `README.md` - Project overview and quick start
- `CLAUDE.md` - This file (guidance for Claude Code)
- `shared/docs/PLAN.md` - Detailed 8-phase implementation plan
- `shared/docs/QUICKSTART.md` - User-friendly setup guide
- `shared/docs/progress.md` - Implementation status tracker

### Tools & Scripts
- `tools/setup-gcp.sh` - Automated infrastructure provisioning
- `tools/validate.sh` - Code validation suite (YAML, K8s, Docker, Maven)
- `.gitignore` - Comprehensive ignore rules for Java, Node, GCP

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
cd services/api
mvn clean package

# Build Docker image
docker build -t us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest .

# Push to Artifact Registry
docker push us-central1-docker.pkg.dev/developer-hub-learning/devhub-repo/devhub-api:latest

# Deploy to GKE
kubectl apply -f ../../deploy/kubernetes/
kubectl rollout status deployment/devhub-api

# View logs
kubectl logs -f deployment/devhub-api
```

### Testing
```bash
# Run Java tests
cd services/api
mvn test

# Run specific test
mvn test -Dtest=ClassName#methodName
```

### Frontend Development (React)
```bash
cd services/web
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

# Apply all Kubernetes manifests
kubectl apply -f deploy/kubernetes/

# Check Config Connector status (when implemented)
kubectl apply -f deploy/gcp/
kubectl get storagebucket -n config-connector
```

### Project Validation
```bash
# Run comprehensive validation suite
./tools/validate.sh

# Validates:
# - YAML syntax (yamllint)
# - Kubernetes manifests (kubectl dry-run)
# - GitHub Actions workflows (actionlint)
# - Dockerfile linting (hadolint)
# - Maven build and dependencies
```

### Infrastructure Setup
```bash
# Automated GCP infrastructure setup
./tools/setup-gcp.sh

# Creates:
# - GCS buckets (frontend + storage)
# - Firestore database
# - Artifact Registry repository
# - GKE Autopilot cluster
# - Workload Identity binding
```

## Architecture

### Service Layer
- **Backend**: Java Spring Boot REST API (port 8080)
  - Framework: Spring Boot 3.2.0, Java 17
  - Controllers: PortfolioController, SnippetsController
  - Services: PortfolioService, SnippetService, StorageService
  - Models: PortfolioLink, CodeSnippet, LearningNote (model only, no controller yet)
  - Dependencies: spring-cloud-gcp-starter-firestore, spring-cloud-gcp-starter-storage
  - Workload Identity integration via serviceAccountName in deployment.yaml
- **Frontend**: React SPA served from GCS bucket
  - Components: Home.jsx, Portfolio.jsx, SnippetManager.jsx
  - Routing: React Router v6
  - HTTP Client: Axios
  - Status: Components complete, package.json pending
- **Database**: Firestore Native mode with collections: portfolio/, snippets/, learningNotes/
- **Storage**: GCS buckets for frontend (public), configs/uploads (private)

### API Endpoints (Implemented)

**Portfolio Management** (`/api/portfolio`)
- `GET /api/portfolio` - Get all portfolio links (sorted by order)
- `GET /api/portfolio/{id}` - Get single portfolio link
- `GET /api/portfolio/category/{category}` - Filter by category
- `POST /api/portfolio` - Create new portfolio link (validated)
- `PUT /api/portfolio/{id}` - Update portfolio link
- `DELETE /api/portfolio/{id}` - Delete portfolio link

**Snippet Management** (`/api/snippets`)
- `GET /api/snippets` - Get all snippets (sorted by creation date, newest first)
- `GET /api/snippets/public` - Get public snippets only
- `GET /api/snippets/language/{language}` - Filter by programming language
- `GET /api/snippets/tag/{tag}` - Filter by tag
- `GET /api/snippets/{id}` - Get single snippet
- `POST /api/snippets` - Create snippet (JSON body)
- `POST /api/snippets/upload` - Create snippet with file upload to GCS
- `PUT /api/snippets/{id}` - Update snippet
- `DELETE /api/snippets/{id}` - Delete snippet and associated GCS file

**Health & Monitoring**
- `GET /actuator/health` - Health check endpoint
- `GET /actuator/info` - Application info
- `GET /actuator/prometheus` - Prometheus metrics

**Note**: Learning Notes API (`/api/learningnotes`) is not yet implemented despite model existing.

### Security Model
Workload Identity binds Kubernetes Service Account (devhub-ksa) to Google Service Account (devhub-gsa@developer-hub-learning.iam.gserviceaccount.com). Pods authenticate automatically without JSON keys. Required roles: roles/datastore.user, roles/storage.objectAdmin.

### Infrastructure as Code Progression
1. Phase 1-2: Manual kubectl apply of deploy/kubernetes/*.yaml files
2. Phase 5: Config Connector manages GCP resources as K8s objects (deploy/gcp/)
3. Optional: Terraform for comparison/production scenarios

### CI/CD
GitHub Actions workflows deploy on push to main:
- **backend-deploy.yml**: Maven build, Docker build/push, kubectl apply (READY)
  - Triggers on changes to `services/api/**`
  - Runs Maven tests (currently no tests exist)
  - Multi-stage Docker build to Artifact Registry
  - Deploys to GKE with sed-replacement of PROJECT_ID
  - 5-minute rollout timeout
- **frontend-deploy.yml**: React build, GCS upload (BLOCKED - needs package.json)
  - Triggers on changes to `services/web/**`
  - Requires package.json with build scripts
  - Sets REACT_APP_API_URL from GitHub secrets
  - Uploads to GCS with cache header optimization
- Workflows located in .github/workflows/ with reference copies in deploy/ci-cd/
- Uses Workload Identity Federation for GCP authentication (no service account keys)

### Implementation Phases Status

**Phase 1: GCP Foundation** - COMPLETE
- GCP project, billing, APIs enabled
- GCS buckets created
- Firestore database initialized
- Artifact Registry repository created

**Phase 2: Backend Development** - COMPLETE
- Java Spring Boot 3.2.0 application
- 3 REST controllers (Portfolio, Snippets, Health)
- 3 service classes with Firestore integration
- GCS file upload/download service
- Multi-stage Docker build
- Kubernetes deployment manifests

**Phase 3: Workload Identity** - COMPLETE
- Google Service Account (devhub-gsa) created
- Kubernetes Service Account (devhub-ksa) configured
- IAM binding established
- Deployment.yaml configured with serviceAccountName

**Phase 4: CI/CD Pipeline** - COMPLETE (Backend), BLOCKED (Frontend)
- GitHub Actions workflows created
- Workload Identity Federation configured
- Backend pipeline: fully functional
- Frontend pipeline: needs package.json to execute

**Phase 5: Config Connector** - DOCUMENTED, NOT IMPLEMENTED
- `deploy/gcp/` directory reserved
- No Config Connector resources created yet
- See shared/docs/PLAN.md for implementation guide

**Phase 6: Enhanced Features** - DOCUMENTED, NOT IMPLEMENTED
- Cloud Functions for backups not implemented
- Secret Manager integration not configured
- Cloud Monitoring dashboards not created

**Phase 7: Cloud Run Comparison** - DOCUMENTED, NOT IMPLEMENTED
- Alternative deployment pattern documented in PLAN.md
- No Cloud Run configuration files created

**Phase 8: Frontend Completion** - PARTIAL
- React components: COMPLETE (3 components)
- Package.json: MISSING (required for build)
- Learning Journal component: NOT IMPLEMENTED

## GCS Bucket Structure
```
devhub-storage/
├── configs/          # Application configurations
├── snippets/         # Uploaded code files
├── backups/          # Firestore exports (automated via Cloud Function)
└── assets/           # Images, diagrams
```

## Firestore Collections Schema

### portfolio/
Document ID: auto-generated
- `title` (string): Display name of the portfolio link
- `url` (string, validated): Full URL to the portfolio item
- `order` (integer): Display order (lower numbers first)
- `category` (string): Classification (e.g., "project", "blog", "github")
- `icon` (string, optional): Icon identifier or URL
- `description` (string, optional): Brief description

### snippets/
Document ID: auto-generated
- `title` (string): Snippet title
- `code` (string): Code content
- `language` (string): Programming language (e.g., "java", "javascript", "python")
- `tags` (array of strings): Keywords for searching
- `createdAt` (timestamp): Creation date
- `updatedAt` (timestamp): Last modification date
- `category` (string, optional): Classification
- `gcsFileUrl` (string, optional): URL to uploaded file in GCS
- `isPublic` (boolean): Whether snippet is publicly visible
- `description` (string, optional): Description of the snippet
- `author` (string, optional): Author name

### learningNotes/
Document ID: auto-generated (Model exists, API not implemented)
- `title` (string): Note title
- `content` (string): Note content (markdown supported)
- `tags` (array of strings): Keywords
- `date` (timestamp): Creation date
- `resources` (array of strings): Related URLs/references
- `category` (string, optional): Classification
- `difficultyLevel` (string, optional): Complexity indicator

## Frontend Setup (Pending)

The React components are complete but `package.json` needs to be created:

### Required Dependencies
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "react-scripts": "^5.0.1"
  }
}
```

### Required Scripts
```json
{
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "eject": "react-scripts eject"
  }
}
```

### Environment Configuration
Create `.env` in `services/web/`:
```bash
REACT_APP_API_URL=http://localhost:8080/api
```

For production, set GitHub secret `REACT_APP_API_URL` to the GKE Ingress URL.

## Testing (Not Yet Implemented)

### Backend Testing
Create test files in `services/api/src/test/java/com/devhub/`:
- `controller/*Test.java` - Controller unit tests
- `service/*Test.java` - Service layer tests
- Integration tests with Testcontainers for Firestore emulator

Run tests:
```bash
cd services/api
mvn test
mvn test -Dtest=PortfolioControllerTest
```

### Frontend Testing
Setup Jest and React Testing Library:
```bash
cd services/web
npm test
npm test -- --coverage
```

Create tests in `services/web/src/components/__tests__/`:
- `Portfolio.test.js`
- `SnippetManager.test.js`
- `Home.test.js`

## Production Hardening Checklist

**Security**
- [ ] Restrict CORS origins in `application.properties` (currently allows all origins with `*`)
- [ ] Enable HTTPS in Ingress with managed certificates
- [ ] Add network policies to restrict pod-to-pod communication
- [ ] Implement rate limiting on API endpoints
- [ ] Add request validation and sanitization
- [ ] Configure Pod Security Standards (restricted profile)

**Configuration**
- [ ] Replace placeholder `PROJECT_ID` in K8s manifests via CI/CD sed commands
- [ ] Move sensitive config to Secret Manager (database URLs, API keys)
- [ ] Create ConfigMaps for environment-specific settings
- [ ] Set up secret rotation policies

**Monitoring & Observability**
- [ ] Configure Cloud Logging log aggregation
- [ ] Set up Cloud Monitoring dashboards for API metrics
- [ ] Configure uptime checks and alerting policies
- [ ] Add distributed tracing with Cloud Trace
- [ ] Set up error reporting with Cloud Error Reporting

**Reliability**
- [ ] Increase replicas in deployment.yaml for production (currently 1)
- [ ] Configure Horizontal Pod Autoscaler (HPA)
- [ ] Add PodDisruptionBudget for high availability
- [ ] Implement graceful shutdown handling
- [ ] Add liveness/readiness probe tuning

**Performance**
- [ ] Enable GCS CDN for frontend bucket
- [ ] Configure HTTP caching headers appropriately
- [ ] Implement API response caching where appropriate
- [ ] Optimize Firestore queries with composite indexes
- [ ] Review and optimize resource requests/limits

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
