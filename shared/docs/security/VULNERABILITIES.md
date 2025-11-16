# Security Vulnerabilities - Detailed Code Reference

## Document Purpose
Quick reference guide to all identified vulnerabilities with exact file locations and code snippets.

---

## CRITICAL VULNERABILITIES

### 1. CORS Wildcard - Cross-Origin Resource Sharing Configuration

#### Location 1: PortfolioController.java
**File:** `/home/user/gcp/services/api/src/main/java/com/devhub/controller/PortfolioController.java`
**Line:** 22

**Vulnerable Code:**
```java
@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")  // ← VULNERABLE: Allows any origin
public class PortfolioController {
```

**Severity:** HIGH
**Risk:** CSRF attacks, unauthorized API access from any domain

**Fix:**
```java
@CrossOrigin(origins = {"https://yourdomain.com", "https://www.yourdomain.com"})
// Or configure in application.properties with specific domains
```

---

#### Location 2: SnippetsController.java
**File:** `/home/user/gcp/services/api/src/main/java/com/devhub/controller/SnippetsController.java`
**Line:** 25

**Vulnerable Code:**
```java
@Slf4j
@RestController
@RequestMapping("/api/snippets")
@CrossOrigin(origins = "*")  // ← VULNERABLE: Allows any origin
public class SnippetsController {
```

**Severity:** HIGH
**Risk:** CSRF attacks, unauthorized API access from any domain

**Fix:** Same as above

---

#### Location 3: application.properties
**File:** `/home/user/gcp/services/api/src/main/resources/application.properties`
**Line:** 32

**Vulnerable Code:**
```properties
# CORS Configuration
# In production, replace with specific origins
cors.allowed-origins=*  # ← VULNERABLE: Wildcard allows all
```

**Severity:** HIGH
**Risk:** Redundant but reinforces the CORS vulnerability

**Fix:**
```properties
cors.allowed-origins=https://yourdomain.com,https://www.yourdomain.com
```

---

### 2. No HTTPS/TLS Enforcement on Ingress

#### Location: ingress.yaml
**File:** `/home/user/gcp/deploy/kubernetes/ingress.yaml`
**Lines:** 1-31

**Vulnerable Code:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: devhub-ingress
  annotations:
    kubernetes.io/ingress.class: "gce"
    # Enable HTTP to HTTPS redirect (optional, requires SSL cert)
    # networking.gke.io/managed-certificates: "devhub-cert"  ← COMMENTED OUT
    # kubernetes.io/ingress.allow-http: "false"               ← COMMENTED OUT
  labels:
    app: devhub-api
spec:
  defaultBackend:  # ← No TLS/SSL configuration
    service:
      name: devhub-api-svc
      port:
        number: 80  # ← HTTP only, not HTTPS
```

**Severity:** HIGH
**Risk:** Man-in-the-middle attacks, unencrypted data transmission

**Fix:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: devhub-ingress
  annotations:
    kubernetes.io/ingress.class: "gce"
    networking.gke.io/managed-certificates: "devhub-cert"
    kubernetes.io/ingress.allow-http: "false"
spec:
  defaultBackend:
    service:
      name: devhub-api-svc
      port:
        number: 80
```

---

### 3. Public Bucket Access (Frontend Deployment)

#### Location: frontend-deploy.yml
**File:** `/home/user/gcp/.github/workflows/frontend-deploy.yml`
**Lines:** 69-72

**Vulnerable Code:**
```yaml
    - name: Make bucket public (if needed)
      run: |
        # Ensure the bucket is publicly readable
        gsutil iam ch allUsers:objectViewer gs://${{ env.FRONTEND_BUCKET }}
        # ↑ Makes entire bucket publicly accessible
```

**Severity:** CRITICAL (intentional but dangerous if misused)
**Risk:** All objects in bucket are publicly readable

**Note:** This is INTENTIONAL for frontend hosting. However:
- Document this design decision
- Ensure only frontend assets (HTML, CSS, JS) are in this bucket
- Keep sensitive files (configs, keys) in separate buckets
- Monitor bucket contents regularly

---

## HIGH PRIORITY VULNERABILITIES

### 4. Image Version Not Pinned to Commit SHA

#### Location 1: deployment.yaml
**File:** `/home/user/gcp/deploy/kubernetes/deployment.yaml`
**Line:** 26

**Vulnerable Code:**
```yaml
      containers:
      - name: devhub-container
        # Replace PROJECT_ID with your actual GCP project ID
        image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:latest
        # ↑ "latest" tag is problematic
```

**Severity:** HIGH
**Risk:** 
- No reproducibility
- Difficult to rollback issues
- Hard to identify which version is running

**Fix:**
```yaml
image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:$GITHUB_SHA
imagePullPolicy: IfNotPresent
```

---

#### Location 2: backend-deploy.yml (CI/CD)
**File:** `/home/user/gcp/.github/workflows/backend-deploy.yml`
**Lines:** 62-69

**Vulnerable Code:**
```yaml
    - name: Build Docker image
      working-directory: ./services/api
      run: |
        docker build \
          -t ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:$GITHUB_SHA \
          -t ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:latest \
          .  # ← Creates both SHA and "latest"

    - name: Push to Artifact Registry
      run: |
        docker push ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:$GITHUB_SHA
        docker push ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:latest
        # ↑ Overwrites "latest" tag each time
```

**Severity:** HIGH
**Risk:** Same as above

**Note:** The workflow correctly creates SHA tag, but deployment uses "latest"

---

### 5. Dockerfile - Base Image Not Pinned

#### Location: Dockerfile
**File:** `/home/user/gcp/services/api/Dockerfile`
**Lines:** 5, 17

**Vulnerable Code:**
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build  # ← No digest hash
WORKDIR /app

...

# Runtime stage
FROM eclipse-temurin:17-jre-alpine  # ← No digest hash
WORKDIR /app
```

**Severity:** HIGH
**Risk:** Latest minor versions pulled; no reproducibility

**Fix:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17@sha256:abc123def456...  AS build
...
FROM eclipse-temurin:17-jre-alpine@sha256:xyz789uvw012...
```

Get digest hashes from:
```bash
docker inspect maven:3.9-eclipse-temurin-17 | grep RepoDigests
docker inspect eclipse-temurin:17-jre-alpine | grep RepoDigests
```

---

### 6. Incomplete Security Context

#### Location: deployment.yaml
**File:** `/home/user/gcp/deploy/kubernetes/deployment.yaml`
**Lines:** 82-87

**Vulnerable Code:**
```yaml
      # Security context
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        # ↑ Missing critical security fields
```

**Severity:** HIGH
**Missing Fields:**
- `readOnlyRootFilesystem: true`
- `allowPrivilegeEscalation: false`
- `capabilities: drop: ["ALL"]`

**Fix:**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
```

---

### 7. No imagePullPolicy Specified

#### Location: deployment.yaml
**File:** `/home/user/gcp/deploy/kubernetes/deployment.yaml`
**Lines:** 24-31

**Vulnerable Code:**
```yaml
      containers:
      - name: devhub-container
        image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:latest
        # ↑ No imagePullPolicy specified - defaults depend on tag
        
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
```

**Severity:** MEDIUM
**Risk:** Without explicit policy, "latest" tag may cache old images

**Fix:**
```yaml
      containers:
      - name: devhub-container
        image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:latest
        imagePullPolicy: Always  # Force fresh pull
```

---

## MEDIUM PRIORITY VULNERABILITIES

### 8. Ingress Not Restricted by Host/Path

#### Location: ingress.yaml
**File:** `/home/user/gcp/deploy/kubernetes/ingress.yaml`
**Lines:** 12-17

**Vulnerable Code:**
```yaml
spec:
  defaultBackend:  # ← Catches ALL traffic
    service:
      name: devhub-api-svc
      port:
        number: 80

  # Optional: Add rules for specific paths or hosts
  # rules:
  # - host: devhub.example.com
  #   http:
  #     paths:
  #     - path: /api
  #       pathType: Prefix
  #       backend:
  #         service:
  #           name: devhub-api-svc
  #           port:
  #             number: 80
```

**Severity:** MEDIUM
**Risk:** All traffic reaches API without hostname/path validation

**Fix:**
```yaml
spec:
  rules:
  - host: devhub.example.com
    http:
      paths:
      - path: /api/
        pathType: Prefix
        backend:
          service:
            name: devhub-api-svc
            port:
              number: 80
```

---

### 9. Environment Variables as Plaintext

#### Location: deployment.yaml
**File:** `/home/user/gcp/deploy/kubernetes/deployment.yaml`
**Lines:** 33-40

**Vulnerable Code:**
```yaml
        env:
        # GCP Project ID - override from ConfigMap or directly
        - name: GCP_PROJECT_ID
          value: "developer-hub-learning"  # ← Plaintext value

        # GCS Bucket name
        - name: GCS_BUCKET
          value: "devhub-storage"  # ← Plaintext value
```

**Severity:** MEDIUM
**Note:** These are non-sensitive, but best practice is to use Secrets

**Fix:**
```yaml
        env:
        - name: GCP_PROJECT_ID
          valueFrom:
            secretKeyRef:
              name: api-config
              key: gcp-project-id
        
        - name: GCS_BUCKET
          valueFrom:
            secretKeyRef:
              name: api-config
              key: gcs-bucket
```

Then create the secret:
```bash
kubectl create secret generic api-config \
  --from-literal=gcp-project-id=developer-hub-learning \
  --from-literal=gcs-bucket=devhub-storage
```

---

### 10. No Image Vulnerability Scanning

#### Location: backend-deploy.yml & frontend-deploy.yml
**File:** `/home/user/gcp/.github/workflows/backend-deploy.yml` (line ~66)
**File:** `/home/user/gcp/.github/workflows/frontend-deploy.yml` (line ~55)

**Missing:** Container image scanning step

**Severity:** MEDIUM
**Risk:** Unknown vulnerabilities in dependencies

**Fix - Add Trivy scanning:**
```yaml
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:$GITHUB_SHA
        format: 'sarif'
        output: 'trivy-results.sarif'
        severity: 'HIGH,CRITICAL'
        exit-code: '1'  # Fail pipeline if HIGH/CRITICAL found

    - name: Upload Trivy results to GitHub Security
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'
```

---

## MISSING SECURITY CONTROLS

### 11. No Network Policies

**Status:** Not Implemented
**Severity:** HIGH
**Files:** None (should be in `/home/user/gcp/deploy/kubernetes/`)

**Missing Configuration:**
A NetworkPolicy that restricts:
- Pod-to-pod communication
- Ingress to only from ingress controller
- Egress to necessary services only

**Fix - Create network-policy.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: devhub-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: devhub-api
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 443
  - to:
    - podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
```

---

### 12. No Pod Security Standards Enforcement

**Status:** Not Implemented
**Severity:** HIGH
**Files:** None (should be in `/home/user/gcp/deploy/kubernetes/`)

**Missing:** Pod Security Policy or Pod Security Standards

**Fix - Add PSS label to namespace:**
```bash
kubectl label namespace default \
  pod-security.kubernetes.io/enforce=restricted \
  pod-security.kubernetes.io/audit=restricted \
  pod-security.kubernetes.io/warn=restricted
```

---

### 13. No RBAC Configuration

**Status:** Minimal (using default service account)
**Severity:** MEDIUM
**Files:** Should be in `/home/user/gcp/deploy/kubernetes/`

**Missing:** Custom Roles and RoleBindings

---

### 14. No Audit Logging

**Status:** Not Configured
**Severity:** MEDIUM
**Files:** Not in deployment manifests (GKE cluster configuration)

---

## SUMMARY TABLE

| Vulnerability | Severity | File | Line | Status |
|---|---|---|---|---|
| CORS Wildcard | CRITICAL | PortfolioController.java | 22 | Open |
| CORS Wildcard | CRITICAL | SnippetsController.java | 25 | Open |
| CORS Wildcard | CRITICAL | application.properties | 32 | Open |
| No HTTPS | HIGH | ingress.yaml | 1-31 | Open |
| Image not pinned | HIGH | deployment.yaml | 26 | Open |
| Image not pinned | HIGH | Dockerfile | 5, 17 | Open |
| Incomplete sec context | HIGH | deployment.yaml | 82-87 | Open |
| imagePullPolicy missing | MEDIUM | deployment.yaml | 24-31 | Open |
| Ingress not restricted | MEDIUM | ingress.yaml | 12-17 | Open |
| Env vars plaintext | MEDIUM | deployment.yaml | 33-40 | Open |
| No image scanning | MEDIUM | backend-deploy.yml | ~66 | Open |
| No NetworkPolicy | HIGH | N/A (missing) | N/A | Not Implemented |
| No PSS | HIGH | N/A (missing) | N/A | Not Implemented |

---

## Quick Fix Priority Order

1. **IMMEDIATELY (Today)**
   - [ ] Fix CORS configuration (3 files)
   - [ ] Enable HTTPS on ingress

2. **THIS WEEK**
   - [ ] Pin image versions (deployment + Dockerfile)
   - [ ] Add imagePullPolicy
   - [ ] Complete security context

3. **THIS MONTH**
   - [ ] Implement NetworkPolicy
   - [ ] Enforce Pod Security Standards
   - [ ] Add image scanning to CI/CD

4. **BEFORE PRODUCTION**
   - [ ] Configure audit logging
   - [ ] Implement RBAC
   - [ ] Create dedicated namespace

---

## Testing the Fixes

### Test CORS:
```bash
curl -H "Origin: https://evil.com" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -X OPTIONS https://api.yourdomain.com/api/portfolio -v
```
Should NOT include `Access-Control-Allow-Origin: *`

### Test HTTPS:
```bash
curl -I http://api.yourdomain.com/api/portfolio
```
Should redirect to HTTPS (301/302)

### Test Image Version:
```bash
kubectl get deployment devhub-api -o yaml | grep image:
```
Should show commit SHA, not "latest"

---

**Last Updated:** 2025-11-16
**Next Review:** After fixes are implemented

