# Solution 1: Complete Retraining Model

## 概述 (Overview)
这个解决方案每次都使用所有历史数据重新训练模型来预测未来5天的股价。

This solution retrains the model from scratch using all historical data every time to predict future 5 days stock prices.

## 特点 (Features)
- 每次训练都使用完整的历史数据集
- 预测未来5天的：收盘价、开盘价、最高价、最低价、交易量
- 使用随机森林回归模型
- 支持时间序列特征工程

## 使用方法 (Usage)

### 1. 安装依赖
```bash
pip install pandas numpy scikit-learn joblib
```

### 2. 运行模型
```bash
cd solution1
python train_and_predict.py
```

### 3. 输出文件
- `predict.csv`: 包含未来5天预测结果的CSV文件

## 文件结构
```
solution1/
├── train_and_predict.py    # 主程序文件
├── predict.csv            # 预测结果输出文件
└── README.md             # 说明文档
```

## 模型参数
- **lookback_days**: 10 (使用过去10天的数据作为特征)
- **模型类型**: RandomForestRegressor
- **树的数量**: 100
- **最大深度**: 10

## 数据格式
输入CSV文件应包含以下列：
- 日期: 日期格式
- 收盘: 收盘价
- 开盘: 开盘价  
- 高: 最高价
- 低: 最低价
- 交易量: 交易量 (支持K格式，如 "290.44K")

## 优缺点

### 优点
- 每次都用最新最全的数据训练
- 模型性能较为稳定
- 不需要维护模型状态

### 缺点  
- 每次训练耗时较长
- 计算资源消耗大
- 不适合频繁更新的场景

## 注意事项
- 确保CSV文件路径正确 (`../沪深300指数历史数据.csv`)
- 数据格式需要符合要求
- 至少需要10天以上的历史数据进行训练