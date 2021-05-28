package com.kmwllc.lucille.producer.data.producer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.kmwllc.lucille.core.Document;
import com.kmwllc.lucille.core.DocumentException;
import com.kmwllc.lucille.producer.data.DocumentProducer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CSVDocumentProducer implements DocumentProducer {
  private static final CsvMapper mapper = new CsvMapper().enable(CsvParser.Feature.WRAP_AS_ARRAY);
  private boolean firstLineSchema = true;
  // TODO: use row number if nothing is provided, otherwise column number/column name
  // TODO: flag for skipping header row
  private String idFieldName = null;
  private final boolean childCopyParentMetadata;


  public CSVDocumentProducer(boolean childCopyParentMetadata) {
    this.childCopyParentMetadata = childCopyParentMetadata;
  }

  public CSVDocumentProducer setReadSchemaFromFirstLine(boolean readFromFirstLine) {
    this.firstLineSchema = readFromFirstLine;
    return this;
  }

  public CSVDocumentProducer setIdFieldName(String idFieldName) {
    this.idFieldName = idFieldName;
    return this;
  }
  // TODO: user can specify schema

  @Override
  public List<Document> produceDocuments(Path file, final Document doc) throws IOException, DocumentException {
    final CsvSchema schema;
    if (firstLineSchema) {
      schema = CsvSchema.emptySchema().withHeader();
    } else {
      // TODO: will this be supported?
      // maybe we can just add all the fields to a list of strings or something
      throw new UnsupportedOperationException("Not sure how to handle without schema");
    }

    // TODO: check out opencsv/make sure this doesn't read it all into memory
    final MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class).with(schema).readValues(file.toFile());
    if (!it.hasNext()) {
      return Collections.singletonList(doc);
    }

    String localId = idFieldName;

    List<Document> docs = new ArrayList<>();
    if (!childCopyParentMetadata) {
      docs.add(doc);
    }
    while (it.hasNext()) {
      Map<String, String> line = it.next();
      if (localId == null) {
        Optional<String> optionalKey = line.keySet().stream().findFirst();
        if (optionalKey.isEmpty()) {
          return Collections.singletonList(doc);
        }
        localId = optionalKey.get();
      }

      Document child;
      String idField = line.getOrDefault(localId, "");
      if (childCopyParentMetadata) {
        child = doc.clone();
        child.setField(Document.ID_FIELD, createId(file + ":" + idField));
      } else {
        child = new Document(createId(file + ":" + idField));
      }
      docs.add(child);
      // TODO: if they have a column with the same value as Document.ID_FIELD, throw an exception
      // TODO: allow user to overwrite id field if they want (but prefix id with user passed ID field)

      // user-specified header row -> pass header row through unless column names are provided
      // no header row -> user specifies comma delimited list of column names to be used as field names
      line.forEach(child::setField);
    }

    return docs;
  }
}
