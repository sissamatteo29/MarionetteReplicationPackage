# Connection Pooling Optimization for User Simulator

## Overview

The user simulator has been optimized with connection pooling to improve efficiency when running multiple user threads. This optimization reduces resource consumption and improves performance, especially for multi-user simulations.

## What Changed

### Before (Inefficient)
- Each `EnhancedOutfitSimulator` instance created its own `requests.Session()`
- Multiple separate connection pools for each user thread
- Higher memory and socket usage
- No connection reuse between threads

### After (Optimized)
- Single shared `ConnectionPoolManager` singleton with configurable connection pool
- All user threads share the same connection pool
- Efficient connection reuse across threads
- Configurable retry strategies and timeouts
- Proper resource cleanup

## Key Features

### 1. Shared Connection Pool
- **Pool Size**: Configurable number of persistent HTTP connections
- **Auto-sizing**: Defaults to max(10, num_users * 2) for optimal performance
- **Connection Reuse**: TCP connections are reused across requests and threads

### 2. Retry Strategy
- **Configurable Retries**: Set maximum number of retry attempts
- **Backoff Factor**: Exponential backoff between retries
- **Status Code Handling**: Automatic retry on 429, 5xx errors

### 3. Resource Management
- **Proper Cleanup**: Connection pool is closed when simulation ends
- **Thread Safety**: Singleton pattern ensures one pool per application
- **Keep-Alive**: HTTP keep-alive headers for persistent connections

## Usage Examples

### Single User with Custom Pool
```bash
python3 user_simulator.py --pool-size 15 --max-retries 5
```

### Multiple Users with Optimized Pool
```bash
python3 user_simulator.py --num-users 10 --pool-size 25 --backoff-factor 0.5
```

### Default Auto-Sizing
```bash
python3 user_simulator.py --num-users 5
# Pool size automatically set to 10 (max(10, 5*2))
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `--pool-size` | auto | Connection pool size (auto = max(10, users*2)) |
| `--max-retries` | 3 | Maximum HTTP retry attempts |
| `--backoff-factor` | 0.3 | Exponential backoff factor |

## Performance Benefits

### Resource Efficiency
- **Reduced Memory**: Single connection pool vs multiple per-thread pools
- **Lower Socket Usage**: Shared connections reduce system socket consumption
- **Better Scalability**: Handles more concurrent users with same resources

### Network Efficiency
- **Connection Reuse**: TCP connections are reused for multiple requests
- **Keep-Alive**: Persistent HTTP connections reduce handshake overhead
- **Retry Logic**: Intelligent retry handling for temporary failures

### Monitoring
- **Pool Statistics**: Connection pool usage displayed in statistics
- **Request Tracking**: Number of pool requests tracked per cycle
- **Resource Visibility**: Pool size and retry configuration shown

## Technical Implementation

### ConnectionPoolManager Class
- **Singleton Pattern**: Ensures only one pool exists
- **Thread-Safe**: Uses threading locks for safe initialization
- **Configurable**: Supports custom pool sizes and retry strategies

### HTTP Adapter Configuration
```python
adapter = HTTPAdapter(
    pool_connections=pool_size,  # Number of connection pools
    pool_maxsize=pool_size,      # Max connections per pool
    max_retries=retry_strategy,
    pool_block=False             # Non-blocking behavior
)
```

### Retry Strategy
```python
retry_strategy = Retry(
    total=max_retries,
    backoff_factor=backoff_factor,
    status_forcelist=[429, 500, 502, 503, 504],
    allowed_methods=["HEAD", "GET", "PUT", "DELETE", "OPTIONS", "TRACE", "POST"]
)
```

## Backwards Compatibility

The optimization maintains full backwards compatibility:
- All existing command-line options work unchanged
- Single-user mode automatically uses connection pooling
- No changes required to existing scripts or workflows

## Monitoring Output

The statistics now include connection pool information:

```
🔗 CONNECTION POOL STATS
Pool size: 20
Max retries: 3
Pool requests: 45
```

This helps monitor:
- How the pool is configured
- How many requests have used the pool
- Whether pool sizing is appropriate for the workload