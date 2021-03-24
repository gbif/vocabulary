package org.gbif.vocabulary.persistence.handlers;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.google.common.collect.Sets;

public class StringSetTypeHandler extends BaseTypeHandler<Set<String>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType) throws SQLException {
    Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toSet(rs.getArray(columnName));
  }

  @Override
  public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toSet(rs.getArray(columnIndex));
  }

  @Override
  public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toSet(cs.getArray(columnIndex));
  }

  private Set<String> toSet(Array pgArray) throws SQLException {
    if (pgArray == null) return new HashSet<>();

    String[] strings = (String[]) pgArray.getArray();
    return containsOnlyNulls(strings) ? new HashSet<>() : Sets.newHashSet(strings);
  }

  private boolean containsOnlyNulls(String[] strings) {
    for (String s : strings) {
      if (s != null) {
        return false;
      }
    }
    return true;
  }
}
