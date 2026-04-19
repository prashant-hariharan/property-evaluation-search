package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.service.TestDataGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TestDataGenerationControllerTest {

    @Mock
    private TestDataGenerationService testDataGenerationService;

    @InjectMocks
    private TestDataGenerationController testDataGenerationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testDataGenerationController).build();
    }

    @Test
    void generateTestData_returnsSummaryAndUsesDefaultParameters() throws Exception {
        TestDataGenerationService.TestDataGenerationSummary summary =
                new TestDataGenerationService.TestDataGenerationSummary(100000, 100000, 100000, 99999, 99998, 1234L);
        when(testDataGenerationService.generate(100000, 100, true, true)).thenReturn(summary);

        mockMvc.perform(post("/api/test-data/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedProperties").value(100000))
                .andExpect(jsonPath("$.createdProperties").value(100000))
                .andExpect(jsonPath("$.createdEvaluations").value(100000))
                .andExpect(jsonPath("$.reindexedDocuments").value(99999))
                .andExpect(jsonPath("$.openSearchReindexedDocuments").value(99998))
                .andExpect(jsonPath("$.durationMs").value(1234));

        verify(testDataGenerationService).generate(100000, 100, true, true);
    }
}
