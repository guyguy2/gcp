# Quick Start Guide

Get the Developer Hub Platform up and running in minutes.

## Prerequisites

- Google Cloud Platform account with billing enabled
- `gcloud` CLI installed
- `kubectl` installed
- `docker` installed
- Java 17 and Maven (for local development)
- Node.js 18+ (for frontend development)

## Option 1: Automated Setup (Recommended)

### 1. Run the setup script

```bash
./setup-gcp.sh
```

This script will:
- Enable all required GCP APIs
- Create GCS buckets
- Setup Firestore
- Create GKE Autopilot cluster
- Setup Workload Identity
- Configure Artifact Registry

### 2. Update configuration files

```bash
# Replace PROJECT_ID in Kubernetes manifests
# Use the project ID you specified in the setup script
PROJECT_ID="developer-hub-learning"  # Change this to your project ID

sed -i "s/PROJECT_ID/$PROJECT_ID/g" backend/k8s/deployment.yaml
sed -i "s/PROJECT_ID/$PROJECT_ID/g" backend/k8s/workload-identity.yaml
```

### 3. Build and deploy backend

```bash
cd backend

# Build with Maven
mvn clean package

# Build Docker image
docker build -t us-central1-docker.pkg.dev/$PROJECT_ID/devhub-repo/devhub-api:latest .

# Push to Artifact Registry
docker push us-central1-docker.pkg.dev/$PROJECT_ID/devhub-repo/devhub-api:latest

# Deploy to Kubernetes
kubectl apply -f k8s/

# Wait for deployment
kubectl rollout status deployment/devhub-api
```

### 4. Get the API endpoint

```bash
# Wait a few minutes for the ingress to get an IP
kubectl get ingress devhub-ingress

# You'll see output like:
# NAME              CLASS    HOSTS   ADDRESS         PORTS   AGE
# devhub-ingress    <none>   *       34.xxx.xxx.xxx  80      5m
```

### 5. Deploy frontend

```bash
cd ../frontend

# Install dependencies
npm install

# Build
npm run build

# Deploy to GCS (replace with your bucket name)
gsutil -m rsync -r build gs://YOUR-BUCKET-NAME-devhub-frontend

# Access your app at:
# http://storage.googleapis.com/YOUR-BUCKET-NAME-devhub-frontend/index.html
```

## Option 2: Manual Setup

Follow the detailed instructions in [README.md](README.md).

## Test the API

```bash
# Get the ingress IP
INGRESS_IP=$(kubectl get ingress devhub-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Test the health endpoint
curl http://$INGRESS_IP/actuator/health

# Test creating a portfolio link
curl -X POST http://$INGRESS_IP/api/portfolio \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My GitHub",
    "url": "https://github.com/username",
    "order": 1,
    "category": "GitHub",
    "description": "My GitHub profile"
  }'

# Get all portfolio links
curl http://$INGRESS_IP/api/portfolio
```

## Local Development

### Backend

```bash
cd backend

# Run with Maven
mvn spring-boot:run

# API available at http://localhost:8080
```

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm start

# App available at http://localhost:3000
```

Update `frontend/src/components/Portfolio.jsx` and `SnippetManager.jsx` to point to `http://localhost:8080/api` for local development.

## Common Issues

### Cluster not accessible

```bash
# Re-authenticate
gcloud container clusters get-credentials devhub-cluster --region us-central1
```

### Docker authentication issues

```bash
# Re-configure Docker
gcloud auth configure-docker us-central1-docker.pkg.dev
```

### Ingress not getting IP

Wait 5-10 minutes. GCP needs time to provision the load balancer.

```bash
# Check status
kubectl describe ingress devhub-ingress
```

### Pod crashes or fails to start

```bash
# Check logs
kubectl logs -f deployment/devhub-api

# Check pod details
kubectl get pods
kubectl describe pod <pod-name>

# Common issue: Workload Identity not configured
# Verify the service account annotation
kubectl get serviceaccount devhub-ksa -o yaml
```

## Cost Management

Monitor your costs to stay within free tier:

```bash
# Set up budget alerts (replace BILLING_ACCOUNT_ID)
gcloud billing budgets create \
    --billing-account=BILLING_ACCOUNT_ID \
    --display-name="Developer Hub Budget" \
    --budget-amount=10USD \
    --threshold-rule=percent=50 \
    --threshold-rule=percent=90
```

## Clean Up (Delete Everything)

To avoid charges when you're done:

```bash
# Delete the GKE cluster (main cost driver)
gcloud container clusters delete devhub-cluster --region us-central1

# Delete GCS buckets
gsutil rm -r gs://YOUR-BUCKET-NAME-devhub-frontend
gsutil rm -r gs://YOUR-BUCKET-NAME-devhub-storage

# Delete Artifact Registry
gcloud artifacts repositories delete devhub-repo --location=us-central1

# Delete the project (optional - deletes everything)
gcloud projects delete developer-hub-learning
```

## Next Steps

- Setup GitHub Actions for CI/CD (see README.md)
- Implement Config Connector for GitOps (Phase 5 in PLAN.md)
- Add Cloud Functions for automated backups (Phase 6 in PLAN.md)
- Deploy to Cloud Run for comparison (Phase 7 in PLAN.md)
- Add authentication (Phase 8 in PLAN.md)

## Need Help?

- Full documentation: [README.md](README.md)
- Implementation plan: [PLAN.md](PLAN.md)
- Progress tracker: [progress.md](progress.md)
- GCP Free Tier: https://cloud.google.com/free
- GKE Autopilot Docs: https://cloud.google.com/kubernetes-engine/docs/concepts/autopilot-overview
