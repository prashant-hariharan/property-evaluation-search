package com.prashant.propertysearch.service.lucene;

import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import com.prashant.propertysearch.repository.PropertyRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.prashant.propertysearch.utils.LuceneDocumentFields.*;
import static com.prashant.propertysearch.utils.LuceneDocumentUtils.toDocument;

/**
 * Service for managing and interacting with a Lucene-based search index.
 * This service provides methods for reindexing, updating, and deleting documents
 * in the Lucene index based on property data and property evaluations.
 *
 * The Lucene index is initialized during the service's lifecycle via a post-construction
 * initialization method and closed during pre-destruction to ensure proper resource management.
 *
 * Thread safety is achieved by synchronizing methods that perform write operations on the Lucene index.
 *
 * Future optimization note:
 * For higher read/write concurrency and near-real-time visibility of writes, this service can be
 * migrated to a shared IndexWriter + SearcherManager setup coordinated by
 * ControlledRealTimeReopenThread. That model removes the need for opening a fresh reader per
 * search and allows waiting for specific write generations when strict read-after-write behavior
 * is required.
 * @author prashant
 */


@Service
@Slf4j
public class LuceneIndexerService {

  private static final int REINDEX_PAGE_SIZE = 1000;
  private static final int REINDEX_PROGRESS_LOG_INTERVAL = 10;
  private final PropertyRepository propertyRepository;
  private final PropertyEvaluationRepository propertyEvaluationRepository;
  private final Directory directory;
  private final Analyzer analyzer;

  private IndexWriter indexWriter;

  public LuceneIndexerService(
    PropertyRepository propertyRepository,
    PropertyEvaluationRepository propertyEvaluationRepository,
    Directory directory,
    Analyzer analyzer
  ) {
    this.propertyRepository = propertyRepository;
    this.propertyEvaluationRepository = propertyEvaluationRepository;
    this.directory = directory;
    this.analyzer = analyzer;
  }


  @PostConstruct
  void init() {
    try {
      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
      indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
      this.indexWriter = new IndexWriter(directory, indexWriterConfig);
      log.info("Lucene index writer initialized");
    } catch (LockObtainFailedException e) {
      throw new IllegalStateException(
        "Failed to initialize Lucene index writer resources. Index is locked.",
        e
      );
    } catch (IOException e) {
      throw new IllegalStateException(
        "Failed to initialize Lucene index writer resources",
        e
      );
    }
  }

  @Transactional(readOnly = true)
  public synchronized int reindexAll() {
    long startNs = System.nanoTime();
    log.info("Starting Lucene reindex. pageSize={}", REINDEX_PAGE_SIZE);
    try {
      indexWriter.deleteAll();
      ReindexStats reindexStats = new ReindexStats();
      int batchNumber = 0;
      Pageable pageable = PageRequest.of(0, REINDEX_PAGE_SIZE);
      Page<UUID> page;

      do {
        page = propertyRepository.findAllPropertyIds(pageable);
        List<UUID> propertyIds = page.getContent();
        if (!propertyIds.isEmpty()) {
          batchNumber++;
          reindexPropertyIds(propertyIds, reindexStats);
          indexWriter.commit();
          log.debug("Indexed Lucene batch={} idsInBatch={} totalIndexed={} totalFailed={}",
            batchNumber,
            propertyIds.size(),
            reindexStats.indexedCount,
            reindexStats.failedPropertyIds.size()
          );
          if (batchNumber % REINDEX_PROGRESS_LOG_INTERVAL == 0 || !page.hasNext()) {
            log.info("Lucene reindex progress. batchesProcessed={} indexedDocuments={} failedDocuments={}",
              batchNumber,
              reindexStats.indexedCount,
              reindexStats.failedPropertyIds.size()
            );
          }
        }
        pageable = page.nextPageable();
      } while (page.hasNext());

      if (reindexStats.indexedCount == 0) {
        indexWriter.commit();
      }
      long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
      log.info("Lucene reindex completed. indexedDocuments={} failedDocuments={} durationMs={}",
        reindexStats.indexedCount,
        reindexStats.failedPropertyIds.size(),
        durationMs
      );
      return reindexStats.indexedCount;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to rebuild Lucene index", e);
    }
  }

  @Transactional(readOnly = true)
  public synchronized ReindexByIdsResult reindexByPropertyIds(List<UUID> propertyIds) {
    if (propertyIds == null || propertyIds.isEmpty()) {
      return new ReindexByIdsResult(0, List.of());
    }

    ReindexStats reindexStats = new ReindexStats();
    try {
      reindexPropertyIds(propertyIds, reindexStats);
      indexWriter.commit();
      log.info("Lucene reindex by ids completed. requestedIds={} indexedDocuments={} failedDocuments={}",
        propertyIds.size(),
        reindexStats.indexedCount,
        reindexStats.failedPropertyIds.size()
      );
      return new ReindexByIdsResult(reindexStats.indexedCount, List.copyOf(reindexStats.failedPropertyIds));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to reindex Lucene documents for property ids", e);
    }
  }

  @Transactional
  public synchronized void upsertPropertyDocument(UUID propertyId) {
    try {
      Term propertyIdTerm = new Term(PROPERTY_ID, propertyId.toString());
      Property property = propertyRepository.findById(propertyId).orElse(null);
      if (property == null) {
        indexWriter.deleteDocuments(propertyIdTerm);
        log.debug("Removed Lucene document for missing propertyId={}", propertyId);
      } else {
        PropertyEvaluation latestEvaluation = propertyEvaluationRepository
          .findTopByPropertyIdOrderByCreatedAtDescIdDesc(propertyId)
          .orElse(null);
        indexWriter.updateDocument(propertyIdTerm, toDocument(property, latestEvaluation));
        log.debug("Upserted Lucene document for propertyId={}", propertyId);
      }
      indexWriter.commit();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to upsert Lucene document for property: " + propertyId, e);
    }
  }

  public synchronized void deletePropertyDocument(UUID propertyId) {
    try {
      indexWriter.deleteDocuments(new Term(PROPERTY_ID, propertyId.toString()));
      indexWriter.commit();
      log.debug("Deleted Lucene document for propertyId={}", propertyId);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete Lucene document for property: " + propertyId, e);
    }
  }

  private PropertyEvaluation selectLatestEvaluation(PropertyEvaluation left, PropertyEvaluation right) {
    Comparator<PropertyEvaluation> comparator = Comparator.comparing(
      PropertyEvaluation::getCreatedAt,
      Comparator.nullsFirst(Comparator.naturalOrder())
    ).thenComparing(
      PropertyEvaluation::getId,
      Comparator.nullsFirst(Comparator.naturalOrder())
    );
    return comparator.compare(left, right) >= 0 ? left : right;
  }

  private void indexProperties(
    List<Property> properties,
    Map<UUID, PropertyEvaluation> latestEvaluationByPropertyId,
    ReindexStats reindexStats
  ) {
    for (Property property : properties) {
      try {
        indexWriter.addDocument(toDocument(property, latestEvaluationByPropertyId.get(property.getId())));
        reindexStats.indexedCount++;
      } catch (Exception e) {
        reindexStats.failedPropertyIds.add(property.getId());
        log.error("Skipping property during Lucene reindex. propertyId={} error={}",
          property.getId(),
          e.getMessage()
        );
      }
    }
  }

  private void reindexPropertyIds(List<UUID> propertyIds, ReindexStats reindexStats) {
    if (propertyIds == null || propertyIds.isEmpty()) {
      return;
    }
    List<Property> properties = propertyRepository.findAllById(propertyIds);
    List<UUID> existingPropertyIds = properties.stream().map(Property::getId).toList();
    Map<UUID, PropertyEvaluation> latestEvaluationByPropertyId = existingPropertyIds.isEmpty()
      ? Map.of()
      : propertyEvaluationRepository.findLatestByPropertyIds(existingPropertyIds)
      .stream()
      .collect(Collectors.toMap(
        e -> e.getProperty().getId(),
        e -> e,
        this::selectLatestEvaluation
      ));
    indexProperties(properties, latestEvaluationByPropertyId, reindexStats);
  }

  private static final class ReindexStats {
    private int indexedCount;
    private final List<UUID> failedPropertyIds = new ArrayList<>();
  }

  public record ReindexByIdsResult(
    int indexedDocuments,
    List<UUID> failedPropertyIds
  ) {
  }

  @PreDestroy
  void destroy() {
    try {
      if (indexWriter != null) {
        indexWriter.close();
      }
      log.info("Closed Lucene index writer resources");
    } catch (IOException ignored) {
      // Best effort shutdown.
    }
  }



}
