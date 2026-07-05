package com.example.document_qa.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The ingestion (ETL) pipeline: read a document, split it into chunks,
 * and store the chunks in the vector store (which embeds them on the way in).
 *
 * The three ETL stages map to three Spring AI components:
 *   Extract   -> TikaDocumentReader (handles PDF, DOCX, TXT, HTML, ...)
 *   Transform -> TokenTextSplitter  (splits text into token-sized chunks)
 *   Load      -> VectorStore        (embeds each chunk and stores the vector)
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // Default splitter settings are a sensible starting point; chunk size
        // is a real tuning knob (too big = imprecise retrieval, too small =
        // lost context). We can revisit this later.
        this.splitter = new TokenTextSplitter();
    }

    /**
     * Ingest one document.
     *
     * @param resource   the uploaded file, wrapped as a Spring Resource
     * @param sourceName original filename, stored as metadata for citations
     * @return the number of chunks stored
     */
    public int ingest(Resource resource, String sourceName) {
        log.info("Ingesting document: {}", sourceName);

        // 1. Extract: pull text out of the file (whatever the format).
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> rawDocuments = reader.read();

        // Tag each document with its source filename so we can cite it later.
        rawDocuments.forEach(doc -> doc.getMetadata().put("source", sourceName));

        // 2. Transform: split into smaller, embed-friendly chunks.
        List<Document> chunks = splitter.split(rawDocuments);
        log.info("Split '{}' into {} chunks", sourceName, chunks.size());

        // 3. Load: store the chunks. The VectorStore embeds them internally
        //    (via the configured Ollama model) and writes vectors to pgvector.
        vectorStore.add(chunks);
        log.info("Stored {} chunks from '{}' in the vector store", chunks.size(), sourceName);

        return chunks.size();
    }
}
