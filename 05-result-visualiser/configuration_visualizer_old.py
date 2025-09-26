"""
Configuration visualizer module.

This module creates elegant visualizations showing the winning system configurations
with their behavioral variants, displaying services, methods, and behaviors
in a clear and visually appealing format.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch
import numpy as np
from typing import Dict, List, Tuple, Any
from pathlib import Path
import textwrap


class ConfigurationVisualizer:
    """Creates elegant visualizations of system configurations and their behavioral variants."""
    
    def __init__(self):
        """Initialize the configuration visualizer."""
        plt.style.use('default')
        
        # Define consistent colors (matching radar chart)
        self.config_colors = [
            '#FF0000',  # Bright Red
            '#0066CC',  # Deep Blue  
            '#00AA00',  # Green
            '#FF6600',  # Orange
            '#9900CC',  # Purple
            '#00CCCC',  # Cyan
            '#FFCC00',  # Yellow
            '#CC0066'   # Magenta
        ]
        
        # Define service colors (lighter variants for service backgrounds)
        self.service_colors = [
            '#FFE6E6',  # Light Red
            '#E6F0FF',  # Light Blue
            '#E6FFE6',  # Light Green
            '#FFF0E6',  # Light Orange
        ]
    
    def extract_configuration_data(self, data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Extract system configuration data with behavioral variants.
        
        Args:
            data: Parsed JSON experiment data
            
        Returns:
            List of configuration dictionaries with structured data
        """
        configurations = []
        ranking_data = data['ranking']
        
        for config in ranking_data:
            config_info = {
                'position': config['position'],
                'name': f"Config-{config['position']}",
                'color': self.config_colors[(config['position'] - 1) % len(self.config_colors)],
                'services': []
            }
            
            if 'systemConfig' in config:
                for service in config['systemConfig']:
                    service_info = {
                        'name': service.get('serviceName', 'Unknown Service'),
                        'display_name': service.get('serviceName', 'Unknown Service').replace('-service', ''),
                        'classes': []
                    }
                    
                    if 'classConfigs' in service:
                        for class_config in service['classConfigs']:
                            class_info = {
                                'name': class_config.get('className', 'Unknown Class'),
                                'short_name': self._extract_class_short_name(class_config.get('className', '')),
                                'behaviors': []
                            }
                            
                            if 'behaviours' in class_config:
                                for behaviour in class_config['behaviours']:
                                    behavior_info = {
                                        'method': behaviour.get('methodName', 'Unknown Method'),
                                        'variant': behaviour.get('behaviourId', 'Unknown Behaviour')
                                    }
                                    class_info['behaviors'].append(behavior_info)
                            
                            service_info['classes'].append(class_info)
                    
                    config_info['services'].append(service_info)
            
            configurations.append(config_info)
        
        return configurations
    
    def _extract_class_short_name(self, full_class_name: str) -> str:
        """Extract a short, readable name from the full class path."""
        if not full_class_name:
            return "Unknown"
        
        # Extract the class name from the full path
        parts = full_class_name.split('/')
        if parts:
            class_name = parts[-1].replace('.java', '')
            # Convert CamelCase to readable format
            result = ''
            for i, char in enumerate(class_name):
                if char.isupper() and i > 0:
                    result += ' '
                result += char
            return result
        return "Unknown"
    
    def create_configuration_overview(self, data: Dict[str, Any], output_path: str) -> str:
        """
        Create an elegant overview visualization of all system configurations.
        
        Args:
            data: Parsed JSON experiment data
            output_path: Path to save the output image
            
        Returns:
            str: Path to the saved plot
        """
        print("Creating system configuration overview visualization...")
        
        # Extract configuration data
        configurations = self.extract_configuration_data(data)
        
        if not configurations:
            raise ValueError("No configuration data found")
        
        # Calculate layout
        num_configs = len(configurations)
        fig_width = max(16, num_configs * 5)
        fig_height = 12
        
        # Create figure
        fig, ax = plt.subplots(figsize=(fig_width, fig_height))
        ax.set_xlim(0, fig_width)
        ax.set_ylim(0, fig_height)
        ax.axis('off')
        
        # Title
        ax.text(fig_width/2, fig_height - 0.8, 'Top System Configurations - Behavioral Variants', 
                fontsize=20, fontweight='bold', ha='center', va='top')
        
        # Calculate positions for configurations
        config_width = fig_width / num_configs * 0.8
        config_spacing = fig_width / num_configs
        start_y = fig_height - 2
        
        for i, config in enumerate(configurations):
            x_center = (i + 0.5) * config_spacing
            x_left = x_center - config_width / 2
            
            self._draw_configuration_panel(ax, config, x_left, start_y, config_width)
        
        # Add legend for behavioral variants
        self._add_behavior_legend(ax, configurations, fig_width, fig_height)
        
        # Save the plot
        plt.tight_layout()
        plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor='white')
        plt.close()
        
        print(f"Configuration overview saved to: {output_path}")
        return output_path
    
    def _draw_configuration_panel(self, ax, config: Dict[str, Any], x: float, y: float, width: float) -> None:
        """Draw a single configuration panel with its services and behaviors."""
        
        # Configuration header
        header_height = 1.2
        header_rect = FancyBboxPatch(
            (x, y - header_height), width, header_height,
            boxstyle="round,pad=0.1",
            facecolor=config['color'],
            edgecolor='black',
            linewidth=2,
            alpha=0.9
        )
        ax.add_patch(header_rect)
        
        # Configuration title
        ax.text(x + width/2, y - header_height/2, config['name'], 
                fontsize=16, fontweight='bold', ha='center', va='center', color='white')
        
        # Ranking position
        ax.text(x + width/2, y - header_height - 0.3, f"Rank #{config['position']}", 
                fontsize=12, fontweight='bold', ha='center', va='center')
        
        # Services
        service_start_y = y - header_height - 0.8
        service_height = 0.6
        service_spacing = 0.1
        
        for j, service in enumerate(config['services']):
            service_y = service_start_y - j * (service_height + service_spacing + 1.5)
            
            # Service background
            service_color = self.service_colors[j % len(self.service_colors)]
            service_rect = FancyBboxPatch(
                (x + 0.1, service_y - service_height), width - 0.2, service_height,
                boxstyle="round,pad=0.05",
                facecolor=service_color,
                edgecolor='gray',
                linewidth=1,
                alpha=0.7
            )
            ax.add_patch(service_rect)
            
            # Service name
            ax.text(x + width/2, service_y - service_height/2, service['display_name'], 
                    fontsize=12, fontweight='bold', ha='center', va='center')
            
            # Behaviors for this service
            behavior_y = service_y - service_height - 0.2
            for class_info in service['classes']:
                for behavior in class_info['behaviors']:
                    # Method and variant
                    method_text = f"{behavior['method']} → {behavior['variant']}"
                    
                    # Wrap text if too long
                    if len(method_text) > 25:
                        method_text = textwrap.fill(method_text, width=25)
                    
                    ax.text(x + width/2, behavior_y, method_text, 
                            fontsize=10, ha='center', va='center',
                            bbox=dict(boxstyle='round,pad=0.3', facecolor='white', 
                                    edgecolor=config['color'], linewidth=1, alpha=0.8))
                    
                    behavior_y -= 0.8
    
    def _add_behavior_legend(self, ax, configurations: List[Dict[str, Any]], fig_width: float, fig_height: float) -> None:
        """Add a legend showing all unique behavioral variants."""
        
        # Collect all unique behaviors
        all_behaviors = set()
        for config in configurations:
            for service in config['services']:
                for class_info in service['classes']:
                    for behavior in class_info['behaviors']:
                        all_behaviors.add((behavior['method'], behavior['variant']))
        
        # Position legend at the bottom
        legend_y = 1.5
        legend_x = 0.5
        
        ax.text(fig_width/2, legend_y + 0.5, 'Behavioral Variants Legend', 
                fontsize=14, fontweight='bold', ha='center', va='center')
        
        # Create legend entries
        behaviors_list = sorted(list(all_behaviors))
        cols = min(3, len(behaviors_list))
        col_width = fig_width / cols
        
        for i, (method, variant) in enumerate(behaviors_list):
            col = i % cols
            row = i // cols
            
            x_pos = legend_x + col * col_width
            y_pos = legend_y - 0.3 - row * 0.4
            
            # Legend entry
            legend_text = f"{method} → {variant}"
            ax.text(x_pos, y_pos, f"• {legend_text}", 
                    fontsize=10, ha='left', va='center',
                    bbox=dict(boxstyle='round,pad=0.2', facecolor='lightgray', alpha=0.5))
    
    def create_configuration_summary_table(self, data: Dict[str, Any]) -> None:
        """
        Create a text summary table of system configurations.
        
        Args:
            data: Parsed JSON experiment data
        """
        print("\n" + "="*80)
        print("SYSTEM CONFIGURATION OVERVIEW")
        print("="*80)
        
        configurations = self.extract_configuration_data(data)
        
        for config in configurations:
            print(f"\n{config['name']} (Rank #{config['position']}):")
            print("-" * 60)
            
            for service in config['services']:
                print(f"\n  📋 Service: {service['display_name']}")
                
                for class_info in service['classes']:
                    print(f"     Class: {class_info['short_name']}")
                    
                    for behavior in class_info['behaviors']:
                        print(f"       • {behavior['method']} → {behavior['variant']}")
        
        print("\n" + "="*80)