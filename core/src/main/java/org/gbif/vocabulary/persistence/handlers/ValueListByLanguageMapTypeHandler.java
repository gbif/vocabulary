package org.gbif.vocabulary.persistence.handlers;

import org.gbif.api.vocabulary.Language;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

/**
 * MyBatis {@link org.apache.ibatis.type.TypeHandler} for a {@link Map} keyed on {@link Language}
 * and stores a {@link List} of strings.
 */
public class ValueListByLanguageMapTypeHandler
    extends BaseTypeHandler<Map<Language, List<String>>> {

  private static final Pattern SEPARATOR = Pattern.compile(",");

  @Override
  public void setNonNullParameter(
      PreparedStatement preparedStatement,
      int i,
      Map<Language, List<String>> languageStringListMap,
      JdbcType jdbcType)
      throws SQLException {
    preparedStatement.setString(i, toString(languageStringListMap));
  }

  @Override
  public Map<Language, List<String>> getNullableResult(ResultSet resultSet, String columnName)
      throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public Map<Language, List<String>> getNullableResult(ResultSet resultSet, int columnIndex)
      throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public Map<Language, List<String>> getNullableResult(
      CallableStatement callableStatement, int columnIndex) throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private String toString(Map<Language, List<String>> languageStringListMap) {
    return HStoreConverter.toString(
        languageStringListMap.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getIso2LetterCode(), Map.Entry::getValue)));
  }

  private Map<Language, List<String>> fromString(String hstring) {
    if (Strings.isNullOrEmpty(hstring)) {
      return new EnumMap<>(Language.class);
    }

    return HStoreConverter.fromString(hstring).entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> Language.fromIsoCode(entry.getKey()),
                entry -> SEPARATOR.splitAsStream(entry.getValue()).collect(Collectors.toList()),
                (k, v) -> {
                  throw new IllegalStateException(String.format("Duplicate key %s", k));
                },
                () -> new EnumMap<>(Language.class)));
  }
}
