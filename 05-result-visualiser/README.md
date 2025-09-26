# Microservice Experiment Results Visualizer

This Python application analyzes and visualizes experimental results from microservice configuration experiments. It creates normalized comparison plots showing different system configurations on a consistent 0-1 scale where 1 is always better.

## Features

- **Modular Architecture**: Extensible pipeline design for easy addition of new visualization steps
- **Three Visualization Types**:
  1. **Normalized Line Graph**: All metrics on 0-1 scale where 1 is always better
  2. **Radar Chart**: Polygon view with offset handling for overlapping configurations
  3. **Individual Metric Graphs**: Raw metric values showing actual performance per configuration
- **Automatic Normalization**: Converts metrics to consistent scale with direction handling
- **Overlap Handling**: Radar chart uses offsets, different colors, line styles, and markers
- **Raw Value Preservation**: Individual metric graphs show actual values without normalization
- **Configurable**: Command-line interface with multiple execution modes

## Project Structure

```
visualiser/
├── main.py                           # Main pipeline controller
├── data_loader.py                    # Data loading and validation
├── system_comparison_visualizer.py   # System-level comparison plots (normalized)
├── radar_chart_visualizer.py         # Radar chart with overlap handling
├── individual_metric_visualizer.py   # Individual metric graphs (raw values)
├── demo.py                          # Demo script
├── final_test.py                    # Comprehensive test script
├── requirements.txt                 # Python dependencies
├── Complex-experiment-2hrs-16conf.json  # Example experiment data
└── output/                          # Generated plots and reports
```

## Installation

1. **Clone or download the project**
2. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

## Usage

### Quick Start (Demo)

Run the demo with the provided example data:

```bash
python demo.py
```

This will:
- Load the example experiment data
- Generate a detailed comparison table in the console
- Create a normalized comparison plot in the `output/` directory

### Command Line Interface

For more control, use the main script:

```bash
# Run full pipeline
python main.py Complex-experiment-2hrs-16conf.json

# Specify output directory
python main.py Complex-experiment-2hrs-16conf.json --output-dir my_results

# Run specific pipeline steps
python main.py Complex-experiment-2hrs-16conf.json --step load
python main.py Complex-experiment-2hrs-16conf.json --step validate
python main.py Complex-experiment-2hrs-16conf.json --step system-comparison
```

## Input Data Format

The application expects a JSON file with the following structure:

```json
{
  "metricConfigs": [
    {
      "metricName": "P95 Response Time",
      "order": 1,
      "unit": "s",
      "direction": "lower"
    }
  ],
  "ranking": [
    {
      "position": 1,
      "systemConfig": [...],
      "systemResults": [
        {
          "metricName": "P95 Response Time",
          "value": 0.7339764129802848,
          "unit": "s"
        }
      ],
      "serviceResults": [...]
    }
  ]
}
```

### Key Fields:

- **metricConfigs**: Defines metrics, their units, and optimization direction
  - `direction`: "lower" (lower is better) or "higher" (higher is better)
  - `order`: Priority order for metrics
- **ranking**: List of system configurations with their results
- **systemResults**: Aggregated metrics at the system level
- **serviceResults**: Individual service metrics (for future use)

## Output

### 1. Comparison Plot
- **X-axis**: Metric names with units
- **Y-axis**: Normalized scores (0-1, where 1 is always better)
- **Lines**: Each system configuration (Config-1, Config-2, etc.)
- **Colors**: Distinct colors for each configuration
- **Format**: High-resolution PNG image

### 2. Detailed Table
Console output showing:
- Raw metric configurations
- Normalized scores for each configuration
- Average scores per configuration
- Best overall configuration recommendation

## Normalization Logic

The application applies the following normalization:

1. **Scale to 0-1**: `(value - min) / (max - min)`
2. **Direction Inversion**: For "lower is better" metrics, apply `1 - normalized_value`
3. **Consistent Interpretation**: Final scores where 1 is always optimal

## Extending the Pipeline

The modular architecture allows easy extension:

### Adding New Visualization Types

1. Create a new visualizer class (e.g., `ServiceComparisonVisualizer`)
2. Add the visualizer to the main pipeline
3. Add corresponding command-line options

### Adding Pipeline Steps

```python
# In main.py
def add_custom_step(self):
    # Your custom logic here
    pass

# Add to pipeline
pipeline.add_pipeline_step("custom-step", pipeline.add_custom_step)
```

## Example Output Interpretation

In the generated plot:
- **Higher lines**: Better performing configurations
- **Steep drops**: Metrics where the configuration performs poorly
- **Consistent high values**: Well-balanced configurations
- **Average scores**: Overall configuration ranking

## Dependencies

- `matplotlib>=3.5.0`: Plotting and visualization
- `numpy>=1.21.0`: Numerical computations
- `seaborn>=0.11.0`: Enhanced plot styling

## Troubleshooting

### Common Issues:

1. **Missing dependencies**: Run `pip install -r requirements.txt`
2. **File not found**: Ensure JSON file path is correct
3. **Invalid JSON**: Validate JSON format using online tools
4. **Permission errors**: Check write permissions for output directory

### Debugging:

Use step-by-step execution to isolate issues:
```bash
python main.py data.json --step load
python main.py data.json --step validate
```

## Future Enhancements

The architecture supports easy addition of:
- Service-level comparison plots
- Time-series analysis
- Statistical significance testing
- Interactive visualizations
- Export to different formats (PDF, SVG, etc.)
- Multi-experiment comparisons
