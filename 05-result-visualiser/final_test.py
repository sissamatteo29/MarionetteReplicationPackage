#!/usr/bin/env python3
"""
Final test script showing all three visualization types:
1. Line graph comparison (normalized)
2. Radar chart (normalized with overlap handling)
3. Individual metric graphs (raw values)
"""

import sys
from pathlib import Path

# Add current directory to path to import our modules
sys.path.append(str(Path(__file__).parent))

from main import ExperimentVisualizationPipeline


def main():
    """Run a complete test of all three visualization types."""
    
    # Path to the experiment data file
    config_file = "Complex-experiment-2hrs-16conf.json"
    
    if not Path(config_file).exists():
        print(f"Error: Configuration file '{config_file}' not found")
        return
    
    print("="*80)
    print("COMPLETE MICROSERVICE EXPERIMENT VISUALIZATION")
    print("="*80)
    
    # Initialize the pipeline
    pipeline = ExperimentVisualizationPipeline(config_file)
    
    # Load and validate data
    print("\n1. Loading and validating experiment data...")
    pipeline.load_data()
    if not pipeline.validate_data():
        print("Data validation failed!")
        return
    
    print(f"✓ Successfully loaded data for {len(pipeline.experiment_data['ranking'])} system configurations")
    print(f"✓ Found {len(pipeline.experiment_data['metricConfigs'])} metrics to analyze")
    
    # Generate detailed comparison table
    print("\n2. Generating detailed comparison analysis...")
    pipeline.system_visualizer.create_detailed_comparison_table(pipeline.experiment_data)
    
    # Generate all three visualization types
    print("\n3. Creating visualizations...")
    
    print("   📊 Generating normalized line graph comparison...")
    pipeline.generate_system_comparison_plots("output")
    
    print("   🎯 Generating radar chart with overlap handling...")
    pipeline.generate_radar_charts("output")
    
    print("   📈 Generating individual metric graphs (raw values)...")
    pipeline.generate_individual_metric_plots("output")
    
    print("\n" + "="*80)
    print("🎉 ALL VISUALIZATIONS COMPLETED SUCCESSFULLY!")
    print("="*80)
    
    print("\n📁 Generated files in 'output' directory:")
    print("   1. system_comparison.png     - Normalized line graph (0-1 scale)")
    print("   2. radar_chart.png          - Radar chart with offset handling")
    print("   3. individual_metrics.png   - Raw metric values per configuration")
    
    print("\n📋 Visualization Summary:")
    print("   • Line Graph: Shows normalized metrics (0-1) where 1 is always better")
    print("   • Radar Chart: Polygon view with offset lines to handle overlapping")
    print("   • Individual Metrics: Raw values for each metric, configurations by ranking")
    
    print("\n💡 Key Features:")
    print("   • Automatic normalization with direction handling (lower/higher is better)")
    print("   • Overlap-resistant radar chart with multiple visual distinctions")
    print("   • Raw value preservation for detailed metric analysis")
    print("   • Extensible pipeline architecture for adding new visualization types")
    
    print("="*80)


if __name__ == "__main__":
    main()
