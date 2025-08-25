#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solution 1: Complete Retraining Model
This model retrains from scratch using all historical data every time.
Predicts future 5 days of stock prices including: 收盘, 开盘, 高, 低, 交易量
"""

import pandas as pd
import numpy as np
from sklearn.preprocessing import MinMaxScaler
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, mean_absolute_error
import joblib
import re
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

class StockPredictionModel:
    def __init__(self, lookback_days=10):
        """
        Initialize the stock prediction model
        
        Args:
            lookback_days (int): Number of previous days to use as features
        """
        self.lookback_days = lookback_days
        self.scalers = {}
        self.models = {}
        self.feature_columns = ['收盘', '开盘', '高', '低', '交易量']
        self.target_columns = ['收盘', '开盘', '高', '低', '交易量']
        
    def clean_numeric_value(self, value):
        """Clean numeric values from string format"""
        if isinstance(value, str):
            # Remove commas and convert K to thousands
            value = value.replace(',', '')
            if 'K' in value.upper():
                value = float(value.upper().replace('K', '')) * 1000
            elif '%' in value:
                value = float(value.replace('%', ''))
            else:
                value = float(value)
        return float(value)
    
    def preprocess_data(self, df):
        """
        Preprocess the raw CSV data
        
        Args:
            df (pd.DataFrame): Raw data from CSV
            
        Returns:
            pd.DataFrame: Processed data ready for modeling
        """
        # Create a copy to avoid modifying original
        processed_df = df.copy()
        
        # Convert date column
        processed_df['日期'] = pd.to_datetime(processed_df['日期'])
        
        # Clean numeric columns
        for col in self.feature_columns:
            processed_df[col] = processed_df[col].apply(self.clean_numeric_value)
        
        # Sort by date (oldest first)
        processed_df = processed_df.sort_values('日期').reset_index(drop=True)
        
        return processed_df
    
    def create_sequences(self, data, target_col):
        """
        Create sequences for time series prediction
        
        Args:
            data (np.array): Scaled feature data
            target_col (str): Target column name
            
        Returns:
            tuple: (X, y) where X is features and y is targets
        """
        X, y = [], []
        
        for i in range(self.lookback_days, len(data)):
            # Use past lookback_days as features
            X.append(data[i-self.lookback_days:i].flatten())
            # Target is the next day's value for the specific column
            col_idx = self.target_columns.index(target_col)
            y.append(data[i, col_idx])
            
        return np.array(X), np.array(y)
    
    def train_model(self, df):
        """
        Train the prediction model using all historical data
        
        Args:
            df (pd.DataFrame): Historical stock data
        """
        print("Starting model training...")
        
        # Preprocess data
        processed_df = self.preprocess_data(df)
        print(f"Processed {len(processed_df)} data points")
        
        # Extract features for scaling
        feature_data = processed_df[self.feature_columns].values
        
        # Initialize and fit scalers for each target
        for i, target_col in enumerate(self.target_columns):
            scaler = MinMaxScaler()
            # Scale all features, but we'll use different scalers for different targets
            scaled_data = scaler.fit_transform(feature_data)
            self.scalers[target_col] = scaler
            
            # Create sequences for this target
            X, y = self.create_sequences(scaled_data, target_col)
            
            if len(X) < 10:  # Need at least some data for training
                print(f"Warning: Not enough data for training {target_col}. Skipping...")
                continue
                
            # Train Random Forest model for this target
            model = RandomForestRegressor(
                n_estimators=100,
                max_depth=10,
                random_state=42,
                n_jobs=-1
            )
            model.fit(X, y)
            self.models[target_col] = model
            
            # Calculate and print training metrics
            y_pred = model.predict(X)
            mse = mean_squared_error(y, y_pred)
            mae = mean_absolute_error(y, y_pred)
            print(f"{target_col} - MSE: {mse:.6f}, MAE: {mae:.6f}")
        
        print("Model training completed!")
    
    def predict_future(self, df, days=5):
        """
        Predict future stock prices
        
        Args:
            df (pd.DataFrame): Historical data
            days (int): Number of days to predict
            
        Returns:
            pd.DataFrame: Predictions for future days
        """
        print(f"Predicting future {days} days...")
        
        # Preprocess data
        processed_df = self.preprocess_data(df)
        
        # Get the last sequence for prediction
        feature_data = processed_df[self.feature_columns].values
        
        predictions = []
        
        for day in range(days):
            day_predictions = {}
            
            for target_col in self.target_columns:
                if target_col not in self.models:
                    print(f"No model found for {target_col}, skipping...")
                    day_predictions[target_col] = 0
                    continue
                    
                # Use the scaler for this target
                scaler = self.scalers[target_col]
                model = self.models[target_col]
                
                # Scale the recent data
                scaled_data = scaler.transform(feature_data)
                
                # Get the last sequence
                last_sequence = scaled_data[-self.lookback_days:].flatten()
                
                # Predict
                pred_scaled = model.predict([last_sequence])[0]
                
                # Create a dummy array to inverse transform
                dummy = np.zeros((1, len(self.feature_columns)))
                col_idx = self.target_columns.index(target_col)
                dummy[0, col_idx] = pred_scaled
                
                # Inverse transform to get actual value
                pred_actual = scaler.inverse_transform(dummy)[0, col_idx]
                day_predictions[target_col] = pred_actual
            
            predictions.append(day_predictions)
            
            # Update feature_data with the new prediction for next iteration
            new_row = [day_predictions[col] for col in self.feature_columns]
            feature_data = np.vstack([feature_data, new_row])
        
        # Create prediction DataFrame
        last_date = processed_df['日期'].max()
        future_dates = [last_date + timedelta(days=i+1) for i in range(days)]
        
        pred_df = pd.DataFrame(predictions)
        pred_df['日期'] = future_dates
        
        # Reorder columns to match input format
        column_order = ['日期'] + self.target_columns
        pred_df = pred_df[column_order]
        
        print("Prediction completed!")
        return pred_df

def main():
    """Main function to run the complete retraining solution"""
    print("=== Solution 1: Complete Retraining Model ===")
    
    # Load data
    data_path = "../沪深300指数历史数据-full.csv"
    try:
        df = pd.read_csv(data_path, encoding='utf-8-sig')
        print(f"Loaded data with {len(df)} records")
    except Exception as e:
        print(f"Error loading data: {e}")
        return
    
    # Initialize and train model
    model = StockPredictionModel(lookback_days=10)
    model.train_model(df)
    
    # Make predictions
    predictions = model.predict_future(df, days=5)
    
    # Save predictions
    output_path = "predict.csv"
    predictions.to_csv(output_path, index=False, encoding='utf-8-sig')
    print(f"\nPredictions saved to {output_path}")
    
    # Display predictions
    print("\n=== Future 5 Days Predictions ===")
    for idx, row in predictions.iterrows():
        print(f"Date: {row['日期'].strftime('%Y-%m-%d')}")
        print(f"  收盘: {row['收盘']:.2f}")
        print(f"  开盘: {row['开盘']:.2f}")
        print(f"  高: {row['高']:.2f}")
        print(f"  低: {row['低']:.2f}")
        print(f"  交易量: {row['交易量']:.0f}")
        print()

if __name__ == "__main__":
    main()