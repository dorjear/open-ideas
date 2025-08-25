#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Solution 2: Incremental Learning Model
This model saves trained models and incrementally updates them with new data.
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
import os
import pickle
import warnings
warnings.filterwarnings('ignore')

class IncrementalStockPredictionModel:
    def __init__(self, lookback_days=10, model_dir="models"):
        """
        Initialize the incremental stock prediction model
        
        Args:
            lookback_days (int): Number of previous days to use as features
            model_dir (str): Directory to save/load models
        """
        self.lookback_days = lookback_days
        self.model_dir = model_dir
        self.scalers = {}
        self.models = {}
        self.feature_columns = ['收盘', '开盘', '高', '低', '交易量']
        self.target_columns = ['收盘', '开盘', '高', '低', '交易量']
        self.training_history = []
        
        # Create model directory if it doesn't exist
        if not os.path.exists(self.model_dir):
            os.makedirs(self.model_dir)
    
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
    
    def save_model(self):
        """Save the trained model and scalers to disk"""
        model_data = {
            'models': self.models,
            'scalers': self.scalers,
            'lookback_days': self.lookback_days,
            'feature_columns': self.feature_columns,
            'target_columns': self.target_columns,
            'training_history': self.training_history
        }
        
        model_path = os.path.join(self.model_dir, 'incremental_model.pkl')
        with open(model_path, 'wb') as f:
            pickle.dump(model_data, f)
        print(f"Model saved to {model_path}")
    
    def load_model(self):
        """Load the trained model and scalers from disk"""
        model_path = os.path.join(self.model_dir, 'incremental_model.pkl')
        
        if not os.path.exists(model_path):
            print("No existing model found. Will train from scratch.")
            return False
        
        try:
            with open(model_path, 'rb') as f:
                model_data = pickle.load(f)
            
            self.models = model_data['models']
            self.scalers = model_data['scalers']
            self.lookback_days = model_data['lookback_days']
            self.feature_columns = model_data['feature_columns']
            self.target_columns = model_data['target_columns']
            self.training_history = model_data.get('training_history', [])
            
            print(f"Model loaded from {model_path}")
            print(f"Previous training sessions: {len(self.training_history)}")
            return True
        except Exception as e:
            print(f"Error loading model: {e}")
            return False
    
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
    
    def train_or_update_model(self, df, is_initial_training=False):
        """
        Train new model or update existing model with new data
        
        Args:
            df (pd.DataFrame): Stock data (can be new data or all data)
            is_initial_training (bool): Whether this is initial training or incremental update
        """
        print(f"{'Initial training' if is_initial_training else 'Incremental update'} starting...")
        
        # Preprocess data
        processed_df = self.preprocess_data(df)
        print(f"Processing {len(processed_df)} data points")
        
        # Extract features for scaling
        feature_data = processed_df[self.feature_columns].values
        
        # Track training metrics
        training_metrics = {
            'timestamp': datetime.now(),
            'data_points': len(processed_df),
            'is_initial': is_initial_training,
            'metrics': {}
        }
        
        for i, target_col in enumerate(self.target_columns):
            print(f"Processing {target_col}...")
            
            if is_initial_training or target_col not in self.scalers:
                # Initial training - create new scaler
                scaler = MinMaxScaler()
                scaled_data = scaler.fit_transform(feature_data)
                self.scalers[target_col] = scaler
            else:
                # Incremental update - use existing scaler
                scaler = self.scalers[target_col]
                scaled_data = scaler.transform(feature_data)
            
            # Create sequences for this target
            X, y = self.create_sequences(scaled_data, target_col)
            
            if len(X) < 5:  # Need at least some data
                print(f"Warning: Not enough data for {target_col}. Skipping...")
                continue
            
            if is_initial_training or target_col not in self.models:
                # Initial training - create new model
                model = RandomForestRegressor(
                    n_estimators=100,
                    max_depth=10,
                    random_state=42,
                    n_jobs=-1,
                    warm_start=False  # For initial training
                )
                model.fit(X, y)
                self.models[target_col] = model
            else:
                # Incremental update - retrain with new data
                # Note: RandomForest doesn't support true incremental learning,
                # so we create a new model with more trees and combine predictions
                existing_model = self.models[target_col]
                
                # Create new model with additional trees
                new_model = RandomForestRegressor(
                    n_estimators=50,  # Fewer trees for the update
                    max_depth=10,
                    random_state=42,
                    n_jobs=-1
                )
                new_model.fit(X, y)
                
                # For simplicity, replace the model (in production, you might ensemble them)
                self.models[target_col] = new_model
                print(f"Updated model for {target_col}")
            
            # Calculate and store training metrics
            y_pred = self.models[target_col].predict(X)
            mse = mean_squared_error(y, y_pred)
            mae = mean_absolute_error(y, y_pred)
            training_metrics['metrics'][target_col] = {'mse': mse, 'mae': mae}
            print(f"{target_col} - MSE: {mse:.6f}, MAE: {mae:.6f}")
        
        # Save training history
        self.training_history.append(training_metrics)
        
        # Save updated model
        self.save_model()
        
        print(f"{'Training' if is_initial_training else 'Update'} completed!")
    
    def predict_future(self, df, days=5):
        """
        Predict future stock prices using the trained model
        
        Args:
            df (pd.DataFrame): Historical data
            days (int): Number of days to predict
            
        Returns:
            pd.DataFrame: Predictions for future days
        """
        print(f"Predicting future {days} days...")
        
        if not self.models:
            raise ValueError("No trained models found. Please train the model first.")
        
        # Preprocess data
        processed_df = self.preprocess_data(df)
        
        # Get the last sequence for prediction
        feature_data = processed_df[self.feature_columns].values
        
        predictions = []
        
        for day in range(days):
            day_predictions = {}
            
            for target_col in self.target_columns:
                if target_col not in self.models:
                    print(f"No model found for {target_col}, using zero...")
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
    
    def add_new_data(self, new_data_path):
        """
        Add new data and update the model incrementally
        
        Args:
            new_data_path (str): Path to new data CSV file
        """
        try:
            new_df = pd.read_csv(new_data_path, encoding='utf-8-sig')
            print(f"Loading new data with {len(new_df)} records")
            self.train_or_update_model(new_df, is_initial_training=False)
        except Exception as e:
            print(f"Error adding new data: {e}")

def main():
    """Main function to run the incremental learning solution"""
    print("=== Solution 2: Incremental Learning Model ===")
    
    # Initialize model
    model = IncrementalStockPredictionModel(lookback_days=10)
    
    # Try to load existing model
    model_exists = model.load_model()
    
    # Load data
    data_path = "../沪深300指数历史数据.csv"
    try:
        df = pd.read_csv(data_path, encoding='utf-8-sig')
        print(f"Loaded data with {len(df)} records")
    except Exception as e:
        print(f"Error loading data: {e}")
        return
    
    if not model_exists:
        # Initial training with all historical data
        print("\nPerforming initial training...")
        model.train_or_update_model(df, is_initial_training=True)
    else:
        # Model exists - this could be used to add new daily data
        print("\nModel already exists. You can:")
        print("1. Use existing model for prediction")
        print("2. Update with new data (simulate by retraining with recent data)")
        
        # For demonstration, we'll update with the most recent data
        recent_data = df.tail(5)  # Use last 5 records as "new" data
        print(f"\nSimulating incremental update with {len(recent_data)} recent records...")
        model.train_or_update_model(recent_data, is_initial_training=False)
    
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
    
    # Display training history
    if model.training_history:
        print(f"\n=== Training History ({len(model.training_history)} sessions) ===")
        for i, session in enumerate(model.training_history):
            print(f"Session {i+1}: {session['timestamp'].strftime('%Y-%m-%d %H:%M:%S')}")
            print(f"  Data points: {session['data_points']}")
            print(f"  Type: {'Initial' if session['is_initial'] else 'Incremental'}")

if __name__ == "__main__":
    main()