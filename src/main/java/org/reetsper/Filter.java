package org.reetsper;

import java.util.function.BiFunction;

/**
 * Filers data from query result doesn't give you eveyrthing, wish generics suported primitives, may be in 16
 */
@FunctionalInterface
public interface Filter extends BiFunction<String, Object, Boolean> {
  @Override
  Boolean apply(String s, Object o);
}
