package com.example.AutocompleteSystem.service;

import com.example.AutocompleteSystem.model.Suggestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.model.*;
import org.typesense.resources.Node;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutocompleteService {

    private static final String COLLECTION_NAME = "autocomplete";
    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);
    private final Client client;
    private ObjectMapper objectMapper;

    @Autowired
    public AutocompleteService() {
        objectMapper = new ObjectMapper();
        List<Node> nodes = new ArrayList<>();
        nodes.add(
                new Node(
                        "http",
                        "localhost",
                        "8108"
                )
        );
        Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2), "sahilkanojiya");
        this.client = new Client(configuration);
        log.info("client set successfully");
        createCollectionIfNotExists();
    }

    private void createCollectionIfNotExists() {
        try {
            client.collections(COLLECTION_NAME).retrieve();
            log.info("Collection '{}' already exists.", COLLECTION_NAME);
        } catch (org.typesense.api.exceptions.ObjectNotFound e) {
            log.info("Collection '{}' does not exist. Creating now...", COLLECTION_NAME);
            CollectionSchema collectionSchema = new CollectionSchema();
            collectionSchema.name(COLLECTION_NAME);
            List<Field> fields = List.of(
                    new Field().name("suggestion").type("string").optional(false).infix(true),
                    new Field().name("score").type("int32").optional(false)
            );
            collectionSchema.setFields(fields);
            collectionSchema.setDefaultSortingField("score");
            try {
                client.collections().create(collectionSchema);
                log.info("Collection '{}' created successfully.", COLLECTION_NAME);
            } catch (Exception ex) {
                log.error("Failed to create collection '{}': {}", COLLECTION_NAME, ex.getMessage(), ex);
                throw new RuntimeException("Collection creation failed", ex);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve collection '{}': {}", COLLECTION_NAME, e.getMessage(), e);
            throw new RuntimeException("Failed to check collection existence", e);
        }
    }

    public void addSuggestion(String suggestion) {
        try {
            SearchParameters searchParameters = new SearchParameters()
                    .q(suggestion)
                    .queryBy("suggestion")
                    .prefix("false");
            SearchResult searchResult = client.collections(COLLECTION_NAME).documents().search(searchParameters);
            if (!searchResult.getHits().isEmpty()) {
                incrementScore(suggestion);
            } else {
                Map<String, Object> document = new HashMap<>();
                document.put("suggestion", suggestion);
                document.put("score", 1);
                client.collections(COLLECTION_NAME).documents().upsert(document);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getSuggestions(String query, int limit) {
        System.out.println(query);
        SearchParameters searchParameters = new SearchParameters()
                .q(query)
                .queryBy("suggestion")
                .sortBy("score:desc")
                .prefix("true")
                .limit(limit);
        try {
            SearchResult searchResult = client.collections(COLLECTION_NAME).documents().search(searchParameters);
            List<String> suggestions = searchResult.getHits().stream()
                    .map(hit -> {
                        String jsonString = hit.getDocument().get("suggestion").toString();
                        return getSuggestionValue(jsonString);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            System.out.println(suggestions);
            return suggestions;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static String getSuggestionValue(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(jsonString);
        try {
            Suggestion suggestion = objectMapper.readValue(jsonString, Suggestion.class);
            return suggestion.getSuggestion();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void incrementScore(String suggestion) {
        try {
            SearchParameters searchParameters = new SearchParameters()
                    .q(suggestion)
                    .queryBy("suggestion")
                    .prefix("false");
            SearchResult searchResult = client.collections(COLLECTION_NAME).documents().search(searchParameters);
            if (!searchResult.getHits().isEmpty()) {
                Map<String, Object> currentDoc = searchResult.getHits().get(0).getDocument();
                int currentScore = (int) currentDoc.get("score");
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("score", currentScore + 1);
                client.collections(COLLECTION_NAME).documents(currentDoc.get("id").toString()).update(updateMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}





