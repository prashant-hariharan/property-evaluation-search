package com.prashant.propertysearch.listener;

import com.prashant.propertysearch.event.PropertyChangedEvent;
import com.prashant.propertysearch.event.PropertyEvaluationChangedEvent;
import com.prashant.propertysearch.service.lucene.LuceneIndexerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PropertySearchIndexEventListener {

    private final LuceneIndexerService luceneIndexerService;

    public PropertySearchIndexEventListener(LuceneIndexerService luceneIndexerService) {
        this.luceneIndexerService = luceneIndexerService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("propertySearchIndexTaskExecutor")
    @Retryable(
        retryFor = IllegalStateException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1500)
    )
    public void onPropertyChanged(PropertyChangedEvent event) {
        try {
            if (event.action() == PropertyChangedEvent.Action.DELETE) {
                luceneIndexerService.deletePropertyDocument(event.propertyId());
                return;
            }
            luceneIndexerService.upsertPropertyDocument(event.propertyId());
        } catch (Exception e) {
            log.error("Failed to modify Lucene index for property change. action={} propertyId={} error={}",
                event.action(),
                event.propertyId(),
                e.getMessage(),
                e
            );
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("propertySearchIndexTaskExecutor")
    @Retryable(
        retryFor = IllegalStateException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1500)
    )
    public void onPropertyEvaluationChanged(PropertyEvaluationChangedEvent event) {
        try {
            luceneIndexerService.upsertPropertyDocument(event.propertyId());
        } catch (Exception e) {
            log.error("Failed to modify Lucene index for property evaluation change. propertyId={} error={}",
                event.propertyId(),
                e.getMessage(),
                e
            );
            throw e;
        }
    }
}
