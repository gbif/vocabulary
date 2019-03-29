package org.gbif.vocabulary.restws.advices;

import javax.validation.ConstraintViolationException;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;

/** This class contains the setuop that will be applied to all the vocabulary. */
@ControllerAdvice
public class GlobalControllerAdvice {

  /** Only applies to query parameters. */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    // trims all strings and converts empty strings to null
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
  public ResponseEntity<String> illegalArgumentException(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}
