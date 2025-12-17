/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.vocabulary.restws.advices;

import org.gbif.vocabulary.model.exception.EntityNotFoundException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;

import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
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
  private static final String IO_ERROR = "IO error";
  private static final String UNSUPPORTED_OPERATION_ERROR = "Unsupported Operation";
  private static final String NOT_FOUND_ERROR = "%s not found";

  @Autowired private ErrorAttributes errorAttributes;

  /** Only applies to query parameters. */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    // trims all strings and converts empty strings to null
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
  public ResponseEntity<Object> handleInvalidFieldsExceptions(WebRequest request, Exception ex) {
    return buildResponse(request, HttpStatus.UNPROCESSABLE_ENTITY, INVALID_PARAM_ERROR);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Object> handleVocabularyNotFoundExceptions(
      WebRequest request, EntityNotFoundException ex) {
    return buildResponse(
        request,
        HttpStatus.NOT_FOUND,
        String.format(NOT_FOUND_ERROR, ex.getEntityType()),
        ex.getMessage());
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<Object> handleDBDuplicatedKeyException(
      WebRequest request, DuplicateKeyException ex) {
    return buildResponse(
        request,
        HttpStatus.UNPROCESSABLE_ENTITY,
        DUPLICATED_ENTITY_ERROR,
        ex.getMostSpecificCause().getMessage());
  }

  @ExceptionHandler(PSQLException.class)
  public ResponseEntity<Object> handleDBException(WebRequest request, PSQLException ex) {
    return buildResponse(
        request,
        HttpStatus.INTERNAL_SERVER_ERROR,
        DB_ERROR,
        ex.getServerErrorMessage().getMessage());
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<Object> handleIOException(WebRequest request, IOException ex) {
    return buildResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, IO_ERROR, ex.getMessage());
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<Object> handleUnsupportedOperationException(
      WebRequest request, UnsupportedOperationException ex) {
    return buildResponse(
        request, HttpStatus.FORBIDDEN, UNSUPPORTED_OPERATION_ERROR, ex.getMessage());
  }

  /**
   * Creates the response entity. It uses the Spring {@link ErrorAttributes} to build the body but
   * overriding some fields if necessary.
   */
  private ResponseEntity<Object> buildResponse(
      WebRequest request, HttpStatus status, String error, String message) {
    Objects.requireNonNull(status);

    Map<String, Object> body = errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.BINDING_ERRORS));
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
