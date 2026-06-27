package com.artha.app.services.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * No-op {@link AiClient} used when no OPENAI_API_KEY is configured or in tests.
 * Implements trivial keyword-based category suggestion for the auto-categorize
 * flow so the rest of the app still works without a real LLM.
 *
 * Keyword dictionary is intentionally tiny — the LLM-backed client is what
 * users actually want for production accuracy.
 */
@Component
@ConditionalOnMissingBean(name = "aiClient")
public class StubAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(StubAiClient.class);

    /** Ordered keyword -> category-name matchers. First hit wins. */
    private static final String[][] KEYWORDS = {
            {"swiggy|zomato|restaurant|cafe|food|pizza|burger", "Food"},
            {"uber|ola|rapido|fuel|petrol|gas|metro",        "Transport"},
            {"amazon|flipkart|myntra|shopping|store",        "Shopping"},
            {"netflix|spotify|hotstar|prime|subscription",   "Entertainment"},
            {"electricity|water|gas bill|recharge|broadband|internet|wifi", "Utilities"},
            {"rent|maintenance|society",                     "Housing"},
            {"salary|payroll|paycheck|invoice received",     "Salary"},
            {"gym|yoga|doctor|medical|pharmacy|health",      "Health"},
            {"school|tuition|books|course",                  "Education"},
    };

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("\"category\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        // Best-effort keyword match for the categorize prompt
        String lower = userPrompt == null ? "" : userPrompt.toLowerCase();
        for (String[] entry : KEYWORDS) {
            if (lower.matches(".*(" + entry[0] + ").*")) {
                log.debug("Stub categorized prompt as '{}' via keyword '{}'", entry[1], entry[0]);
                return "{\"category\":\"" + entry[1] + "\",\"confidence\":0.55}";
            }
        }
        return null;
    }

    @Override
    public String name() {
        return "stub";
    }

    /** Helper for callers that want to parse a stub response defensively. */
    public static String extractCategoryName(String completion) {
        if (completion == null) return null;
        Matcher m = CATEGORY_PATTERN.matcher(completion);
        return m.find() ? m.group(1) : null;
    }
}