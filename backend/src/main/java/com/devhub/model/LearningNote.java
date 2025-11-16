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
 * Learning note entity stored in Firestore.
 * Represents a learning journal entry with resources and tags.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "learningNotes")
public class LearningNote {

    @DocumentId
    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private List<String> tags = new ArrayList<>();

    private Timestamp date;

    private List<String> resources = new ArrayList<>();  // URLs or references

    private String category;  // e.g., "GCP", "Kubernetes", "Java"

    private Integer difficultyLevel;  // 1-5 scale
}
