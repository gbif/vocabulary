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
package org.gbif.vocabulary.persistence.handlers;

import org.gbif.vocabulary.model.LanguageRegion;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;

/**
 * MyBatis {@link org.apache.ibatis.type.TypeHandler} for a {@link Map} keyed on {@link
 * LanguageRegion} and stores a String.
 */
public class ValueByLanguageMapTypeHandler extends BaseTypeHandler<Map<LanguageRegion, String>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectReader OBJECT_READER =
      OBJECT_MAPPER.readerFor(new TypeReference<Map<LanguageRegion, String>>() {});

  @Override
  public void setNonNullParameter(
      PreparedStatement preparedStatement,
      int i,
      Map<LanguageRegion, String> languageStringMap,
      JdbcType jdbcType)
      throws SQLException {
    preparedStatement.setString(i, toString(languageStringMap));
  }

  @Override
  public Map<LanguageRegion, String> getNullableResult(ResultSet resultSet, String columnName)
      throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public Map<LanguageRegion, String> getNullableResult(ResultSet resultSet, int columnIndex)
      throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public Map<LanguageRegion, String> getNullableResult(
      CallableStatement callableStatement, int columnIndex) throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private String toString(Map<LanguageRegion, String> languageStringMap) {
    try {
      return OBJECT_MAPPER.writeValueAsString(languageStringMap);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Couldn't convert language map to JSON: " + languageStringMap.toString(), e);
    }
  }

  private Map<LanguageRegion, String> fromString(String json) {
    if (Strings.isNullOrEmpty(json)) {
      return new EnumMap<>(LanguageRegion.class);
    }

    try {
      return OBJECT_READER.readValue(json);
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't deserialize JSON from DB: " + json, e);
    }
  }
}
