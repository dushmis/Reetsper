package org.reetsper;

import java.sql.PreparedStatement;

/**
 * {@link PreparedStatement stuff}
 */
@FunctionalInterface
public interface PreparedParameters {
  void setStatement(PreparedStatement statement) throws Exception;
}
