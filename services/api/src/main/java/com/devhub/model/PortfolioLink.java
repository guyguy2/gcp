package com.devhub.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Portfolio link entity stored in Firestore.
 * Represents a link to external resources (GitHub, LinkedIn, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "portfolio")
public class PortfolioLink {

    @DocumentId
    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "Order is required")
    private Integer order;

    private String category;  // e.g., "GitHub", "LinkedIn", "Blog"

    private String icon;  // Icon name or URL for frontend display

    private String description;  // Optional description
}
