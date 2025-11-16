package com.devhub.controller;

import com.devhub.model.PortfolioLink;
import com.devhub.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * REST controller for portfolio link management.
 * Provides CRUD operations for portfolio links.
 */
@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")  // Configure appropriately for production
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    /**
     * GET /api/portfolio
     * Get all portfolio links ordered by display order.
     */
    @GetMapping
    public ResponseEntity<List<PortfolioLink>> getAllLinks() {
        try {
            List<PortfolioLink> links = portfolioService.getAllLinks();
            return ResponseEntity.ok(links);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching portfolio links", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/portfolio/category/{category}
     * Get portfolio links by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<PortfolioLink>> getLinksByCategory(@PathVariable String category) {
        try {
            List<PortfolioLink> links = portfolioService.getLinksByCategory(category);
            return ResponseEntity.ok(links);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching portfolio links by category: {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/portfolio/{id}
     * Get a specific portfolio link by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioLink> getLinkById(@PathVariable String id) {
        try {
            PortfolioLink link = portfolioService.getLinkById(id);
            if (link == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(link);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching portfolio link: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/portfolio
     * Create a new portfolio link.
     */
    @PostMapping
    public ResponseEntity<String> createLink(@Valid @RequestBody PortfolioLink link) {
        try {
            String id = portfolioService.createLink(link);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error creating portfolio link", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/portfolio/{id}
     * Update an existing portfolio link.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLink(@PathVariable String id, @Valid @RequestBody PortfolioLink link) {
        try {
            portfolioService.updateLink(id, link);
            return ResponseEntity.ok().build();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error updating portfolio link: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/portfolio/{id}
     * Delete a portfolio link.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable String id) {
        try {
            portfolioService.deleteLink(id);
            return ResponseEntity.noContent().build();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error deleting portfolio link: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
