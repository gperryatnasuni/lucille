package com.kmwllc.lucille.util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkedMultiMap implements MultiMap {

  private final Set<Class<?>> supportedTypes;
  private final Map<String, Class<?>> types;
  private final Map<String, Object> single;
  private final Map<String, List<Object>> multi;

  public LinkedMultiMap(Set<Class<?>> supportedTypes) {

    if (supportedTypes == null || supportedTypes.isEmpty()) {
      throw new IllegalArgumentException("supportedTypes cannot be null or empty");
    }

    this.supportedTypes = supportedTypes;
    this.types = new LinkedHashMap<>();
    this.single = new LinkedHashMap<>();
    this.multi = new LinkedHashMap<>();

    //    if (singleValued == null || multiValued == null || types == null) {
    //      throw new IllegalArgumentException("constructor parameters must not be null");
    //    }
    //
    //    if (!Collections.disjoint(singleValued.keySet(), multiValued.keySet())) {
    //      throw new IllegalArgumentException("singleValued and multiValued must have disjoint
    // keys");
    //    }
    //
    //    this.keys = new HashSet<>(singleValued.keySet());
    //    keys.addAll(multiValued.keySet());
    //
    //    if(!keys.equals(types.keySet())) {
    //      throw new IllegalArgumentException("types must have the same keys as singleValued and
    // multiValued");
    //    }
    //
    //    this.singleValued = singleValued;
    //    this.multiValued = multiValued;
    //    this.types = types;
  }

  @Override
  public boolean isEmpty() {
    return single.isEmpty() && multi.isEmpty();
  }

  @Override
  public boolean contains(String key) {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    return single.containsKey(key) || multi.containsKey(key);
  }

  @Override
  public boolean isMultiValued(String key) {
    checkKey(key);
    return multi.containsKey(key);
  }

  @Override
  public int size() {
    return single.size() + multi.size();
  }

  @Override
  public int length(String name) {

    if (!isMultiValued(name)) {
      return 1;
    }

    List<Object> list = multi.get(name);
    if (list == null) {
      throw new IllegalStateException("name " + name + " is multi-valued but has no values");
    }
    return list.size();
  }

  @Override
  public Set<String> getKeys() {
    return Stream.concat(single.keySet().stream(), multi.keySet().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<String> getSingleKeys() {
    return new LinkedHashSet<>(single.keySet());
  }

  @Override
  public Set<String> getMultiKeys() {
    return new LinkedHashSet<>(multi.keySet());
  }

  @Override
  public Map<String, Object> getSingleValued() {
    return new LinkedHashMap<>(single);
  }

  @Override
  public Map<String, List<Object>> getMultiValued() {
    return new LinkedHashMap<>(multi);
  }

  @Override
  public Class<?> getType(String key) {
    checkKey(key);
    return types.get(key);
  }

  @Override
  public Map<String, Class<?>> getTypes() {
    return new LinkedHashMap<>(types);
  }

  @Override
  public Object getOne(String name) {

    if (isMultiValued(name)) {
      List<Object> list = multi.get(name);
      if (list.isEmpty()) {
        throw new IllegalArgumentException("name " + name + " is multi-valued but has no values");
      }
      return list.get(0);
    }

    return single.get(name);
  }

  @Override
  public List<Object> getMany(String name) {
    return isMultiValued(name)
        ? new ArrayList<>(multi.get(name))
        : Collections.singletonList(single.get(name));
  }

  @Override
  public MultiMap deepCopy() {

    // todo might need to copy each individual list and object within
    LinkedMultiMap copy = new LinkedMultiMap(supportedTypes);
    for (String key : getSingleKeys()) {
      copy.putOne(key, getOne(key));
    }
    for (String key : getMultiKeys()) {
      copy.putMany(key, getMany(key));
    }
    return copy;
  }

  @Override
  public void putOne(String key, Object value) throws IllegalArgumentException {

    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }

    if (value == null) {
      types.remove(key);
    } else {
      Class<?> type = value.getClass();
      if (!supportedTypes.contains(type)) {
        throw new IllegalArgumentException(
            "value must be one of " + supportedTypes + " but was " + type);
      }
      types.put(key, type);
    }

    multi.remove(key);
    single.put(key, value);
  }

  @Override
  public void putMany(String name, List<Object> values) {

    if (name == null || values == null) {
      throw new IllegalArgumentException("name and values must not be null");
    }

    // some values may be null - find non null
    Class<?> c = getClassFromList(values);
    if (c != null) {
      types.put(name, c);
    }

    single.remove(name);
    multi.put(name, new ArrayList<>(values));
  }

  @Override
  public void add(String name, Object value) {

    addValueType(name, value);

    if (!contains(name)) {
      multi.put(name, makeList(value));
      return;
    }

    if (isMultiValued(name)) {
      multi.get(name).add(value);
    } else {
      multi.put(name, makeList(single.remove(name), value));
    }
  }

  @Override
  public void addAll(String name, List<Object> values) {
    for (Object value : values) {
      add(name, value);
    }
  }

  private void addValueType(String name, Object value) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    if (value != null) {
      Class<?> type = value.getClass();
      if (!supportedTypes.contains(type)) {
        throw new IllegalArgumentException(
            "value must be one of " + supportedTypes + " but was " + type);
      }
      if (types.containsKey(name)) {
        if (!types.get(name).equals(type)) {
          throw new IllegalArgumentException(
              "value must be of type " + types.get(name) + " but was " + type);
        }
      } else {
        types.put(name, type);
      }
    }
  }

  @Override
  public void setOrAdd(
      String name,
      Object value) { // todo if used to be an empty list need to add type, write a test
    if (!contains(name)) {
      putOne(name, value);
    } else {
      add(name, value);
    }
  }

  @Override
  public void clear() {
    types.clear();
    single.clear();
    multi.clear();
  }

  @Override
  public void rename(String oldName, String newName) {
    checkKey(oldName);
    if (newName == null || contains(newName)) {
      throw new IllegalArgumentException("newName must not be null or already exist");
    }
    // todo review which approach is better
    types.remove(oldName);
    if (isMultiValued(oldName)) {
      //      multi.put(newName, multi.remove(oldName));
      putMany(newName, multi.remove(oldName));
    } else {
      //      single.put(newName, single.remove(oldName));
      putOne(newName, single.remove(oldName));
    }
    //    types.put(newName, types.remove(oldName));
  }

  @Override
  public void remove(String name) {
    checkKey(name);
    types.remove(name);
    single.remove(name);
    multi.remove(name);
  }

  @Override
  public void removeFromArray(String name, int index) {
    if (!isMultiValued(name)) {
      throw new IllegalArgumentException("name must be multi-valued");
    }
    List<Object> list = multi.get(name);
    if (index < 0 || index >= list.size()) {
      throw new IllegalArgumentException(
          "given index " + index + " is out of bounds for list of size " + list.size());
    }
    list.remove(index);
  }

  @Override
  public void removeDuplicates(String name) {
    if (!isMultiValued(name)) {
      throw new IllegalArgumentException("name must be multi-valued");
    }
    multi.put(name, new ArrayList<>(new LinkedHashSet<>(multi.get(name))));
  }

  @Override
  public String toString() {
    return Stream.concat(single.entrySet().stream(), multi.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LinkedMultiMap multiMap = (LinkedMultiMap) o;
    return Objects.equals(types, multiMap.types)
        && Objects.equals(single, multiMap.single)
        && Objects.equals(multi, multiMap.multi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(types, single, multi);
  }

  private void checkKey(String key) {
    if (!contains(key)) {
      throw new IllegalArgumentException("key " + key + " does not exist");
    }
  }

  private Class<?> getClassFromList(List<Object> list) {
    Class<?> type = null;
    for (Object o : list) {
      if (o != null) {
        if (type == null) {
          type = o.getClass();
          if (!supportedTypes.contains(type)) {
            throw new IllegalArgumentException("unsupported type " + type);
          }
        } else {
          if (!o.getClass().equals(type)) {
            throw new IllegalArgumentException("values must all be of the same type");
          }
        }
      }
    }
    return type;
  }

  // todo check if can be removed
  private List<Object> makeList(Object... values) {
    return new ArrayList<>(Arrays.asList(values));
  }
}
