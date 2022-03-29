package com.kmwllc.lucille.stage;

import com.kmwllc.lucille.core.Document;
import com.kmwllc.lucille.core.Stage;
import com.kmwllc.lucille.core.StageException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DictionaryLookupTest {

  private StageFactory factory = StageFactory.of(DictionaryLookup.class);

  @Test
  public void testDictionaryLookup() throws StageException {
    Stage stage = factory.get("DictionaryLookupTest/config.conf");

    // Ensure that exact matches are correctly extracted
    Document doc = new Document("doc");
    doc.setField("input1", "China");
    stage.processDocument(doc);
    assertEquals("China", doc.getStringList("output1").get(0));

    // Ensure that multiple matches can be extracted, even in multivalued fields
    Document doc2 = new Document("doc2");
    doc2.setField("input1", "Canada");
    doc2.addToField("input1", "United States");
    doc2.setField("input3", "Taiwan");
    stage.processDocument(doc2);
    assertEquals("Canada", doc2.getStringList("output1").get(0));
    assertEquals("United States", doc2.getStringList("output1").get(1));
    assertEquals("Taiwan", doc2.getStringList("output3").get(0));

    // ensure that partial matches do not get extracted
    Document doc3 = new Document("doc2");
    doc3.setField("input2", "United States of America");
    stage.processDocument(doc3);
    assertNull(doc3.getStringList("output2"));
    
    
    // Ensure that if there are multiple payloads, that the payloads get mapped to the output as expected.
    Document doc4 = new Document("doc4");
    doc4.setField("input1", "foo");
    stage.processDocument(doc4);
    System.out.println(doc4);
    List<String> vals = doc4.getStringList("output1");
    assertEquals(vals.size(), 3);
    assertEquals(vals.get(0), "bar");
    assertEquals(vals.get(1), "baz");
    assertEquals(vals.get(2), "boom");
    
    

  }
}
