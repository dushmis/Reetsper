package org.reetsper.connections;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * connections that implements {@link AutoCloseable}
 */
public class Connections implements AutoCloseable {
  public static Connection getConnection(ConnectionType connectionType) {
    return null;
  }

  /**
   * @param objects objects
   */
  public static void close(AutoCloseable... objects) {
    Arrays.stream(objects)
            .filter(Objects::nonNull)
            .forEach(Connections::close0);
  }

  /**
   * hate this
   *
   * @param closeable that closes
   */
  private static void close0(AutoCloseable closeable) {
    try {
      closeable.close();
    } catch (Exception ignored) {
      //hate this
    }
  }

  /**
   * to release connections back to pool
   *
   * @param connectionType this is to use all connection pool
   * @param connection     connection duh
   */
  public static void free(ConnectionType connectionType, Connection connection) {
    throw new UnsupportedOperationException();
  }

  /**
   * TBD
   *
   * @param connectionType connecion
   * @return Optional Connectino if available
   */
  public static Optional<Connection> get(ConnectionType connectionType) {
    return Optional.empty();
  }

  @Override
  public void close() throws Exception {
    throw new UnsupportedOperationException();
  }
}
