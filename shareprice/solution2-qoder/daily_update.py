#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Daily Update Script for Incremental Learning Model
This script demonstrates how to add new daily data and update the model incrementally.
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import os
import sys

# Import the incremental model class
from train_and_predict_incremental import IncrementalStockPredictionModel

def create_sample_new_data(last_date, days=1):
    """
    Create sample new data for demonstration
    In real usage, this would be replaced with actual new data from your data source
    
    Args:
        last_date (datetime): The last date in existing data
        days (int): Number of new days to generate
        
    Returns:
        pd.DataFrame: New data in the same format as historical data
    """
    new_data = []
    
    for i in range(days):
        # Generate new date
        new_date = last_date + timedelta(days=i+1)
        
        # Generate realistic sample data (in real usage, use actual market data)
        # This is just for demonstration - use real data in production
        base_price = 4300 + np.random.normal(0, 50)  # Random walk around 4300
        volatility = np.random.uniform(0.01, 0.03)   # 1-3% daily volatility
        
        open_price = base_price * (1 + np.random.normal(0, volatility))
        high_price = open_price * (1 + abs(np.random.normal(0, volatility/2)))
        low_price = open_price * (1 - abs(np.random.normal(0, volatility/2)))
        close_price = open_price + np.random.normal(0, volatility * open_price)
        volume = np.random.uniform(200, 400) * 1000  # 200K-400K volume
        
        # Ensure high >= max(open, close) and low <= min(open, close)
        high_price = max(high_price, open_price, close_price)
        low_price = min(low_price, open_price, close_price)
        
        new_row = {
            '日期': new_date.strftime('%Y-%m-%d'),
            '收盘': f"{close_price:.2f}",
            '开盘': f"{open_price:.2f}",
            '高': f"{high_price:.2f}",
            '低': f"{low_price:.2f}",
            '交易量': f"{volume/1000:.2f}K",
            '涨跌幅': f"{np.random.uniform(-3, 3):.2f}%"
        }
        new_data.append(new_row)
    
    return pd.DataFrame(new_data)

def load_existing_data(data_path):
    """Load existing historical data"""
    try:
        df = pd.read_csv(data_path, encoding='utf-8-sig')
        print(f"Loaded existing data with {len(df)} records")
        return df
    except Exception as e:
        print(f"Error loading existing data: {e}")
        return None

def save_updated_data(df, data_path):
    """Save updated data back to CSV"""
    try:
        df.to_csv(data_path, index=False, encoding='utf-8-sig')
        print(f"Updated data saved to {data_path}")
    except Exception as e:
        print(f"Error saving updated data: {e}")

def daily_update_workflow(new_data_source="simulate"):
    """
    Complete workflow for daily model update
    
    Args:
        new_data_source (str): "simulate" for demo data, "file" for real CSV file, "api" for API data
    """
    print("=== Daily Incremental Update Workflow ===")
    
    # Paths
    historical_data_path = "../沪深300指数历史数据.csv"
    
    # Initialize model
    model = IncrementalStockPredictionModel(lookback_days=10)
    
    # Load existing model if available
    model_exists = model.load_model()
    
    if not model_exists:
        print("No existing model found. Please run initial training first:")
        print("python3 train_and_predict_incremental.py")
        return
    
    # Load existing historical data
    existing_df = load_existing_data(historical_data_path)
    if existing_df is None:
        return
    
    # Convert date column for processing
    existing_df['日期'] = pd.to_datetime(existing_df['日期'])
    last_date = existing_df['日期'].max()
    
    print(f"Last date in existing data: {last_date.strftime('%Y-%m-%d')}")
    
    # Get new data based on source
    if new_data_source == "simulate":
        print("\n--- Simulating new daily data ---")
        new_df = create_sample_new_data(last_date, days=1)
        print(f"Generated new data for: {new_df['日期'].iloc[0]}")
        
    elif new_data_source == "file":
        print("\n--- Loading new data from file ---")
        new_data_path = input("Enter path to new data CSV file: ")
        try:
            new_df = pd.read_csv(new_data_path, encoding='utf-8-sig')
            print(f"Loaded {len(new_df)} new records from file")
        except Exception as e:
            print(f"Error loading new data file: {e}")
            return
            
    elif new_data_source == "api":
        print("\n--- API data source not implemented in this demo ---")
        print("In production, you would fetch data from your market data API here")
        return
    
    else:
        print(f"Unknown data source: {new_data_source}")
        return
    
    # Display new data
    print("\n--- New Data to Add ---")
    for _, row in new_df.iterrows():
        print(f"Date: {row['日期']}")
        print(f"  收盘: {row['收盘']}")
        print(f"  开盘: {row['开盘']}")
        print(f"  高: {row['高']}")
        print(f"  低: {row['低']}")
        print(f"  交易量: {row['交易量']}")
        print()
    
    # Update the model with new data
    print("--- Updating Model ---")
    model.train_or_update_model(new_df, is_initial_training=False)
    
    # Optionally update the historical data file
    update_historical = input("Update historical data file with new data? (y/n): ").lower().strip()
    if update_historical == 'y':
        # Convert back to string format for saving
        existing_df['日期'] = existing_df['日期'].dt.strftime('%Y-%m-%d')
        combined_df = pd.concat([existing_df, new_df], ignore_index=True)
        save_updated_data(combined_df, historical_data_path)
    
    # Make new predictions
    print("\n--- Making New Predictions ---")
    # Use updated historical data for prediction
    if update_historical == 'y':
        prediction_data = combined_df
    else:
        # Convert back to string format for prediction
        existing_df['日期'] = existing_df['日期'].dt.strftime('%Y-%m-%d')
        prediction_data = pd.concat([existing_df, new_df], ignore_index=True)
    
    predictions = model.predict_future(prediction_data, days=5)
    
    # Save predictions
    output_path = "predict.csv"
    predictions.to_csv(output_path, index=False, encoding='utf-8-sig')
    print(f"Updated predictions saved to {output_path}")
    
    # Display predictions
    print("\n=== Updated 5-Day Predictions ===")
    for idx, row in predictions.iterrows():
        print(f"Date: {row['日期'].strftime('%Y-%m-%d')}")
        print(f"  收盘: {row['收盘']:.2f}")
        print(f"  开盘: {row['开盘']:.2f}")
        print(f"  高: {row['高']:.2f}")
        print(f"  低: {row['低']:.2f}")
        print(f"  交易量: {row['交易量']:.0f}")
        print()

def main():
    """Main function with options for different update scenarios"""
    print("=== Incremental Model Daily Update Tool ===")
    print("Choose data source:")
    print("1. Simulate new daily data (for demo)")
    print("2. Load from CSV file")
    print("3. Fetch from API (placeholder)")
    
    choice = input("Enter choice (1-3): ").strip()
    
    if choice == "1":
        daily_update_workflow("simulate")
    elif choice == "2":
        daily_update_workflow("file")
    elif choice == "3":
        daily_update_workflow("api")
    else:
        print("Invalid choice. Using simulation mode.")
        daily_update_workflow("simulate")

if __name__ == "__main__":
    main()