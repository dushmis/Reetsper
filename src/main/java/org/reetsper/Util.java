package org.reetsper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * singleton threadsafe util class, hope gson is threadsafe otherwise use {@link ThreadLocal}
 */
public class Util {
  private static volatile Util INSTANCE;
  private final Gson gson;

  private Util() {
    gson = new GsonBuilder().setPrettyPrinting().create();
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

  public String toJson(Object o) {
    return this.gson.toJson(o);
  }
}
