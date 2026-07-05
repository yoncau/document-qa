package com.example.document_qa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic context-load smoke test.
 *
 * We supply a dummy API key and disable startup model-pulling / schema init so
 * the context can load in a test environment without real Ollama, Postgres, or
 * Anthropic access. This verifies the application wiring is valid.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"spring.ai.anthropic.api-key=test-key-not-used",
		"spring.ai.ollama.init.pull-model-strategy=never",
		"spring.ai.vectorstore.pgvector.initialize-schema=false"
})
class DocumentQaApplicationTests {

	@Test
	void contextLoads() {
		// If the Spring context starts successfully, the wiring is valid.
	}
}