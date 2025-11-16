# Deployment Security Audit Report
## Developer Hub Platform - GCP Learning Project

**Report Date:** November 16, 2025
**Scope:** Deploy directory, GitHub Actions workflows, Docker configurations

---

## Executive Summary

The deployment architecture demonstrates **good foundational security practices**, particularly around authentication and secrets management. However, there are **several vulnerabilities** that require immediate attention, especially around CORS configuration and public data exposure. The overall security posture is **MEDIUM-HIGH** with critical issues that must be addressed before production deployment.

---

## 1. KUBERNETES YAML CONFIGURATIONS

### 1.1 Deployment Security Context Analysis

**File:** `/home/user/gcp/deploy/kubernetes/deployment.yaml`

#### Strengths:
- ✅ Non-root user enforcement: `runAsNonRoot: true`, `runAsUser: 1000`
- ✅ File system group set: `fsGroup: 1000`
- ✅ Resource limits defined: Memory (512Mi-1Gi), CPU (250m-500m)
- ✅ Health checks configured (liveness, readiness, startup probes)

#### Critical Issues:

**[CRITICAL] CORS Configuration - Wildcard Origins**
```yaml
@CrossOrigin(origins = "*")  # Line 22 in PortfolioController.java
@CrossOrigin(origins = "*")  # Line 25 in SnippetsController.java
cors.allowed-origins=*       # application.properties
```
- **Severity:** HIGH
- **Risk:** Allows any domain to make cross-origin requests to your API
- **Impact:** Enables CSRF attacks, unauthorized access, data exfiltration
- **Recommendation:** Specify exact origins (e.g., `https://yourdomain.com`)

**[MEDIUM] Missing imagePullPolicy**
```yaml
image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:latest
```
- **Severity:** MEDIUM
- **Risk:** Without explicit `imagePullPolicy: Always`, pods might run outdated images
- **Recommendation:** Set `imagePullPolicy: IfNotPresent` or better, use commit SHAs instead of `latest`

**[MEDIUM] "latest" Image Tag**
- **Severity:** MEDIUM
- **Risk:** No version pinning; difficult to troubleshoot issues or rollback
- **Recommendation:** Use `devhub-api:$GITHUB_SHA` (commit hash) for reproducible deployments

**[MEDIUM] Incomplete Security Context**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  # Missing fields:
```
- **Severity:** MEDIUM
- **Missing:** 
  - `readOnlyRootFilesystem: true`
  - `allowPrivilegeEscalation: false`
  - `capabilities: drop: ["ALL"]`
- **Recommendation:** Add these fields to reduce attack surface

### 1.2 Service Configuration Analysis

**File:** `/home/user/gcp/deploy/kubernetes/service.yaml`

- ✅ **Good:** Uses ClusterIP (not LoadBalancer/NodePort) - minimizes exposure
- ✅ Good port naming: `name: http`
- ⚠️ **Note:** Service is internal-only, which is correct for API

### 1.3 Ingress Configuration Analysis

**File:** `/home/user/gcp/deploy/kubernetes/ingress.yaml`

#### Critical Issues:

**[HIGH] No HTTPS/TLS Configuration**
```yaml
# Commented out SSL certificate configuration:
# networking.gke.io/managed-certificates: "devhub-cert"
# kubernetes.io/ingress.allow-http: "false"
```
- **Severity:** HIGH
- **Risk:** API traffic transmitted in plain HTTP; susceptible to MITM attacks
- **Recommendation:** 
  1. Provision SSL certificate (Google-managed or custom)
  2. Configure managed-certificates annotation
  3. Force HTTPS redirects

**[MEDIUM] Unrestricted Ingress Access**
```yaml
spec:
  defaultBackend:
    service:
      name: devhub-api-svc
```
- **Severity:** MEDIUM
- **Risk:** All traffic reaches API without hostname restrictions
- **Recommendation:** Configure ingress rules with host and path restrictions

### 1.4 Workload Identity Configuration

**File:** `/home/user/gcp/deploy/kubernetes/workload-identity.yaml`

#### Strengths:
- ✅ Excellent security model: Workload Identity with no JSON keys
- ✅ Proper service account annotation for GCP binding
- ✅ Removes need for secret management for GCP credentials

#### Minor Issues:
- ⚠️ `PROJECT_ID` placeholder needs substitution (workflow handles this)

---

## 2. GITHUB ACTIONS SECURITY ANALYSIS

### 2.1 Backend Deployment Workflow

**File:** `.github/workflows/backend-deploy.yml`

#### Strengths:
- ✅ Uses Workload Identity Federation (no service account keys)
- ✅ Proper permissions scoping:
  ```yaml
  permissions:
    contents: 'read'
    id-token: 'write'
  ```
- ✅ No hardcoded credentials anywhere
- ✅ Maven tests run before deployment
- ✅ Uses GitHub Actions official actions (well-maintained)

#### Issues:

**[MEDIUM] "latest" Tag in CI/CD**
```yaml
-t ${{ secrets.ARTIFACT_REGISTRY }}/$PROJECT_ID/devhub-repo/$IMAGE:latest
```
- **Severity:** MEDIUM
- **Risk:** Overwrites previous images; no version history
- **Recommendation:** Keep both tags - use commit SHA as primary

**[MEDIUM] Maven Build Skips Tests in Package Phase**
```yaml
run: mvn clean package -DskipTests  # Line 52
run: mvn test                       # Line 56 (separate step)
```
- **Concern:** Tests run after packaging; if they fail, image still pushed
- **Recommendation:** Fail fast - don't package if tests will fail

### 2.2 Frontend Deployment Workflow

**File:** `.github/workflows/frontend-deploy.yml`

#### Critical Issues:

**[CRITICAL] Public Bucket Permissions Set Automatically**
```yaml
- name: Make bucket public (if needed)
  run: |
    gsutil iam ch allUsers:objectViewer gs://${{ env.FRONTEND_BUCKET }}
```
- **Severity:** CRITICAL
- **Risk:** Makes entire frontend bucket publicly readable (intended for frontend)
- **Concern:** No access control; anyone can access all files

**[HIGH] API URL Exposed in Frontend Build**
```yaml
env:
  REACT_APP_API_URL: ${{ secrets.API_URL }}
```
- **Severity:** MEDIUM
- **Risk:** API URL embedded in built JavaScript is publicly visible
- **Concern:** Allows direct API targeting by attackers
- **Note:** This is acceptable for public APIs but document it

#### Strengths:
- ✅ Cache control headers set appropriately:
  - Static assets: 1 year cache
  - index.html: no-cache
- ✅ Uses Workload Identity Federation
- ✅ Proper npm ci (instead of npm install)

---

## 3. DOCKER IMAGE SECURITY

### 3.1 Dockerfile Security Analysis

**File:** `/home/user/gcp/services/api/Dockerfile`

#### Strengths:
- ✅ Multi-stage build (reduces final image size)
- ✅ Non-root user: `adduser -S spring -G spring`
- ✅ Alpine Linux base (smaller attack surface)
- ✅ Health check configured
- ✅ Proper JVM memory configuration: `-XX:MaxRAMPercentage=75.0`

#### Issues:

**[MEDIUM] Base Image Not Pinned to Specific Version**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
FROM eclipse-temurin:17-jre-alpine
```
- **Severity:** MEDIUM
- **Risk:** Latest minor versions pulled; no reproducibility
- **Recommendation:** Use specific digests:
  ```dockerfile
  FROM maven:3.9-eclipse-temurin-17@sha256:... AS build
  FROM eclipse-temurin:17-jre-alpine@sha256:...
  ```

**[MEDIUM] No SBOM or Vulnerability Scanning**
- **Severity:** MEDIUM
- **Recommendation:** Add Trivy or Snyk scanning in CI/CD

### 3.2 .dockerignore Configuration

**File:** `/home/user/gcp/services/api/.dockerignore`

- ✅ Good: Excludes IDE files, Maven build artifacts, git directory
- ✅ Excludes documentation and logs
- ⚠️ Could also exclude `.git/` and test directories

---

## 4. SECRETS AND CREDENTIALS ANALYSIS

### 4.1 GitHub Secrets Used

**Required Secrets (Properly Managed):**
1. `GCP_PROJECT_ID` - Non-sensitive
2. `GKE_CLUSTER` - Non-sensitive
3. `GKE_REGION` - Non-sensitive
4. `ARTIFACT_REGISTRY` - Non-sensitive
5. `WIF_PROVIDER` - Workload Identity Federation Provider
6. `WIF_SERVICE_ACCOUNT` - Service account email
7. `FRONTEND_BUCKET` - Bucket name
8. `API_URL` - API endpoint (embedded in frontend)

#### Assessment:
- ✅ **EXCELLENT:** No service account keys stored
- ✅ **EXCELLENT:** No hardcoded credentials in YAML files
- ✅ **EXCELLENT:** Using Workload Identity Federation

### 4.2 Application Configuration

**File:** `/home/user/gcp/services/api/src/main/resources/application.properties`

```properties
gcp.storage.bucket=${GCS_BUCKET:devhub-storage}
spring.cloud.gcp.project-id=${GCP_PROJECT_ID:developer-hub-learning}
cors.allowed-origins=*  # ← CRITICAL ISSUE
```

#### Issues:
- ⚠️ Default values in properties (okay for non-secrets)
- 🔴 **CRITICAL:** CORS wildcard configuration hardcoded
- ⚠️ Logging level set to INFO for `com.google.cloud` (consider WARN)

### 4.3 .gitignore Coverage

**File:** `/home/user/gcp/.gitignore`

- ✅ Excellent coverage:
  - `*.json` (catches service account keys)
  - `.env*` files
  - `credentials/` directory
  - `service-account*.json`
- ✅ `frontend/.env.local` and similar patterns

#### Assessment:
- ✅ **EXCELLENT:** Comprehensive secret protection

---

## 5. MISSING SECURITY CONTROLS

### 5.1 Network Policies

**Status:** NOT IMPLEMENTED

- ❌ No NetworkPolicy resources found
- **Risk:** All pods can communicate with each other
- **Recommendation:** 
  ```yaml
  apiVersion: networking.k8s.io/v1
  kind: NetworkPolicy
  metadata:
    name: devhub-network-policy
  spec:
    podSelector:
      matchLabels:
        app: devhub-api
    policyTypes:
    - Ingress
    ingress:
    - from:
      - podSelector:
          matchLabels:
            role: frontend
      ports:
      - protocol: TCP
        port: 8080
  ```

### 5.2 Pod Security Standards

**Status:** NOT ENFORCED

- ❌ No PodSecurityPolicy or Pod Security Standards
- **Recommendation:** Enforce restricted PSS in default namespace

### 5.3 RBAC Configuration

**Status:** MINIMAL

- ⚠️ Using default namespace and service account
- **Recommendation:** 
  - Create dedicated namespace for application
  - Configure Role/RoleBinding for minimal permissions

### 5.4 Secrets Management

**Status:** PARTIALLY IMPLEMENTED

- ✅ No secrets stored in Kubernetes YAML files
- ❌ Environment variables passed as plaintext in deployment
- **Recommendation:** Use Kubernetes Secrets for sensitive data:
  ```yaml
  valueFrom:
    secretKeyRef:
      name: api-secrets
      key: gcp-project-id
  ```

### 5.5 Audit Logging

**Status:** NOT CONFIGURED

- ❌ No audit logging mentioned
- **Recommendation:** Enable GKE audit logging in cluster

---

## 6. COMPLIANCE AND BEST PRACTICES

### 6.1 Security Best Practices Checklist

| Control | Status | Evidence |
|---------|--------|----------|
| Non-root user | ✅ PASS | runAsUser: 1000 |
| Resource limits | ✅ PASS | 512Mi-1Gi memory, 250m-500m CPU |
| Health checks | ✅ PASS | Liveness, readiness, startup probes |
| No hardcoded secrets | ✅ PASS | All secrets in GitHub Secrets |
| Workload Identity | ✅ PASS | Using WIF, no JSON keys |
| Image scanning | ❌ FAIL | No Trivy/Snyk integration |
| HTTPS enforced | ❌ FAIL | HTTP only ingress |
| CORS restricted | ❌ FAIL | Wildcard origins |
| NetworkPolicy | ❌ FAIL | Not implemented |
| Pod Security Policy | ❌ FAIL | Not enforced |
| Regular updates | ⚠️ PARTIAL | Using "latest" tags |
| Vulnerability scanning | ❌ FAIL | Not implemented |
| Secret rotation | ⚠️ UNKNOWN | Not documented |
| Access logging | ❌ FAIL | Not configured |

---

## 7. RECOMMENDATIONS BY PRIORITY

### CRITICAL (Fix Immediately)

1. **Restrict CORS Origins**
   - Change `@CrossOrigin(origins = "*")` to specific domains
   - Update `cors.allowed-origins` configuration
   - Files: PortfolioController.java, SnippetsController.java, application.properties

2. **Enable HTTPS on Ingress**
   - Provision SSL certificate
   - Enable managed-certificates annotation
   - Force HTTP→HTTPS redirect

3. **Document Public Frontend Access**
   - The frontend bucket IS supposed to be public
   - Document this design decision
   - Ensure no sensitive data in frontend

### HIGH (Fix Before Production)

4. **Pin Image Versions**
   - Replace "latest" with commit SHA ($GITHUB_SHA)
   - Pin base images to specific digest hashes
   - Update deployment strategy

5. **Implement NetworkPolicy**
   - Restrict pod-to-pod communication
   - Allow ingress only from ingress controller
   - Deny egress to unnecessary services

6. **Add Pod Security Standards**
   - Enforce restricted PSS in default namespace
   - Configure security context completeness
   - Add `readOnlyRootFilesystem: true`

### MEDIUM (Fix Before GA)

7. **Move to Dedicated Namespace**
   - Create `devhub` namespace
   - Configure RBAC roles
   - Separate from default namespace

8. **Implement Image Scanning**
   - Add Trivy or Snyk scanning
   - Fail pipeline on HIGH/CRITICAL vulnerabilities
   - Store SBOM artifacts

9. **Use Kubernetes Secrets for Configuration**
   - Move environment variables to Secrets
   - Update deployment to use valueFrom
   - Encrypt Secrets at rest

10. **Add Audit Logging**
    - Enable GKE audit logging
    - Configure log retention
    - Set up alerting for suspicious activities

### LOW (Enhancement)

11. **Use Specific Docker Build Stages**
    - Pin Maven to specific version (3.9.1)
    - Pin Java versions with digests

12. **Implement Secret Rotation**
    - Document rotation schedule
    - Automate WIF credential rotation

13. **Add Security Headers**
    - Implement middleware for CORS, CSP, HSTS
    - Add to Spring Boot configuration

---

## 8. DEPLOYMENT WORKFLOW SECURITY SUMMARY

### Current Flow:
```
GitHub Push → GitHub Actions Workflow
  ↓
Authenticate via Workload Identity Federation (NO KEYS!)
  ↓
Build & Test (Maven/npm)
  ↓
Build Docker Image
  ↓
Push to Artifact Registry
  ↓
Deploy to GKE via kubectl
```

### Security Strengths:
- ✅ No static credentials stored
- ✅ Workload Identity Federation for cloud auth
- ✅ Tests run before deployment
- ✅ Proper permission scoping (read contents, write id-token)

### Improvements Needed:
- ❌ Add SBOM/SCA scanning
- ❌ Pin Docker base image versions
- ❌ Add security scanning in pipeline
- ❌ Document deployment rollback procedures

---

## 9. INCIDENT RESPONSE RECOMMENDATIONS

1. **Compromise Scenario - Workload Identity Compromised:**
   - Workload Identity principal has minimal scope (storage.user, datastore.user)
   - Rapid credential rotation possible via WIF
   - No long-lived keys to rotate

2. **Compromise Scenario - Container Image Compromised:**
   - Always use commit SHA tags for quick identification
   - Maintain deployment history for rollback
   - Implement ImagePolicyWebhook (admission control)

3. **Compromise Scenario - CORS Misuse:**
   - Update CORS origins immediately
   - Review API access logs for anomalies
   - Implement rate limiting on API endpoints

---

## 10. FILE-BY-FILE SECURITY CHECKLIST

### Kubernetes Files

✅ **deployment.yaml** - 7/10 Security Score
- [x] Non-root user
- [x] Resource limits
- [x] Health checks
- [ ] Complete security context
- [ ] Image version pinning
- [ ] imagePullPolicy specified
- [ ] CORS issue in code

❌ **ingress.yaml** - 4/10 Security Score
- [ ] No HTTPS/TLS
- [ ] No hostname restrictions
- [ ] No rate limiting
- [x] Correct ingress class

✅ **service.yaml** - 9/10 Security Score
- [x] Internal ClusterIP type
- [x] Proper port naming
- [ ] Could add network policy selector

✅ **workload-identity.yaml** - 10/10 Security Score
- [x] Proper WIF binding
- [x] No credentials stored
- [x] Best practice for GCP auth

### CI/CD Files

✅ **backend-deploy.yml** - 8/10 Security Score
- [x] WIF authentication
- [x] Proper permissions
- [x] No hardcoded secrets
- [x] Test execution
- [ ] Latest tag used
- [ ] No container scanning

⚠️ **frontend-deploy.yml** - 7/10 Security Score
- [x] WIF authentication
- [x] Cache headers set correctly
- [x] npm ci used
- [ ] Public bucket access documented
- [ ] No SCA scanning

### Application Files

❌ **PortfolioController.java** - 6/10 Security Score
- [ ] CORS wildcard (CRITICAL)
- [x] Input validation via @Valid
- [x] Proper error handling
- [x] Logging enabled

❌ **SnippetsController.java** - 6/10 Security Score
- [ ] CORS wildcard (CRITICAL)
- [x] File upload handling
- [x] Error handling
- [ ] File size limits enforced

✅ **application.properties** - 6/10 Security Score
- [ ] CORS wildcard configuration
- [x] No hardcoded credentials
- [x] Actuator properly configured
- [x] Logging levels appropriate
- [ ] Consider HTTPS requirements

✅ **Dockerfile** - 8/10 Security Score
- [x] Multi-stage build
- [x] Non-root user
- [x] Alpine base
- [x] Health checks
- [ ] Image version pinning
- [ ] No secrets in RUN commands

---

## 11. COMPARISON TO GCP BEST PRACTICES

| GCP Recommended Control | Implemented | Notes |
|------------------------|-------------|-------|
| Workload Identity | ✅ YES | Excellent - no JSON keys |
| Config Connector | ❌ NO | Planned for Phase 5 |
| Container Analysis/Vulnerability Scanning | ❌ NO | Should be added |
| Binary Authorization | ❌ NO | Consider for prod |
| Cloud Armor | ❌ NO | Consider for ingress |
| VPC Service Controls | ❌ NO | Not required for learning |
| Confidential Computing | ❌ NO | Not required for learning |
| CMEK Encryption | ❌ NO | GCP managed keys sufficient |
| Secret Manager | ❌ NO | GitHub Secrets adequate for now |

---

## 12. CONCLUSION

**Overall Security Rating: 6.5/10**

### Summary:
- **Strong:** Secrets management, authentication (Workload Identity), CI/CD pipeline
- **Weak:** CORS configuration, HTTPS enforcement, network policies, image versioning
- **Missing:** Container scanning, pod security standards, audit logging

### For Learning Project Status: ✅ ACCEPTABLE
The project is suitable for learning with noted vulnerabilities documented.

### For Production Deployment: ❌ NOT READY
Must address critical issues (CORS, HTTPS, image pinning) before production use.

### Estimated Remediation Time:
- **Critical items:** 4-6 hours
- **High priority:** 8-12 hours
- **Medium priority:** 12-16 hours
- **Total:** 2-3 days of focused work

---

## APPENDIX: Quick Fix Commands

### Fix CORS in Controllers:
```java
@CrossOrigin(origins = {"https://yourdomain.com", "https://www.yourdomain.com"})
```

### Fix Ingress YAML:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: devhub-ingress
  annotations:
    kubernetes.io/ingress.class: "gce"
    networking.gke.io/managed-certificates: "devhub-cert"
spec:
  defaultBackend:
    service:
      name: devhub-api-svc
      port:
        number: 80
```

### Fix Deployment Image:
```yaml
image: us-central1-docker.pkg.dev/PROJECT_ID/devhub-repo/devhub-api:$GITHUB_SHA
imagePullPolicy: IfNotPresent
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

**Report Generated:** 2025-11-16
**Audit Performed By:** Claude Code Security Analysis
**Next Review Recommended:** After implementing critical fixes

