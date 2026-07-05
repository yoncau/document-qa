package com.example.document_qa.web;

import com.example.document_qa.ingest.IngestionService;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Accepts a document upload and hands it to the ingestion pipeline.
 * POST /ingest  (multipart form field named "file")
 */
@RestController
public class IngestController {

    // File types Apache Tika can extract text from that make sense here.
    // (Kept as a readable allow-list; extend if you need more formats.)
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "doc", "docx", "ppt", "pptx", "html", "htm", "rtf", "csv"
    );

    private final IngestionService ingestionService;

    public IngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public IngestResponse ingest(@RequestParam("file") MultipartFile file) throws IOException {
        String sourceName = validate(file);

        Resource resource = file.getResource();
        int chunks = ingestionService.ingest(resource, sourceName);
        return new IngestResponse(sourceName, chunks);
    }

    /**
     * Validate the upload and return a safe source name.
     * Throws InvalidRequestException (-> 400) with a clear message on any problem.
     */
    private String validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file is empty.");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new InvalidRequestException("Uploaded file has no filename.");
        }

        String extension = extensionOf(original);
        if (extension.isEmpty()) {
            throw new InvalidRequestException(
                    "File has no extension; cannot determine its type. "
                            + "Supported types: " + SUPPORTED_EXTENSIONS + ".");
        }
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new InvalidRequestException(
                    "Unsupported file type '." + extension + "'. "
                            + "Supported types: " + SUPPORTED_EXTENSIONS + ".");
        }

        return original;
    }

    /** Lower-cased extension without the dot, or "" if there isn't one. */
    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Response body: { "source": "...", "chunksStored": N } */
    public record IngestResponse(String source, int chunksStored) {}
}