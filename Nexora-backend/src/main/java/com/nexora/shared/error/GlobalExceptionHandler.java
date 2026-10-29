package com.nexora.shared.error;

import java.net.URI;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

// One place that turns every exception into the same JSON shape (RFC 9457 ProblemDetail + our "code").
// Extending ResponseEntityExceptionHandler keeps Spring MVC's own errors (404, 405, 400 bad JSON ...)
// as proper problem responses; the catch-all at the bottom only sees what is left.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // PostgreSQL SQLSTATE for "unique_violation"
    private static final String UNIQUE_VIOLATION = "23505";
    // Business-number constraints are named uq_<table>_<prefix>_number (party codes: uq_parties_party_code).
    private static final Pattern BUSINESS_NUMBER_CONSTRAINT =
            Pattern.compile("\\buq_[a-z_]+_number\\b|\\buq_parties_party_code\\b");

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---- our own business exceptions -------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail pd = build(ex.getCode(), ex.getMessage(), request.getRequestURI());
        if (ex instanceof FieldValidationException fve) {
            // same errors[] shape as a Bean Validation failure, so the client handles both alike
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("field", fve.getField());
            err.put("message", fve.getMessage());
            if (fve.getRejectedValue() != null) {
                err.put("rejectedValue", String.valueOf(fve.getRejectedValue()));
            }
            pd.setProperty("errors", List.of(err));
        }
        return pd;
    }

    // ---- persistence ---------------------------------------------------------------------

    // Someone else saved the same row first (@Version check failed).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                              HttpServletRequest request) {
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "The resource was modified by another user. Please reload and try again.",
                request.getRequestURI());
    }

    // A row lock was not obtained: PostgreSQL's lock_timeout (5 s, ADR-030) ran out, or PostgreSQL broke a
    // deadlock by aborting this transaction. Nothing was stored (the transaction rolled back), so the client
    // may simply retry (FR-INV-06 AC2).
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handlePessimisticLock(PessimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("event=lock_not_acquired path={} cause={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "Another user is changing the same data right now. Please try again.", request.getRequestURI());
    }

    // A database constraint rejected the statement. A unique violation is a client-visible conflict (409).
    // Any other violation (FK, CHECK, NOT NULL) means our own validation missed something: that is a bug (500).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        if (root instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
            log.warn("Unique constraint violated: {}", root.getMessage());
            if (root.getMessage() != null && BUSINESS_NUMBER_CONSTRAINT.matcher(root.getMessage()).find()) {
                // generated numbers are gap-free under a row lock; a clash means the counter was reset (a restore)
                return build(ErrorCode.DUPLICATE_BUSINESS_NUMBER,
                        "This business number is already in use; the numbering needs attention.",
                        request.getRequestURI());
            }
            return build(ErrorCode.DUPLICATE_RESOURCE,
                    "A record with the same unique value already exists.", request.getRequestURI());
        }
        return handleGeneric(ex, request);
    }

    // ---- security ------------------------------------------------------------------------

    // Thrown by @PreAuthorize when the user is logged in but lacks the permission.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // security event (09-SECURITY 13): who tried what; the user id is the token subject
        log.warn("event=access_denied user={} method={} path={}",
                request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName(),
                request.getMethod(), request.getRequestURI());
        return build(ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action.",
                request.getRequestURI());
    }

    // ---- bad input that Spring Data rejects (e.g. ?sort=doesNotExist) ---------------------

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleBadProperty(PropertyReferenceException ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, "Unknown property: " + ex.getPropertyName(),
                request.getRequestURI());
    }

    // A request that is not the multipart form an upload expects (POST /documents with JSON, a broken form).
    // MaxUploadSizeExceededException, a subclass, keeps its own more specific handler below.
    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipart(MultipartException ex, HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST, "Send the document as a multipart form with a 'file' part.",
                request.getRequestURI());
    }

    // ---- catch-all -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        // Full details go to the log only. The client gets a generic message (never ex.getMessage()).
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred. Please try again later.",
                request.getRequestURI());
    }

    // ---- Spring MVC's own exceptions (overrides of ResponseEntityExceptionHandler) ---------

    // @Valid on a request body failed: 400 VALIDATION_FAILED with one entry per bad field.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            Object rejected = error instanceof FieldError fe ? fe.getRejectedValue() : null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("field", field);
            map.put("message", String.valueOf(error.getDefaultMessage()));
            if (rejected != null) {
                map.put("rejectedValue", String.valueOf(rejected));
            }
            errors.add(map);
        }
        return validationFailed(ex, errors, headers, request);
    }

    // @Validated constraints on @RequestParam / @PathVariable failed.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<Map<String, Object>> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            Object arg = result.getArgument();
            result.getResolvableErrors().forEach(error -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("field", name == null ? "parameter" : name);
                map.put("message", String.valueOf(error.getDefaultMessage()));
                if (arg != null) {
                    map.put("rejectedValue", String.valueOf(arg));
                }
                errors.add(map);
            });
        });
        return validationFailed(ex, errors, headers, request);
    }

    // A multipart body above the servlet limit: the same 400 the 10 MB document check gives (FR-DOC-01 AC2),
    // not Spring's default 413.
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        Map<String, Object> err = Map.of("field", "file", "message", "A document can be at most 10 MB.");
        return validationFailed(ex, List.of(err), headers, request);
    }

    // Every other MVC exception (404, 405, 415, unreadable JSON ...) is built by the parent class;
    // we only add our "code" and "timestamp" so the body has the same shape as every other error.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd) {
            if (pd.getProperties() == null || !pd.getProperties().containsKey("code")) {
                pd.setProperty("code", codeFor(statusCode).name());
            }
            if (pd.getProperties() == null || !pd.getProperties().containsKey("timestamp")) {
                pd.setProperty("timestamp", Instant.now(clock));
            }
        }
        return response;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private ResponseEntity<Object> validationFailed(Exception ex, List<Map<String, Object>> errors,
                                                    HttpHeaders headers, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        pd.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
        pd.setProperty("timestamp", Instant.now(clock));
        pd.setProperty("errors", errors);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail build(ErrorCode code, String detail, String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setProperty("code", code.name());
        pd.setProperty("timestamp", Instant.now(clock));
        pd.setInstance(URI.create(path));
        return pd;
    }

    // Default error code for a status produced by Spring itself.
    private static ErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> ErrorCode.UNAUTHENTICATED;
            case 403 -> ErrorCode.ACCESS_DENIED;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.MALFORMED_REQUEST;
        };
    }
}
