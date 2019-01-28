package org.gbif.vocabulary.service.persistence.handler;

import org.gbif.api.vocabulary.Language;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

/**
 * MyBatis {@link org.apache.ibatis.type.TypeHandler} for a {@link Map} keyed on {@link Language}
 * and stores a String.
 */
public class ValueListByLanguageMapTypeHandler extends BaseTypeHandler<Map<Language, String>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement preparedStatement,
      int i,
      Map<Language, String> languageStringMap,
      JdbcType jdbcType)
      throws SQLException {
    preparedStatement.setString(i, HStoreConverter.toString(languageStringMap));
  }

  @Override
  public Map<Language, String> getNullableResult(ResultSet resultSet, String columnName)
      throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public Map<Language, String> getNullableResult(ResultSet resultSet, int columnIndex)
      throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public Map<Language, String> getNullableResult(
      CallableStatement callableStatement, int columnIndex) throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private Map<Language, String> fromString(String hstring) {
    Map<Language, String> map = new EnumMap<>(Language.class);

    if (Strings.isNullOrEmpty(hstring)) {
      return map;
    }

    return HStoreConverter.fromString(hstring).entrySet().stream()
        .collect(
            Collectors.toMap(entry -> Language.fromIsoCode(entry.getKey()), Map.Entry::getValue));
  }
}
