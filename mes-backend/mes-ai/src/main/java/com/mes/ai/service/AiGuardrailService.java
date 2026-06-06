package com.mes.ai.service;

public interface AiGuardrailService {

    GuardrailDecision inspectQuestion(String question);

    String sanitizeAnswer(String answer);
}
