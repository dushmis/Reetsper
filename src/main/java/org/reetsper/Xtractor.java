package org.reetsper;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.reetsper.connections.ConnectionType;
import org.reetsper.connections.Connections;

/**
 * needs example here
 *
 * @param <E> IF you want to extract data in {@link Bean}
 */
@SuppressWarnings({"unused", "RedundantSuppression", "SpellCheckingInspection"})
@Beta
public class Xtractor<E extends Bean> {
  private final PreparedParameters preparedParameters;
  private final String query;
  private final ConnectionType connectionType;
  private final Object[] preparedParameterValues;


  //non final as we're changing these via setter
  private Filter filters;
  private Transform transform;
  //make these final
  private HashMap<String, Cast> castMap;
  private int[] preparedParameterValueTypes;

  private static final LinkedHashMap<Class<?>, Object> instanceMap = new LinkedHashMap<>();

  /**
   * @param connection {@link ConnectionType}
   * @param query      query, if it doesn't have any params
   */
  public Xtractor(ConnectionType connection, String query) {
    this.connectionType = connection;
    this.query = query;
    this.preparedParameters = null;
    this.castMap = null;
    this.preparedParameterValues = null;
    this.preparedParameterValueTypes = null;
  }


  /**
   * @param connection              {@link ConnectionType}
   * @param query                   query
   * @param preparedParameterValues yes! for "?"s
   */
  public Xtractor(ConnectionType connection, String query, Object... preparedParameterValues) {
    this.connectionType = connection;
    this.query = query;
    this.preparedParameters = null;
    this.castMap = null;
    this.preparedParameterValues = preparedParameterValues;
    this.preparedParameterValueTypes = null;
  }

  /**
   * needs improvement
   *
   * @param connection         {@link ConnectionType}
   * @param query              query sql
   * @param preparedParameters ofcourse for "?"s
   */
  public Xtractor(ConnectionType connection, String query, PreparedParameters preparedParameters) {
    this.connectionType = connection;
    this.query = query;
    this.preparedParameters = preparedParameters;
    this.castMap = null;
    this.preparedParameterValues = null;
    this.preparedParameterValueTypes = null;
  }

  /**
   * give class instance if it's avaiable in cache
   *
   * @param className class name
   * @return instance of E
   */
  private E getClassInstance(Class<E> className) {
    //noinspection unchecked
    return (E) instanceMap.computeIfAbsent(className, aClass -> {
      try {
        return aClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        //damn
      }
      return null;
    });
  }

  /**
   * break it in parts
   *
   * @param className class that implements {@link Bean}
   * @return List of E
   * @throws Exception ofcourse
   */
  @SuppressWarnings("resource")
  public List<E> get(Class<E> className) throws Exception {
    List<E> arrayList = new ArrayList<>();
    E classInstance;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    ResultSetMetaData resultSetMetaData;
    Connection connection = Connections.getConnection(this.connectionType);
    HashMap<String, Field> hashMap = getDeclaredFields(className);
    try {
      //noinspection ConstantConditions
      statement = connection.prepareStatement(this.query);
      setStatementObjects(statement);
      resultSet = statement.executeQuery();
      resultSetMetaData = resultSet.getMetaData();
      while (resultSet.next()) {
        classInstance = getClassInstance(className);
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
          String colName = resultSetMetaData.getColumnLabel(i).toLowerCase();
          if (hashMap.containsKey(colName)) {
            Field field = hashMap.get(colName);
            field.setAccessible(true);
            Class<?> type = field.getType();
            Object raw = resultSet.getObject(colName);
            try {
              final Object cast = type.cast(getCastedObject(colName, raw));
              if (filters == null || filters.apply(colName, raw)) {
                //noinspection UnusedAssignment
                raw = transform != null ? transform.apply(colName, raw) : raw;
                field.set(classInstance, cast);
              }
            } catch (ClassCastException e) {
              throw new ClassCastException(String.format("%s for field %s", e.getLocalizedMessage(), field));
            }
          }
        }
        arrayList.add(classInstance);
      }
    } finally {
      Connections.close(resultSet, statement);
      Connections.free(connectionType, connection);
    }
    return arrayList;
  }

  /**
   * private
   *
   * @param colName column
   * @param raw     object
   * @return object casted
   */
  private Object getCastedObject(String colName, Object raw) {
    if (castMap == null) {
      return raw;
    }
    final Cast cast = castMap.get(colName);
    return (cast != null) ? cast.apply(raw) : raw;
  }

  /**
   * @return map of {@link Cast}
   */
  public final HashMap<String, Cast> getCastMap() {
    return castMap;
  }

  /**
   * private stuff
   *
   * @param className class
   * @return map of fields
   * @throws SecurityException as if
   */
  private HashMap<String, Field> getDeclaredFields(Class<?> className) throws SecurityException {
    return Arrays.stream(className.getDeclaredFields()).collect(Collectors.toMap(field -> field.getName().toLowerCase(), field -> field, (a, b) -> b, HashMap::new));
  }

  /**
   * @return getter for {@link Filter}
   */
  public final Filter getFilters() {
    return filters;
  }

  /**
   * @return getter
   */
  public final int[] getPreparedParameterValueTypes() {
    return preparedParameterValueTypes;
  }

  /**
   * @return getter for {@link Transform}
   */
  public final Transform getTransform() {
    return transform;
  }


  /**
   * @param castMap to cast things to different things
   */
  public final void setCastMap(HashMap<String, Cast> castMap) {
    this.castMap = castMap;
  }

  /**
   * @param filters to filter stuff
   */
  public final void setFilters(Filter filters) {
    this.filters = filters;
  }

  /**
   * @param preparedParameterValueTypes to set
   */
  @Deprecated
  public final void setPreparedParameterValueTypes(int... preparedParameterValueTypes) {
    this.preparedParameterValueTypes = preparedParameterValueTypes;
  }

  /**
   * private stuff, move away
   *
   * @param statement {@link PreparedStatement}
   * @throws Exception ISGW
   */
  private void setStatementObjects(PreparedStatement statement) throws Exception {
    if (preparedParameters != null) {
      preparedParameters.setStatement(statement);
    }

    if (null != preparedParameterValues) {
      if (null != preparedParameterValueTypes && preparedParameterValues.length == preparedParameterValueTypes.length) {
        for (int i = 0; i < preparedParameterValues.length; i++) {
          statement.setObject((i + 1), preparedParameterValues[i], preparedParameterValueTypes[i]);
        }
      } else if (null == preparedParameterValueTypes) {
        for (int i = 0; i < preparedParameterValues.length; i++) {
          statement.setObject((i + 1), preparedParameterValues[i]);
        }
      } else {
        throw new Exception("parameter values and parameter types both should have same number of elements");
      }
    }
  }

  /**
   * if you have some fields which needs modifying like changing double to String or {@link String} to {@link Double} or {@link java.math.BigInteger}
   *
   * @param transform is a {@link java.util.function.BiFunction}
   */
  public final void setTransform(Transform transform) {
    this.transform = transform;
  }

  /**
   * wasn't needed, couldve been resolved with other methods, {@link Deprecated}
   *
   * @return List
   * @throws Exception if something goes wrong
   */
  @Deprecated
  public List<Map<String, Object>> toCaseInsensitiveMap() throws Exception {
    return this.toMap(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
  }

  /**
   * @return List of {@link Map}
   * @throws Exception if something eoes wrong
   */
  public List<Map<String, Object>> toMap() throws Exception {
    return this.toMap(LinkedHashMap::new);
  }

  /**
   * @param <X> generic
   * @return returns Promise that might or might not finish, upto you
   */
  public <X> Optional<List<Map<String, X>>> map() {
    //noinspection RedundantTypeArguments
    return this.<X>map(LinkedHashMap::new);
  }

  /**
   * very nice method, that takes {@link LinkedHashMap} as default
   *
   * @return returns Promise that might or might not finish, upto you
   */
  public CompletableFuture<Optional<List<Map<String, String>>>> future() {
    return CompletableFuture.supplyAsync(this::asList);
  }

  /**
   * love this
   *
   * @param supplier how do you want to supply the result? in which map
   * @return returns Promise that might or might not finish, upto you
   */
  public CompletableFuture<Optional<List<Map<String, Object>>>> future(Supplier<Map<String, Object>> supplier) {
    return CompletableFuture.supplyAsync(() -> this.map(supplier));
  }

  /**
   * love this
   *
   * @param executor on which threadpool to run this query
   * @return returns Promise that might or might not finish, upto you
   */
  public CompletableFuture<Optional<List<Map<String, String>>>> future(Executor executor) {
    return CompletableFuture.supplyAsync(this::asList, executor);
  }

  /**
   * very nice function method, doesn't throw any error and works very well
   *
   * @param rawMap which map to use?
   * @param <X>    generrics everywhere
   * @return returns {@link Optional} {@link List} of {@link Map} of {@link String} or X ;)
   */
  public <X> Optional<List<Map<String, X>>> map(Supplier<Map<String, X>> rawMap) {
    Map<String, X> hashMap;
    Optional<List<Map<String, X>>> optionalList = Optional.empty();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    ResultSetMetaData resultSetMetaData;
    final Optional<Connection> optionalConnection = Connections.get(this.connectionType);
    try {
      if (optionalConnection.isPresent()) {
        final Connection connection = optionalConnection.get();
        try {
          statement = connection.prepareStatement(this.query);
          setStatementObjects(statement);
          resultSet = statement.executeQuery();
          resultSetMetaData = resultSet.getMetaData();
          List<Map<String, X>> list = new ArrayList<>();
          while (resultSet.next()) {
            hashMap = rawMap.get();
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
              final String columnLabel = resultSetMetaData.getColumnLabel(i);
              Object object = resultSet.getObject(i);
              if (filters == null || filters.apply(columnLabel, object)) {
                object = transform != null ? transform.apply(columnLabel, object) : object;
                //noinspection unchecked
                hashMap.put(columnLabel, (X) getCastedObject(columnLabel, object));
              }
            }
            list.add(hashMap);
          }
          if (list.size() == 0) {
            optionalList = Optional.empty();
          } else {
            optionalList = Optional.of(list);
          }
        } finally {
          Connections.close(resultSet, statement);
          Connections.free(connectionType, connection);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      optionalList = Optional.empty();
    }
    return optionalList;
  }

  /**
   * @param rawMap which map to use, sorted, hash, set?
   * @return returns {@link List} of {@link Map} of given param
   * @throws Exception ofcourse if soemthign goes wrong
   */
  public List<Map<String, Object>> toMap(Supplier<Map<String, Object>> rawMap) throws Exception {
    Map<String, Object> hashMap;
    List<Map<String, Object>> list = new ArrayList<>();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    ResultSetMetaData resultSetMetaData;
    Connection connection = Connections.getConnection(this.connectionType);
    try {
      //noinspection ConstantConditions
      statement = connection.prepareStatement(this.query);
      setStatementObjects(statement);
      resultSet = statement.executeQuery();
      resultSetMetaData = resultSet.getMetaData();
      while (resultSet.next()) {
        hashMap = rawMap.get();
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
          final String columnLabel = resultSetMetaData.getColumnLabel(i);
          Object object = resultSet.getObject(i);
          if (filters == null || filters.apply(columnLabel, object)) {
            object = transform != null ? transform.apply(columnLabel, object) : object;
            hashMap.put(columnLabel, getCastedObject(columnLabel, object));
          }
        }
        list.add(hashMap);
      }
    } finally {
      Connections.close(resultSet, statement);
      Connections.free(connectionType, connection);
    }
    return list;
  }

  /**
   * Very nice method
   *
   * @return returns {@link Optional} result of {@link List}
   */
  public Optional<List<Map<String, String>>> asList() {
    Optional<List<Map<String, Object>>> objectMap = this.map();
    Optional<List<Map<String, String>>> optionalMap;
    List<Map<String, String>> list = new ArrayList<>();
    if (objectMap.isPresent()) {
      objectMap.get().forEach(map -> list.add(map.keySet()
              .stream()
              .collect(Collectors.toMap(Function.identity(),
                      key -> String.valueOf(map.get(key)),
                      (a, b) -> b))));
      optionalMap = Optional.of(list);
    } else {
      return Optional.empty();
    }
    return optionalMap;
  }

  /**
   * @return {@link List} of {@link Map}
   * @throws Exception if something goes wrong, but there are better methods available with {@link Optional} i'd use those
   */
  public List<Map<String, String>> toStringMap() throws Exception {
    List<Map<String, String>> list = new ArrayList<>();
    this.toMap().forEach(map -> list.add(map.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> String.valueOf(map.get(key)), (a, b) -> b))));
    return list;
  }

  /**
   * @return {@link List} of {@link Map}
   * @throws Exception if something goes wrong, but there are better methods available with {@link Optional} i'd use those
   */
  @Deprecated
  private List<Map<String, String>> toStringMap0() throws Exception {
    List<Map<String, Object>> objectMap = this.toMap();
    List<Map<String, String>> list = new ArrayList<>();
    for (Map<String, Object> map : objectMap) list.add(map.keySet().stream().collect(Collectors.toMap(key -> key, key -> String.valueOf(map.get(key)), (a, b) -> b)));
    return list;
  }
}

