package com.devhub.service;

import com.devhub.model.PortfolioLink;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing portfolio links in Firestore.
 */
@Slf4j
@Service
public class PortfolioService {

    private static final String COLLECTION_NAME = "portfolio";

    @Autowired
    private Firestore firestore;

    /**
     * Get all portfolio links ordered by display order.
     */
    public List<PortfolioLink> getAllLinks() throws ExecutionException, InterruptedException {
        log.info("Fetching all portfolio links");
        List<PortfolioLink> links = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .orderBy("order", Query.Direction.ASCENDING)
                .get();

        for (DocumentSnapshot doc : query.get().getDocuments()) {
            PortfolioLink link = doc.toObject(PortfolioLink.class);
            if (link != null) {
                link.setId(doc.getId());
                links.add(link);
            }
        }

        log.info("Retrieved {} portfolio links", links.size());
        return links;
    }

    /**
     * Get portfolio links by category.
     */
    public List<PortfolioLink> getLinksByCategory(String category) throws ExecutionException, InterruptedException {
        log.info("Fetching portfolio links for category: {}", category);
        List<PortfolioLink> links = new ArrayList<>();

        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("category", category)
                .orderBy("order", Query.Direction.ASCENDING)
                .get();

        for (DocumentSnapshot doc : query.get().getDocuments()) {
            PortfolioLink link = doc.toObject(PortfolioLink.class);
            if (link != null) {
                link.setId(doc.getId());
                links.add(link);
            }
        }

        log.info("Retrieved {} links for category {}", links.size(), category);
        return links;
    }

    /**
     * Get a single portfolio link by ID.
     */
    public PortfolioLink getLinkById(String id) throws ExecutionException, InterruptedException {
        log.info("Fetching portfolio link with ID: {}", id);
        DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(id).get().get();

        if (!doc.exists()) {
            log.warn("Portfolio link not found: {}", id);
            return null;
        }

        PortfolioLink link = doc.toObject(PortfolioLink.class);
        if (link != null) {
            link.setId(doc.getId());
        }
        return link;
    }

    /**
     * Create a new portfolio link.
     */
    public String createLink(PortfolioLink link) throws ExecutionException, InterruptedException {
        log.info("Creating new portfolio link: {}", link.getTitle());
        ApiFuture<DocumentReference> result = firestore.collection(COLLECTION_NAME).add(link);
        String id = result.get().getId();
        log.info("Created portfolio link with ID: {}", id);
        return id;
    }

    /**
     * Update an existing portfolio link.
     */
    public void updateLink(String id, PortfolioLink link) throws ExecutionException, InterruptedException {
        log.info("Updating portfolio link with ID: {}", id);
        firestore.collection(COLLECTION_NAME).document(id).set(link).get();
        log.info("Updated portfolio link: {}", id);
    }

    /**
     * Delete a portfolio link.
     */
    public void deleteLink(String id) throws ExecutionException, InterruptedException {
        log.info("Deleting portfolio link with ID: {}", id);
        firestore.collection(COLLECTION_NAME).document(id).delete().get();
        log.info("Deleted portfolio link: {}", id);
    }
}
