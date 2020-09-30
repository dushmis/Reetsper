package org.reetsper;

import java.util.function.BiFunction;

/**
 * {@link BiFunction} to transform Objects
 */
@FunctionalInterface
public interface Transform extends BiFunction<String, Object, Object> {
}
