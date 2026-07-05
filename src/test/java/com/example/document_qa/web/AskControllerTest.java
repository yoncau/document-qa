package com.example.document_qa.web;

import com.example.document_qa.query.QaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the /ask endpoint's request handling and response shape.
 * QaService is mocked, so no retrieval or model call happens.
 */
@WebMvcTest(AskController.class)
@Import(AskControllerTest.StubConfig.class)
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class StubConfig {
        @Bean
        QaService qaService() {
            QaService service = mock(QaService.class);
            when(service.answer(anyString()))
                    .thenReturn(new QaService.Answer(
                            "A J48 decision tree was used, ~79% accuracy.",
                            List.of("paper.pdf")));
            return service;
        }
    }

    @Test
    void answersValidQuestionWithSources() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"What classifier was used?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A J48 decision tree was used, ~79% accuracy."))
                .andExpect(jsonPath("$.sources[0]").value("paper.pdf"));
    }

    @Test
    void rejectsBlankQuestionWith400() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
