package org.gbif.vocabulary.persistence.handlers;

import org.gbif.api.vocabulary.TranslationLanguage;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * MyBatis {@link org.apache.ibatis.type.TypeHandler} for a {@link Map} keyed on {@link
 * TranslationLanguage} and stores a {@link List} of strings.
 */
public class ValueListByLanguageMapTypeHandler
    extends BaseTypeHandler<Map<TranslationLanguage, List<String>>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectReader OBJECT_READER =
      OBJECT_MAPPER.readerFor(new TypeReference<Map<TranslationLanguage, List<String>>>() {});

  @Override
  public void setNonNullParameter(
      PreparedStatement preparedStatement,
      int i,
      Map<TranslationLanguage, List<String>> languageStringListMap,
      JdbcType jdbcType)
      throws SQLException {
    preparedStatement.setString(i, toString(languageStringListMap));
  }

  @Override
  public Map<TranslationLanguage, List<String>> getNullableResult(
      ResultSet resultSet, String columnName) throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public Map<TranslationLanguage, List<String>> getNullableResult(
      ResultSet resultSet, int columnIndex) throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public Map<TranslationLanguage, List<String>> getNullableResult(
      CallableStatement callableStatement, int columnIndex) throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private String toString(Map<TranslationLanguage, List<String>> languageStringListMap) {
    try {
      return OBJECT_MAPPER.writeValueAsString(languageStringListMap);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Couldn't convert TranslationLanguage map to JSON: " + languageStringListMap, e);
    }
  }

  private Map<TranslationLanguage, List<String>> fromString(String json) {
    if (Strings.isNullOrEmpty(json)) {
      return new EnumMap<>(TranslationLanguage.class);
    }

    try {
      return OBJECT_READER.readValue(json);
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't deserialize JSON from DB: " + json, e);
    }
  }
}
