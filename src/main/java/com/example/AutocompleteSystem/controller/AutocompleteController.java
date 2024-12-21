package com.example.AutocompleteSystem.controller;

import com.example.AutocompleteSystem.service.AutocompleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.typesense.api.Document;
import org.typesense.model.SearchResult;

import java.util.List;

@RestController
@RequestMapping("/api/autocomplete")
public class AutocompleteController {

    @Autowired
    private AutocompleteService autocompleteService;

    @PostMapping("/search")
    public void addSuggestion(@RequestBody String suggestion) {
        autocompleteService.addSuggestion(suggestion);
    }

    @GetMapping
    public List<String> getSuggestions(@RequestParam String str, @RequestParam(defaultValue = "5") int limit) {
        return autocompleteService.getSuggestions(str, limit);
    }
}
