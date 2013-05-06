package aQute.lib.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Logic {

  public static <T> Collection<T> retain(Collection<T> first, Collection<T>... sets) {
    Set<T> result = new HashSet<T>(first);
    for (Collection<T> set : sets) {
      result.retainAll(set);
    }
    return result;
  }

  public static <T> Collection<T> remove(Collection<T> first, Collection<T>... sets) {
    Set<T> result = new HashSet<T>(first);
    for (Collection<T> set : sets) {
      result.removeAll(set);
    }
    return result;
  }
}
