package com.prashant.propertysearch.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchSearchResultDto {

    private HitsDto hits;

    public HitsDto getHits() {
        return hits;
    }

    public void setHits(HitsDto hits) {
        this.hits = hits;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HitsDto {

        private TotalDto total;

        private List<HitDto> hits;

        public TotalDto getTotal() {
            return total;
        }

        public void setTotal(TotalDto total) {
            this.total = total;
        }

        public List<HitDto> getHits() {
            return hits;
        }

        public void setHits(List<HitDto> hits) {
            this.hits = hits;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TotalDto {

        private Integer value;

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HitDto {

        @JsonProperty("_source")
        private SourceDto source;

        @JsonProperty("_score")
        private Double score;

        public SourceDto getSource() {
            return source;
        }

        public void setSource(SourceDto source) {
            this.source = source;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SourceDto {

        private String propertyId;
        private String title;
        private String city;
        private String postalCode;
        private String propertyType;
        private String description;
        private Double latitude;
        private Double longitude;
        private Double areaInSquareMeter;
        private Double evaluationMarketValue;

        public String getPropertyId() {
            return propertyId;
        }

        public void setPropertyId(String propertyId) {
            this.propertyId = propertyId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getPropertyType() {
            return propertyType;
        }

        public void setPropertyType(String propertyType) {
            this.propertyType = propertyType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getAreaInSquareMeter() {
            return areaInSquareMeter;
        }

        public void setAreaInSquareMeter(Double areaInSquareMeter) {
            this.areaInSquareMeter = areaInSquareMeter;
        }

        public Double getEvaluationMarketValue() {
            return evaluationMarketValue;
        }

        public void setEvaluationMarketValue(Double evaluationMarketValue) {
            this.evaluationMarketValue = evaluationMarketValue;
        }
    }
}
