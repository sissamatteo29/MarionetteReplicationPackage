"""
Service-level metric visualizer module.

This module creates multi-bar vertical plots for each metric, showing service-level
performance across different system configurations. Each metric gets its own graph
with services on the x-axis and grouped bars for each configuration.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import numpy as np
from typing import Dict, List, Tuple, Any
from pathlib import Path
import math


class ServiceLevelVisualizer:
    """Creates service-level comparison plots with grouped bars for each metric."""
    
    def __init__(self):
        """Initialize the service-level visualizer."""
        plt.style.use('default')
    
    def extract_service_data(self, data: Dict[str, Any]) -> Tuple[List[Dict], List[str], List[str]]:
        """
        Extract service-level data from experiment results.
        
        Args:
            data: Parsed JSON data
            
        Returns:
            Tuple containing metrics data, service names, and configuration names
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
                'service_data': {}  # Will store {service_name: [values for each config]}
            }
            metrics_info.append(metric_info)
        
        # Extract service names from the first configuration
        service_names = []
        if ranking_data:
            first_config = ranking_data[0]
            service_names = [service['serviceName'] for service in first_config.get('serviceResults', [])]
        
        # Sort configurations by ranking position
        sorted_configs = sorted(ranking_data, key=lambda x: x['position'])
        config_names = [f"Config-{config['position']}" for config in sorted_configs]
        
        # Initialize service data structure
        for metric_info in metrics_info:
            for service_name in service_names:
                metric_info['service_data'][service_name] = []
        
        # Extract values for each configuration and service
        for config in sorted_configs:
            service_results = config.get('serviceResults', [])
            
            for service_result in service_results:
                service_name = service_result['serviceName']
                
                for i, metric_config in enumerate(metric_configs):
                    metric_name = metric_config['metricName']
                    
                    # Find the metric value for this service
                    for result in service_result.get('results', []):
                        if result['metricName'] == metric_name:
                            value = result['value']
                            metrics_info[i]['service_data'][service_name].append(value)
                            break
                    else:
                        # If metric not found, append None
                        metrics_info[i]['service_data'][service_name].append(None)
        
        return metrics_info, service_names, config_names
    
    def scale_values_for_visualization(self, values: List[float], metric_name: str, unit: str) -> Tuple[List[float], str]:
        """
        Scale values to make them more visually understandable while preserving original measures.
        
        Args:
            values: List of metric values
            metric_name: Name of the metric
            unit: Unit of the metric
            
        Returns:
            Tuple of scaled values and new unit description
        """
        # Remove None values for scaling calculation
        valid_values = [v for v in values if v is not None]
        
        if not valid_values:
            return values, unit
        
        max_val = max(valid_values)
        min_val = min(valid_values)
        
        # Scale based on magnitude to improve readability
        if 'Memory' in metric_name or 'memory' in metric_name:
            # Convert bytes to more readable units
            if max_val >= 1e9:  # GB
                scaled_values = [v / 1e9 if v is not None else None for v in values]
                return scaled_values, "GB"
            elif max_val >= 1e6:  # MB
                scaled_values = [v / 1e6 if v is not None else None for v in values]
                return scaled_values, "MB"
            elif max_val >= 1e3:  # KB
                scaled_values = [v / 1e3 if v is not None else None for v in values]
                return scaled_values, "KB"
        
        elif 'Time' in metric_name or 'time' in metric_name:
            # Keep time in seconds, but may scale if very small
            if max_val < 0.001:  # microseconds
                scaled_values = [v * 1e6 if v is not None else None for v in values]
                return scaled_values, "μs"
            elif max_val < 1:  # milliseconds
                scaled_values = [v * 1000 if v is not None else None for v in values]
                return scaled_values, "ms"
        
        # For other metrics or if no scaling needed, return original values
        return values, unit
    
    def create_service_level_comparison(self, data: Dict[str, Any], output_path: str = "output/service_level_comparison.png") -> str:
        """
        Create service-level comparison plots with grouped bars for each metric.
        
        Args:
            data: Parsed JSON data
            output_path: Path to save the combined graph
            
        Returns:
            Path to the saved graph
        """
        metrics_info, service_names, config_names = self.extract_service_data(data)
        num_metrics = len(metrics_info)
        num_configs = len(config_names)
        
        if not service_names:
            print("No service data found!")
            return output_path
        
        # Calculate subplot layout
        if num_metrics <= 2:
            rows, cols = 1, num_metrics
        elif num_metrics == 3:
            rows, cols = 1, 3
        elif num_metrics <= 6:
            rows, cols = 2, 3
        elif num_metrics <= 9:
            rows, cols = 3, 3
        else:
            rows = math.ceil(math.sqrt(num_metrics))
            cols = math.ceil(num_metrics / rows)
        
        # Create the figure with subplots
        fig, axes = plt.subplots(rows, cols, figsize=(7*cols, 6*rows))  # increased size slightly

        
        # Handle different subplot configurations
        if num_metrics == 1:
            axes = [axes]
        elif rows == 1 or cols == 1:
            axes = axes.flatten() if hasattr(axes, 'flatten') else [axes]
        else:
            axes = axes.flatten()
        
        # Define distinct colors for each configuration (matching radar chart)
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
        
        # Set up bar positioning
        bar_width = 0.8 / num_configs  # Total width of 0.8, divided by number of configs
        x_positions = np.arange(len(service_names))
        
        for i, metric_info in enumerate(metrics_info):
            ax = axes[i]
            
            # Collect all values for scaling
            all_values = []
            for service_name in service_names:
                all_values.extend([v for v in metric_info['service_data'][service_name] if v is not None])
            
            # Scale values for better visualization
            scaled_data = {}
            if all_values:
                scaled_values, display_unit = self.scale_values_for_visualization(all_values, metric_info['name'], metric_info['unit'])
                
                # Apply scaling to each service
                start_idx = 0
                for service_name in service_names:
                    service_values = metric_info['service_data'][service_name]
                    end_idx = start_idx + len(service_values)
                    scaled_service_values = scaled_values[start_idx:end_idx]
                    scaled_data[service_name] = scaled_service_values
                    start_idx = end_idx
            else:
                display_unit = metric_info['unit']
                for service_name in service_names:
                    scaled_data[service_name] = metric_info['service_data'][service_name]
            
            # Create grouped bars
            for config_idx, config_name in enumerate(config_names):
                values_for_config = []
                
                for service_name in service_names:
                    if config_idx < len(scaled_data[service_name]):
                        value = scaled_data[service_name][config_idx]
                        values_for_config.append(value if value is not None else 0)
                    else:
                        values_for_config.append(0)
                
                # Calculate x positions for this configuration's bars
                x_offset = (config_idx - num_configs/2 + 0.5) * bar_width
                x_pos = x_positions + x_offset
                
                # Create bars
                color = config_colors[config_idx % len(config_colors)]
                bars = ax.bar(x_pos, values_for_config, bar_width, 
                             label=config_name, color=color, alpha=0.8,
                             edgecolor='white', linewidth=1)
                
                # Add value labels on bars for better readability
                for j, (bar, value) in enumerate(zip(bars, values_for_config)):
                    if value > 0:  # Only label non-zero values
                        height = bar.get_height()
                        ax.text(bar.get_x() + bar.get_width()/2., height + height*0.01,
                               f'{value:.2f}' if value < 100 else f'{value:.0f}',
                               ha='center', va='bottom', fontsize=11, rotation=0)
            
            ymax = ax.get_ylim()[1]              # current automatic max
            ax.set_ylim(0, ymax * 1.6) 
            
            # Customize the subplot
            ax.set_title(f'{metric_info["name"]}\n({display_unit})', 
                        fontsize=18, fontweight='bold', pad=15)
            
            ax.set_ylabel(f'Value ({display_unit})', fontsize=16, fontweight='bold')
            
            # Set x-axis labels
            ax.set_xticks(x_positions)
            ax.set_xticklabels([name.replace('-service', '') for name in service_names], 
                              rotation=45, ha='right', fontsize=15)
            
            # Add grid for better readability
            ax.grid(True, alpha=0.3, axis='y')
            
            # Add legend to the first subplot only
            # if i == 0:
            #    ax.legend(bbox_to_anchor=(1.05, 1), loc='upper left', fontsize=13)
        
        # Hide unused subplots
        for i in range(num_metrics, len(axes)):
            axes[i].set_visible(False)
        
        last_ax = axes[num_metrics - 1]
        last_ax.legend(
            loc='upper right',
            fontsize=15,
            frameon=False
        )
        
        
        # Adjust layout
        plt.tight_layout()
        plt.subplots_adjust(right=0.95, wspace=0.25, hspace=0.35)
        
        
        # Save the figure
        plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor='white')
        plt.close()
        
        print(f"Service-level comparison saved to: {output_path}")
        return output_path
    
    def create_service_summary_table(self, data: Dict[str, Any]) -> None:
        """
        Create a summary table showing service-level metric values.
        
        Args:
            data: Parsed JSON data
        """
        metrics_info, service_names, config_names = self.extract_service_data(data)
        
        print("\n" + "="*80)
        print("SERVICE-LEVEL METRIC ANALYSIS")
        print("="*80)
        
        for metric_info in metrics_info:
            print(f"\n{metric_info['name']} ({metric_info['unit']}):")
            print("-" * 60)
            
            direction_desc = "lower is better" if metric_info['direction'] == 'lower' else "higher is better"
            print(f"Direction: {direction_desc}")
            
            # Create table
            print(f"\n{'Service':<25}", end="")
            for config_name in config_names:
                print(f"{config_name:>12}", end="")
            print()
            print("-" * (25 + 12 * len(config_names)))
            
            for service_name in service_names:
                print(f"{service_name:<25}", end="")
                values = metric_info['service_data'][service_name]
                
                for j, value in enumerate(values):
                    if value is not None:
                        if value < 0.001:
                            print(f"{value:>12.2e}", end="")
                        elif value < 1:
                            print(f"{value:>12.4f}", end="")
                        elif value < 1000:
                            print(f"{value:>12.2f}", end="")
                        else:
                            print(f"{value:>12.0f}", end="")
                    else:
                        print(f"{'N/A':>12}", end="")
                print()
        
        print("="*80)
