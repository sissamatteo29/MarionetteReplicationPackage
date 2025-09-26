#!/usr/bin/env python3
"""
Main controller for the experiment results visualization pipeline.

This script serves as the main entry point for processing and visualizing
experimental results from microservice configuration experiments.
The architecture is designed to be extensible for adding new pipeline steps.
"""

import json
import argparse
import sys
from pathlib import Path
from typing import Dict, Any, List

from data_loader import ExperimentDataLoader
from system_comparison_visualizer import SystemComparisonVisualizer
from radar_chart_visualizer_new import RadarChartVisualizer
from individual_metric_visualizer import IndividualMetricVisualizer
from service_level_visualizer import ServiceLevelVisualizer
from aggregate_metric_visualizer import AggregateMetricVisualizer
from configuration_visualizer import ConfigurationVisualizer


class ExperimentVisualizationPipeline:
    """
    Main pipeline controller for experiment visualization.
    
    This class orchestrates the entire workflow from data loading to visualization.
    New pipeline steps can be easily added by extending this class.
    """
    
    def __init__(self, config_file_path: str):
        """
        Initialize the pipeline with the experiment configuration file.
        
        Args:
            config_file_path (str): Path to the JSON configuration file
        """
        self.config_file_path = Path(config_file_path)
        self.data_loader = ExperimentDataLoader()
        self.system_visualizer = SystemComparisonVisualizer()
        self.radar_visualizer = RadarChartVisualizer()
        self.individual_metric_visualizer = IndividualMetricVisualizer()
        self.service_level_visualizer = ServiceLevelVisualizer()
        self.aggregate_metric_visualizer = AggregateMetricVisualizer()
        self.configuration_visualizer = ConfigurationVisualizer()
        self.experiment_data = None
        
    def load_data(self) -> Dict[str, Any]:
        """
        Load and validate experiment data from the configuration file.
        
        Returns:
            Dict[str, Any]: Loaded experiment data
        """
        print(f"Loading experiment data from: {self.config_file_path}")
        self.experiment_data = self.data_loader.load_from_file(self.config_file_path)
        print(f"Loaded data for {len(self.experiment_data['ranking'])} system configurations")
        return self.experiment_data
    
    def validate_data(self) -> bool:
        """
        Validate the loaded experiment data structure.
        
        Returns:
            bool: True if data is valid, False otherwise
        """
        if not self.experiment_data:
            print("Error: No data loaded")
            return False
            
        required_keys = ['metricConfigs', 'ranking']
        for key in required_keys:
            if key not in self.experiment_data:
                print(f"Error: Missing required key '{key}' in experiment data")
                return False
        
        print("Data validation passed")
        return True
    
    def generate_system_comparison_plots(self, output_dir: str = "output") -> None:
        """
        Generate system-level comparison plots.
        
        Args:
            output_dir (str): Directory to save output plots
        """
        if not self.experiment_data:
            raise ValueError("No data loaded. Call load_data() first.")
            
        print("Generating system comparison visualization...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        self.system_visualizer.create_normalized_comparison_plot(
            self.experiment_data,
            save_path=output_path / "system_comparison.png"
        )
        print(f"System comparison plot saved to: {output_path / 'system_comparison.png'}")
    
    def generate_radar_charts(self, output_dir: str = "output") -> None:
        """
        Generate radar chart visualization.
        
        Args:
            output_dir (str): Directory to save output plots
        """
        if not self.experiment_data:
            raise ValueError("No data loaded. Call load_data() first.")
            
        print("Generating radar chart visualization...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        # Generate radar chart with offset handling
        chart_path = self.radar_visualizer.create_radar_chart(
            self.experiment_data,
            str(output_path / "radar_chart.png")
        )
        print(f"Radar chart saved to: {chart_path}")
    
    def generate_individual_metric_plots(self, output_dir: str = "output") -> None:
        """
        Generate individual metric visualization plots.
        
        Args:
            output_dir (str): Directory to save output plots
        """
        if not self.experiment_data:
            raise ValueError("No data loaded. Call load_data() first.")
            
        print("Generating individual metric visualizations...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        # Generate individual metric analysis
        metric_path = self.individual_metric_visualizer.create_individual_metric_graphs(
            self.experiment_data,
            str(output_path / "individual_metrics.png")
        )
        
        # Generate detailed metric summary
        self.individual_metric_visualizer.create_metric_summary_table(self.experiment_data)
    
    def generate_service_level_plots(self, output_dir: str = "output") -> None:
        """
        Generate service-level comparison plots.
        
        Args:
            output_dir (str): Directory to save output plots
        """
        if not self.experiment_data:
            raise ValueError("No data loaded. Call load_data() first.")
            
        print("Generating service-level metric comparison visualizations...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        # Generate service-level comparison chart
        service_path = self.service_level_visualizer.create_service_level_comparison(
            self.experiment_data,
            str(output_path / "service_level_comparison.png")
        )
        
        # Generate detailed service summary table
        self.service_level_visualizer.create_service_summary_table(self.experiment_data)
    
    def generate_aggregate_metric_plots(self, output_dir: str = "output") -> None:
        """
        Generate aggregate metric value plots.
        
        Args:
            output_dir (str): Directory to save output plots
        """
        if not self.experiment_data:
            raise ValueError("No data loaded. Call load_data() first.")
            
        print("Generating aggregate metric value visualizations...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        # Generate aggregate metric comparison chart
        aggregate_path = self.aggregate_metric_visualizer.create_aggregate_metric_comparison(
            self.experiment_data,
            str(output_path / "aggregate_metrics.png")
        )
        
        # Generate detailed aggregate summary table
        self.aggregate_metric_visualizer.create_aggregate_summary_table(self.experiment_data)
    
    def generate_configuration_overview(self, output_dir: str = "output") -> None:
        """
        Generate system configuration overview visualization.
        
        Args:
            output_dir (str): Directory to save output plots
        """
        if not self.experiment_data:
            raise ValueError("No data loaded. Call load_data() first.")
            
        print("Generating system configuration overview visualization...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        # Generate configuration overview chart
        config_path = self.configuration_visualizer.create_configuration_overview(
            self.experiment_data,
            str(output_path / "configuration_overview.png")
        )
        
        # Generate detailed configuration summary table
        self.configuration_visualizer.create_configuration_summary_table(self.experiment_data)
    
    def run_full_pipeline(self, output_dir: str = "output", include_radar: bool = True) -> None:
        """
        Run the complete visualization pipeline.
        
        Args:
            output_dir (str): Directory to save output files
            include_radar (bool): Whether to generate radar charts
        """
        try:
            # Step 1: Load data
            self.load_data()
            
            # Step 2: Validate data
            if not self.validate_data():
                sys.exit(1)
            
            # Step 3: Generate visualizations
            self.generate_system_comparison_plots(output_dir)
            
            # Step 4: Generate radar charts
            if include_radar:
                self.generate_radar_charts(output_dir)
            
            # Step 5: Generate individual metric plots
            self.generate_individual_metric_plots(output_dir)
            
            # Step 6: Generate service-level comparison plots
            self.generate_service_level_plots(output_dir)
            
            # Step 7: Generate aggregate metric plots
            self.generate_aggregate_metric_plots(output_dir)
            
            # Step 8: Generate configuration overview
            self.generate_configuration_overview(output_dir)
            
            print("Pipeline completed successfully!")
            
        except Exception as e:
            print(f"Pipeline failed with error: {e}")
            sys.exit(1)
    
    def add_pipeline_step(self, step_name: str, step_function, position: int = -1):
        """
        Add a new step to the pipeline (for future extensibility).
        
        Args:
            step_name (str): Name of the pipeline step
            step_function: Function to execute for this step
            position (int): Position to insert the step (-1 for append)
        """
        # This method can be extended to support dynamic pipeline modification
        print(f"Pipeline step '{step_name}' registration not yet implemented")
        pass


def main():
    """Main entry point for the visualization application."""
    parser = argparse.ArgumentParser(
        description="Visualize microservice experiment results"
    )
    parser.add_argument(
        "config_file",
        help="Path to the experiment configuration JSON file"
    )
    parser.add_argument(
        "--output-dir",
        default="output",
        help="Directory to save output plots (default: output)"
    )
    parser.add_argument(
        "--step",
        choices=["load", "validate", "system-comparison", "radar", "individual-metrics", "service-level", "aggregate-metrics", "configuration-overview", "all"],
        default="all",
        help="Run specific pipeline step (default: all)"
    )
    parser.add_argument(
        "--no-radar",
        action="store_true",
        help="Skip radar chart generation"
    )
    parser.add_argument(
        "--individual-radar",
        action="store_true",
        help="Generate individual radar charts for each configuration"
    )
    
    args = parser.parse_args()
    
    # Initialize pipeline
    pipeline = ExperimentVisualizationPipeline(args.config_file)
    
    # Run requested step(s)
    if args.step == "all":
        pipeline.run_full_pipeline(
            args.output_dir, 
            include_radar=not args.no_radar
        )
    elif args.step == "load":
        pipeline.load_data()
    elif args.step == "validate":
        pipeline.load_data()
        pipeline.validate_data()
    elif args.step == "system-comparison":
        pipeline.load_data()
        if pipeline.validate_data():
            pipeline.generate_system_comparison_plots(args.output_dir)
    elif args.step == "radar":
        pipeline.load_data()
        if pipeline.validate_data():
            pipeline.generate_radar_charts(args.output_dir)
    elif args.step == "individual-metrics":
        pipeline.load_data()
        if pipeline.validate_data():
            pipeline.generate_individual_metric_plots(args.output_dir)
    elif args.step == "service-level":
        pipeline.load_data()
        if pipeline.validate_data():
            pipeline.generate_service_level_plots(args.output_dir)
    elif args.step == "aggregate-metrics":
        pipeline.load_data()
        if pipeline.validate_data():
            pipeline.generate_aggregate_metric_plots(args.output_dir)
    elif args.step == "configuration-overview":
        pipeline.load_data()
        if pipeline.validate_data():
            pipeline.generate_configuration_overview(args.output_dir)


if __name__ == "__main__":
    main()
