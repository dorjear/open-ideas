#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Programmatic Usage Examples for Incremental Stock Prediction Model
This shows how to integrate the model into your own applications
"""

import pandas as pd
from datetime import datetime
from train_and_predict_incremental import IncrementalStockPredictionModel

def example_1_basic_prediction():
    """
    Example 1: Basic prediction using existing model
    """
    print("=== Example 1: Basic Prediction ===")
    
    # Initialize and load existing model
    model = IncrementalStockPredictionModel()
    
    if not model.load_model():
        print("No existing model found. Please run initial training first.")
        return
    
    # Load historical data
    data_path = "../沪深300指数历史数据.csv"
    df = pd.read_csv(data_path, encoding='utf-8-sig')
    
    # Make predictions
    predictions = model.predict_future(df, days=5)
    
    print("Predictions:")
    for _, row in predictions.iterrows():
        print(f"{row['日期'].strftime('%Y-%m-%d')}: Close={row['收盘']:.2f}")

def example_2_add_single_day_data():
    """
    Example 2: Add single day of new data and get updated predictions
    """
    print("\n=== Example 2: Add New Data ===")
    
    # Create sample new data
    new_data = {
        '日期': ['2025-8-23'],
        '收盘': ['4,385.20'],
        '开盘': ['4,378.00'],
        '高': ['4,395.45'],
        '低': ['4,375.10'],
        '交易量': ['275.32K'],
        '涨跌幅': ['0.16%']
    }
    new_df = pd.DataFrame(new_data)
    
    # Initialize model
    model = IncrementalStockPredictionModel()
    model.load_model()
    
    # Update model with new data
    print("Updating model with new data...")
    model.train_or_update_model(new_df, is_initial_training=False)
    
    # Load all data for prediction
    historical_data = pd.read_csv("../沪深300指数历史数据.csv", encoding='utf-8-sig')
    combined_data = pd.concat([historical_data, new_df], ignore_index=True)
    
    # Make new predictions
    predictions = model.predict_future(combined_data, days=5)
    
    print("Updated predictions:")
    for _, row in predictions.iterrows():
        print(f"{row['日期'].strftime('%Y-%m-%d')}: Close={row['收盘']:.2f}")

def example_3_batch_update():
    """
    Example 3: Batch update with multiple days of data
    """
    print("\n=== Example 3: Batch Update ===")
    
    # Create sample batch data (multiple days)
    batch_data = {
        '日期': ['2025-8-23', '2025-8-24'],
        '收盘': ['4,385.20', '4,390.50'],
        '开盘': ['4,378.00', '4,385.20'],
        '高': ['4,395.45', '4,400.30'],
        '低': ['4,375.10', '4,380.15'],
        '交易量': ['275.32K', '280.15K'],
        '涨跌幅': ['0.16%', '0.12%']
    }
    batch_df = pd.DataFrame(batch_data)
    
    # Initialize model
    model = IncrementalStockPredictionModel()
    model.load_model()
    
    # Update with batch data
    print("Updating model with batch data...")
    model.train_or_update_model(batch_df, is_initial_training=False)
    
    print(f"Model updated with {len(batch_df)} days of data")

def example_4_check_model_history():
    """
    Example 4: Check model training history and performance
    """
    print("\n=== Example 4: Model History ===")
    
    # Load model
    model = IncrementalStockPredictionModel()
    model.load_model()
    
    # Display training history
    print(f"Total training sessions: {len(model.training_history)}")
    
    for i, session in enumerate(model.training_history):
        print(f"\nSession {i+1}:")
        print(f"  Timestamp: {session['timestamp'].strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"  Data points: {session['data_points']}")
        print(f"  Type: {'Initial Training' if session['is_initial'] else 'Incremental Update'}")
        
        if 'metrics' in session and session['metrics']:
            print("  Performance metrics:")
            for target, metrics in session['metrics'].items():
                print(f"    {target}: MSE={metrics['mse']:.6f}, MAE={metrics['mae']:.6f}")

def example_5_production_integration():
    """
    Example 5: Production-ready integration pattern
    """
    print("\n=== Example 5: Production Integration ===")
    
    class StockPredictionService:
        def __init__(self):
            self.model = IncrementalStockPredictionModel()
            self.model.load_model()
            
        def add_daily_data(self, date, open_price, high, low, close, volume):
            """Add a single day of market data"""
            new_data = {
                '日期': [date],
                '收盘': [f"{close:.2f}"],
                '开盘': [f"{open_price:.2f}"],
                '高': [f"{high:.2f}"],
                '低': [f"{low:.2f}"],
                '交易量': [f"{volume/1000:.2f}K"],
                '涨跌幅': [f"{((close-open_price)/open_price*100):.2f}%"]
            }
            new_df = pd.DataFrame(new_data)
            
            # Update model
            self.model.train_or_update_model(new_df, is_initial_training=False)
            print(f"Model updated with data for {date}")
            
        def get_predictions(self, historical_data_path, days=5):
            """Get future predictions"""
            df = pd.read_csv(historical_data_path, encoding='utf-8-sig')
            predictions = self.model.predict_future(df, days=days)
            
            # Convert to simple dict format
            result = []
            for _, row in predictions.iterrows():
                result.append({
                    'date': row['日期'].strftime('%Y-%m-%d'),
                    'close': round(row['收盘'], 2),
                    'open': round(row['开盘'], 2),
                    'high': round(row['高'], 2),
                    'low': round(row['低'], 2),
                    'volume': int(row['交易量'])
                })
            return result
    
    # Example usage
    service = StockPredictionService()
    
    # Add new daily data
    service.add_daily_data(
        date='2025-8-23',
        open_price=4378.00,
        high=4395.45,
        low=4375.10,
        close=4385.20,
        volume=275320
    )
    
    # Get predictions
    predictions = service.get_predictions("../沪深300指数历史数据.csv")
    
    print("Service predictions:")
    for pred in predictions:
        print(f"  {pred['date']}: Close={pred['close']}")

def main():
    """Run all examples"""
    print("Stock Prediction Model - Usage Examples")
    print("=" * 50)
    
    # Run examples
    example_1_basic_prediction()
    example_2_add_single_day_data()
    example_3_batch_update()
    example_4_check_model_history()
    example_5_production_integration()
    
    print("\n" + "=" * 50)
    print("All examples completed!")

if __name__ == "__main__":
    main()