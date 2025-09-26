"""
Individual metric visualizer module.

This module creates individual line graphs for each metric, showing how each configuration
performs for that specific metric. Each graph shows configurations on the x-axis 
(ordered by ranking) and actual metric values on the y-axis.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import numpy as np
from typing import Dict, List, Tuple, Any
from pathlib import Path
import math


class IndividualMetricVisualizer:
    """Creates individual line graphs for each metric across all configurations."""
    
    def __init__(self):
        """Initialize the individual metric visualizer."""
        plt.style.use('default')
    
    def extract_metric_data(self, data: Dict[str, Any]) -> Tuple[List[Dict], List[str]]:
        """
        Extract metric data from experiment results.
        
        Args:
            data: Parsed JSON data
            
        Returns:
            Tuple containing metric data and configuration names
        """
        metric_configs = data['metricConfigs']
        ranking_data = data['ranking']
        
        # Extract metric information
        metrics_info = []
        for metric_config in metric_configs:
            metric_info = {
                'name': metric_config['metricName'],
                'unit': metric_config['unit'],
                'direction': metric_config['direction'],
                'order': metric_config['order'],
                'values': [],
                'config_names': []
            }
            metrics_info.append(metric_info)
        
        # Extract values for each configuration (ordered by ranking position)
        sorted_configs = sorted(ranking_data, key=lambda x: x['position'])
        config_names = [f"Config-{config['position']}" for config in sorted_configs]
        
        for config in sorted_configs:
            for i, metric_config in enumerate(metric_configs):
                metric_name = metric_config['metricName']
                
                # Find the metric value in systemResults
                for result in config['systemResults']:
                    if result['metricName'] == metric_name:
                        metrics_info[i]['values'].append(result['value'])
                        if not metrics_info[i]['config_names']:
                            metrics_info[i]['config_names'] = config_names
                        break
        
        return metrics_info, config_names
    
    def create_individual_metric_graphs(self, data: Dict[str, Any], output_path: str = "output/individual_metrics.png") -> str:
        """
        Create a combined image with individual line graphs for each metric.
        
        Args:
            data: Parsed JSON data
            output_path: Path to save the combined graph
            
        Returns:
            Path to the saved graph
        """
        metrics_info, config_names = self.extract_metric_data(data)
        num_metrics = len(metrics_info)
        
        # Calculate subplot layout (prefer rectangular arrangement)
        rows, cols = 1, 3
        
        # Create the figure with subplots
        fig, axes = plt.subplots(rows, cols, figsize=(5*cols, 4*rows))
        
        # Handle case where we have only one subplot
        if num_metrics == 1:
            axes = [axes]
        elif rows == 1 or cols == 1:
            axes = axes.flatten() if hasattr(axes, 'flatten') else [axes]
        else:
            axes = axes.flatten()
        
        # Colors for the line (using a nice gradient from best to worst)
        line_color = '#2E86AB'  # Nice blue color
        # Configuration marker colors (matching radar chart)
        marker_colors = [
            '#FF0000',  # Bright Red
            '#0066CC',  # Deep Blue  
            '#00AA00',  # Green
            '#FF6600',  # Orange
            '#9900CC',  # Purple
            '#00CCCC',  # Cyan
            '#FFCC00',  # Yellow
            '#CC0066'   # Magenta
        ]
        
        for i, metric_info in enumerate(metrics_info):
            ax = axes[i]
            
            values = metric_info['values']
            x_positions = list(range(1, len(values) + 1))
            
            # Create the line plot
            ax.plot(x_positions, values, color=line_color, linewidth=3, alpha=0.7)
            
            # Add markers for each configuration with different colors
            for j, (x, y) in enumerate(zip(x_positions, values)):
                marker_color = marker_colors[j % len(marker_colors)]
                ax.scatter(x, y, color=marker_color, s=100, zorder=5, 
                          edgecolors='white', linewidth=2)
                
                # Add configuration label near the point
                ax.annotate(config_names[j], (x, y), xytext=(5, 5), 
                           textcoords='offset points', fontsize=13, 
                           fontweight='bold', alpha=0.8)
            
            # Customize the subplot
            ax.set_title(f"{metric_info['name']}\n({metric_info['unit']})", 
                        fontsize=16, fontweight='bold', pad=15)
            
            ax.set_xlabel('Configuration (Ranked Order)', fontsize=15, fontweight='bold')
            ax.set_ylabel(f"Value ({metric_info['unit']})", fontsize=15, fontweight='bold')
            
            # Set x-axis ticks and labels
            ax.set_xticks(x_positions)
            ax.set_xticklabels([f"#{pos}" for pos in x_positions])
            
            # Add grid for better readability
            ax.grid(True, alpha=0.3, linestyle='--')
            
            # Highlight best and worst performers
            best_idx = 0 if metric_info['direction'] == 'higher' else len(values) - 1
            worst_idx = len(values) - 1 if metric_info['direction'] == 'higher' else 0
            
            if metric_info['direction'] == 'lower':
                best_idx = values.index(min(values))
                worst_idx = values.index(max(values))
            else:
                best_idx = values.index(max(values))
                worst_idx = values.index(min(values))
            
            # Add best/worst annotations
            ax.scatter(best_idx + 1, values[best_idx], color='green', s=150, 
                      marker='*', zorder=10, label='Best')
            ax.scatter(worst_idx + 1, values[worst_idx], color='red', s=150, 
                      marker='X', zorder=10, label='Worst')
        
        # Hide unused subplots
        for i in range(num_metrics, len(axes)):
            axes[i].set_visible(False)
        
        # Adjust layout to prevent overlap (no main title)
        plt.tight_layout()
        
        # Save the figure
        plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor='white')
        plt.close()
        
        print(f"Individual metric graphs saved to: {output_path}")
        return output_path
    
    def create_metric_summary_table(self, data: Dict[str, Any]) -> None:
        """
        Create a summary table showing metric values for all configurations.
        
        Args:
            data: Parsed JSON data
        """
        metrics_info, config_names = self.extract_metric_data(data)
        
        print("\n" + "="*80)
        print("INDIVIDUAL METRIC ANALYSIS")
        print("="*80)
        
        for metric_info in metrics_info:
            print(f"\n{metric_info['name']} ({metric_info['unit']}):")
            print("-" * 50)
            
            direction_desc = "lower is better" if metric_info['direction'] == 'lower' else "higher is better"
            print(f"Direction: {direction_desc}")
            
            values = metric_info['values']
            
            # Find best and worst
            if metric_info['direction'] == 'lower':
                best_idx = values.index(min(values))
                worst_idx = values.index(max(values))
                best_value, worst_value = min(values), max(values)
            else:
                best_idx = values.index(max(values))
                worst_idx = values.index(min(values))
                best_value, worst_value = max(values), min(values)
            
            print(f"Best:  {config_names[best_idx]} = {best_value:.4f}")
            print(f"Worst: {config_names[worst_idx]} = {worst_value:.4f}")
            
            # Show all values
            print("All values:")
            for config_name, value in zip(config_names, values):
                status = ""
                if config_name == config_names[best_idx]:
                    status = " ★ (Best)"
                elif config_name == config_names[worst_idx]:
                    status = " ✗ (Worst)"
                    
                print(f"  {config_name}: {value:.4f}{status}")
        
        print("="*80)
