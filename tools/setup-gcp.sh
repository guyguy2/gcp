#!/bin/bash

# Developer Hub Platform - GCP Infrastructure Setup Script
# This script sets up the GCP infrastructure needed for the Developer Hub Platform
# Run this after creating your GCP project and enabling billing

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Developer Hub Platform Setup${NC}"
echo -e "${GREEN}==================================${NC}"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    echo "Please install it from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Prompt for project ID
read -p "Enter your GCP project ID (or press Enter for 'developer-hub-learning'): " PROJECT_ID
PROJECT_ID=${PROJECT_ID:-developer-hub-learning}

echo -e "${YELLOW}Using project ID: ${PROJECT_ID}${NC}"
echo ""

# Set the project
echo -e "${GREEN}Setting GCP project...${NC}"
gcloud config set project $PROJECT_ID || {
    echo -e "${YELLOW}Project doesn't exist. Creating it...${NC}"
    gcloud projects create $PROJECT_ID
    gcloud config set project $PROJECT_ID
}

# Prompt for bucket prefix
read -p "Enter a unique prefix for GCS buckets (e.g., your-name): " BUCKET_PREFIX
if [ -z "$BUCKET_PREFIX" ]; then
    echo -e "${RED}Error: Bucket prefix is required${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Step 1: Enabling required APIs...${NC}"
gcloud services enable \
    container.googleapis.com \
    artifactregistry.googleapis.com \
    firestore.googleapis.com \
    storage-api.googleapis.com \
    cloudbuild.googleapis.com \
    cloudscheduler.googleapis.com \
    secretmanager.googleapis.com

echo ""
echo -e "${GREEN}Step 2: Creating GCS buckets...${NC}"

# Frontend bucket (public)
echo "Creating frontend bucket: ${BUCKET_PREFIX}-devhub-frontend"
gsutil mb -l us-central1 gs://${BUCKET_PREFIX}-devhub-frontend || echo "Bucket already exists"
gsutil iam ch allUsers:objectViewer gs://${BUCKET_PREFIX}-devhub-frontend

# Storage bucket (private)
echo "Creating storage bucket: ${BUCKET_PREFIX}-devhub-storage"
gsutil mb -l us-central1 gs://${BUCKET_PREFIX}-devhub-storage || echo "Bucket already exists"

echo ""
echo -e "${GREEN}Step 3: Creating Firestore database...${NC}"
gcloud firestore databases create --location=us-central || echo "Firestore database already exists"

echo ""
echo -e "${GREEN}Step 4: Creating Artifact Registry...${NC}"
gcloud artifacts repositories create devhub-repo \
    --repository-format=docker \
    --location=us-central1 \
    --description="Developer Hub containers" || echo "Repository already exists"

echo ""
echo -e "${GREEN}Step 5: Creating GKE Autopilot cluster...${NC}"
echo -e "${YELLOW}This may take 5-10 minutes...${NC}"
gcloud container clusters create-auto devhub-cluster \
    --region us-central1 \
    --project $PROJECT_ID || echo "Cluster already exists"

# Get credentials
echo "Getting cluster credentials..."
gcloud container clusters get-credentials devhub-cluster --region us-central1

echo ""
echo -e "${GREEN}Step 6: Setting up Workload Identity...${NC}"

# Create Google Service Account
echo "Creating Google Service Account..."
gcloud iam service-accounts create devhub-gsa \
    --display-name="DevHub GKE Service Account" || echo "Service account already exists"

# Grant permissions
echo "Granting IAM permissions..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:devhub-gsa@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/datastore.user"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:devhub-gsa@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/storage.objectAdmin"

# Create Kubernetes Service Account
echo "Creating Kubernetes Service Account..."
kubectl create serviceaccount devhub-ksa -n default || echo "Service account already exists"

# Bind KSA to GSA
echo "Binding Kubernetes SA to Google SA..."
gcloud iam service-accounts add-iam-policy-binding \
    devhub-gsa@${PROJECT_ID}.iam.gserviceaccount.com \
    --role roles/iam.workloadIdentityUser \
    --member "serviceAccount:${PROJECT_ID}.svc.id.goog[default/devhub-ksa]"

# Annotate KSA
echo "Annotating Kubernetes Service Account..."
kubectl annotate serviceaccount devhub-ksa \
    iam.gke.io/gcp-service-account=devhub-gsa@${PROJECT_ID}.iam.gserviceaccount.com \
    --overwrite

echo ""
echo -e "${GREEN}Step 7: Configuring Docker for Artifact Registry...${NC}"
gcloud auth configure-docker us-central1-docker.pkg.dev

echo ""
echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}==================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update backend/k8s/deployment.yaml - Replace PROJECT_ID with: ${PROJECT_ID}"
echo "2. Update backend/k8s/workload-identity.yaml - Replace PROJECT_ID with: ${PROJECT_ID}"
echo "3. Build and push your Docker image:"
echo "   cd backend"
echo "   docker build -t us-central1-docker.pkg.dev/${PROJECT_ID}/devhub-repo/devhub-api:latest ."
echo "   docker push us-central1-docker.pkg.dev/${PROJECT_ID}/devhub-repo/devhub-api:latest"
echo "4. Deploy to Kubernetes:"
echo "   kubectl apply -f k8s/"
echo ""
echo -e "${YELLOW}Important:${NC}"
echo "- Frontend bucket: gs://${BUCKET_PREFIX}-devhub-frontend"
echo "- Storage bucket: gs://${BUCKET_PREFIX}-devhub-storage"
echo "- Cluster: devhub-cluster (us-central1)"
echo "- Artifact Registry: us-central1-docker.pkg.dev/${PROJECT_ID}/devhub-repo"
echo ""
echo -e "${GREEN}For full deployment instructions, see README.md${NC}"
