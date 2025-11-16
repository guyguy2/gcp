package com.devhub.service;

import com.devhub.model.CodeSnippet;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing code snippets in Firestore.
 */
@Slf4j
@Service
public class SnippetService {

    private static final String COLLECTION_NAME = "snippets";

    @Autowired
    private Firestore firestore;

    /**
     * Get all snippets ordered by creation date (most recent first).
     */
    public List<CodeSnippet> getAllSnippets() throws ExecutionException, InterruptedException {
        log.info("Fetching all code snippets");
        List<CodeSnippet> snippets = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();

        for (DocumentSnapshot doc : query.get().getDocuments()) {
            CodeSnippet snippet = doc.toObject(CodeSnippet.class);
            if (snippet != null) {
                snippet.setId(doc.getId());
                snippets.add(snippet);
            }
        }

        log.info("Retrieved {} code snippets", snippets.size());
        return snippets;
    }

    /**
     * Get public snippets only (for portfolio display).
     */
    public List<CodeSnippet> getPublicSnippets() throws ExecutionException, InterruptedException {
        log.info("Fetching public code snippets");
        List<CodeSnippet> snippets = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();

        for (DocumentSnapshot doc : query.get().getDocuments()) {
            CodeSnippet snippet = doc.toObject(CodeSnippet.class);
            if (snippet != null) {
                snippet.setId(doc.getId());
                snippets.add(snippet);
            }
        }

        log.info("Retrieved {} public code snippets", snippets.size());
        return snippets;
    }

    /**
     * Get snippets by language.
     */
    public List<CodeSnippet> getSnippetsByLanguage(String language) throws ExecutionException, InterruptedException {
        log.info("Fetching snippets for language: {}", language);
        List<CodeSnippet> snippets = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("language", language)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();

        for (DocumentSnapshot doc : query.get().getDocuments()) {
            CodeSnippet snippet = doc.toObject(CodeSnippet.class);
            if (snippet != null) {
                snippet.setId(doc.getId());
                snippets.add(snippet);
            }
        }

        log.info("Retrieved {} snippets for language {}", snippets.size(), language);
        return snippets;
    }

    /**
     * Get snippets by tag.
     */
    public List<CodeSnippet> getSnippetsByTag(String tag) throws ExecutionException, InterruptedException {
        log.info("Fetching snippets with tag: {}", tag);
        List<CodeSnippet> snippets = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .whereArrayContains("tags", tag)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();

        for (DocumentSnapshot doc : query.get().getDocuments()) {
            CodeSnippet snippet = doc.toObject(CodeSnippet.class);
            if (snippet != null) {
                snippet.setId(doc.getId());
                snippets.add(snippet);
            }
        }

        log.info("Retrieved {} snippets with tag {}", snippets.size(), tag);
        return snippets;
    }

    /**
     * Get a single snippet by ID.
     */
    public CodeSnippet getSnippetById(String id) throws ExecutionException, InterruptedException {
        log.info("Fetching snippet with ID: {}", id);
        DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(id).get().get();

        if (!doc.exists()) {
            log.warn("Snippet not found: {}", id);
            return null;
        }

        CodeSnippet snippet = doc.toObject(CodeSnippet.class);
        if (snippet != null) {
            snippet.setId(doc.getId());
        }
        return snippet;
    }

    /**
     * Create a new code snippet.
     */
    public String createSnippet(CodeSnippet snippet) throws ExecutionException, InterruptedException {
        log.info("Creating new code snippet: {}", snippet.getTitle());

        // Set timestamps
        Timestamp now = Timestamp.now();
        snippet.setCreatedAt(now);
        snippet.setUpdatedAt(now);

        ApiFuture<DocumentReference> result = firestore.collection(COLLECTION_NAME).add(snippet);
        String id = result.get().getId();
        log.info("Created snippet with ID: {}", id);
        return id;
    }

    /**
     * Update an existing snippet.
     */
    public void updateSnippet(String id, CodeSnippet snippet) throws ExecutionException, InterruptedException {
        log.info("Updating snippet with ID: {}", id);

        // Update timestamp
        snippet.setUpdatedAt(Timestamp.now());

        firestore.collection(COLLECTION_NAME).document(id).set(snippet).get();
        log.info("Updated snippet: {}", id);
    }

    /**
     * Delete a snippet.
     */
    public void deleteSnippet(String id) throws ExecutionException, InterruptedException {
        log.info("Deleting snippet with ID: {}", id);
        firestore.collection(COLLECTION_NAME).document(id).delete().get();
        log.info("Deleted snippet: {}", id);
    }
}
