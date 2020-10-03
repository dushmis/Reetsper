package org.reetsper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;

/**
 * singleton threadsafe util class, hope gson is threadsafe otherwise use {@link ThreadLocal}
 */
public class Util {
  private static volatile Util INSTANCE;
  private final Gson gson;
  private final HashMap<Class<?>, Class<?>> map;

  private Util() {
    gson = new GsonBuilder().setPrettyPrinting().create();
    map = new HashMap<>();
    map.put(Integer.TYPE, Integer.class);
    map.put(Double.TYPE, Double.class);
    map.put(Long.TYPE, Long.class);
    map.put(Boolean.TYPE, Boolean.class);
    map.put(Float.TYPE, Float.class);
    map.put(Short.TYPE, Short.class);
    map.put(Byte.TYPE, Byte.class);
    map.put(Character.TYPE, Character.class);
  }

  public static Util getInstance() {
    if (INSTANCE == null) {
      synchronized (Util.class) {
        if (INSTANCE == null) {
          INSTANCE = new Util();
        }
      }
    }
    return INSTANCE;
  }

  public String toJson(Bean o) {
    return this.gson.toJson(o);
  }


  /**
   * final Class<Integer> integerClass = primitiveToWrapper(Integer.TYPE);
   *
   * @param type type of primitive Integer.TYPE;
   * @param <T>  type
   * @return type
   */
  @SuppressWarnings("SingleStatementInBlock")
  public <T> Class<T> primitiveToWrapper(final Class<T> type) {
    if (type == null || !type.isPrimitive()) {
      return type;
    }
    //noinspection unchecked
    return (Class<T>) map.getOrDefault(type, type);
  }
}
