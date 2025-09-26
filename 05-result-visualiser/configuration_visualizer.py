"""
Configuration visualizer module.

This module creates clean and elegant visualizations showing the system 
configurations that won the experiment, displaying their behavioral variants 
in a simple and visually appealing way.
"""

import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend for headless environments
import matplotlib.pyplot as plt
import numpy as np
from typing import Dict, List, Any
from pathlib import Path


class ConfigurationVisualizer:
    """Creates clean visualizations of winning system configurations."""
    
    def __init__(self):
        """Initialize the configuration visualizer."""
        plt.style.use('default')
        
        # Define consistent colors (matching other visualizations)
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
    
    def create_configuration_overview(self, data: Dict[str, Any], output_path: str) -> str:
        """
        Create a clean and elegant configuration overview visualization.
        
        Args:
            data: Parsed JSON experiment data
            output_path: Path to save the output image
            
        Returns:
            str: Path to the saved plot
        """
        print("Creating system configuration overview visualization...")
        
        # Extract configuration data
        ranking_data = data['ranking']
        
        # Create a clean, minimal figure (increased size for larger text)
        fig, ax = plt.subplots(figsize=(18, 12))
        ax.set_xlim(0, 16)
        ax.set_ylim(0, len(ranking_data) * 5 + 3)
        ax.axis('off')
        
        # Title
        ax.text(8, len(ranking_data) * 5 + 2, 'Top System Configurations', 
               fontsize=56, fontweight='bold', ha='center')
        
        # Draw each configuration
        for i, config in enumerate(ranking_data):
            y_base = len(ranking_data) * 5 - i * 5 - 2
            color = self.config_colors[i % len(self.config_colors)]
            config_name = f"Config-{config['position']}"
            
            # Configuration circle with rank number (larger circle and text)
            ax.scatter(1.5, y_base, s=5000, c=color, alpha=0.8, zorder=3, edgecolors='white', linewidth=3)
            ax.text(1.5, y_base, str(config['position']), fontsize=44, fontweight='bold', 
                    ha='center', va='center', color='white', zorder=4)
            ax.text(3.5, y_base, config_name, fontsize=40, fontweight='bold', va='center')
            
            # Extract behavioral variants grouped by service
            service_variants = {}
            if 'systemConfig' in config:
                for service in config['systemConfig']:
                    service_name = service.get('serviceName', 'Unknown Service')
                    # Simplify service name for display
                    display_service_name = service_name.replace('-service', '')
                    
                    service_methods = []
                    if 'classConfigs' in service:
                        for class_config in service['classConfigs']:
                            if 'behaviours' in class_config:
                                for behaviour in class_config['behaviours']:
                                    method = behaviour.get('methodName', '')
                                    variant = behaviour.get('behaviourId', '')
                                    service_methods.append(f"{method}: {variant}")
                    
                    if service_methods:
                        service_variants[display_service_name] = service_methods
            
            # Display service configurations as a block, vertically centered with config title
            if service_variants:
                # Create the service block
                service_lines = []
                for service_name, methods in service_variants.items():
                    methods_text = ", ".join(methods)
                    service_line = f"[{service_name}] {methods_text}"
                    service_lines.append(service_line)
                
                # Calculate vertical positioning to center the block with the config title
                num_services = len(service_lines)
                block_height = (num_services - 1) * 1.0  # Increased spacing for larger font
                start_y = y_base + block_height / 2  # Start above center to center the block
                
                # Position each service line in the block (adjusted for larger layout)
                for i, service_line in enumerate(service_lines):
                    line_y = start_y - i * 1.0  # Increased spacing for larger font
                    ax.text(9.0, line_y, service_line, fontsize=28, 
                            va='center', ha='left', color='#555555', style='italic')
            
            # Add subtle separator line (except for last item)
            if i < len(ranking_data) - 1:
                ax.axhline(y=y_base - 2.5, xmin=0.1, xmax=0.9, 
                          color='lightgray', alpha=0.4, linewidth=1)
        
        plt.tight_layout()
        plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor='white')
        plt.close()
        
        print(f"Configuration overview saved to: {output_path}")
        return output_path
    
    def create_configuration_summary_table(self, data: Dict[str, Any]) -> None:
        """
        Create a text summary table of system configurations.
        
        Args:
            data: Parsed JSON experiment data
        """
        print("\n" + "="*80)
        print("SYSTEM CONFIGURATION OVERVIEW")
        print("="*80)
        
        ranking_data = data['ranking']
        
        for i, config in enumerate(ranking_data):
            config_name = f"Config-{config['position']}"
            print(f"\n{config_name} (Rank #{config['position']}):")
            print("-" * 60)
            
            if 'systemConfig' in config:
                for service in config['systemConfig']:
                    service_name = service.get('serviceName', 'Unknown Service')
                    print(f"\n  📋 Service: {service_name}")
                    
                    if 'classConfigs' in service:
                        for class_config in service['classConfigs']:
                            if 'behaviours' in class_config:
                                for behaviour in class_config['behaviours']:
                                    method = behaviour.get('methodName', 'Unknown Method')
                                    variant = behaviour.get('behaviourId', 'Unknown Behaviour')
                                    print(f"       • {method} → {variant}")
        
        print("\n" + "="*80)