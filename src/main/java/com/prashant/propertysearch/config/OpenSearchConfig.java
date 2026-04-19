package com.prashant.propertysearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenSearchConfig {

    private static final String PROPERTY_TEXT_ANALYZER = "property_text_analyzer";
    private static final String SYNONYM_FILTER = "synonym_filter";

    @Bean
    public RestClient openSearchRestClient(@Value("${app.opensearch.base-url:http://localhost:9200}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("openSearchAnalysisSettings")
    public Map<String, Object> openSearchAnalysisSettings() {
        return Map.of(
                "settings", Map.of(
                        "analysis", Map.of(
                                "filter", Map.of(
                                        SYNONYM_FILTER, Map.of(
                                                "type", "synonym_graph",
                                                "synonyms", List.of(
                                                        "flat, apartment",
                                                        "garage, parking",
                                                        "metro, subway",
                                                        "loft, studio"
                                                )
                                        )
                                ),
                                "analyzer", Map.of(
                                        PROPERTY_TEXT_ANALYZER, Map.of(
                                                "tokenizer", "standard",
                                                "filter", List.of("lowercase", SYNONYM_FILTER)
                                        )
                                )
                        )
                )
        );
    }
}
