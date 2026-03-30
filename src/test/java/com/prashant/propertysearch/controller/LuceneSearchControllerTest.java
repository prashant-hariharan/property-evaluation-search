package com.prashant.propertysearch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.service.lucene.LuceneIndexerService;
import com.prashant.propertysearch.service.lucene.LuceneSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LuceneSearchControllerTest {

    @Mock
    private LuceneIndexerService luceneIndexerService;
    @Mock
    private LuceneSearchService luceneSearchService;

    @InjectMocks
    private LuceneSearchController luceneSearchController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(luceneSearchController).build();
    }

    @Test
    void reindex_returnsIndexedDocumentsCount() throws Exception {
        when(luceneIndexerService.reindexAll()).thenReturn(25);

        mockMvc.perform(post("/api/lucene/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexedDocuments").value(25));

        verify(luceneIndexerService).reindexAll();
    }

    @Test
    void search_returnsSearchResponse() throws Exception {
        SearchRequest request = new SearchRequest(
                "waterfront",
                "Frankfurt",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                20
        );
        SearchResponse response = new SearchResponse();
        response.setTotalHits(3);
        when(luceneSearchService.search(request)).thenReturn(response);

        mockMvc.perform(post("/api/lucene/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(3));

        verify(luceneSearchService).search(request);
    }

    @Test
    void generateTestDataRouteOnLuceneController_isNotFound() throws Exception {
        mockMvc.perform(post("/api/lucene/test-data/generate"))
                .andExpect(status().isNotFound());
    }
}
