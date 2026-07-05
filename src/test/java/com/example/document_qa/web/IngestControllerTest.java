package com.example.document_qa.web;

import com.example.document_qa.ingest.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the upload validation and happy path for /ingest.
 * The IngestionService is mocked, so no embedding or DB work happens.
 */
@WebMvcTest(IngestController.class)
@Import(IngestControllerTest.StubConfig.class)
class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class StubConfig {
        @Bean
        IngestionService ingestionService() {
            IngestionService service = mock(IngestionService.class);
            // Whatever is ingested, pretend it produced 3 chunks.
            when(service.ingest(any(), anyString())).thenReturn(3);
            return service;
        }
    }

    @Test
    void acceptsValidPdfAndReturnsChunkCount() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "some pdf bytes".getBytes());

        mockMvc.perform(multipart("/ingest").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("report.pdf"))
                .andExpect(jsonPath("$.chunksStored").value(3));
    }

    @Test
    void rejectsEmptyFileWith400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/ingest").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnsupportedTypeWith400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "not a document".getBytes());

        mockMvc.perform(multipart("/ingest").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFileWithNoExtensionWith400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "README", "application/octet-stream", "content".getBytes());

        mockMvc.perform(multipart("/ingest").file(file))
                .andExpect(status().isBadRequest());
    }
}
