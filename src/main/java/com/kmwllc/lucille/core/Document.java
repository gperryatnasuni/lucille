package com.kmwllc.lucille.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public interface Document extends Cloneable {

  String ID_FIELD = "id";
  String RUNID_FIELD = "run_id";
  String ERROR_FIELD = "errors";
  String CHILDREN_FIELD = ".children";

  Set<String> RESERVED_FIELDS = new HashSet<>(List.of(ID_FIELD, RUNID_FIELD, CHILDREN_FIELD));

  void removeField(String name);

  void removeFromArray(String name, int index);

  /**
   * Updates the designated field according to the provided UpdateMode.
   *
   * <p>APPEND: the provided values will be appended to the field. OVERWRITE: the provided values
   * will overwrite any current field values SKIP: the provided values will populate the field if
   * the field didn't previously exist; otherwise no change will be made.
   *
   * <p>In all cases the field will be created if it doesn't already exist.
   */
  void update(String name, UpdateMode mode, String... values);

  void update(String name, UpdateMode mode, Long... values);

  void update(String name, UpdateMode mode, Integer... values);

  void update(String name, UpdateMode mode, Boolean... values);

  void update(String name, UpdateMode mode, Double... values);

  void update(String name, UpdateMode mode, Instant... values);

  void initializeRunId(String value);

  void clearRunId();

  void setField(String name, String value);

  void setField(String name, Long value);

  void setField(String name, Integer value);

  void setField(String name, Boolean value);

  void setField(String name, Double value);

  void setField(String name, JsonNode value);

  void setField(String name, Instant value);

  void renameField(String oldName, String newName, UpdateMode mode);

  // todo had to add for concrete implementation
  ObjectNode getData();

  // This will return null in two cases : 1) If the field is absent 2) IF the field is present but
  // contains a null.
  // To distinguish between these, you can call has().
  // Calling getString for a field which is multivalued will return the first value in the list of
  // Strings.
  String getString(String name);

  List<String> getStringList(String name);

  Integer getInt(String name);

  List<Integer> getIntList(String name);

  Double getDouble(String name);

  List<Double> getDoubleList(String name);

  Boolean getBoolean(String name);

  List<Boolean> getBooleanList(String name);

  Long getLong(String name);

  List<Long> getLongList(String name);

  Instant getInstant(String name);

  List<Instant> getInstantList(String name);

  int length(String name);

  String getId();

  String getRunId();

  boolean has(String name);

  boolean hasNonNull(String name);

  boolean isMultiValued(String name);

  void addToField(String name, String value);

  void addToField(String name, Long value);

  void addToField(String name, Integer value);

  void addToField(String name, Boolean value);

  void addToField(String name, Double value);

  /**
   * Converts a given date in Instant form to a string according to DateTimeFormatter.ISO_INSTANT,
   * it can then be accessed as a string via getString() or a converted back to an Instant via
   * getInstant().
   *
   * @param name
   * @param value
   */
  void addToField(String name, Instant value);

  /**
   * Sets the field to the given value if the field is not already present; otherwise adds it to the
   * field.
   *
   * <p>If the field does not already exist and this method is called once, the field will be
   * created as single-valued; if the field already exists and/or this method is called more than
   * once, the field will converted to a list of values.
   */
  void setOrAdd(String name, String value);

  void setOrAdd(String name, Long value);

  void setOrAdd(String name, Integer value);

  void setOrAdd(String name, Boolean value);

  void setOrAdd(String name, Double value);

  /**
   * Adds a given date in Instant form to a document according to DateTimeFormatter.ISO_INSTANT, can
   * then be accessed as a string via getString() or a converted back to an Instant via
   * getInstant().
   *
   * @param name
   * @param value
   */
  void setOrAdd(String name, Instant value);

  /**
   * Adds a given field from the designated "other" document to the current document. If a field is
   * already present on the current document, the field is converted to a list.
   *
   * @param name the name of the field to add
   * @param other the document to add the field from
   * @throws IllegalArgumentException if this method is called with a reserved field like id
   */
  void setOrAdd(String name, Document other) throws IllegalArgumentException;

  /**
   * Adds all the fields of the designated "other" document to the current document, excluding
   * reserved fields like id. If a field is already present on the current document, the field is
   * converted to a list and the new value is appended.
   */
  void setOrAddAll(Document other);

  void logError(String description);

  Map<String, Object> asMap();

  void addChild(Document document);

  boolean hasChildren();

  List<Document> getChildren();

  Set<String> getFieldNames();

  /**
   * A method to remove duplicate values from multivalued fields in a document and place the values
   * into a target field. If the target field is null or the same as the original field, then
   * modification will happen in place.
   *
   * @param fieldName the field to remove duplicate values from
   * @param targetFieldName the field to copy to
   */
  void removeDuplicateValues(String fieldName, String targetFieldName);

  // todo had to add this because clone has protected access access in Object
  // todo what is the difference between this and clone?
  Document copy();

  static Document create(ObjectNode node) throws DocumentException {
    return new JsonDocument(node);
  }

  static Document create(String id) {
    return new JsonDocument(id);
  }

  static Document create(String id, String runId) {
    return new JsonDocument(id, runId);
  }

  static Document createFromJson(String json) throws DocumentException, JsonProcessingException {
    return JsonDocument.fromJsonString(json);
  }

  static Document createFromJson(String json, UnaryOperator<String> idUpdater)
      throws DocumentException, JsonProcessingException {
    return JsonDocument.fromJsonString(json, idUpdater);
  }
}
