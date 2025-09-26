"""
Radar chart visualizer module.

This module creates radar chart visualizations for comparing system configurations.
Each configuration is shown as a polygon with metrics on the vertices using offset lines
to handle overlapping configurations.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import numpy as np
from typing import Dict, List, Tuple, Any


class RadarChartVisualizer:
    """Creates radar charts for system configuration comparison with offset handling."""
    
    def __init__(self):
        """Initialize the radar chart visualizer."""
        plt.style.use('default')
    
    def normalize_metrics(self, data: Dict[str, Any]) -> Tuple[List[List[float]], List[str], List[str]]:
        """
        Normalize metrics for radar chart visualization.
        
        Args:
            data: Parsed JSON data
            
        Returns:
            Tuple containing normalized values, metric labels, and config names
        """
        metric_configs = data['metricConfigs']
        ranking_data = data['ranking']
        
        # Extract metric information
        metric_names = [config['metricName'] for config in metric_configs]
        metric_units = [config['unit'] for config in metric_configs]
        metric_directions = [config['direction'] for config in metric_configs]
        
        # Create metric labels
        metric_labels = [f"{name}\n({unit})" for name, unit in zip(metric_names, metric_units)]
        
        # Extract raw values
        raw_values = []
        config_names = []
        
        for config in ranking_data:
            config_values = []
            config_names.append(f"Config-{config['position']}")
            
            for metric_name in metric_names:
                for result in config['systemResults']:
                    if result['metricName'] == metric_name:
                        config_values.append(result['value'])
                        break
            
            raw_values.append(config_values)
        
        # Convert to numpy array for easier manipulation
        raw_values = np.array(raw_values)
        
        # Normalize each metric to 0-1 scale
        normalized_values = []
        
        for config_idx in range(len(config_names)):
            normalized_config = []
            
            for i in range(len(metric_names)):
                metric_values = raw_values[:, i]
                min_val = np.min(metric_values)
                max_val = np.max(metric_values)
                
                if max_val == min_val:
                    normalized = 0.5
                else:
                    normalized = (raw_values[config_idx, i] - min_val) / (max_val - min_val)
                
                # Invert if direction is "lower" (lower is better)
                if metric_directions[i] == "lower":
                    normalized = 1 - normalized
                
                normalized_config.append(normalized)
            
            normalized_values.append(normalized_config)
        
        return normalized_values, metric_labels, config_names
    
    def create_radar_chart(self, data: Dict[str, Any], output_path: str = "output/radar_chart.png") -> str:
        """
        Create a radar chart visualization with offset lines to handle overlapping.
        
        Args:
            data: Parsed JSON data
            output_path: Path to save the chart
            
        Returns:
            Path to the saved chart
        """
        # Normalize data
        normalized_values, metric_labels, config_names = self.normalize_metrics(data)
        
        # Number of metrics
        num_metrics = len(metric_labels)
        num_configs = len(config_names)
        
        # Compute angles for each metric
        angles = np.linspace(0, 2 * np.pi, num_metrics, endpoint=False).tolist()
        
        # Create figure and polar subplot
        fig, ax = plt.subplots(figsize=(14, 12), subplot_kw=dict(projection='polar'))
        
        # Define distinct, high-contrast colors
        distinct_colors = [
            '#FF0000',  # Bright Red
            '#0066CC',  # Deep Blue  
            '#00AA00',  # Green
            '#FF6600',  # Orange
            '#9900CC',  # Purple
            '#00CCCC',  # Cyan
            '#FFCC00',  # Yellow
            '#CC0066'   # Magenta
        ]
        
        # Define different line styles for additional distinction
        line_styles = ['-', '--', '-.', ':', '-', '--', '-.', ':']
        
        # Define different markers for additional distinction
        markers = ['o', 's', '^', 'D', 'v', 'p', '*', 'h']
        
        # Calculate offset values to separate overlapping lines
        offset_amount = 0.02  # Small offset to separate lines
        
        # Plot each configuration with offset
        for i, (values, config_name) in enumerate(zip(normalized_values, config_names)):
            # Apply small offset to separate overlapping values
            offset_values = []
            for j, value in enumerate(values):
                # Create a small offset based on configuration index
                offset = (i - num_configs/2) * offset_amount * (j % 2 * 2 - 1)
                offset_values.append(max(0, min(1, value + offset)))
            
            # Close the polygon by repeating the first value
            offset_values_closed = offset_values + [offset_values[0]]
            angles_closed = angles + [angles[0]]
            
            # Get style elements
            color = distinct_colors[i % len(distinct_colors)]
            line_style = line_styles[i % len(line_styles)]
            marker = markers[i % len(markers)]
            
            # Plot the polygon with enhanced visibility
            ax.plot(angles_closed, offset_values_closed, 
                   linestyle=line_style, marker=marker, linewidth=3, 
                   label=config_name, color=color, markersize=8, 
                   markerfacecolor=color, markeredgecolor='white', 
                   markeredgewidth=1.5, alpha=0.8)
            
            # Fill the polygon with transparency
            ax.fill(angles_closed, offset_values_closed, alpha=0.15, color=color)
        
        # Customize the chart
        ax.set_xticks(angles)
        ax.set_xticklabels(metric_labels, fontsize=23, fontweight='bold')
        ax.set_ylim(0, 1.1)  # Slightly extended to accommodate offsets
        ax.set_yticks([0, 0.2, 0.4, 0.6, 0.8, 1.0])
        ax.set_yticklabels(['0', '0.2', '0.4', '0.6', '0.8', '1.0'], fontsize=15)
        ax.grid(True, alpha=0.3)
        
        # Add radial grid lines for better readability
        ax.set_rgrids([0.2, 0.4, 0.6, 0.8, 1.0], alpha=0.5)
        
        # Add title
        plt.title('System Configuration Comparison - Radar Chart', 
                 fontsize=30, fontweight='bold', pad=30)
        
        # Position legend outside the plot
        plt.legend(loc='upper right', bbox_to_anchor=(1.3, 1.0), fontsize=27)
        
        # Adjust layout to prevent legend cutoff
        plt.tight_layout()
        
        # Save the chart
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        plt.close()
        
        print(f"Radar chart saved to: {output_path}")
        return output_path
