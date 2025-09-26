#!/usr/bin/env python3
"""
Test script for the improved radar chart with overlap handling.
"""

import sys
from pathlib import Path

# Add current directory to path to import our modules
sys.path.append(str(Path(__file__).parent))

from radar_chart_visualizer import RadarChartVisualizer
import json


def main():
    """Test the improved radar chart functionality."""
    
    # Path to the experiment data file
    config_file = "Complex-experiment-2hrs-16conf.json"
    
    if not Path(config_file).exists():
        print(f"Error: Configuration file '{config_file}' not found")
        return
    
    print("="*60)
    print("RADAR CHART OVERLAP HANDLING TEST")
    print("="*60)
    
    # Load data
    with open(config_file, 'r') as f:
        data = json.load(f)
    
    # Initialize visualizer
    visualizer = RadarChartVisualizer()
    
    print("Creating improved radar charts...")
    
    # Create standard radar chart with overlap improvements
    print("1. Generating standard radar chart with overlap handling...")
    path1 = visualizer.create_radar_chart(data, "output/radar_improved.png")
    print(f"   Saved to: {path1}")
    
    # Create offset radar chart
    print("2. Generating offset radar chart...")
    path2 = visualizer.create_radar_chart_with_offsets(data, "output/radar_offset.png")
    print(f"   Saved to: {path2}")
    
    print("\n" + "="*60)
    print("RADAR CHART TEST COMPLETED!")
    print("Check the 'output' directory for:")
    print("- radar_improved.png (with line styles, markers, and transparency)")
    print("- radar_offset.png (with angular offsets for overlapping lines)")
    print("="*60)


if __name__ == "__main__":
    main()
