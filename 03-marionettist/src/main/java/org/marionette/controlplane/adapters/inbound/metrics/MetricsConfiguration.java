package org.marionette.controlplane.adapters.inbound.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConfigurationProperties(prefix = "marionette.metrics")
public class MetricsConfiguration {
    
    private Map<String, MetricQueryConfig> queries = new HashMap<>();
    private boolean enabled = true;
    private int defaultTimeRangeMinutes = 15;
    private String defaultStep = "30s";
    
    public MetricsConfiguration() {
    }
    
    @PostConstruct
    public void populateQueriesFromEnvironment() {
        // Pattern to match: MARIONETTE_METRICS_QUERIES_<KEY>_<PROPERTY>
        Pattern pattern = Pattern.compile("^MARIONETTE_METRICS_QUERIES_([A-Z_]+)_(DISPLAYNAME|QUERY|UNIT|DESCRIPTION|ENABLED)$");
        
        Map<String, MetricQueryConfig> tempQueries = new HashMap<>();
        
        // Iterate through all environment variables
        System.getenv().forEach((key, value) -> {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                String queryKey = matcher.group(1).toLowerCase();
                String property = matcher.group(2).toLowerCase();
                
                // Get or create MetricQueryConfig for this queryKey
                MetricQueryConfig config = tempQueries.computeIfAbsent(queryKey, k -> new MetricQueryConfig());
                
                // Set the appropriate property
                switch (property) {
                    case "displayname":
                        config.setDisplayName(value);
                        break;
                    case "query":
                        config.setQuery(value);
                        break;
                    case "unit":
                        config.setUnit(value);
                        break;
                    case "description":
                        config.setDescription(value);
                        break;
                    case "enabled":
                        config.setEnabled(Boolean.parseBoolean(value));
                        break;
                }
            }
        });
        
        // Only add complete configurations (those with at least displayName and query)
        tempQueries.forEach((key, config) -> {
            if (config.getDisplayName() != null && config.getQuery() != null) {
                queries.put(key, config);
            }
        });
    }
    
    // Getters and setters
    public Map<String, MetricQueryConfig> getQueries() {
        return queries;
    }
    
    public void setQueries(Map<String, MetricQueryConfig> queries) {
        this.queries = queries;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getDefaultTimeRangeMinutes() {
        return defaultTimeRangeMinutes;
    }
    
    public void setDefaultTimeRangeMinutes(int defaultTimeRangeMinutes) {
        this.defaultTimeRangeMinutes = defaultTimeRangeMinutes;
    }
    
    public String getDefaultStep() {
        return defaultStep;
    }
    
    public void setDefaultStep(String defaultStep) {
        this.defaultStep = defaultStep;
    }
    
    public static class MetricQueryConfig {
        private String displayName;
        private String query;
        private String unit;
        private String description;
        private boolean enabled = true;
        
        public MetricQueryConfig() {}
        
        public MetricQueryConfig(String displayName, String query, String unit, String description) {
            this.displayName = displayName;
            this.query = query;
            this.unit = unit;
            this.description = description;
        }
        
        // Getters and setters
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}