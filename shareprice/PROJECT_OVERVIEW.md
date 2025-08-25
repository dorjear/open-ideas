# 股票预测模型项目 / Stock Prediction Model Project

## 项目概述 / Project Overview

本项目实现了两种股票价格预测模型，用于预测沪深300指数未来5天的价格走势，包括收盘价、开盘价、最高价、最低价和交易量。

This project implements two stock price prediction models to forecast the next 5 days of CSI 300 index prices, including closing price, opening price, high, low, and trading volume.

## 目录结构 / Directory Structure

```
shareprice/
├── 沪深300指数历史数据.csv           # 原始历史数据
├── solution1/                      # 方案1：完全重训练模型
│   ├── train_and_predict.py       # 主程序
│   ├── predict.csv                # 预测结果
│   └── README.md                  # 使用说明
├── solution2/                      # 方案2：增量学习模型
│   ├── train_and_predict_incremental.py  # 主程序
│   ├── models/                    # 模型存储目录
│   │   └── incremental_model.pkl  # 保存的模型文件
│   ├── predict.csv                # 预测结果
│   └── README.md                  # 使用说明
└── PROJECT_OVERVIEW.md            # 项目总览（本文件）
```

## 两种方案对比 / Comparison of Two Solutions

| 特性 / Feature | 方案1 / Solution 1 | 方案2 / Solution 2 |
|----------------|-------------------|-------------------|
| **训练方式** | 每次使用全部历史数据重新训练 | 增量学习，保存并更新模型 |
| **Training Method** | Complete retraining with all data | Incremental learning with saved models |
| **计算效率** | 低（每次都要完整训练） | 高（只需更新模型） |
| **Computational Efficiency** | Low (full training each time) | High (only model updates) |
| **存储需求** | 低（无需保存模型） | 中等（需要保存模型文件） |
| **Storage Requirements** | Low (no model saving) | Medium (model files needed) |
| **适用场景** | 离线批量分析 | 生产环境日常更新 |
| **Use Cases** | Offline batch analysis | Production daily updates |
| **模型一致性** | 高（每次完全重训练） | 中等（增量更新可能漂移） |
| **Model Consistency** | High (complete retraining) | Medium (incremental drift possible) |

## 技术实现 / Technical Implementation

### 数据预处理 / Data Preprocessing
- 日期格式转换和排序
- 数值清理（去除逗号，处理K单位）
- 特征缩放（MinMaxScaler）

### 模型架构 / Model Architecture
- **算法**: 随机森林回归 (Random Forest Regressor)
- **特征窗口**: 过去10天的数据
- **预测目标**: 未来5天的5个指标
- **模型数量**: 每个目标指标独立训练一个模型

### 特征工程 / Feature Engineering
- 时间序列滑动窗口
- 多目标回归（每个指标单独建模）
- 特征标准化

## 预测结果示例 / Prediction Results Example

最近一次预测结果 (Latest Prediction Results):

```
Date: 2025-08-23
  收盘: 4327.47
  开盘: 4268.20
  高: 4328.98
  低: 4261.62
  交易量: 263148

Date: 2025-08-24
  收盘: 4324.65
  开盘: 4269.76
  高: 4330.44
  低: 4262.55
  交易量: 251038

... (继续4天预测)
```

## 性能指标 / Performance Metrics

训练完成后的模型性能 (Model Performance After Training):

| 指标 / Metric | MSE | MAE |
|--------------|-----|-----|
| 收盘 / Close | 0.002911 | 0.036093 |
| 开盘 / Open | 0.001884 | 0.038053 |
| 高 / High | 0.002191 | 0.031677 |
| 低 / Low | 0.002614 | 0.042980 |
| 交易量 / Volume | 0.004596 | 0.057800 |

## 快速开始 / Quick Start

### 安装依赖 / Install Dependencies
```bash
pip install pandas numpy scikit-learn joblib
```

### 运行方案1 / Run Solution 1
```bash
cd shareprice/solution1
python3 train_and_predict.py
```

### 运行方案2 / Run Solution 2
```bash
cd shareprice/solution2
python3 train_and_predict_incremental.py
```

## 输出文件 / Output Files

两种方案都会生成 `predict.csv` 文件，包含：
- 日期：预测日期
- 收盘：预测收盘价
- 开盘：预测开盘价
- 高：预测最高价
- 低：预测最低价
- 交易量：预测交易量

Both solutions generate a `predict.csv` file containing:
- Date: Prediction date
- Close: Predicted closing price
- Open: Predicted opening price
- High: Predicted highest price
- Low: Predicted lowest price
- Volume: Predicted trading volume

## 数据格式要求 / Data Format Requirements

输入CSV文件需包含以下列 / Input CSV file should contain:
- 日期 (Date)
- 收盘 (Close) 
- 开盘 (Open)
- 高 (High)
- 低 (Low)
- 交易量 (Volume) - 支持K格式 (K format supported)

## 注意事项 / Important Notes

1. **数据质量**: 确保历史数据的完整性和准确性
2. **模型限制**: 预测仅基于历史价格模式，无法预测突发事件影响
3. **风险提示**: 本模型仅供学习和研究使用，不构成投资建议
4. **数据量**: 建议至少15天以上的历史数据进行训练

1. **Data Quality**: Ensure completeness and accuracy of historical data
2. **Model Limitations**: Predictions based only on historical patterns, cannot predict sudden events
3. **Risk Warning**: This model is for learning and research only, not investment advice
4. **Data Volume**: Recommend at least 15 days of historical data for training

## 扩展功能 / Extended Features

### 可能的改进方向 / Potential Improvements
- 添加技术指标特征（RSI、MACD等）
- 集成多种机器学习算法
- 实现在线学习算法
- 添加模型性能监控
- 支持多个股票指数

### 集成外部数据 / External Data Integration
- 宏观经济指标
- 新闻情感分析
- 市场波动率指数
- 其他相关金融指标

## 联系方式 / Contact
如有问题或建议，请通过项目仓库提交issue。
For questions or suggestions, please submit issues through the project repository.