package com.kmwllc.lucille.callback;

import com.kmwllc.lucille.core.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

class Indexer implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(Indexer.class);
  
  private final IndexerDocumentManager manager;

  private volatile boolean running = true;

  public void terminate() {
    running = false;
    log.info("terminate");
  }

  public Indexer() {
    this.manager = new IndexerDocumentManager();
  }

  @Override
  public void run() {
    while (running) {
      Document doc;
      try {
        log.info("polling");
        doc = manager.retrieveCompleted();
      } catch (Exception e) {
        log.info("Indexer interrupted ", e);
        terminate();
        return;
      }
      if (doc == null) {
        log.info("received nothing");
        continue;
      }

      // TODO
      if (!doc.has("run_id")) {
        continue;
      }

      String runId = doc.getString("run_id");
      try {
        manager.sendToSolr(Collections.singletonList(doc));
        Receipt receipt = new Receipt(doc.getId(), runId, "SUCCEEDED", false);
        log.info("submitting receipt " + receipt);
        manager.submitReceipt(receipt);
      } catch (Exception e) {
        try {
          manager.submitReceipt(new Receipt(doc.getId(), runId, "FAILED" + e.getMessage(), false));
        } catch (Exception e2) {
          e2.printStackTrace();
        }
      }
    }
    try {
      manager.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.info("exit");
  }

}
