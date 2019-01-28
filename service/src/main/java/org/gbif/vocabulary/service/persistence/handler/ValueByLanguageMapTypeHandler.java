package org.gbif.vocabulary.service.persistence.handler;

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
public class ValueByLanguageMapTypeHandler extends BaseTypeHandler<Map<Language, List<String>>> {

  private static final Pattern SEPARATOR = Pattern.compile(",");

  @Override
  public void setNonNullParameter(
      PreparedStatement preparedStatement,
      int i,
      Map<Language, List<String>> languageStringMap,
      JdbcType jdbcType)
      throws SQLException {
    preparedStatement.setString(i, HStoreConverter.toString(languageStringMap));
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

  private Map<Language, List<String>> fromString(String hstring) {
    Map<Language, List<String>> map = new EnumMap<>(Language.class);

    if (Strings.isNullOrEmpty(hstring)) {
      return map;
    }

    return HStoreConverter.fromString(hstring).entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> Language.fromIsoCode(entry.getKey()),
                entry -> SEPARATOR.splitAsStream(entry.getValue()).collect(Collectors.toList())));
  }
}
