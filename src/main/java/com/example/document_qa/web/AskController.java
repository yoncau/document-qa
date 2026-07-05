package com.example.document_qa.web;

import com.example.document_qa.query.QaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Answers a question using RAG over the ingested documents.
 * POST /ask   body: { "question": "..." }
 */
@RestController
public class AskController {

    private final QaService qaService;

    public AskController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        String question = request.question();
        if (question == null || question.isBlank()) {
            throw new InvalidRequestException("Field 'question' must not be empty.");
        }

        QaService.Answer answer = qaService.answer(question);
        return new AskResponse(answer.answer(), answer.sources());
    }

    /** Request body: { "question": "..." } */
    public record AskRequest(String question) {}

    /** Response body: { "answer": "...", "sources": [ ... ] } */
    public record AskResponse(String answer, List<String> sources) {}
}
