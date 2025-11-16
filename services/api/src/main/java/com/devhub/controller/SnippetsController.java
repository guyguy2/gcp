package com.devhub.controller;

import com.devhub.model.CodeSnippet;
import com.devhub.service.SnippetService;
import com.devhub.service.StorageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * REST controller for code snippet management.
 * Provides CRUD operations and file upload for code snippets.
 */
@Slf4j
@RestController
@RequestMapping("/api/snippets")
@CrossOrigin(origins = "*")  // Configure appropriately for production
public class SnippetsController {

    @Autowired
    private SnippetService snippetService;

    @Autowired
    private StorageService storageService;

    /**
     * GET /api/snippets
     * Get all code snippets.
     */
    @GetMapping
    public ResponseEntity<List<CodeSnippet>> getAllSnippets() {
        try {
            List<CodeSnippet> snippets = snippetService.getAllSnippets();
            return ResponseEntity.ok(snippets);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching snippets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/snippets/public
     * Get only public snippets (for portfolio display).
     */
    @GetMapping("/public")
    public ResponseEntity<List<CodeSnippet>> getPublicSnippets() {
        try {
            List<CodeSnippet> snippets = snippetService.getPublicSnippets();
            return ResponseEntity.ok(snippets);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching public snippets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/snippets/language/{language}
     * Get snippets by programming language.
     */
    @GetMapping("/language/{language}")
    public ResponseEntity<List<CodeSnippet>> getSnippetsByLanguage(@PathVariable String language) {
        try {
            List<CodeSnippet> snippets = snippetService.getSnippetsByLanguage(language);
            return ResponseEntity.ok(snippets);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching snippets by language: {}", language, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/snippets/tag/{tag}
     * Get snippets by tag.
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<CodeSnippet>> getSnippetsByTag(@PathVariable String tag) {
        try {
            List<CodeSnippet> snippets = snippetService.getSnippetsByTag(tag);
            return ResponseEntity.ok(snippets);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching snippets by tag: {}", tag, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/snippets/{id}
     * Get a specific snippet by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CodeSnippet> getSnippetById(@PathVariable String id) {
        try {
            CodeSnippet snippet = snippetService.getSnippetById(id);
            if (snippet == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(snippet);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error fetching snippet: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/snippets
     * Create a new code snippet.
     */
    @PostMapping
    public ResponseEntity<String> createSnippet(@Valid @RequestBody CodeSnippet snippet) {
        try {
            String id = snippetService.createSnippet(snippet);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error creating snippet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/snippets/upload
     * Create a snippet with file upload to GCS.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> createSnippetWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("language") String language,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic) {
        try {
            // Upload file to GCS
            String gcsUrl = storageService.uploadFile(file, "snippets");

            // Read file content for the code field
            String code = new String(file.getBytes());

            // Create snippet
            CodeSnippet snippet = new CodeSnippet();
            snippet.setTitle(title);
            snippet.setCode(code);
            snippet.setLanguage(language);
            snippet.setTags(tags != null ? tags : List.of());
            snippet.setCategory(category);
            snippet.setGcsFileUrl(gcsUrl);
            snippet.setIsPublic(isPublic);

            String id = snippetService.createSnippet(snippet);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (ExecutionException | InterruptedException | IOException e) {
            log.error("Error creating snippet with file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/snippets/{id}
     * Update an existing snippet.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSnippet(@PathVariable String id, @Valid @RequestBody CodeSnippet snippet) {
        try {
            snippetService.updateSnippet(id, snippet);
            return ResponseEntity.ok().build();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error updating snippet: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/snippets/{id}
     * Delete a snippet and its associated GCS file if it exists.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSnippet(@PathVariable String id) {
        try {
            // Get snippet to check for GCS file
            CodeSnippet snippet = snippetService.getSnippetById(id);
            if (snippet != null && snippet.getGcsFileUrl() != null) {
                // Delete file from GCS
                storageService.deleteFile(snippet.getGcsFileUrl());
            }

            // Delete snippet from Firestore
            snippetService.deleteSnippet(id);
            return ResponseEntity.noContent().build();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error deleting snippet: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
