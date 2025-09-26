"""
Aggregate metric visualizer module.

This module creates visualizations showing the actual aggregate metric values
for each system configuration, with one plot per metric displaying 
configurations on the x-axis and their actual aggregate values on the y-axis.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import numpy as np
from typing import Dict, List, Tuple, Any
from pathlib import Path
import math


class AggregateMetricVisualizer:
    """Creates aggregate metric value plots for system configurations."""
    
    def __init__(self):
        """Initialize the aggregate metric visualizer."""
        plt.style.use('default')
    
    def extract_aggregate_data(self, data: Dict[str, Any]) -> Tuple[List[Dict], List[str]]:
        """
        Extract aggregate metric data from experiment results.
        
        Args:
            data: Parsed JSON data
            
        Returns:
            Tuple containing metrics data and configuration names
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
                'values': [],  # Will store values for each configuration
                'config_names': []  # Will store configuration names
            }
            metrics_info.append(metric_info)
        
        # Extract configuration names and aggregate values
        config_names = []
        for config in ranking_data:
            config_name = f"Config-{config['position']}"
            config_names.append(config_name)
            
            # Extract system results for this configuration
            system_results = config.get('systemResults', [])
            
            # Map results to metrics
            for metric_info in metrics_info:
                value_found = False
                for result in system_results:
                    if result['metricName'] == metric_info['name']:
                        metric_info['values'].append(result['value'])
                        metric_info['config_names'].append(config_name)
                        value_found = True
                        break
                
                # If no value found for this metric in this config, add None
                if not value_found:
                    metric_info['values'].append(None)
                    metric_info['config_names'].append(config_name)
        
        return metrics_info, config_names
    
    def create_aggregate_metric_comparison(self, data: Dict[str, Any], output_path: str) -> str:
        """
        Create aggregate metric comparison visualization.
        
        Args:
            data: Parsed JSON experiment data
            output_path: Path to save the output image
            
        Returns:
            str: Path to the saved plot
        """
        print("Creating aggregate metric comparison visualization...")
        
        # Extract data
        metrics_info, config_names = self.extract_aggregate_data(data)
        
        # Filter out metrics with no valid data
        valid_metrics = [m for m in metrics_info if any(v is not None for v in m['values'])]
        
        if not valid_metrics:
            raise ValueError("No valid metric data found for aggregate comparison")
        
        # Calculate subplot layout
        num_metrics = len(valid_metrics)
        cols = min(3, num_metrics)  # Max 3 columns
        rows = math.ceil(num_metrics / cols)
        
        # Create figure with subplots
        fig, axes = plt.subplots(rows, cols, figsize=(6*cols, 5.5*rows))
        
        # Handle different subplot configurations
        if num_metrics == 1:
            axes = [axes]
        elif rows == 1 or cols == 1:
            axes = axes.flatten() if hasattr(axes, 'flatten') else [axes]
        else:
            axes = axes.flatten()
        
        # Define consistent colors (matching radar chart)
        config_colors = [
            '#FF0000',  # Bright Red
            '#0066CC',  # Deep Blue  
            '#00AA00',  # Green
            '#FF6600',  # Orange
            '#9900CC',  # Purple
            '#00CCCC',  # Cyan
            '#FFCC00',  # Yellow
            '#CC0066'   # Magenta
        ]
        
        # Create plots for each metric
        for i, metric_info in enumerate(valid_metrics):
            ax = axes[i]
            
            # Prepare data
            values = metric_info['values']
            valid_indices = [j for j, v in enumerate(values) if v is not None]
            valid_values = [values[j] for j in valid_indices]
            valid_configs = [config_names[j] for j in valid_indices]
            
            if not valid_values:
                ax.text(0.5, 0.5, 'No Data Available', 
                       horizontalalignment='center', verticalalignment='center',
                       transform=ax.transAxes, fontsize=12)
                ax.set_title(f"{metric_info['name']}")
                continue
            
            # Create x positions for configurations
            x_positions = np.arange(len(valid_configs))
            
            # Create bar plot
            bars = ax.bar(x_positions, valid_values, 
                         color=[config_colors[j % len(config_colors)] for j in valid_indices],
                         alpha=0.8)
            
            # Customize the plot
            ax.set_xlabel('System Configuration', fontweight='bold', fontsize=16)
            ax.set_ylabel(f'Value ({metric_info["unit"]})', fontweight='bold', fontsize=16)
            ax.set_title(f'{metric_info["name"]} - Aggregate Values', fontweight='bold', fontsize=17, pad=25)

            
            # Set x-axis labels
            ax.set_xticks(x_positions)
            ax.set_xticklabels(valid_configs, fontsize=16)
            ax.tick_params(axis='y', labelsize=14)
            
            # Add value labels on top of bars
            for j, (bar, value) in enumerate(zip(bars, valid_values)):
                height = bar.get_height()
                ax.text(bar.get_x() + bar.get_width()/2., height,
                       f'{value:.4f}' if isinstance(value, float) else str(value),
                       ha='center', va='bottom', fontsize=13)
            ymax = ax.get_ylim()[1]
            ax.set_ylim(0, ymax * 1.3)  # 20% headroom
            
            # Add grid for better readability
            ax.grid(True, alpha=0.3, axis='y')
        
        # Hide unused subplots
        for i in range(num_metrics, len(axes)):
            axes[i].set_visible(False)
        
        # Adjust layout and save
        plt.tight_layout(pad=3.0)
        plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor='white')
        plt.close()
        
        print(f"Aggregate metric comparison saved to: {output_path}")
        return output_path
    
    def create_aggregate_summary_table(self, data: Dict[str, Any]) -> None:
        """
        Create a text summary table of aggregate metric values.
        
        Args:
            data: Parsed JSON experiment data
        """
        print("\n" + "="*80)
        print("AGGREGATE METRIC VALUES SUMMARY")
        print("="*80)
        
        metrics_info, config_names = self.extract_aggregate_data(data)
        
        for metric_info in metrics_info:
            print(f"\n{metric_info['name']} ({metric_info['unit']}):")
            print("-" * 60)
            
            direction_desc = "lower is better" if metric_info['direction'] == 'lower' else "higher is better"
            print(f"Direction: {direction_desc}")
            
            # Show values for each configuration
            valid_values = [(i, v) for i, v in enumerate(metric_info['values']) if v is not None]
            
            if not valid_values:
                print("No data available for this metric")
                continue
            
            print(f"\nAggregate values:")
            for i, value in valid_values:
                config_name = config_names[i]
                print(f"  {config_name}: {value:.6f}")
            
            # Identify best and worst
            if len(valid_values) > 1:
                if metric_info['direction'] == 'lower':
                    best_idx, best_val = min(valid_values, key=lambda x: x[1])
                    worst_idx, worst_val = max(valid_values, key=lambda x: x[1])
                else:
                    best_idx, best_val = max(valid_values, key=lambda x: x[1])
                    worst_idx, worst_val = min(valid_values, key=lambda x: x[1])
                
                print(f"\n  Best:  {config_names[best_idx]} = {best_val:.6f} ★")
                print(f"  Worst: {config_names[worst_idx]} = {worst_val:.6f} ✗")
        
        print("\n" + "="*80)