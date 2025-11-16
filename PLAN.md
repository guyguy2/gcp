# Combined GCP Learning Project Plan: Developer Hub

Let me merge the best elements of both approaches into a **superior learning path** that's educational, practical, and free-tier optimized.

## Project: Developer Hub Platform

**What you'll build:** A developer portfolio that also serves as your personal code snippet manager and learning journal - actually useful for your daily work while teaching you GCP comprehensively.

**Why this combination is better:**
- Starts simple, scales complex (progressive learning)
- **GKE Autopilot** focus (critical for staying free)
- Real-world security (Workload Identity)
- Multiple IaC approaches (K8s YAMLs -> Config Connector -> Terraform)
- Cloud Run comparison (understand when to use what)
- Actually useful (you'll use this daily)

---

## Critical Free Tier Rules

**MUST DO:**
1. **Use GKE Autopilot** - Standard GKE will cost money
2. **Single cluster in free-eligible region** - `us-central1`, `us-west1`, or `us-east1`
3. **Monitor costs** - Set budget alerts at $5 and $10
4. **Use Artifact Registry** - GCR is deprecated

**Free Tier Allowances:**
- GKE: 1 Autopilot cluster free (management + certain vCPU/memory)
- GCS: 5GB-months
- Firestore: 1 GiB storage + 50K reads, 20K writes, 20K deletes/day
- Cloud Functions: 2M invocations/month
- Cloud Build: 120 build-minutes/day

---

## Project Phases (Progressive Complexity)

### Phase 1: Foundation (Week 1)
**Goal:** Get infrastructure running with K8s YAMLs

#### 1.1 GCP Project Setup
```bash
# Create project
gcloud projects create developer-hub-learning
gcloud config set project developer-hub-learning

# Enable APIs
gcloud services enable \
  container.googleapis.com \
  artifactregistry.googleapis.com \
  firestore.googleapis.com \
  storage-api.googleapis.com \
  cloudbuild.googleapis.com
```

#### 1.2 Create GCS Buckets
```bash
# Frontend bucket (public)
gsutil mb -l us-central1 gs://[YOUR-NAME]-devhub-frontend
gsutil iam ch allUsers:objectViewer gs://[YOUR-NAME]-devhub-frontend

# Config/uploads bucket (private)
gsutil mb -l us-central1 gs://[YOUR-NAME]-devhub-storage
```

**Bucket structure:**
```
devhub-storage/
+-- configs/          # App configs (satisfies your requirement)
+-- snippets/         # Code file uploads
+-- backups/          # Firestore exports
+-- assets/           # Images, diagrams
```

#### 1.3 Firestore Setup
```bash
# Create Firestore in Native mode
gcloud firestore databases create --location=us-central
```

**Collections:**
```
portfolio/
  {linkId}/
    - title, url, order, category, icon

snippets/
  {snippetId}/
    - title, code, language, tags[], createdAt
    - category, gcsFileUrl (optional)
    - isPublic (for portfolio display)

learningNotes/
  {noteId}/
    - title, content, tags[], date, resources[]
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

---

### Phase 2: Simple Backend (Week 1-2)
**Goal:** Java API with Firestore, deployed via K8s YAMLs

#### 2.1 Java Spring Boot Application

**Project structure:**
```
backend/
+-- src/main/java/com/devhub/
|   +-- controller/
|   |   +-- PortfolioController.java  # GET /api/portfolio
|   |   +-- SnippetsController.java   # CRUD /api/snippets
|   +-- service/
|   |   +-- FirestoreService.java
|   |   +-- StorageService.java
|   +-- model/
|   |   +-- PortfolioLink.java
|   |   +-- CodeSnippet.java
|   +-- DevHubApplication.java
+-- k8s/
|   +-- deployment.yaml
|   +-- service.yaml
|   +-- ingress.yaml
+-- Dockerfile
+-- pom.xml
```

**Key Dependencies (pom.xml):**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.google.cloud</groupId>
        <artifactId>spring-cloud-gcp-starter-firestore</artifactId>
    </dependency>
    <dependency>
        <groupId>com.google.cloud</groupId>
        <artifactId>spring-cloud-gcp-starter-storage</artifactId>
    </dependency>
</dependencies>
```

**Sample Controller:**
```java
@RestController
@RequestMapping("/api")
public class SnippetsController {
    
    @Autowired
    private Firestore firestore;
    
    @GetMapping("/snippets")
    public List<CodeSnippet> getSnippets() throws Exception {
        List<CodeSnippet> snippets = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = firestore.collection("snippets")
            .orderBy("createdAt", Direction.DESCENDING)
            .get();
        
        for (DocumentSnapshot doc : query.get().getDocuments()) {
            snippets.add(doc.toObject(CodeSnippet.class));
        }
        return snippets;
    }
    
    @PostMapping("/snippets")
    public CodeSnippet createSnippet(@RequestBody CodeSnippet snippet) {
        snippet.setCreatedAt(Timestamp.now());
        firestore.collection("snippets").add(snippet);
        return snippet;
    }
}
```

#### 2.2 Dockerfile (Multi-stage for efficiency)
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2.3 Kubernetes YAMLs

**k8s/deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: devhub-api
  labels:
    app: devhub-api
spec:
  replicas: 1
  selector:
    matchLabels:
      app: devhub-api
  template:
    metadata:
      labels:
        app: devhub-api
    spec:
      serviceAccountName: devhub-ksa  # For Workload Identity (Phase 3)
      containers:
      - name: devhub-container
        image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: GCP_PROJECT_ID
          value: "developer-hub-learning"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

**k8s/service.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: devhub-api-svc
spec:
  type: ClusterIP
  selector:
    app: devhub-api
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
```

**k8s/ingress.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: devhub-ingress
  annotations:
    kubernetes.io/ingress.class: "gce"
    # Optional: For HTTPS (requires domain)
    # cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  defaultBackend:
    service:
      name: devhub-api-svc
      port:
        number: 80
```

---

### Phase 3: Security Best Practices (Week 2)
**Goal:** Implement Workload Identity (industry standard)

#### 3.1 Setup Workload Identity

**Why this matters:** 
- No JSON key files in containers
- Fine-grained permissions
- Auditable access
- Industry best practice

**Steps:**
```bash
# 1. Create GCP Service Account
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

**Result:** Your pods now automatically authenticate as the GSA without any keys!

---

### Phase 4: CI/CD Pipeline (Week 2-3)
**Goal:** Automated deployment on every push

#### 4.1 GitHub Secrets Setup
```
GCP_PROJECT_ID: developer-hub-learning
GKE_CLUSTER: devhub-cluster
GKE_REGION: us-central1
ARTIFACT_REGISTRY: us-central1-docker.pkg.dev
```

**For auth, use Workload Identity Federation (no keys!):**
- Follow: https://github.com/google-github-actions/auth#setup

#### 4.2 GitHub Actions Workflow

**.github/workflows/backend-deploy.yml:**
```yaml
name: Deploy Backend to GKE

on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/backend-deploy.yml'

env:
  PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
  GKE_CLUSTER: ${{ secrets.GKE_CLUSTER }}
  GKE_REGION: ${{ secrets.GKE_REGION }}
  IMAGE: devhub-api

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    permissions:
      contents: 'read'
      id-token: 'write'
    
    steps:
    - name: Checkout
      uses: actions/checkout@v3
    
    - name: Authenticate to Google Cloud
      uses: google-github-actions/auth@v1
      with:
        workload_identity_provider: ${{ secrets.WIF_PROVIDER }}
        service_account: ${{ secrets.WIF_SERVICE_ACCOUNT }}
    
    - name: Set up Cloud SDK
      uses: google-github-actions/setup-gcloud@v1
    
    - name: Configure Docker for Artifact Registry
      run: gcloud auth configure-docker ${{ secrets.ARTIFACT_REGISTRY }}
    
    - name: Build with Maven
      working-directory: ./backend
      run: mvn clean package -DskipTests
    
    - name: Build Docker image
      working-directory: ./backend
      run: |
        docker build -t ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:$GITHUB_SHA \
                     -t ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:latest .
    
    - name: Push to Artifact Registry
      run: |
        docker push ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:$GITHUB_SHA
        docker push ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:latest
    
    - name: Get GKE credentials
      uses: google-github-actions/get-gke-credentials@v1
      with:
        cluster_name: ${{ env.GKE_CLUSTER }}
        location: ${{ env.GKE_REGION }}
    
    - name: Deploy to GKE
      working-directory: ./backend
      run: |
        kubectl apply -f k8s/
        kubectl rollout status deployment/devhub-api
        kubectl get services
```

**.github/workflows/frontend-deploy.yml:**
```yaml
name: Deploy Frontend to GCS

on:
  push:
    branches: [main]
    paths:
      - 'frontend/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    permissions:
      contents: 'read'
      id-token: 'write'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Authenticate to Google Cloud
      uses: google-github-actions/auth@v1
      with:
        workload_identity_provider: ${{ secrets.WIF_PROVIDER }}
        service_account: ${{ secrets.WIF_SERVICE_ACCOUNT }}
    
    - name: Upload to GCS
      uses: google-github-actions/upload-cloud-storage@v1
      with:
        path: frontend
        destination: ${{ secrets.FRONTEND_BUCKET }}
        parent: false
```

---

### Phase 5: Config Connector (Week 3)
**Goal:** Manage GCP infrastructure with K8s YAMLs

This is **powerful** - you manage GCP resources the same way you manage K8s resources!

#### 5.1 Install Config Connector
```bash
# Install Config Connector
kubectl apply -f https://raw.githubusercontent.com/GoogleCloudPlatform/k8s-config-connector/master/install-bundles/install-bundle-gcp-identity/0-cnrm-system.yaml

# Create namespace
kubectl create namespace config-connector

# Configure identity
kubectl create serviceaccount config-connector -n config-connector

gcloud iam service-accounts add-iam-policy-binding \
    devhub-gsa@developer-hub-learning.iam.gserviceaccount.com \
    --member="serviceAccount:developer-hub-learning.svc.id.goog[config-connector/config-connector]" \
    --role="roles/owner"
```

#### 5.2 Example Infrastructure YAMLs

**infra/gcs-bucket.yaml:**
```yaml
apiVersion: storage.cnrm.cloud.google.com/v1beta1
kind: StorageBucket
metadata:
  name: devhub-backups-bucket
  namespace: config-connector
spec:
  location: us-central1
  storageClass: STANDARD
  lifecycleRule:
    - action:
        type: Delete
      condition:
        age: 30  # Delete backups older than 30 days
```

**Apply it:**
```bash
kubectl apply -f infra/gcs-bucket.yaml
# Config Connector creates the actual GCS bucket!
```

**Educational value:** You now manage infrastructure the "GitOps" way!

---

### Phase 6: Enhanced Features (Week 4)
**Goal:** Add production-grade capabilities

#### 6.1 Cloud Functions for Automation

**Function 1: Nightly Firestore Backup**
```javascript
// functions/backup-firestore/index.js
const {Firestore} = require('@google-cloud/firestore');
const {Storage} = require('@google-cloud/storage');

exports.backupFirestore = async (req, res) => {
    const firestore = new Firestore();
    const storage = new Storage();
    
    const bucket = storage.bucket('devhub-storage');
    const date = new Date().toISOString().split('T')[0];
    
    await firestore.export({
        outputUriPrefix: `gs://devhub-storage/backups/${date}/`
    });
    
    res.send('Backup completed');
};
```

**Deploy:**
```bash
gcloud functions deploy backupFirestore \
    --runtime nodejs18 \
    --trigger-http \
    --allow-unauthenticated
```

**Schedule with Cloud Scheduler:**
```bash
gcloud scheduler jobs create http firestore-backup \
    --schedule="0 2 * * *" \
    --uri="https://REGION-PROJECT.cloudfunctions.net/backupFirestore" \
    --http-method=GET
```

#### 6.2 Secret Manager Integration

```bash
# Create secret
echo -n "super-secret-api-key" | gcloud secrets create api-key --data-file=-

# Grant access
gcloud secrets add-iam-policy-binding api-key \
    --member="serviceAccount:devhub-gsa@PROJECT.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

**Mount in deployment:**
```yaml
# Add to deployment.yaml
spec:
  containers:
  - name: devhub-container
    env:
    - name: API_KEY
      valueFrom:
        secretKeyRef:
          name: api-key-secret
          key: latest
```

#### 6.3 Monitoring & Logging

**Add to Java app:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-logging</artifactId>
</dependency>
```

**Create Log-based Metrics:**
```bash
gcloud logging metrics create error-rate \
    --description="API error rate" \
    --log-filter='resource.type="k8s_container" severity>=ERROR'
```

**Set up Alerts:**
```bash
gcloud alpha monitoring policies create \
    --notification-channels=CHANNEL_ID \
    --display-name="High Error Rate" \
    --condition-display-name="Error Rate > 10/min"
```

---

### Phase 7: Cloud Run Comparison (Week 4)
**Goal:** Understand when to use Cloud Run vs GKE

#### 7.1 Deploy Same App to Cloud Run

**Modify backend for Cloud Run:**
```dockerfile
# Same Dockerfile works!
```

**Deploy:**
```bash
gcloud run deploy devhub-api-cloudrun \
    --source ./backend \
    --region us-central1 \
    --allow-unauthenticated \
    --set-env-vars GCP_PROJECT_ID=developer-hub-learning
```

#### 7.2 Comparison Matrix

| Feature | GKE Autopilot | Cloud Run |
|---------|--------------|-----------|
| **Setup Complexity** | High (K8s knowledge needed) | Low (single command) |
| **Scaling** | Manual replica management | Automatic 0?N |
| **Cost** | Constant (pods always running) | Pay per request |
| **Networking** | Full K8s networking | Simple HTTP |
| **Use Case** | Microservices, stateful apps | Stateless APIs, webhooks |
| **Cold Start** | None (pods always warm) | Yes (~1-2s) |
| **Best For** | Learning K8s, complex orchestration | Simple APIs, cost optimization |

**Educational insight:** For this simple API, Cloud Run is actually better! But GKE teaches you orchestration.

---

### Phase 8: Frontend (Week 4-5)
**Goal:** Modern React frontend with API integration

#### 8.1 React Application

```bash
npx create-react-app frontend
cd frontend
npm install axios
```

**src/components/SnippetManager.jsx:**
```javascript
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://YOUR_INGRESS_IP/api';

export default function SnippetManager() {
    const [snippets, setSnippets] = useState([]);
    const [newSnippet, setNewSnippet] = useState({
        title: '', code: '', language: '', tags: []
    });

    useEffect(() => {
        fetchSnippets();
    }, []);

    const fetchSnippets = async () => {
        const response = await axios.get(`${API_URL}/snippets`);
        setSnippets(response.data);
    };

    const createSnippet = async () => {
        await axios.post(`${API_URL}/snippets`, newSnippet);
        fetchSnippets();
        setNewSnippet({ title: '', code: '', language: '', tags: [] });
    };

    return (
        <div>
            <h2>Code Snippets</h2>
            
            {/* Form to add snippet */}
            <div>
                <input 
                    value={newSnippet.title}
                    onChange={e => setNewSnippet({...newSnippet, title: e.target.value})}
                    placeholder="Title"
                />
                <textarea 
                    value={newSnippet.code}
                    onChange={e => setNewSnippet({...newSnippet, code: e.target.value})}
                    placeholder="Code"
                />
                <button onClick={createSnippet}>Save Snippet</button>
            </div>

            {/* Display snippets */}
            <div>
                {snippets.map(snippet => (
                    <div key={snippet.id}>
                        <h3>{snippet.title}</h3>
                        <pre><code>{snippet.code}</code></pre>
                    </div>
                ))}
            </div>
        </div>
    );
}
```

**Build and deploy:**
```bash
npm run build
gsutil -m rsync -r build gs://YOUR-FRONTEND-BUCKET
```

---

## Learning Outcomes by Phase

| Phase | Skills Learned | GCP Services |
|-------|----------------|--------------|
| 1 | Project setup, basic GCP CLI | GCS, Firestore, GKE |
| 2 | Containerization, K8s basics | Artifact Registry, GKE |
| 3 | IAM, security best practices | Workload Identity |
| 4 | CI/CD, automation | Cloud Build, GitHub Actions |
| 5 | GitOps, declarative infrastructure | Config Connector |
| 6 | Serverless, monitoring | Cloud Functions, Logging |
| 7 | Architectural decisions | Cloud Run vs GKE |
| 8 | Full-stack integration | End-to-end deployment |

---

## Why This Combined Approach is Superior

1. **Progressive complexity** - Start simple, add features incrementally
2. **Real security** - Workload Identity from the start
3. **Multiple IaC approaches** - K8s YAMLs -> Config Connector -> (optional) Terraform
4. **Practical comparison** - GKE vs Cloud Run side-by-side
5. **Production patterns** - Monitoring, secrets, backups, CI/CD
6. **Actually useful** - You'll use this tool daily
7. **Free tier optimized** - Autopilot, careful service selection
8. **Java-centric** - Plays to your strengths

---

## Cost Monitoring Setup

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

---

## Next Steps

**Week 1:** Phases 1-2 (Get it running)
**Week 2:** Phases 3-4 (Secure it, automate it)
**Week 3:** Phase 5 (Infrastructure as Code)
**Week 4:** Phases 6-7 (Advanced features, comparisons)
**Week 5:** Phase 8 (Polish the frontend)

Want me to generate specific code for any phase? I can provide:
- Complete Java service implementations
- Detailed Config Connector examples
- Terraform alternative configurations
- Advanced monitoring dashboards
- Or dive deeper into any specific area!