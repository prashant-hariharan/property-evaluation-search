package com.prashant.propertysearch.service;

import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import com.prashant.propertysearch.entity.PropertyType;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import com.prashant.propertysearch.repository.PropertyRepository;
import com.prashant.propertysearch.service.lucene.LuceneIndexerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for generating synthetic test data for properties and property evaluations.
 * This class provides utilities to generate batches of property and evaluation data for testing purposes.
 * It can also handle clearing existing data and reindexing Lucene indexes after data generation.
 *
 * The synthetic data is generated using predefined sets of city, street, and description values,
 * along with various randomization techniques to provide variability in the generated data.
 *
 * @author prashant
 */
@Service
@Slf4j
@AllArgsConstructor
public class TestDataGenerationService {
    private static final int SPECIAL_DATA_COUNT = 12;

    public static final String FRANKFURT = "Frankfurt";
    public static final String BERLIN = "Berlin";

    private static final String[] CITY_VALUES = {
      BERLIN, "Munich", "Hamburg", FRANKFURT, "Cologne","Nuremberg","Bonn",
            "Stuttgart", "Dusseldorf", "Leipzig", "Dresden", "Bremen"
    };
    private static final String[] STREET_VALUES = {
            "Bahnhof", "Haupt", "Garten", "Linden", "Schiller",
            "Goethe", "Berg", "Wiesen", "Sonnen", "Feld"
    };
    private static final String[] DESCRIPTION_VALUES = {
            "Spacious property with natural light and balcony",
            "Recently renovated unit with modern kitchen",
            "Quiet neighborhood and excellent transport links",
            "Investment-ready property with stable rental demand",
            "Family-friendly layout close to schools and parks",
            "Bright apartment with open-plan living and city views",
            "Move-in ready home near shopping streets and cafes",
            "Well-maintained property with efficient floor plan",
            "Modern interiors with premium finishes and storage space",
            "Commuter-friendly location close to metro and tram stops"
    };

    private final PropertyRepository propertyRepository;
    private final PropertyEvaluationRepository propertyEvaluationRepository;
    private final LuceneIndexerService luceneIndexerService;

    @PersistenceContext
    private EntityManager entityManager;

    public TestDataGenerationSummary generate(
            int propertyCount,
            int batchSize,
            boolean clearExistingData,
            boolean reindexAfterGeneration
    ) {
        long startNs = System.nanoTime();
        log.info("Starting synthetic test data generation. propertyCount={} batchSize={} clearExistingData={} reindexAfterGeneration={}",
                propertyCount, batchSize, clearExistingData, reindexAfterGeneration);

        if (clearExistingData) {
            log.info("Clearing existing property and evaluation data before generation");
            propertyEvaluationRepository.deleteAllInBatch();
            propertyRepository.deleteAllInBatch();
            entityManager.clear();
        }

        int createdProperties = 0;
        int createdEvaluations = 0;
        int sequence = 1;
        int batchNumber = 0;

        while (createdProperties < propertyCount) {
            batchNumber++;
            int currentBatchSize = Math.min(batchSize, propertyCount - createdProperties);
            List<Property> properties = new ArrayList<>(currentBatchSize);
            for (int i = 0; i < currentBatchSize; i++) {
                properties.add(buildProperty(sequence++));
            }

            propertyRepository.saveAll(properties);
            propertyRepository.flush();

            List<PropertyEvaluation> evaluations = new ArrayList<>(currentBatchSize);
            for (Property property : properties) {
                evaluations.add(buildEvaluation(property));
            }

            propertyEvaluationRepository.saveAll(evaluations);
            propertyEvaluationRepository.flush();

            entityManager.clear();
            createdProperties += currentBatchSize;
            createdEvaluations += currentBatchSize;
            if (batchNumber % 10 == 0 || createdProperties == propertyCount) {
                log.info("Generated data progress. batchesProcessed={} createdProperties={} createdEvaluations={}",
                        batchNumber, createdProperties, createdEvaluations);
            }
        }

        Integer reindexedDocuments = null;
        if (reindexAfterGeneration) {
            log.info("Triggering Lucene reindex after synthetic data generation");
            reindexedDocuments = luceneIndexerService.reindexAll();
            log.info("Lucene reindex after generation completed. reindexedDocuments={}", reindexedDocuments);
        }

        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info("Synthetic test data generation completed. requestedProperties={} createdProperties={} createdEvaluations={} durationMs={}",
                propertyCount, createdProperties, createdEvaluations, durationMs);
        return new TestDataGenerationSummary(
                propertyCount,
                createdProperties,
                createdEvaluations,
                reindexedDocuments,
                durationMs
        );
    }

    private Property buildProperty(int sequence) {
        if (sequence <= SPECIAL_DATA_COUNT) {
            return buildSpecialProperty(sequence);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String city = CITY_VALUES[random.nextInt(CITY_VALUES.length)];
        String street = STREET_VALUES[random.nextInt(STREET_VALUES.length)];
        PropertyType propertyType = PropertyType.values()[random.nextInt(PropertyType.values().length)];
        BigDecimal areaInSquareMeter = scaled(random.nextDouble(30.0, 350.0));

        return Property.builder()
                .title("Property " + sequence + " in " + city)
                .address((random.nextInt(1, 250) + " " + street + " Strasse"))
                .city(city)
                .postalCode(String.format("%05d", random.nextInt(0, 100000)))
                .areaInSquareMeter(areaInSquareMeter)
                .constructionYear(random.nextInt(1950, 2026))
                .propertyType(propertyType)
                .description(DESCRIPTION_VALUES[random.nextInt(DESCRIPTION_VALUES.length)])
                .build();
    }

    private PropertyEvaluation buildEvaluation(Property property) {
        if (isSpecialProperty(property)) {
            return PropertyEvaluation.builder()
                    .property(property)
                    .marketValue(resolveSpecialMarketValue(property))
                    .notes("Synthetic comparison evaluation")
                    .build();
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double multiplier = random.nextDouble(1200.0, 9000.0);
        BigDecimal marketValue = scaled(property.getAreaInSquareMeter().doubleValue() * multiplier);

        return PropertyEvaluation.builder()
                .property(property)
                .marketValue(marketValue)
                .notes("Synthetic benchmark evaluation")
                .build();
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private Property buildSpecialProperty(int sequence) {
        if (sequence == 1) {
            return Property.builder()
                    .title("Modern apartment with garage in Berlin")
                    .address("1 Benchmark Street")
                    .city(BERLIN)
                    .postalCode("10115")
                    .areaInSquareMeter(scaled(84.0))
                    .constructionYear(2018)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Bright apartment close to tram and river walk with fast metro access")
                    .build();
        }
        if (sequence == 2) {
            return Property.builder()
                    .title("Waterfront loft apartment with city skyline view")
                    .address("2 Benchmark Street")
                    .city(FRANKFURT)
                    .postalCode("60311")
                    .areaInSquareMeter(scaled(88.0))
                    .constructionYear(2019)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Open layout with industrial finishes and panoramic skyline interior")
                    .build();
        }
        if (sequence == 3) {
            return Property.builder()
                    .title("Premium apartment with lift and skyline terrace")
                    .address("3 Benchmark Street")
                    .city(FRANKFURT)
                    .postalCode("60313")
                    .areaInSquareMeter(scaled(92.0))
                    .constructionYear(2020)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Premium waterfront apartment with direct lift access and open loft plan")
                    .build();
        }
        if (sequence == 4) {
            return Property.builder()
                    .title("Waterfront loft residence near Frankfurt promenade")
                    .address("4 Benchmark Street")
                    .city(FRANKFURT)
                    .postalCode("60314")
                    .areaInSquareMeter(scaled(79.0))
                    .constructionYear(2017)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Waterfront loft inspired apartment with bright living area")
                    .build();
        }
        if (sequence == 5) {
            return Property.builder()
                    .title("Industrial apartment in Frankfurt river district")
                    .address("5 Benchmark Street")
                    .city(FRANKFURT)
                    .postalCode("60316")
                    .areaInSquareMeter(scaled(86.0))
                    .constructionYear(2016)
                    .propertyType(PropertyType.APARTMENT)
                    .description("River district location with renovated open plan design")
                    .build();
        }
        if (sequence == 6) {
            return Property.builder()
                    .title("Berlin apartment with school metro access")
                    .address("6 Benchmark Street")
                    .city(BERLIN)
                    .postalCode("10117")
                    .areaInSquareMeter(scaled(82.0))
                    .constructionYear(2015)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Well connected home with school metro access and family-friendly surroundings")
                    .build();
        }
        if (sequence == 7) {
            return Property.builder()
                    .title("Modern Berlin apartment near school metro access")
                    .address("7 Benchmark Street")
                    .city(BERLIN)
                    .postalCode("10119")
                    .areaInSquareMeter(scaled(78.0))
                    .constructionYear(2014)
                    .propertyType(PropertyType.APARTMENT)
                    .description("City center apartment offering direct school metro access for commuters")
                    .build();
        }
        if (sequence == 8) {
            return Property.builder()
                    .title("Contemporary apartment in Munich city quarter")
                    .address("8 Benchmark Street")
                    .city("Munich")
                    .postalCode("80331")
                    .areaInSquareMeter(scaled(74.0))
                    .constructionYear(2013)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Renovated apartment with efficient layout close to daily amenities")
                    .build();
        }
        if (sequence == 9) {
            return Property.builder()
                    .title("Hamburg apartment with bright living area")
                    .address("9 Benchmark Street")
                    .city("Hamburg")
                    .postalCode("20095")
                    .areaInSquareMeter(scaled(81.0))
                    .constructionYear(2012)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Well-maintained home with practical floor plan in central district")
                    .build();
        }
        if (sequence == 10) {
            return Property.builder()
                    .title("Cologne apartment near business district")
                    .address("10 Benchmark Street")
                    .city("Cologne")
                    .postalCode("50667")
                    .areaInSquareMeter(scaled(77.0))
                    .constructionYear(2011)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Comfortable apartment with balanced room sizes and modern finishes")
                    .build();
        }
        if (sequence == 11) {
            return Property.builder()
                    .title("Stuttgart apartment in quiet residential area")
                    .address("11 Benchmark Street")
                    .city("Stuttgart")
                    .postalCode("70173")
                    .areaInSquareMeter(scaled(83.0))
                    .constructionYear(2010)
                    .propertyType(PropertyType.APARTMENT)
                    .description("Clean and functional apartment suitable for long-term occupancy")
                    .build();
        }
        return Property.builder()
                .title("Nuremberg apartment near central shopping street")
                .address("12 Benchmark Street")
                .city("Nuremberg")
                .postalCode("90402")
                .areaInSquareMeter(scaled(76.0))
                .constructionYear(2009)
                .propertyType(PropertyType.APARTMENT)
                .description("Move-in ready apartment with natural light and compact storage solutions")
                .build();
    }

    private boolean isSpecialProperty(Property property) {
        return property.getAddress() != null && property.getAddress().endsWith("Benchmark Street");
    }

    private BigDecimal resolveSpecialMarketValue(Property property) {
        if ("1 Benchmark Street".equals(property.getAddress())) {
            return scaled(545000.00);
        }
        if ("2 Benchmark Street".equals(property.getAddress())) {
            return scaled(690000.00);
        }
        if ("3 Benchmark Street".equals(property.getAddress())) {
            return scaled(735000.00);
        }
        if ("4 Benchmark Street".equals(property.getAddress())) {
            return scaled(640000.00);
        }
        if ("5 Benchmark Street".equals(property.getAddress())) {
            return scaled(670000.00);
        }
        if ("6 Benchmark Street".equals(property.getAddress())) {
            return scaled(605000.00);
        }
        if ("7 Benchmark Street".equals(property.getAddress())) {
            return scaled(590000.00);
        }
        if ("8 Benchmark Street".equals(property.getAddress())) {
            return scaled(510000.00);
        }
        if ("9 Benchmark Street".equals(property.getAddress())) {
            return scaled(535000.00);
        }
        if ("10 Benchmark Street".equals(property.getAddress())) {
            return scaled(520000.00);
        }
        if ("11 Benchmark Street".equals(property.getAddress())) {
            return scaled(515000.00);
        }
        if ("12 Benchmark Street".equals(property.getAddress())) {
            return scaled(505000.00);
        }
        return scaled(property.getAreaInSquareMeter().doubleValue() * 5000.0);
    }

    public record TestDataGenerationSummary(
            int requestedProperties,
            int createdProperties,
            int createdEvaluations,
            Integer reindexedDocuments,
            long durationMs
    ) {
    }
}
