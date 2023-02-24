package org.gbif.vocabulary.persistence.handlers;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.gbif.vocabulary.model.LanguageRegion;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes({LanguageRegion.class})
public class LanguageRegionTypeHandler implements TypeHandler<LanguageRegion> {

  @Override
  public void setParameter(
      PreparedStatement ps, int i, LanguageRegion languageRegion, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, languageRegion == null ? null : languageRegion.getLocale());
  }

  @Override
  public LanguageRegion getResult(ResultSet rs, String columnName) throws SQLException {
    return LanguageRegion.fromLocale(rs.getString(columnName));
  }

  @Override
  public LanguageRegion getResult(ResultSet rs, int columnIndex) throws SQLException {
    return LanguageRegion.fromLocale(rs.getString(columnIndex));
  }

  @Override
  public LanguageRegion getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return LanguageRegion.fromLocale(cs.getString(columnIndex));
  }
}
