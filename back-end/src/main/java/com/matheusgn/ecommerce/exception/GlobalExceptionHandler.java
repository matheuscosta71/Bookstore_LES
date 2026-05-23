package com.matheusgn.ecommerce.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEBUG_LOG_PATH =
            "/home/dell/Documentos/Trabalhos-clientes/Clientes/Matheus-GN/matheus-gn/.cursor/debug-9dce4d.log";
    private static final String DEBUG_SESSION_ID = "9dce4d";

    private void appendDebugLog(String hypothesisId,
                                 String runId,
                                 String location,
                                 String message,
                                 Map<String, Object> data) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sessionId", DEBUG_SESSION_ID);
            payload.put("id", "log_" + System.currentTimeMillis());
            payload.put("timestamp", Instant.now().toEpochMilli());
            payload.put("hypothesisId", hypothesisId);
            payload.put("runId", runId);
            payload.put("location", location);
            payload.put("message", message);
            payload.put("data", data);

            String line = OBJECT_MAPPER.writeValueAsString(payload);
            Files.writeString(
                    Path.of(DEBUG_LOG_PATH),
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
            // instrumentation-only; never break the API response
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[GlobalExceptionHandler][handleHttpMessageNotReadable] {}", ex.getMessage());
        // #region agent debug log
        String rawMessage = ex.getMessage();
        String invalidEnum = null;
        try {
            Pattern p = Pattern.compile("from String \"([^\"]*)\"");
            Matcher m = p.matcher(rawMessage == null ? "" : rawMessage);
            if (m.find()) invalidEnum = m.group(1);
        } catch (Exception ignored) {
            // ignore parsing errors
        }

        Map<String, Object> data = new HashMap<>();
        data.put("invalidEnum", invalidEnum);
        data.put("exceptionMessage", rawMessage);
        appendDebugLog(
                "H1_enum_mismatch",
                "debug_run_pre_fix",
                "GlobalExceptionHandler#handleHttpMessageNotReadable",
                "JSON parse error during request body deserialization",
                data
        );
        // #endregion

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        pd.setTitle("Requisição inválida");
        pd.setType(URI.create("about:blank"));
        pd.setProperty("details", rawMessage);
        return pd;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        log.warn("[GlobalExceptionHandler][handleForbidden] {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Acesso negado");
        pd.setType(URI.create("about:blank"));
        return pd;
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
        log.warn("[GlobalExceptionHandler][handleAuthenticationFailed] {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Falha de autenticação");
        pd.setType(URI.create("about:blank"));
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("[GlobalExceptionHandler][handleNotFound] {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso não encontrado");
        pd.setType(URI.create("about:blank"));
        return pd;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        log.warn("[GlobalExceptionHandler][handleDuplicate] field={} message={}", ex.getField(), ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflito de dados");
        pd.setType(URI.create("about:blank"));
        pd.setProperty("field", ex.getField());
        return pd;
    }

    /**
     * Mapeia violações de unique no banco (ex.: ISBN duplicado) para 409 com mensagem clara.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String chain = messageChain(ex).toLowerCase(Locale.ROOT);
        log.warn("[GlobalExceptionHandler][handleDataIntegrity] {}", chain);

        if (chain.contains("uk_books_isbn") || (chain.contains("books") && chain.contains("isbn") && chain.contains("unique"))) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "Já existe um livro cadastrado com este ISBN.");
            pd.setTitle("Conflito de dados");
            pd.setType(URI.create("about:blank"));
            pd.setProperty("field", "isbn");
            return pd;
        }
        if (chain.contains("uk_books_code") || (chain.contains("books") && chain.contains("code") && chain.contains("unique"))) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "Código de livro já utilizado.");
            pd.setTitle("Conflito de dados");
            pd.setType(URI.create("about:blank"));
            pd.setProperty("field", "code");
            return pd;
        }
        if (chain.contains("uk_customers_email") || (chain.contains("customers") && chain.contains("email") && chain.contains("unique"))) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "E-mail já cadastrado.");
            pd.setTitle("Conflito de dados");
            pd.setType(URI.create("about:blank"));
            pd.setProperty("field", "email");
            return pd;
        }
        if (chain.contains("uk_customers_cpf") || (chain.contains("customers") && chain.contains("cpf") && chain.contains("unique"))) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "CPF já cadastrado.");
            pd.setTitle("Conflito de dados");
            pd.setType(URI.create("about:blank"));
            pd.setProperty("field", "cpf");
            return pd;
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Não foi possível salvar: violação de integridade dos dados.");
        pd.setTitle("Conflito de dados");
        pd.setType(URI.create("about:blank"));
        return pd;
    }

    private static String messageChain(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable t = ex;
        int depth = 0;
        while (t != null && depth++ < 12) {
            if (t.getMessage() != null) {
                sb.append(t.getMessage()).append(' ');
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[GlobalExceptionHandler][handleIllegalArgument] {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Requisição inválida");
        pd.setType(URI.create("about:blank"));
        return pd;
    }

    @ExceptionHandler(AiProviderException.class)
    public ProblemDetail handleAiProvider(AiProviderException ex) {
        log.error("[GlobalExceptionHandler][handleAiProvider] {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        pd.setTitle("Provedor de IA indisponível");
        pd.setType(URI.create("about:blank"));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("[GlobalExceptionHandler][handleValidation] erros={}", ex.getBindingResult().getErrorCount());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        pd.setTitle("Validação");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }
}
