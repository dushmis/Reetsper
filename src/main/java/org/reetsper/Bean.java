package org.reetsper;

//marker interface

import java.io.Serializable;

/**
 * IMP STUFF
 */
public interface Bean extends Serializable {
  /**
   * @return json using {@link com.google.gson.Gson}
   */
  public default String toJson() {
    return Util.getInstance().toJson(this);
  }
}
