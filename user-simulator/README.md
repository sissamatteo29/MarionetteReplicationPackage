
# Enhanced User Behavior Simulator for Outfit-App

This simulator provides realistic user behavior simulation for the Outfit-App with support for multiple concurrent users.

## Features

- **Multi-User Simulation**: Support for multiple concurrent fake users running in separate threads
- **Browser-like Behavior**: Downloads HTML pages and all associated images (simulating real browser behavior)
- **Thread-Safe Statistics**: Shared statistics across all user threads
- **Parent Thread Cleanup**: Dedicated parent thread handles cleanup operations while child threads perform user actions
- **Comprehensive Metrics**: Tracks uploads, downloads, timing, and success rates

## Architecture

### Single User Mode
- One thread performs both user actions and cleanup operations
- Traditional behavior for simple testing

### Multi-User Mode
- **Child Threads**: Each fake user runs in its own thread, performing upload and browse actions
- **Parent Thread**: Waits for all child threads to complete each cycle, then performs cleanup operations
- **Synchronization**: Uses threading events to coordinate cycle completion and cleanup timing
- **Shared Statistics**: All threads update shared, thread-safe statistics

## Usage

### Basic Usage
```bash
# Single user (default)
python3 user_simulator.py

# With custom cycle duration
python3 user_simulator.py --cycle-duration 600

# Multiple users (multi-threaded)
python3 user_simulator.py --num-users 5

# Complete example with multiple users
python3 user_simulator.py --num-users 3 --cycle-duration 300 --max-cycles 5 --images-folder ./test-images
```

### Command Line Arguments

- `--images-folder`: Folder containing test images (default: `./test-images`)
- `--cycle-duration`: Duration of each cycle in seconds (default: `10`)
- `--max-cycles`: Maximum number of cycles to run (default: unlimited)
- `--num-users`: Number of fake users to simulate (default: `1`)

### Examples

```bash
# Single user, short cycles for testing
python3 user_simulator.py --cycle-duration 30 --max-cycles 3

# Multiple users with longer cycles
python3 user_simulator.py --num-users 5 --cycle-duration 600

# Heavy load testing
python3 user_simulator.py --num-users 10 --cycle-duration 300 --max-cycles 10
```

## How It Works

### Single User Mode (`--num-users 1`)
1. Starts one simulator instance
2. Runs cycles with user actions (upload/browse)
3. Performs cleanup at the end of each cycle
4. Prints statistics after each cycle

### Multi-User Mode (`--num-users > 1`)
1. **Parent Thread**: Creates and manages child threads, performs cleanup
2. **Child Threads**: Each simulates a fake user performing actions
3. **Cycle Flow**:
   - Parent creates N child threads for N users
   - Each child thread runs for the specified cycle duration
   - Parent waits for all child threads to complete
   - Parent signals cleanup and waits for threads to finish
   - Parent performs cleanup operations (clear repository, update stats)
   - Cycle repeats

### Cleanup Operations
- Clear the image repository (`/admin/clear-repository`)
- Reset cycle-specific statistics
- Print comprehensive statistics
- Only performed by the parent thread to avoid conflicts

## Thread Safety

- **SharedStats Class**: Thread-safe statistics management using locks
- **Individual Sessions**: Each user thread has its own HTTP session
- **Synchronized Cleanup**: Only parent thread performs cleanup operations
- **Event Coordination**: Threading events ensure proper cycle synchronization

## Statistics Tracked

- Total actions performed
- Upload attempts and successes
- Browse requests
- Images downloaded and failures
- Data transfer amounts and timing
- Success rates and averages
- Repository state

The simulator provides comprehensive metrics for performance analysis and load testing.
