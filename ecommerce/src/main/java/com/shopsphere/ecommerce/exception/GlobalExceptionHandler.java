package com.shopsphere.ecommerce.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static Map<String, String> body(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message != null ? message : "Something went wrong");
        return error;
    }

    // NEW: every typed error (404 / 403 / 400 / 409 / 401)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(body(ex.getMessage()));
    }

    // NEW: FK / unique-constraint violations (e.g. deleting a product that
    // already appears in an order)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(
            DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body("This action conflicts with existing data "
                        + "(it may be referenced elsewhere or already exist)"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        // "error" holds one readable line so the frontend can display it
        errors.put("error", errors.values().stream().findFirst()
                .orElse("Validation failed"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body("Access denied"));
    }

    // Legacy catch-all: older code still throws RuntimeException with a
    // user-facing message. Keep behaviour (400) but log it server side.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(
            RuntimeException ex) {
        log.warn("Unhandled runtime exception: {}", ex.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(ex.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCategoryNotFound(
            CategoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(exception.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(
            ProductNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(exception.getMessage()));
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAddressNotFound(
            AddressNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(exception.getMessage()));
    }

    // CHANGED: JSON {"error": "..."} like every other error
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(
            UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(
            InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUser(
            DuplicateUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(ex.getMessage()));
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, String>> handleEmptyCart(
            EmptyCartException ex) {
        return ResponseEntity.badRequest().body(body(ex.getMessage()));
    }

    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<Map<String, String>> handleOrderCancellation(
            OrderCancellationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(ex.getMessage()));
    }
}
