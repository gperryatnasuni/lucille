package com.kmwllc.lucille.stage;

import com.kmwllc.lucille.core.Document;
import com.kmwllc.lucille.core.Stage;
import com.kmwllc.lucille.core.StageException;
import com.kmwllc.lucille.core.UpdateMode;
import com.typesafe.config.Config;
import java.util.List;
import org.ahocorasick.trie.PayloadTrie;

/**
 * Checks if any of the given fields contain any of the given values and tags the given output field
 * with the value.
 *
 * <p>Config Parameters:
 *
 * <p>- contains (List<String>) : A list of values to search for - output (String) : The field to
 * tag if a match is found - value (String) : The value to tag the output field with - ignoreCase
 * (Boolean, Optional) : Determines if the matching should be case insensitive. Defaults to true. -
 * field (List<String>) : The fields to be searched
 */
public class Contains extends Stage {

  private final List<String> contains;
  private final String output;
  private final String value;
  private final boolean ignoreCase;
  private final List<String> fields;

  private PayloadTrie<String> trie;

  public Contains(Config config) {
    super(
        new StageSpec(config)
            .withRequiredProperties("contains", "output", "value", "fields")
            .withOptionalProperties("ignoreCase"));

    this.contains = config.getStringList("contains");
    this.output = config.getString("output");
    this.value = config.getString("value");
    this.fields = config.getStringList("fields");

    this.ignoreCase = config.hasPath("ignoreCase") ? config.getBoolean("ignoreCase") : true;
  }

  @Override
  public void start() throws StageException {
    if (contains.isEmpty())
      throw new StageException("Must supply at least one value to check on the field.");

    trie = buildTrie();
  }

  private PayloadTrie<String> buildTrie() {
    PayloadTrie.PayloadTrieBuilder<String> trieBuilder = PayloadTrie.builder();

    // For each of the possible Trie settings
    trieBuilder = trieBuilder.stopOnHit();
    trieBuilder = trieBuilder.onlyWholeWords();
    if (ignoreCase) {
      trieBuilder = trieBuilder.ignoreCase();
    }

    for (String val : contains) {
      trieBuilder = trieBuilder.addKeyword(val, val);
    }

    return trieBuilder.build();
  }

  @Override
  public List<Document> processDocument(Document doc) throws StageException {
    for (String field : fields) {
      if (!doc.has(field)) continue;

      List<String> values = doc.getStringList(field);
      for (String value : values) {
        if (trie.containsMatch(value)) {
          doc.update(output, UpdateMode.DEFAULT, this.value);
          return null;
        }
      }
    }

    return null;
  }
}
