import React from 'react';

function Home() {
  return (
    <div>
      <h1>Welcome to Developer Hub</h1>

      <div className="card">
        <h2>About This Platform</h2>
        <p>
          Developer Hub is a comprehensive learning platform built on Google Cloud Platform (GCP).
          It combines:
        </p>
        <ul>
          <li><strong>Portfolio Management:</strong> Showcase your projects and links</li>
          <li><strong>Code Snippet Manager:</strong> Store and organize your code snippets</li>
          <li><strong>Learning Journal:</strong> Document your learning journey</li>
        </ul>
      </div>

      <div className="card">
        <h2>Technology Stack</h2>
        <ul>
          <li><strong>Backend:</strong> Java Spring Boot with GCP Firestore & Cloud Storage</li>
          <li><strong>Frontend:</strong> React hosted on Google Cloud Storage</li>
          <li><strong>Infrastructure:</strong> GKE Autopilot (Kubernetes)</li>
          <li><strong>CI/CD:</strong> GitHub Actions with Workload Identity Federation</li>
          <li><strong>Security:</strong> Workload Identity (no JSON keys)</li>
        </ul>
      </div>

      <div className="card">
        <h2>Features</h2>
        <ul>
          <li>RESTful API with Spring Boot</li>
          <li>Secure GCP authentication using Workload Identity</li>
          <li>Automated deployments with GitHub Actions</li>
          <li>Scalable Kubernetes deployment on GKE Autopilot</li>
          <li>Cloud-native logging and monitoring</li>
          <li>Free tier optimized architecture</li>
        </ul>
      </div>

      <div className="card">
        <h2>Getting Started</h2>
        <p>
          Navigate to <strong>Portfolio</strong> to view and manage your portfolio links, or
          go to <strong>Code Snippets</strong> to start organizing your code snippets.
        </p>
      </div>
    </div>
  );
}

export default Home;
