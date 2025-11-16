package com.devhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for Developer Hub Platform.
 *
 * A GCP learning project that combines:
 * - Developer portfolio management
 * - Code snippet manager
 * - Learning journal
 *
 * Deployed on GKE Autopilot with Workload Identity for secure GCP authentication.
 */
@SpringBootApplication
public class DevHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevHubApplication.class, args);
    }
}
