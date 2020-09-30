package org.reetsper.connections;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.reetsper.Xtractor;

/**
 * juice coming way
 */
public enum ConnectionType {
  DB1, DB2;


  /**
   * @param query      query
   * @param parameters parameters for query
   * @param <E>        E
   * @return Optional of E
   */
  public <E> Optional<List<Map<String, E>>> fetch(String query, Object... parameters) {
    return new Xtractor<>(this, query, parameters).map();
  }

  /**
   * word under process
   *
   * @param query      query
   * @param parameters parameters
   * @param <E>        E
   * @return Optional
   */
  @Deprecated
  public <E> Optional<List<Map<String, E>>> fetchCachable(String query, Object... parameters) {
    return new Xtractor<>(this, query, parameters).map();
  }
}
