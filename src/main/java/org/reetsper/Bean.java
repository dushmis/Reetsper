package org.reetsper;

//marker interface

/**
 * IMP STUFF
 */
public interface Bean {
  /**
   * @return json using {@link com.google.gson.Gson}
   */
  public default String toJson() {
    return Util.getInstance().toJson(this);
  }
}
