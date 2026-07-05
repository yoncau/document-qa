# document-qa

A document question-answering service built with **Retrieval-Augmented Generation (RAG)**.
Upload documents, then ask questions and get answers grounded in *your* documents —
with the source files cited.

Built with Spring Boot and Spring AI. Uses **Claude** for generation, a **local
embedding model** (via Ollama) for turning text into vectors, and **PostgreSQL +
pgvector** as the vector store.

## Why this architecture

RAG lets a language model answer questions about documents it was never trained on.
Instead of relying on the model's built-in knowledge, the service retrieves the most
relevant chunks of your documents and gives them to the model as context — so answers
are grounded in your data, not the model's memory.

A deliberate choice here: **embeddings are generated locally** (Ollama), while only the
final generation step calls a cloud model (Claude). Document content is embedded on your
own machine rather than sent to an embedding vendor — the more privacy-preserving option,
and a realistic production consideration.

## How it works

The service has two flows, exposed as two endpoints:

**Ingestion (`POST /ingest`)** — the offline "load the knowledge base" step:
1. **Extract** — read text from the uploaded file (`TikaDocumentReader`, handles PDF, DOCX, TXT, HTML, ...).
2. **Transform** — split the text into token-sized chunks (`TokenTextSplitter`).
3. **Load** — embed each chunk (local Ollama model) and store the vector in pgvector.

**Query (`POST /ask`)** — the per-question step:
1. Embed the question with the same model.
2. Similarity-search pgvector for the most relevant chunks.
3. Pass those chunks + the question to Claude, which answers using only that context.
4. Return the answer plus the list of source documents it drew from.

Retrieval and prompt-augmentation are handled by Spring AI's `QuestionAnswerAdvisor`,
attached to the `ChatClient`.

## Tech stack

- Java 21, Spring Boot 3.5, Spring AI
- Claude (Anthropic) — answer generation
- Ollama + `nomic-embed-text` (768-dim) — local embeddings
- PostgreSQL + pgvector — vector store (via Docker)
- Apache Tika — text extraction from many document formats

## Prerequisites

- **Java 21**
- **Docker** (for PostgreSQL + pgvector)
- **Ollama** (for local embeddings) — https://ollama.com
- An **Anthropic API key** — https://console.anthropic.com

## Setup

**1. Start the vector database**

```bash
docker compose up -d
```

This starts PostgreSQL 17 with the pgvector extension on `localhost:5432`.

**2. Pull the embedding model**

```bash
ollama pull nomic-embed-text
```

**3. Set your Anthropic API key**

The key is read from an environment variable and is never stored in the code:

```bash
export ANTHROPIC_API_KEY="your-key-here"
```

(In IntelliJ, set it under Run → Edit Configurations → Environment variables.)

**4. Run the application**

```bash
./mvnw spring-boot:run
```

On first start, Spring AI creates the `vector_store` table automatically.

## Usage

**Ingest a document**

```bash
curl -X POST http://localhost:8080/ingest \
  -F "file=@/path/to/document.pdf"
```

Response:

```json
{ "source": "document.pdf", "chunksStored": 9 }
```

Supported formats: PDF, TXT, MD, DOC, DOCX, PPT, PPTX, HTML, RTF, CSV.

**Ask a question**

```bash
curl -X POST http://localhost:8080/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What classifier was used and what accuracy was achieved?"}'
```

Response:

```json
{
  "answer": "A J48 decision tree classifier was used, achieving about 79% accuracy...",
  "sources": ["document.pdf"]
}
```

If the answer isn't in the ingested documents, the service says so rather than
inventing one — answers are constrained to your data.

## Design notes

- **Secrets stay out of the codebase.** The API key comes from an environment variable.
  (The local Postgres credentials in config are throwaway dev-only values for the Docker container.)
- **Errors are handled centrally.** A `@RestControllerAdvice` returns RFC 9457
  `ProblemDetail` responses: bad input and unsupported/malformed uploads return `400`
  with a clear message; unexpected failures return `502` with the cause logged server-side.
- **Embedding/vector dimensions must match.** `nomic-embed-text` produces 768-dim vectors,
  so pgvector is configured with `dimensions=768`. A mismatch is the classic RAG setup bug.
- **Ollama is embeddings-only here.** The Ollama starter also auto-configures a *chat*
  model by default; since generation is handled by Claude, that chat auto-config is
  excluded so there's a single, unambiguous chat model.

## Tuning

Retrieval behaviour lives in `QaService`:
- `TOP_K` — how many chunks to retrieve per question.
- `SIMILARITY_THRESHOLD` — minimum similarity to accept (too high filters out real
  answers; too low lets in irrelevant context).

Chunking behaviour lives in `IngestionService` (`TokenTextSplitter`); chunk size trades
retrieval precision against preserved context.

## Possible next steps

- Streaming responses; retries/timeouts on the model call.
- Metadata filtering (e.g. ask within a specific document or category).
- An evaluation harness to measure answer quality as settings change.
