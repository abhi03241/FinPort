package com.artha.app.services.ai;

import com.artha.app.models.Category;
import com.artha.app.services.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Suggests a category for a transaction description using the configured
 * {@link AiClient}. Falls back gracefully when the LLM is unavailable
 * or returns something we can't match to a known category.
 */
@Service
public class AutoCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(AutoCategorizationService.class);

    private final AiClient aiClient;
    private final CategoryService categoryService;

    public AutoCategorizationService(AiClient aiClient, CategoryService categoryService) {
        this.aiClient = aiClient;
        this.categoryService = categoryService;
    }

    public record Suggestion(Long categoryId, String categoryName, double confidence, String provider) {}

    public Suggestion suggest(String description, Double amount) {
        if (description == null || description.isBlank()) return null;

        List<Category> categories = categoryService.getAllCategories();
        String categoryList = categories.stream()
                .map(c -> "- " + c.getName() + " (id=" + c.getId() + ")")
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String system = """
                You are a personal-finance categorizer. Given a transaction description
                (and optional amount), pick the SINGLE most appropriate category from the
                user's list. Reply with strict JSON: {"category":"<name>","confidence":<0..1>}.
                Use only category names from the list — if none fit, return {"category":null}.
                """;
        String user = "Description: " + description
                + (amount != null ? "\nAmount: " + amount : "")
                + "\nCategories:\n" + categoryList;

        String completion = aiClient.complete(system, user);
        if (completion == null) return null;

        String suggestedName = StubAiClient.extractCategoryName(completion);
        if (suggestedName == null || "null".equalsIgnoreCase(suggestedName)) return null;

        Double confidence = extractConfidence(completion);

        String normalized = suggestedName.trim().toLowerCase(Locale.ROOT);
        for (Category c : categories) {
            if (c.getName().toLowerCase(Locale.ROOT).equals(normalized)) {
                return new Suggestion(c.getId(), c.getName(),
                        confidence != null ? confidence : 0.5, aiClient.name());
            }
        }
        log.debug("LLM suggested category '{}' which is not in the user's list", suggestedName);
        return null;
    }

    private Double extractConfidence(String completion) {
        try {
            int i = completion.indexOf("\"confidence\"");
            if (i < 0) return null;
            int colon = completion.indexOf(':', i);
            int end = completion.length();
            for (int j = colon + 1; j < completion.length(); j++) {
                char ch = completion.charAt(j);
                if (ch == ',' || ch == '}') {
                    end = j;
                    break;
                }
            }
            String token = completion.substring(colon + 1, end).trim();
            return Double.parseDouble(token);
        } catch (Exception ex) {
            return null;
        }
    }
}