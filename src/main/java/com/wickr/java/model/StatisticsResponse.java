package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsResponse {
    @JsonProperty("statistics")
    private Statistics statistics;

    @Deprecated
    public StatisticsResponse() {

    }

    public boolean isEmpty() {
        return null == this.statistics || this.statistics.isEmpty();
    }

    public Statistics getStatistics() {
        return statistics != null ? statistics : Statistics.createEmpty();
    }
}
