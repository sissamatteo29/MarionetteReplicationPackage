"""
Simple radar chart test for debugging matplotlib issues.
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
import json
import sys
sys.path.append('.')


def create_simple_radar_test():
    """Create a very simple radar chart to test matplotlib."""
    
    print("Creating simple radar chart...")
    
    # Load data
    with open('Complex-experiment-2hrs-16conf.json', 'r') as f:
        data = json.load(f)
    
    # Extract just the first two configurations for testing
    configs = data['ranking'][:2]
    metrics = data['metricConfigs']
    
    # Simple normalization
    all_values = []
    for config in configs:
        config_values = []
        for metric in metrics:
            for result in config['systemResults']:
                if result['metricName'] == metric['metricName']:
                    val = result['value']
                    # Simple 0-1 normalization (just for testing)
                    if metric['metricName'] == 'P95 Response Time':
                        val = 1 - min(1, val)  # Invert and cap at 1
                    elif 'Memory' in metric['metricName']:
                        val = 1 - min(1, val / 500000000)  # Rough normalization
                    else:  # CPU
                        val = 1 - min(1, val)  # Invert
                    config_values.append(max(0, val))
                    break
        all_values.append(config_values)
    
    print(f"Normalized values: {all_values}")
    
    # Create angles
    num_metrics = len(metrics)
    angles = np.linspace(0, 2 * np.pi, num_metrics, endpoint=False).tolist()
    angles += angles[:1]
    
    print("Creating matplotlib figure...")
    
    # Try regular plot first instead of polar
    fig, ax = plt.subplots(figsize=(8, 8))
    
    # Convert polar to cartesian for regular plot
    for i, values in enumerate(all_values):
        values += values[:1]  # Close the polygon
        
        x = [r * np.cos(a) for r, a in zip(values, angles)]
        y = [r * np.sin(a) for r, a in zip(values, angles)]
        
        ax.plot(x, y, 'o-', linewidth=2, label=f'Config-{i+1}')
        ax.fill(x, y, alpha=0.25)
    
    ax.set_xlim(-1.1, 1.1)
    ax.set_ylim(-1.1, 1.1)
    ax.set_aspect('equal')
    ax.grid(True)
    ax.legend()
    ax.set_title('System Configuration Comparison - Radar Chart')
    
    print("Saving plot...")
    plt.savefig('output/simple_radar_cartesian.png', dpi=300, bbox_inches='tight')
    plt.close()
    
    print("Simple radar chart created successfully!")
    return 'output/simple_radar_cartesian.png'


if __name__ == "__main__":
    create_simple_radar_test()
