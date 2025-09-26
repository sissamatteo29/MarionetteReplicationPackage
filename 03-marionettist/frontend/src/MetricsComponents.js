import React, { useState, useEffect, useRef } from 'react';
import { BarChart3, Zap, Activity, AlertTriangle, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area } from 'recharts';

// API base URL - dynamically detect the current host for Minikube compatibility
const API_BASE_URL = (() => {
  const { protocol, hostname, port } = window.location;
  
  if (hostname === 'localhost' && port === '3000') {
    return 'http://localhost:8080/api';
  }
  
  return `${protocol}//${hostname}${port ? ':' + port : ''}/api`;
})();

// Safe number formatting utility
const safeFormat = (value, decimals = 2, fallback = 'N/A') => {
  if (value === null || value === undefined || isNaN(value)) {
    return fallback;
  }
  return Number(value).toFixed(decimals);
};

// Safe percentage formatting
const safeFormatPercentage = (value, fallback = 'N/A') => {
  if (value === null || value === undefined || isNaN(value)) {
    return fallback;
  }
  return `${(Number(value) * 100).toFixed(1)}%`;
};

// Format bytes to human readable format
const formatBytes = (bytes) => {
  if (bytes === null || bytes === undefined || isNaN(bytes)) return 'N/A';
  
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) return '0 B';
  
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${sizes[i]}`;
};

// Format milliseconds
const formatMilliseconds = (ms) => {
  if (ms === null || ms === undefined || isNaN(ms)) return 'N/A';
  
  if (ms < 1) return `${(ms * 1000).toFixed(0)}μs`;
  if (ms < 1000) return `${ms.toFixed(1)}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
};

export const MetricsCard = ({ title, value, unit = '', change, icon: Icon, color = "blue", formatType = "number" }) => {
  const formatValue = () => {
    switch (formatType) {
      case 'percentage':
        return safeFormatPercentage(value);
      case 'bytes':
        return formatBytes(value);
      case 'milliseconds':
        return formatMilliseconds(value);
      case 'time':
        return value !== null && value !== undefined ? formatMilliseconds(value * 1000) : 'N/A';
      default:
        return value !== null && value !== undefined ? `${safeFormat(value)}${unit}` : 'N/A';
    }
  };

  const getTrendIcon = () => {
    if (change === null || change === undefined || isNaN(change)) return <Minus size={12} />;
    if (change > 0) return <TrendingUp size={12} />;
    if (change < 0) return <TrendingDown size={12} />;
    return <Minus size={12} />;
  };

  return (
    <div className={`metrics-card ${color}`}>
      <div className="metrics-card-header">
        <div className="metrics-icon">
          <Icon size={20} />
        </div>
        <div className="metrics-title">{title}</div>
      </div>
      <div className="metrics-value">
        {formatValue()}
      </div>
      {change !== null && change !== undefined && !isNaN(change) && (
        <div className={`metrics-change ${change >= 0 ? 'positive' : 'negative'}`}>
          {getTrendIcon()}
          {change >= 0 ? '+' : ''}{safeFormat(change, 1)}%
        </div>
      )}
    </div>
  );
};

export const MetricsChart = ({ data, title, yAxisLabel, color = "#3b82f6", formatType = "number" }) => {
  if (!data || data.length === 0) {
    return (
      <div className="metrics-chart">
        <h4>{title}</h4>
        <div className="no-data">
          <p>No data available</p>
        </div>
      </div>
    );
  }

  const formatTooltipValue = (value, name) => {
    if (value === null || value === undefined || isNaN(value)) return 'N/A';
    
    switch (formatType) {
      case 'time':
        return formatMilliseconds(value * 1000);
      case 'rate':
        return `${safeFormat(value)} req/s`;
      case 'percentage':
        return safeFormatPercentage(value);
      case 'bytes':
        return formatBytes(value);
      default:
        return safeFormat(value);
    }
  };

  const formatXAxisTick = (tickItem) => {
    return new Date(tickItem).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatYAxisTick = (value) => {
    switch (formatType) {
      case 'bytes':
        return formatBytes(value);
      case 'percentage':
        return `${(value * 100).toFixed(0)}%`;
      default:
        return safeFormat(value, 1);
    }
  };

  return (
    <div className="metrics-chart">
      <h4>{title}</h4>
      <div style={{ width: '100%', height: 200 }}>
        <ResponsiveContainer>
          <AreaChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
            <XAxis 
              dataKey="timestamp" 
              tickFormatter={formatXAxisTick}
              stroke="#9CA3AF"
              fontSize={12}
            />
            <YAxis 
              stroke="#9CA3AF"
              tickFormatter={formatYAxisTick}
              fontSize={12}
              width={60}
            />
            <Tooltip 
              formatter={formatTooltipValue}
              labelFormatter={(label) => new Date(label).toLocaleString()}
              contentStyle={{ 
                backgroundColor: '#1F2937', 
                border: '1px solid #374151',
                borderRadius: '6px',
                fontSize: '12px'
              }}
            />
            <Area 
              type="monotone" 
              dataKey="value" 
              stroke={color} 
              fill={color}
              fillOpacity={0.2}
              strokeWidth={2}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export const ServiceMetricsPanel = ({ serviceName, onClose }) => {
  const [metricsData, setMetricsData] = useState(null);
  const [liveMetricsData, setLiveMetricsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRange, setTimeRange] = useState(15); // minutes
  const intervalRef = useRef(null);

  const fetchMetrics = async () => {
    try {
      console.log(`Fetching metrics for ${serviceName} with ${timeRange} minutes range`);
      const response = await fetch(`${API_BASE_URL}/metrics/${serviceName}?minutes=${timeRange}`);
      if (!response.ok) throw new Error(`Failed to fetch metrics: ${response.status}`);
      const rawData = await response.json();
      console.log('Raw metrics response:', rawData);
      
      setMetricsData(rawData);
      setError(null);
    } catch (err) {
      setError(err.message);
      console.error('Failed to load metrics:', err);
    }
  };

  const fetchLiveMetrics = async () => {
    try {
      console.log(`Fetching live metrics for ${serviceName}`);
      const response = await fetch(`${API_BASE_URL}/metrics/${serviceName}/live`);
      if (!response.ok) throw new Error(`Failed to fetch live metrics: ${response.status}`);
      const data = await response.json();
      console.log('Live metrics response:', data);
      
      setLiveMetricsData(data);
    } catch (err) {
      console.error('Failed to load live metrics:', err);
    }
  };

  useEffect(() => {
    const loadInitialData = async () => {
      setLoading(true);
      await Promise.all([fetchMetrics(), fetchLiveMetrics()]);
      setLoading(false);
    };

    loadInitialData();

    // Set up periodic updates for live metrics
    intervalRef.current = setInterval(fetchLiveMetrics, 30000); // Update every 30 seconds

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [serviceName, timeRange]);

  // Helper function to determine metric display properties based on key and configuration
  const getMetricDisplayProps = (metricKey, config, value) => {
    const key = metricKey.toLowerCase();
    const displayName = config?.displayName || metricKey.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
    const unit = config?.unit || '';
    
    // Determine icon, color, and format type based on metric key and configuration
    let icon = Activity;
    let color = "blue";
    let formatType = "number";
    
    if (key.includes('memory') || key.includes('jvm_memory')) {
      icon = Activity;
      color = "purple";
      formatType = "bytes";
    } else if (key.includes('request') || key.includes('http')) {
      icon = Activity;
      color = "green";
      formatType = unit.includes('req/s') ? "number" : "number";
    } else if (key.includes('response') || key.includes('duration') || key.includes('time')) {
      icon = Zap;
      color = "blue";
      formatType = unit.includes('s') || unit.includes('seconds') ? "time" : "milliseconds";
    } else if (key.includes('error')) {
      icon = AlertTriangle;
      color = value > 0.01 ? "red" : "green";
      formatType = unit.includes('%') ? "percentage" : "number";
    } else if (key.includes('cpu') || key.includes('system_cpu')) {
      icon = TrendingUp;
      color = "yellow";
      // Check if values are in 0-1 range (need percentage conversion) or already 0-100 range
      if (unit.includes('%')) {
        formatType = value !== null && value < 1 ? "percentage" : "number";
      } else {
        formatType = "percentage"; // Default to percentage for CPU metrics
      }
    } else if (key.includes('rate')) {
      icon = BarChart3;
      color = "green";
      formatType = "number";
    } else {
      // Default based on unit or value
      if (unit.includes('%')) {
        formatType = "percentage";
        color = "yellow";
      } else if (unit.includes('bytes')) {
        formatType = "bytes";
        color = "purple";
      } else if (unit.includes('s')) {
        formatType = "time";
        color = "blue";
        icon = Zap;
      }
    }
    
    return { displayName, icon, color, formatType, unit };
  };

  // Transform time series data from backend format - improved to handle multiple series
  const transformMetricsData = (timeSeriesData) => {
    if (!timeSeriesData || timeSeriesData.length === 0) return [];
    
    console.log('Transforming data:', timeSeriesData);
    
    const seriesArray = Array.isArray(timeSeriesData) ? timeSeriesData : [timeSeriesData];
    
    // For multiple series, we'll aggregate them by timestamp (sum, average, or pick first non-zero)
    const timestampMap = new Map();
    
    seriesArray.forEach(series => {
      if (series && series.dataPoints && Array.isArray(series.dataPoints)) {
        series.dataPoints.forEach(point => {
          if (point && point.timestamp && point.value !== null && point.value !== undefined) {
            const timestamp = point.timestamp;
            const value = Number(point.value);
            
            if (!timestampMap.has(timestamp)) {
              timestampMap.set(timestamp, []);
            }
            timestampMap.get(timestamp).push(value);
          }
        });
      }
    });
    
    // Convert map to array and aggregate values per timestamp
    const allDataPoints = Array.from(timestampMap.entries()).map(([timestamp, values]) => {
      // For most metrics, we want to sum the values (like different memory areas)
      // But for rates and percentages, we might want average
      let aggregatedValue;
      
      if (values.length === 1) {
        aggregatedValue = values[0];
      } else {
        // Sum for memory-like metrics, average for rates/percentages
        // You could make this more sophisticated based on metric type
        aggregatedValue = values.reduce((sum, val) => sum + val, 0);
        
        // If all values are very small (< 1), it might be a percentage/rate, so average instead
        if (values.every(val => val < 1) && values.some(val => val > 0)) {
          aggregatedValue = values.reduce((sum, val) => sum + val, 0) / values.length;
        }
      }
      
      return {
        timestamp: Number(timestamp),
        value: aggregatedValue
      };
    });
    
    return allDataPoints.sort((a, b) => a.timestamp - b.timestamp);
  };

  // Get current value for a metric from live data
  const getCurrentValue = (metricKey) => {
    if (!liveMetricsData?.metrics) return null;
    
    const value = liveMetricsData.metrics[metricKey];
    if (typeof value === 'number') return value;
    if (value && typeof value === 'object' && value.value !== undefined) return value.value;
    return null;
  };

  if (loading) {
    return (
      <div className="metrics-panel">
        <div className="metrics-header">
          <h3>
            <BarChart3 size={20} />
            Metrics for {serviceName}
          </h3>
          <button onClick={onClose} className="close-button">×</button>
        </div>
        <div className="loading-metrics">
          <div className="spinner"></div>
          <p>Loading metrics...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="metrics-panel">
        <div className="metrics-header">
          <h3>
            <BarChart3 size={20} />
            Metrics for {serviceName}
          </h3>
          <button onClick={onClose} className="close-button">×</button>
        </div>
        <div className="error-metrics">
          <AlertTriangle size={24} />
          <p>Failed to load metrics: {error}</p>
          <button onClick={fetchMetrics} className="retry-button">Retry</button>
        </div>
      </div>
    );
  }

  // Extract available metrics from both historical and live data
  const availableMetrics = new Set();
  
  // Add metrics from historical data
  if (metricsData?.metrics) {
    Object.keys(metricsData.metrics).forEach(key => availableMetrics.add(key));
  }
  
  // Add metrics from live data
  if (liveMetricsData?.metrics) {
    Object.keys(liveMetricsData.metrics).forEach(key => availableMetrics.add(key));
  }
  
  const metricsArray = Array.from(availableMetrics);
  console.log('Available metrics:', metricsArray);

  return (
    <div className="metrics-panel">
      <div className="metrics-header">
        <h3>
          <BarChart3 size={20} />
          Metrics for {serviceName}
        </h3>
        <div className="metrics-controls">
          <select 
            value={timeRange} 
            onChange={(e) => setTimeRange(parseInt(e.target.value))}
            className="time-range-select"
          >
            <option value={5}>Last 5 minutes</option>
            <option value={15}>Last 15 minutes</option>
            <option value={30}>Last 30 minutes</option>
            <option value={60}>Last 1 hour</option>
          </select>
          <button onClick={onClose} className="close-button">×</button>
        </div>
      </div>

      {metricsArray.length > 0 ? (
        <>
          {/* Dynamic Live Metrics Cards */}
          <div className="live-metrics">
            {metricsArray.map(metricKey => {
              const config = metricsData?.configurations?.[metricKey] || liveMetricsData?.configurations?.[metricKey];
              const currentValue = getCurrentValue(metricKey);
              const displayProps = getMetricDisplayProps(metricKey, config, currentValue);
              
              return (
                <MetricsCard
                  key={metricKey}
                  title={displayProps.displayName}
                  value={currentValue}
                  unit={displayProps.unit}
                  icon={displayProps.icon}
                  color={displayProps.color}
                  formatType={displayProps.formatType}
                />
              );
            })}
          </div>

          {/* Dynamic Historical Charts */}
          <div className="metrics-charts">
            {metricsArray.map(metricKey => {
              const config = metricsData?.configurations?.[metricKey];
              const historicalData = metricsData?.metrics?.[metricKey];
              
              if (!historicalData || historicalData.length === 0) {
                return null; // Skip metrics without historical data
              }
              
              const chartData = transformMetricsData(historicalData);
              if (chartData.length === 0) {
                return null;
              }
              
              const displayProps = getMetricDisplayProps(metricKey, config, null);
              
              return (
                <MetricsChart
                  key={`chart-${metricKey}`}
                  data={chartData}
                  title={displayProps.displayName}
                  yAxisLabel={displayProps.displayName}
                  color={displayProps.color === "blue" ? "#3b82f6" : 
                         displayProps.color === "green" ? "#10b981" :
                         displayProps.color === "red" ? "#ef4444" :
                         displayProps.color === "yellow" ? "#f59e0b" :
                         displayProps.color === "purple" ? "#8b5cf6" : "#6366f1"}
                  formatType={displayProps.formatType}
                />
              );
            })}
          </div>
        </>
      ) : (
        <div className="no-metrics">
          <AlertTriangle size={48} />
          <h3>No Metrics Available</h3>
          <p>No metrics data found for this service.</p>
          {metricsData && (
            <details style={{marginTop: '1rem', fontSize: '0.875rem', color: '#6b7280'}}>
              <summary>Debug Information</summary>
              <pre style={{marginTop: '0.5rem', fontSize: '0.75rem', overflow: 'auto'}}>
                {JSON.stringify({
                  metricsData: metricsData,
                  liveMetricsData: liveMetricsData
                }, null, 2)}
              </pre>
            </details>
          )}
        </div>
      )}
    </div>
  );
};