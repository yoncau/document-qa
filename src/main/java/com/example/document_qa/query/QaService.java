package com.example.document_qa.query;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The query side of RAG. For a question we:
 *   1. retrieve the most relevant chunks from the vector store (semantic search),
 *   2. let Claude answer using only those chunks as context,
 *   3. also return which source documents were used, for transparency.
 *
 * The QuestionAnswerAdvisor handles retrieve-and-augment automatically: it embeds
 * the question, searches the vector store, and injects the retrieved chunks into
 * the prompt before it reaches Claude. Its default template also tells the model
 * to answer only from the context and to say it doesn't know otherwise - which is
 * exactly the grounded behaviour we want from RAG.
 */
@Service
public class QaService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // How many chunks to retrieve, and the minimum similarity to accept.
    private static final int TOP_K = 4;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    public QaService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    public Answer answer(String question) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        // Ask Claude, with the RAG advisor injecting retrieved context.
        String reply = chatClient.prompt()
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(searchRequest)
                        .build())
                .user(question)
                .call()
                .content();

        // Separately, retrieve the same chunks so we can report the sources used.
        // (The advisor does this internally; we repeat it to expose the sources.)
        List<Document> retrieved = vectorStore.similaritySearch(searchRequest);
        List<String> sources = retrieved.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "unknown"))
                .distinct()
                .toList();

        return new Answer(reply, sources);
    }

    /** The answer plus the list of source documents it drew from. */
    public record Answer(String answer, List<String> sources) {}
}
