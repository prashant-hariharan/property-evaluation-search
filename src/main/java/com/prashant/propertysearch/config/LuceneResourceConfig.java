package com.prashant.propertysearch.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.util.CharsRef;

/**
 *
 * Configuration class for providing Lucene-related resources such as the directory
 * for storing indexes and a custom analyzer for processing text with synonym support.
  <p>
 * Note: The Lucene Directory and Analyzer beans include destroy methods to handle resource
 * cleanup when these beans are no longer needed.
 *
 * @author prashant
 */
@Configuration
public class LuceneResourceConfig {

    @Bean(destroyMethod = "close")
    public Directory luceneDirectory(@Value("${app.lucene.index-path:target/lucene-index}") String indexPath) throws IOException {
        Path resolvedIndexPath = Path.of(indexPath).toAbsolutePath().normalize();
        Files.createDirectories(resolvedIndexPath);
        return FSDirectory.open(resolvedIndexPath);
    }

    @Bean(destroyMethod = "close")
    public Analyzer luceneAnalyzer() throws IOException {
        SynonymMap synonymMap = buildSynonymMap();
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new StandardTokenizer();
                TokenStream tokenStream = new LowerCaseFilter(tokenizer);
                tokenStream = new SynonymGraphFilter(tokenStream, synonymMap, true);
                return new TokenStreamComponents(tokenizer, tokenStream);
            }
        };
    }

    private SynonymMap buildSynonymMap() throws IOException {
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        addBidirectionalSynonym(builder, "flat", "apartment");
        addBidirectionalSynonym(builder, "garage", "parking");
        addBidirectionalSynonym(builder, "metro", "subway");
        addBidirectionalSynonym(builder, "loft", "studio");
        return builder.build();
    }

    private void addBidirectionalSynonym(SynonymMap.Builder builder, String left, String right) {
        builder.add(new CharsRef(left), new CharsRef(right), true);
        builder.add(new CharsRef(right), new CharsRef(left), true);
    }
}
