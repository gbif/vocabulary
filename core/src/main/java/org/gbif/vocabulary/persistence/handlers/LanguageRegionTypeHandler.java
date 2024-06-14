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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import com.google.common.base.Strings;

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
    return convertResult(rs.getString(columnName));
  }

  @Override
  public LanguageRegion getResult(ResultSet rs, int columnIndex) throws SQLException {
    return convertResult(rs.getString(columnIndex));
  }

  @Override
  public LanguageRegion getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return convertResult(cs.getString(columnIndex));
  }

  private LanguageRegion convertResult(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return null;
    }

    return LanguageRegion.fromLocale(value);
  }
}
