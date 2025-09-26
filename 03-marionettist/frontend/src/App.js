import React, { useState, useEffect } from 'react';
import { ChevronRight, ChevronDown, Settings, Activity, Layers, Code, BarChart3, Search, RotateCcw } from 'lucide-react';
import { ServiceMetricsPanel } from './MetricsComponents';
import './App.css';

// API base URL - dynamically detect the current host for Minikube compatibility
const API_BASE_URL = (() => {
  const { protocol, hostname, port } = window.location;

  if (hostname === 'localhost' && port === '3000') {
    return 'http://localhost:8080/api';
  }

  return `${protocol}//${hostname}${port ? ':' + port : ''}/api`;
})();

const ServiceOverviewCard = ({ serviceName, serviceConfig, onServiceClick }) => {
  const classCount = serviceConfig.classConfigs ? serviceConfig.classConfigs.length : 0;
  const methodCount = serviceConfig.classConfigs ?
    serviceConfig.classConfigs.reduce(
      (total, classConfig) => total + (classConfig.methodConfigs ? classConfig.methodConfigs.length : 0), 0
    ) : 0;

  // Count methods by behavior status
  const behaviorStats = serviceConfig.classConfigs ?
    serviceConfig.classConfigs.reduce((stats, classConfig) => {
      if (classConfig.methodConfigs) {
        classConfig.methodConfigs.forEach(method => {
          if (method.currentBehaviourId === method.defaultBehaviourId) {
            stats.normal++;
          } else {
            stats.modified++;
          }
        });
      }
      return stats;
    }, { normal: 0, modified: 0 })
    : { normal: 0, modified: 0 };


  return (
    <div
      className="service-card"
      onClick={() => onServiceClick(serviceName)}
    >
      <div className="service-header">
        <div className="service-info">
          <div className="service-icon">
            <Activity size={24} />
          </div>
          <div>
            <h3>{serviceName}</h3>
            <p>Microservice Configuration</p>
          </div>
        </div>
        <ChevronRight size={20} />
      </div>

      <div className="service-stats">
        <div className="stat">
          <div className="stat-number">{classCount}</div>
          <div className="stat-label">
            <Layers size={12} />
            Classes
          </div>
        </div>
        <div className="stat">
          <div className="stat-number">{methodCount}</div>
          <div className="stat-label">
            <Code size={12} />
            Methods
          </div>
        </div>
        <div className="stat">
          <div className="stat-number modified">{behaviorStats.modified}</div>
          <div className="stat-label">
            <Settings size={12} />
            Modified
          </div>
        </div>
      </div>
    </div>
  );
};

const BehaviorBadge = ({ behavior, isDefault, isCurrent }) => {
  return (
    <span className={`badge ${behavior.toLowerCase()} ${isCurrent ? 'current' : ''}`}>
      {behavior}
      {isDefault && <span className="default-indicator">(default)</span>}
    </span>
  );
};

const MethodConfigRow = ({ method, onBehaviorChange }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="method-config">
      <div
        className="method-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="method-info">
          {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          <Code size={16} />
          <span>{method.methodName}</span>
        </div>
        <BehaviorBadge
          behavior={method.currentBehaviourId}
          isCurrent={true}
        />
      </div>

      {isExpanded && (
        <div className="method-details">
          <div className="behavior-selector">
            <label>Current Behavior</label>
            <select
              value={method.currentBehaviourId}
              onChange={(e) => onBehaviorChange(method.methodName, e.target.value)}
            >
              {method.availableBehaviourIds.map(behaviorId => (
                <option key={behaviorId} value={behaviorId}>
                  {behaviorId} {behaviorId === method.defaultBehaviourId ? '(default)' : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="available-behaviors">
            <span className="label">Available Behaviors:</span>
            <div className="behavior-list">
              {method.availableBehaviourIds.map(behaviorId => (
                <BehaviorBadge
                  key={behaviorId}
                  behavior={behaviorId}
                  isDefault={behaviorId === method.defaultBehaviourId}
                  isCurrent={behaviorId === method.currentBehaviourId}
                />
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const ClassConfigSection = ({ classConfig, onBehaviorChange }) => {
  const [isExpanded, setIsExpanded] = useState(true);
  const methodCount = classConfig.methodConfigs ? classConfig.methodConfigs.length : 0;

  return (
    <div className="class-config">
      <div
        className="class-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="class-info">
          {isExpanded ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
          <Layers size={20} />
          <div>
            <h4>{classConfig.className}</h4>
            <p>{methodCount} method{methodCount !== 1 ? 's' : ''}</p>
          </div>
        </div>
      </div>

      {isExpanded && (
        <div className="class-methods">
          {classConfig.methodConfigs && classConfig.methodConfigs.map(method =>  (
            <MethodConfigRow
              key={method.methodName}
              method={method}
              onBehaviorChange={(methodName, newBehavior) =>
                onBehaviorChange(classConfig.className, methodName, newBehavior)
              }
            />
          ))}
        </div>
      )}
    </div>
  );
};

const ServiceDetailView = ({ serviceName, serviceConfig, onBack, onReset, onBehaviorChange }) => {
  const [showMetrics, setShowMetrics] = useState(false);
  const classCount = serviceConfig.classConfigs ? serviceConfig.classConfigs.length : 0;
  const methodCount = serviceConfig.classConfigs ? serviceConfig.classConfigs.reduce(
    (total, classConfig) => total + (classConfig.methodConfigs ? classConfig.methodConfigs.length : 0), 0
  ) : 0;

  return (
    <div className="service-detail">
      <div className="service-detail-header">
        <div className="header-content">
          <div className="navigation">
            <button onClick={onBack} className="back-button">
              ← Back to Services
            </button>
            <div className="service-title">
              <div className="service-icon">
                <Activity size={24} />
              </div>
              <div>
                <h1>{serviceName}</h1>
                <p>{classCount} classes, {methodCount} methods</p>
              </div>
            </div>
          </div>
          <div className="service-actions">
            <button
              onClick={onReset}
              className="reset-button"
              title="Reset to template configuration"
            >
              <RotateCcw size={20} />
              Reset to Template
            </button>
            <button
              onClick={() => setShowMetrics(!showMetrics)}
              className={`metrics-toggle ${showMetrics ? 'active' : ''}`}
            >
              <BarChart3 size={20} />
              {showMetrics ? 'Hide Metrics' : 'Show Metrics'}
            </button>
          </div>
        </div>
      </div>

      <div className="service-detail-content">
        <div className="service-detail-layout">
          <div className="service-config-section">
            <div className="classes-list">
              {serviceConfig.classConfigs && serviceConfig.classConfigs.map(classConfig => (
                <ClassConfigSection
                  key={classConfig.className}
                  classConfig={classConfig}
                  onBehaviorChange={onBehaviorChange}
                />
              ))}
            </div>
          </div>

          {showMetrics && (
            <div className="service-metrics-section">
              <ServiceMetricsPanel
                serviceName={serviceName}
                onClose={() => setShowMetrics(false)}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// API functions to communicate with backend
const fetchAllServices = async () => {
  const url = `${API_BASE_URL}/services`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch services');
  return response.json();
};

const triggerServiceDiscovery = async (fullRefresh = false) => {
  const response = await fetch(`${API_BASE_URL}/services/discover?fullRefresh=${fullRefresh}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    }
  });
  if (!response.ok) throw new Error('Failed to trigger discovery');
  return response.text(); // Changed from response.json() to response.text()
};

const resetServiceToTemplate = async (serviceName) => {
  const response = await fetch(`${API_BASE_URL}/services/${serviceName}/reset`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    }
  });
  if (!response.ok) throw new Error('Failed to reset service');
  return response.text();
};

const updateMethodBehavior = async (serviceName, className, methodName, newBehavior) => {

  const requestBody = {
    className: className,
    methodName: methodName,
    behaviourId: newBehavior
  }

  const response = await fetch(
    `${API_BASE_URL}/services/${serviceName}/changeBehaviour`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody)
    }
  );

  if (!response.ok) throw new Error('Failed to update behavior');
  return response.text();
};

const MarionetteControlPanel = () => {
  const [servicesData, setServicesData] = useState(null);
  const [selectedService, setSelectedService] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [showTestDialog, setShowTestDialog] = useState(false);
  const [testDuration, setTestDuration] = useState({ hours: 0, minutes: 5, seconds: 0 });
  const [isRunningTest, setIsRunningTest] = useState(false);
  const [hasTestResults, setHasTestResults] = useState(false);

  // Load data from your Java API when component mounts
  useEffect(() => {
    const loadServices = async () => {
      try {
        setLoading(true);
        setError(null); // Clear any previous errors
        const data = await fetchAllServices();

        // Handle case where backend returns empty or null data
        const safeData = data || { serviceConfigs: [] };
        
        const transformedData = {
          serviceConfigs: safeData.serviceConfigs || [],
          totalServices: safeData.serviceConfigs ? safeData.serviceConfigs.length : 0,
          unavailableServices: 0,
          lastDiscovery: new Date().toISOString()
        }
        setServicesData(transformedData);
        
        // Check for test results availability
        checkResultsAvailability();
      } catch (err) {
        setError(err.message);
        console.error('Failed to load services:', err);
        // Set empty data on error so UI doesn't break
        setServicesData({
          serviceConfigs: [],
          totalServices: 0,
          unavailableServices: 0,
          lastDiscovery: new Date().toISOString()
        });
      } finally {
        setLoading(false);
      }
    };

    loadServices();
  }, []);

  const handleRefreshServices = async (fullRefresh = false) => {
    try {
      setIsDiscovering(true);
      setError(null); // Clear any previous errors

      if (fullRefresh) {
        // Trigger full discovery
        await triggerServiceDiscovery(true);
        // Wait a bit for discovery to complete, then refresh
        setTimeout(async () => {
          try {
            const data = await fetchAllServices();
            const safeData = data || { serviceConfigs: [] };
            const transformedData = {
              serviceConfigs: safeData.serviceConfigs || [],
              totalServices: safeData.serviceConfigs ? safeData.serviceConfigs.length : 0,
              unavailableServices: 0,
              lastDiscovery: new Date().toISOString()
            };
            setServicesData(transformedData);
          } catch (refreshErr) {
            setError(`Failed to refresh after discovery: ${refreshErr.message}`);
          } finally {
            setIsDiscovering(false);
          }
        }, 3000);
      } else {
        // Quick refresh
        const data = await fetchAllServices();
        const safeData = data || { serviceConfigs: [] };
        const transformedData = {
          serviceConfigs: safeData.serviceConfigs || [],
          totalServices: safeData.serviceConfigs ? safeData.serviceConfigs.length : 0,
          unavailableServices: 0,
          lastDiscovery: new Date().toISOString()
        };
        setServicesData(transformedData);
        setIsDiscovering(false);
      }
    } catch (err) {
      setError(err.message);
      setIsDiscovering(false);
    }
  };

  const handleResetService = async (serviceName) => {
    try {
      await resetServiceToTemplate(serviceName);
      // Refresh the data to show the reset
      const data = await fetchAllServices(true);
      setServicesData(data);
      alert(`Successfully reset ${serviceName} to template configuration`);
    } catch (err) {
      alert(`Failed to reset service: ${err.message}`);
    }
  };

  const handleServiceClick = (serviceName) => {
    setSelectedService(serviceName);
  };

  const handleBack = () => {
    setSelectedService(null);
  };

const handleBehaviorChange = async (serviceName, className, methodName, newBehavior) => {
  try {
    // Optimistically update the UI with CORRECT structure
    setServicesData(prev => ({
      ...prev,
      serviceConfigs: prev.serviceConfigs.map(service => 
        service.serviceName === serviceName
          ? {
              ...service,
              classConfigs: service.classConfigs.map(classConfig =>
                classConfig.className === className
                  ? {
                      ...classConfig,
                      methodConfigs: classConfig.methodConfigs.map(method =>
                        method.methodName === methodName
                          ? { ...method, currentBehaviourId: newBehavior }
                          : method
                      )
                    }
                  : classConfig
              )
            }
          : service
      )
    }));

    await updateMethodBehavior(serviceName, className, methodName, newBehavior);
    console.log(`Successfully updated ${serviceName}.${className}.${methodName} to ${newBehavior}`);
  } catch (err) {
    console.error('Failed to update behavior:', err);
    alert('Failed to update behavior: ' + err.message);
    // Refresh to get the correct state
    const data = await fetchAllServices();
    setServicesData(data);
  }
};  const handleStartAbTest = async () => {
    const totalSeconds = testDuration.hours * 3600 + testDuration.minutes * 60 + testDuration.seconds;
    
    if (totalSeconds < 30) {
      alert('Test duration must be at least 30 seconds');
      return;
    }

    try {
      setIsRunningTest(true);
      setShowTestDialog(false);
      setError(null);

      const response = await fetch(`${API_BASE_URL}/services/start-ab-test?durationSeconds=${totalSeconds}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }

      const successMessage = await response.text();
      alert(successMessage);

      // Wait for the test duration plus some buffer time before checking for results
      setTimeout(() => {
        checkResultsAvailability();
      }, (totalSeconds + 10) * 1000); // Add 10 seconds buffer

    } catch (err) {
      setError(`Failed to start A/B test: ${err.message}`);
      alert(`Failed to start A/B test: ${err.message}`);
    } finally {
      setIsRunningTest(false);
    }
  };

const formatDurationDisplay = () => {
  const totalSeconds = testDuration.hours * 3600 + testDuration.minutes * 60 + testDuration.seconds;
  if (totalSeconds < 60) {
    return `${totalSeconds} seconds`;
  } else if (totalSeconds < 3600) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return seconds > 0 ? `${minutes} minutes, ${seconds} seconds` : `${minutes} minutes`;
  } else {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    let result = `${hours} hour${hours > 1 ? 's' : ''}`;
    if (minutes > 0) result += `, ${minutes} minute${minutes > 1 ? 's' : ''}`;
    if (seconds > 0) result += `, ${seconds} second${seconds > 1 ? 's' : ''}`;
    return result;
  }
};

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'Never';
    return new Date(timestamp).toLocaleString();
  };

  // Test Duration Dialog Component
  const TestDurationDialog = () => (
    <div className="dialog-overlay" onClick={(e) => e.target.classList.contains('dialog-overlay') && setShowTestDialog(false)}>
      <div className="dialog">
        <div className="dialog-header">
          <h3>Start System Test</h3>
          <button onClick={() => setShowTestDialog(false)} className="close-button">×</button>
        </div>
        <div className="dialog-content">
          <p>Configure the total duration for the A/B testing cycle:</p>
          
          <div className="duration-inputs">
            <div className="duration-input">
              <label>Hours</label>
              <input
                type="number"
                min="0"
                max="23"
                value={testDuration.hours}
                onChange={(e) => setTestDuration(prev => ({ ...prev, hours: Math.max(0, parseInt(e.target.value) || 0) }))}
              />
            </div>
            <div className="duration-input">
              <label>Minutes</label>
              <input
                type="number"
                min="0"
                max="59"
                value={testDuration.minutes}
                onChange={(e) => setTestDuration(prev => ({ ...prev, minutes: Math.max(0, parseInt(e.target.value) || 0) }))}
              />
            </div>
            <div className="duration-input">
              <label>Seconds</label>
              <input
                type="number"
                min="0"
                max="59"
                value={testDuration.seconds}
                onChange={(e) => setTestDuration(prev => ({ ...prev, seconds: Math.max(0, parseInt(e.target.value) || 0) }))}
              />
            </div>
          </div>
          
          <div className="duration-summary">
            <strong>Total Duration: {formatDurationDisplay()}</strong>
          </div>
          
          <div className="dialog-actions">
            <button onClick={() => setShowTestDialog(false)} className="cancel-button">
              Cancel
            </button>
            <button 
              onClick={handleStartAbTest} 
              className="start-test-button"
              disabled={testDuration.hours === 0 && testDuration.minutes === 0 && testDuration.seconds === 0}
            >
              Start Test
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  const checkResultsAvailability = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/downloadresult/available`);
      if (response.ok) {
        const available = await response.json();
        setHasTestResults(available);
      }
    } catch (err) {
      console.log('Failed to check results availability:', err);
      // Don't show error to user, just assume no results
      setHasTestResults(false);
    }
  };

  const downloadTestResults = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/downloadresult`);
      
      if (!response.ok) {
        throw new Error('Failed to download results');
      }

      const resultsData = await response.json();
      
      // Create and download file
      const jsonString = JSON.stringify(resultsData, null, 2);
      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      
      const link = document.createElement('a');
      link.href = url;
      link.download = `marionette-test-results-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      URL.revokeObjectURL(url);
      
    } catch (err) {
      alert(`Failed to download test results: ${err.message}`);
    }
  };

  useEffect(() => {
    // Check for test results availability every 10 seconds
    const interval = setInterval(() => {
      checkResultsAvailability();
    }, 10000);

    // Cleanup interval on unmount
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="loading">
        <div className="loading-content">
          <div className="spinner"></div>
          <p>Loading services...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error">
        <div className="error-content">
          <div className="error-message">Error loading services</div>
          <p>{error}</p>
          <button onClick={() => handleRefreshServices(false)} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (selectedService && servicesData?.serviceConfigs) {

    const selectedServiceConfig = servicesData.serviceConfigs.find(
      service => service.serviceName === selectedService
    );

    if (selectedServiceConfig) {
      return (
        <ServiceDetailView
          serviceName={selectedService}
          serviceConfig={selectedServiceConfig}
          onBack={handleBack}
          onReset={() => handleResetService(selectedService)}
          onBehaviorChange={(className, methodName, newBehavior) =>
            handleBehaviorChange(selectedService, className, methodName, newBehavior)
          }
        />
      );
    }

  }

  return (
    <div className="app">
      <div className="app-header">
        <div className="header-content">
          <div className="app-title">
            <Settings size={32} />
            <div>
              <h1>Marionette Control Panel</h1>
              <p>Manage microservice behavior configurations with real-time metrics</p>
            </div>
          </div>
          <div className="app-actions">
            <button
              onClick={() => setShowTestDialog(true)}
              className="action-button start-test-button"
              disabled={isRunningTest || isDiscovering}
            >
              <Activity size={20} />
              {isRunningTest ? 'Running Test...' : 'Start System Test'}
            </button>
            <button
              onClick={() => handleRefreshServices(true)}
              className="action-button discover-button"
              disabled={isDiscovering || isRunningTest}
            >
              <Search size={20} />
              {isDiscovering ? 'Discovering...' : 'Full Discovery'}
            </button>
            {hasTestResults && (
              <button
                onClick={downloadTestResults}
                className="action-button download-button"
                disabled={isRunningTest || isDiscovering}
              >
                <BarChart3 size={20} />
                Download Last Test Results
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="app-content">
        <div className="services-section">
          <div className="services-header">
            <div>
              <h2>Services Overview</h2>
              <p>Click on a service to configure its behavior settings and view metrics</p>
            </div>
            <div className="discovery-status">
              <div className="status-item">
                <span className="label">Total Services:</span>
                <span className="value">{servicesData?.totalServices || 0}</span>
              </div>
              <div className="status-item">
                <span className="label">Unavailable:</span>
                <span className={`value ${servicesData?.unavailableServices > 0 ? 'warning' : ''}`}>
                  {servicesData?.unavailableServices || 0}
                </span>
              </div>
              <div className="status-item">
                <span className="label">Last Discovery:</span>
                <span className="value">{formatTimestamp(servicesData?.lastDiscovery)}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="services-grid">
          {servicesData?.serviceConfigs && servicesData.serviceConfigs.map((serviceConfig) => (
            <ServiceOverviewCard
              key={serviceConfig.serviceName}
              serviceName={serviceConfig.serviceName}
              serviceConfig={serviceConfig}
              onServiceClick={handleServiceClick}
            />
          ))}
        </div>

        {(!servicesData?.serviceConfigs || servicesData.serviceConfigs.length === 0) && (
          <div className="no-services">
            <Activity size={48} />
            <h3>No Services Found</h3>
            <p>No marionette-enabled services were discovered in your cluster.</p>
            <button onClick={() => handleRefreshServices(true)} className="discover-button">
              <Search size={20} />
              Discover Services
            </button>
          </div>
        )}
      </div>

      {showTestDialog && <TestDurationDialog />}
    </div>
  );
};

export default MarionetteControlPanel;