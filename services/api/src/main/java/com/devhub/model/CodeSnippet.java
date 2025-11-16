package com.devhub.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Code snippet entity stored in Firestore.
 * Represents a code snippet with metadata for the snippet manager.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "snippets")
public class CodeSnippet {

    @DocumentId
    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Language is required")
    private String language;  // e.g., "java", "python", "javascript"

    private List<String> tags = new ArrayList<>();  // Tags for categorization

    private Timestamp createdAt;  // Firestore timestamp

    private Timestamp updatedAt;  // Last modification timestamp

    private String category;  // e.g., "algorithms", "utilities", "patterns"

    private String gcsFileUrl;  // Optional URL to full file in GCS

    private Boolean isPublic = false;  // Whether to display in portfolio

    private String description;  // Optional description

    private String author;  // Optional author name
}
