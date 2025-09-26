#!/usr/bin/env python3
"""
Demo script to test the visualization pipeline with the provided data.
"""

import sys
from pathlib import Path

# Add current directory to path to import our modules
sys.path.append(str(Path(__file__).parent))

from main import ExperimentVisualizationPipeline


def main():
    """Run a demo of the visualization pipeline."""
    
    # Path to the experiment data file
    config_file = "Complex-experiment-2hrs-16conf.json"
    
    if not Path(config_file).exists():
        print(f"Error: Configuration file '{config_file}' not found")
        print("Please ensure the JSON file is in the current directory")
        return
    
    print("="*60)
    print("EXPERIMENT VISUALIZATION DEMO")
    print("="*60)
    
    # Initialize the pipeline
    pipeline = ExperimentVisualizationPipeline(config_file)
    
    # Load and validate data
    print("\n1. Loading data...")
    pipeline.load_data()
    
    print("\n2. Validating data...")
    if not pipeline.validate_data():
        print("Data validation failed!")
        return
    
    # Generate detailed comparison table
    print("\n3. Generating detailed comparison...")
    pipeline.system_visualizer.create_detailed_comparison_table(pipeline.experiment_data)
    
    # Generate the plot
    print("\n4. Creating visualization...")
    pipeline.generate_system_comparison_plots("output")
    
    print("\n" + "="*60)
    print("DEMO COMPLETED SUCCESSFULLY!")
    print("Check the 'output' directory for generated plots.")
    print("="*60)


if __name__ == "__main__":
    main()
