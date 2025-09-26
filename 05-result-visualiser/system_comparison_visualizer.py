"""
System comparison visualizer module.

This module creates normalized comparison plots for different system configurations,
showing metrics in a 0-1 scale where 1 is always better.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import numpy as np
from typing import Dict, Any, List, Tuple
from pathlib import Path
import seaborn as sns


class SystemComparisonVisualizer:
    """
    Creates visualizations comparing different system configurations.
    
    This class handles normalization of metrics and creation of comparison plots
    where all metrics are scaled to 0-1 range with 1 being the best value.
    """
    
    def __init__(self):
        """Initialize the visualizer with default styling."""
        # Set up matplotlib style
        plt.style.use('default')
        sns.set_palette("husl")
        
    def normalize_metrics(self, data: Dict[str, Any]) -> Tuple[List[str], np.ndarray]:
        """
        Normalize all metrics to 0-1 scale where 1 is always better.
        
        Args:
            data (Dict[str, Any]): Experiment data containing metrics and results
            
        Returns:
            Tuple[List[str], np.ndarray]: Metric labels and normalized values matrix
                - metric_labels: List of metric names with units
                - normalized_matrix: 2D array where rows are configurations and columns are metrics
        """
        metric_configs = data['metricConfigs']
        ranking_data = data['ranking']
        
        # Extract metric information
        metric_names = [config['metricName'] for config in metric_configs]
        metric_units = [config['unit'] for config in metric_configs]
        metric_directions = [config['direction'] for config in metric_configs]
        
        # Create metric labels with units
        metric_labels = [f"{name}\n({unit})" for name, unit in zip(metric_names, metric_units)]
        
        # Extract values for each configuration and metric
        num_configs = len(ranking_data)
        num_metrics = len(metric_configs)
        values_matrix = np.zeros((num_configs, num_metrics))
        
        for config_idx, config in enumerate(ranking_data):
            for metric_idx, metric_name in enumerate(metric_names):
                # Find the metric value in systemResults
                for result in config['systemResults']:
                    if result['metricName'] == metric_name:
                        values_matrix[config_idx, metric_idx] = result['value']
                        break
        
        # Normalize each metric to 0-1 scale
        normalized_matrix = np.zeros_like(values_matrix)
        
        for metric_idx in range(num_metrics):
            metric_values = values_matrix[:, metric_idx]
            min_val = np.min(metric_values)
            max_val = np.max(metric_values)
            
            if max_val == min_val:
                # All values are the same, set to 0.5
                normalized_matrix[:, metric_idx] = 0.5
            else:
                # Normalize to 0-1 range
                normalized_values = (metric_values - min_val) / (max_val - min_val)
                
                # If direction is "lower", invert the values (lower is better -> higher normalized is better)
                if metric_directions[metric_idx] == "lower":
                    normalized_values = 1 - normalized_values
                
                normalized_matrix[:, metric_idx] = normalized_values
        
        return metric_labels, normalized_matrix
    
    def create_normalized_comparison_plot(self, data: Dict[str, Any], save_path: Path = None) -> None:
        """
        Create a line plot comparing system configurations with normalized metrics.
        
        Args:
            data (Dict[str, Any]): Experiment data
            save_path (Path, optional): Path to save the plot
        """
        metric_labels, normalized_matrix = self.normalize_metrics(data)
        num_configs = normalized_matrix.shape[0]
        
        # Create the plot
        plt.figure(figsize=(16, 9))  # slightly larger for larger labels
        
        # Create x-axis positions
        x_positions = np.arange(len(metric_labels))
        
        # Define distinct colors for each configuration (matching radar chart)
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
        
        for config_idx in range(num_configs):
            config_label = f"Config-{config_idx + 1}"
            color = distinct_colors[config_idx % len(distinct_colors)]
            plt.plot(
                x_positions, 
                normalized_matrix[config_idx, :], 
                marker='o', 
                linewidth=2.5,
                markersize=8,
                label=config_label,
                color=color
            )
        
        # Customize the plot
        plt.xlabel('Metrics', fontsize=24, fontweight='bold')
        plt.ylabel('Normalized Score', fontsize=24, fontweight='bold')
        plt.title('System Configuration Comparison - Multi-Line Chart', 
                 fontsize=27, fontweight='bold', pad=25)
        
        # Set x-axis labels
        plt.xticks(x_positions, metric_labels, rotation=45, ha='right', fontsize=18)
        
        # Set y-axis limits
        plt.ylim(-0.05, 1.05)
        
        # Add grid
        plt.grid(True, alpha=0.3, linestyle='--')
        
        # Add legend
        plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left', fontsize=19)
        
        # Add horizontal reference lines
        plt.axhline(y=0, color='red', linestyle='-', alpha=0.3, linewidth=1)
        plt.axhline(y=0.5, color='orange', linestyle='--', alpha=0.5, linewidth=1)
        plt.axhline(y=1, color='green', linestyle='-', alpha=0.3, linewidth=1)
        
        # Adjust layout to prevent label cutoff
        plt.tight_layout()
        
        # Save or show the plot
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"Plot saved to: {save_path}")
        else:
            plt.show()
        
        plt.close()
    
    def create_detailed_comparison_table(self, data: Dict[str, Any]) -> None:
        """
        Create a detailed comparison table showing raw and normalized values.
        
        Args:
            data (Dict[str, Any]): Experiment data
        """
        metric_configs = data['metricConfigs']
        ranking_data = data['ranking']
        metric_labels, normalized_matrix = self.normalize_metrics(data)
        
        print("\n" + "="*80)
        print("DETAILED SYSTEM CONFIGURATION COMPARISON")
        print("="*80)
        
        # Print metric information
        print("\nMETRIC CONFIGURATIONS:")
        for i, config in enumerate(metric_configs):
            direction_desc = "lower is better" if config['direction'] == 'lower' else "higher is better"
            print(f"  {i+1}. {config['metricName']} ({config['unit']}) - {direction_desc}")
        
        print(f"\nNORMALIZED SCORES (0-1 scale, 1 is always better):")
        print("-" * 60)
        
        # Print header
        header = "Config".ljust(8)
        for label in metric_labels:
            clean_label = label.split('\n')[0][:12]  # Take metric name, truncate to 12 chars
            header += clean_label.ljust(15)
        print(header)
        print("-" * len(header))
        
        # Print normalized values for each configuration
        for config_idx in range(len(ranking_data)):
            row = f"Conf-{config_idx + 1}".ljust(8)
            for metric_idx in range(len(metric_configs)):
                score = normalized_matrix[config_idx, metric_idx]
                row += f"{score:.3f}".ljust(15)
            print(row)
        
        print("-" * len(header))
        
        # Calculate and print average scores
        avg_scores = np.mean(normalized_matrix, axis=1)
        print("\nAVERAGE SCORES:")
        for config_idx, avg_score in enumerate(avg_scores):
            print(f"  Config-{config_idx + 1}: {avg_score:.3f}")
        
        # Find best configuration
        best_config_idx = np.argmax(avg_scores)
        print(f"\nBEST OVERALL CONFIGURATION: Config-{best_config_idx + 1} (avg score: {avg_scores[best_config_idx]:.3f})")
        print("="*80)
