package org.gbif.vocabulary.restws.advices;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.validation.ConstraintViolationException;

import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;

/** This class contains the setup that will be applied to all the controllers. */
@ControllerAdvice
public class GlobalControllerAdvice {

  private static final String INVALID_PARAM_ERROR = "Invalid Field";
  private static final String DB_ERROR = "DB error";
  private static final String DUPLICATED_ENTITY_ERROR = "Duplicated Entity";

  @Autowired private ErrorAttributes errorAttributes;

  /** Only applies to query parameters. */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    // trims all strings and converts empty strings to null
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
  public ResponseEntity<Object> invalidFieldsExceptions(WebRequest request, Exception ex) {
    return buildResponse(request, HttpStatus.UNPROCESSABLE_ENTITY, INVALID_PARAM_ERROR);
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<Object> dbDuplicatedKeyException(
      WebRequest request, DuplicateKeyException ex) {
    return buildResponse(
        request,
        HttpStatus.UNPROCESSABLE_ENTITY,
        DUPLICATED_ENTITY_ERROR,
        ex.getMostSpecificCause().getMessage());
  }

  @ExceptionHandler(PSQLException.class)
  public ResponseEntity<Object> dbException(WebRequest request, PSQLException ex) {
    return buildResponse(
        request,
        HttpStatus.INTERNAL_SERVER_ERROR,
        DB_ERROR,
        ex.getServerErrorMessage().getMessage());
  }

  /**
   * Creates the response entity. It uses the Spring {@link ErrorAttributes} to build the body but
   * overriding some fields if necessary.
   */
  private ResponseEntity<Object> buildResponse(
      WebRequest request, HttpStatus status, String error, String message) {
    Objects.requireNonNull(status);

    Map<String, Object> body = errorAttributes.getErrorAttributes(request, false);
    body.put("status", status.value());
    Optional.ofNullable(message).ifPresent(m -> body.put("message", m));
    Optional.ofNullable(error).ifPresent(e -> body.put("error", e));

    return ResponseEntity.status(status).body(body);
  }

  private ResponseEntity<Object> buildResponse(
      WebRequest request, HttpStatus status, String error) {
    return buildResponse(request, status, error, null);
  }
}
