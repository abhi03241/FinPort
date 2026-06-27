package com.artha.app.services.ai;

/**
 * Thin abstraction over a generative-AI completion provider. Returning
 * {@code null} signals "I have no idea" and lets callers fall back to
 * deterministic behaviour (e.g. a default category).
 */
public interface AiClient {

    /**
     * Returns a model completion for the given conversation, or {@code null}
     * on hard failure (network, auth, rate-limit). Implementations should
     * never throw — failures degrade gracefully.
     */
    String complete(String systemPrompt, String userPrompt);

    /** Diagnostic name of this provider (e.g. "stub", "openai"). */
    String name();
}