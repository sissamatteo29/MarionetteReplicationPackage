"""
Data loader module for experiment configuration files.

This module handles loading and basic validation of experiment data
from JSON configuration files.
"""

import json
from pathlib import Path
from typing import Dict, Any, List


class ExperimentDataLoader:
    """Handles loading of experiment configuration data."""
    
    def load_from_file(self, file_path: Path) -> Dict[str, Any]:
        """
        Load experiment data from a JSON file and limit to first three configurations.
        
        Args:
            file_path (Path): Path to the JSON configuration file
            
        Returns:
            Dict[str, Any]: Loaded experiment data (limited to first 3 configurations)
            
        Raises:
            FileNotFoundError: If the file doesn't exist
            json.JSONDecodeError: If the file contains invalid JSON
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                data = json.load(file)
            
            # Limit analysis to only the first three system configurations
            if 'ranking' in data and len(data['ranking']) > 3:
                original_count = len(data['ranking'])
                data['ranking'] = data['ranking'][:3]
                print(f"Limited analysis to first 3 configurations (from {original_count} total)")
            
            return data
        except FileNotFoundError:
            raise FileNotFoundError(f"Configuration file not found: {file_path}")
        except json.JSONDecodeError as e:
            raise json.JSONDecodeError(f"Invalid JSON in file {file_path}: {e}")
    
    def extract_metric_configs(self, data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Extract metric configurations from experiment data.
        
        Args:
            data (Dict[str, Any]): Full experiment data
            
        Returns:
            List[Dict[str, Any]]: List of metric configurations
        """
        return data.get('metricConfigs', [])
    
    def extract_ranking_data(self, data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Extract ranking data from experiment data.
        
        Args:
            data (Dict[str, Any]): Full experiment data
            
        Returns:
            List[Dict[str, Any]]: List of ranked system configurations
        """
        return data.get('ranking', [])
    
    def get_system_results(self, ranking_data: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Extract system-level results from ranking data.
        
        Args:
            ranking_data (List[Dict[str, Any]]): Ranking data
            
        Returns:
            List[Dict[str, Any]]: System-level results for each configuration
        """
        system_results = []
        for config in ranking_data:
            system_results.append({
                'position': config.get('position'),
                'results': config.get('systemResults', [])
            })
        return system_results
    
    def get_service_results(self, ranking_data: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Extract service-level results from ranking data.
        
        Args:
            ranking_data (List[Dict[str, Any]]): Ranking data
            
        Returns:
            List[Dict[str, Any]]: Service-level results for each configuration
        """
        service_results = []
        for config in ranking_data:
            service_results.append({
                'position': config.get('position'),
                'services': config.get('serviceResults', [])
            })
        return service_results
