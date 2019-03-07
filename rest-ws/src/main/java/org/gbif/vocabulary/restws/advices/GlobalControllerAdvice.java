package org.gbif.vocabulary.restws.advices;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;

/** This class contains the setuop that will be applied to all the vocabulary. */
@ControllerAdvice
public class GlobalControllerAdvice {

  /** Only applies to query parameters. */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    // trims all strings and converts empty strings to null
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  public void illegalArguments() {}
}
