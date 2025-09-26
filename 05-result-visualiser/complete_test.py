#!/usr/bin/env python3
"""
Complete test script showing both line graph and radar chart visualizations.
"""

import sys
from pathlib import Path

# Add current directory to path to import our modules
sys.path.append(str(Path(__file__).parent))

from main import ExperimentVisualizationPipeline


def main():
    """Run a complete test of both visualization types."""
    
    # Path to the experiment data file
    config_file = "Complex-experiment-2hrs-16conf.json"
    
    if not Path(config_file).exists():
        print(f"Error: Configuration file '{config_file}' not found")
        return
    
    print("="*70)
    print("COMPLETE VISUALIZATION TEST")
    print("="*70)
    
    # Initialize the pipeline
    pipeline = ExperimentVisualizationPipeline(config_file)
    
    # Load and validate data
    print("\n1. Loading and validating data...")
    pipeline.load_data()
    if not pipeline.validate_data():
        print("Data validation failed!")
        return
    
    # Generate detailed comparison table
    print("\n2. Generating detailed comparison table...")
    pipeline.system_visualizer.create_detailed_comparison_table(pipeline.experiment_data)
    
    # Generate line graph comparison
    print("\n3. Creating line graph visualization...")
    pipeline.generate_system_comparison_plots("output")
    
    # Generate radar chart
    print("\n4. Creating radar chart visualization...")
    pipeline.generate_radar_charts("output")
    
    print("\n" + "="*70)
    print("VISUALIZATION TEST COMPLETED SUCCESSFULLY!")
    print("\nGenerated files in 'output' directory:")
    print("  - system_comparison.png (Line graph)")
    print("  - radar_chart.png (Radar chart with offset handling)")
    print("\nBoth charts show normalized metrics (0-1 scale) where 1 is always better.")
    print("The radar chart uses different colors, line styles, and markers")
    print("with small offsets to handle overlapping configurations.")
    print("="*70)


if __name__ == "__main__":
    main()
