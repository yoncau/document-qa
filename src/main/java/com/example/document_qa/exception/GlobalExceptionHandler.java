package com.example.document_qa.exception;

import com.example.document_qa.web.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Centralised error handling: turns exceptions into RFC 9457 ProblemDetail
 * JSON responses instead of leaking stack traces to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Our own validation failure (e.g. empty file, blank question) -> 400. */
    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /**
     * The client sent a malformed upload: not a proper multipart request, or
     * missing the expected 'file' part/parameter. This is a client mistake,
     * so it's a 400 with a helpful message - not a generic 502.
     */
    @ExceptionHandler({
            MultipartException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ProblemDetail handleBadUpload(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Expected a file upload in a 'file' form field. " +
                        "With curl, use -F \"file=@/path/to/document\" (note the @).");
        problem.setTitle("Malformed upload");
        return problem;
    }

    /** Anything unexpected (model down, DB down, parse failure) -> 502, cause logged. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error handling request", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The service could not process the request. Please try again later.");
        problem.setTitle("Processing error");
        return problem;
    }
}
