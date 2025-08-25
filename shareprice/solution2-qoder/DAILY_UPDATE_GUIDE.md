# Solution 2 Daily Update Guide
# 方案2 每日增量更新指南

## 概览 / Overview

方案2的核心优势是支持增量学习，这意味着你可以每天添加新的市场数据来更新模型，而无需重新训练整个模型。本指南详细介绍了如何实现日常数据更新。

Solution 2's core advantage is supporting incremental learning, which means you can add new market data daily to update the model without retraining the entire model. This guide details how to implement daily data updates.

## 🚀 快速开始 / Quick Start

### 第一次使用 / First Time Setup
```bash
# 1. 初始训练（使用历史数据）
python3 train_and_predict_incremental.py

# 2. 第二天开始，使用新数据更新
python3 automated_daily_update.py --data-source file --new-data-file today_data.csv
```

### 每日更新流程 / Daily Update Process
1. **准备新数据** - 创建包含当天数据的CSV文件
2. **运行更新** - 执行自动化更新脚本
3. **获取预测** - 自动生成新的5天预测
4. **检查日志** - 确认更新成功

## 📁 数据准备 / Data Preparation

### 新数据文件格式 / New Data File Format
创建CSV文件，包含当天的市场数据：

```csv
"日期","收盘","开盘","高","低","交易量","涨跌幅"
"2025-8-23","4,385.20","4,378.00","4,395.45","4,375.10","275.32K","0.16%"
```

### 数据格式要求 / Data Format Requirements
- **日期**: YYYY-M-D 格式 (如: 2025-8-23)
- **价格**: 支持逗号分隔 (如: 4,385.20)
- **交易量**: 支持K单位 (如: 275.32K)
- **涨跌幅**: 百分比格式 (如: 0.16%)

### 批量数据 / Batch Data
也可以一次添加多天数据：
```csv
"日期","收盘","开盘","高","低","交易量","涨跌幅"
"2025-8-23","4,385.20","4,378.00","4,395.45","4,375.10","275.32K","0.16%"
"2025-8-24","4,390.50","4,385.20","4,400.30","4,380.15","280.15K","0.12%"
```

## 🔧 三种更新方式 / Three Update Methods

### 方式1: 自动化脚本（推荐）/ Method 1: Automated Script (Recommended)

**适用场景**: 生产环境、定时任务
**优点**: 完全自动化、有日志记录、错误处理

```bash
# 使用默认配置
python3 automated_daily_update.py

# 指定数据文件
python3 automated_daily_update.py --data-source file --new-data-file today_data.csv

# 查看帮助
python3 automated_daily_update.py --help
```

**特点**:
- ✅ 自动加载已保存的模型
- ✅ 支持多种数据源（文件、API）
- ✅ 自动更新历史数据文件
- ✅ 生成新的预测结果
- ✅ 详细的日志记录
- ✅ 错误处理和恢复

### 方式2: 交互式脚本 / Method 2: Interactive Script

**适用场景**: 手动操作、测试环境
**优点**: 灵活控制、实时反馈

```bash
python3 daily_update.py
```

选择数据源：
1. 模拟数据（演示用）
2. 从CSV文件加载
3. 从API获取

### 方式3: 程序接口 / Method 3: Programmatic Interface

**适用场景**: 集成到其他系统
**优点**: 最大灵活性、可定制

```python
from train_and_predict_incremental import IncrementalStockPredictionModel

# 初始化模型
model = IncrementalStockPredictionModel()
model.load_model()

# 添加新数据
model.add_new_data("new_daily_data.csv")

# 获取预测
predictions = model.predict_future(historical_data, days=5)
```

## 📊 实际使用示例 / Real Usage Examples

### 示例1: 每日定时更新 / Example 1: Daily Scheduled Update

创建定时任务脚本：
```bash
#!/bin/bash
# daily_stock_update.sh

cd /path/to/solution2

# 获取今天的数据（这里需要你的数据获取逻辑）
# fetch_today_data.py > today_data.csv

# 更新模型
python3 automated_daily_update.py --data-source file --new-data-file today_data.csv

# 检查结果
if [ $? -eq 0 ]; then
    echo "Daily update successful at $(date)"
    # 可以发送邮件通知或上传结果
else
    echo "Daily update failed at $(date)"
    # 错误处理
fi
```

设置cron定时任务：
```bash
# 每天早上9点运行
0 9 * * * /path/to/daily_stock_update.sh >> /var/log/stock_update.log 2>&1
```

### 示例2: API数据集成 / Example 2: API Data Integration

修改 `automated_daily_update.py` 中的API函数：

```python
def get_new_data_from_api():
    import requests
    import json
    
    # 示例：使用tushare获取数据
    try:
        # 替换为你的API调用
        api_url = "https://api.your-data-provider.com/daily"
        headers = {"Authorization": "Bearer YOUR_API_KEY"}
        
        response = requests.get(api_url, headers=headers)
        data = response.json()
        
        # 转换为所需格式
        formatted_data = []
        for item in data['results']:
            formatted_data.append({
                '日期': item['date'],
                '收盘': str(item['close']),
                '开盘': str(item['open']),
                '高': str(item['high']),
                '低': str(item['low']),
                '交易量': f"{item['volume']/1000:.2f}K",
                '涨跌幅': f"{item['change_percent']:.2f}%"
            })
        
        return pd.DataFrame(formatted_data)
        
    except Exception as e:
        logging.error(f"API data fetch failed: {e}")
        return None
```

### 示例3: 生产环境服务 / Example 3: Production Service

创建Web服务接口：
```python
from flask import Flask, request, jsonify
from train_and_predict_incremental import IncrementalStockPredictionModel

app = Flask(__name__)
model = IncrementalStockPredictionModel()
model.load_model()

@app.route('/add_data', methods=['POST'])
def add_daily_data():
    """添加新的日数据"""
    try:
        data = request.json
        # 验证数据格式
        required_fields = ['date', 'open', 'high', 'low', 'close', 'volume']
        if not all(field in data for field in required_fields):
            return jsonify({'error': 'Missing required fields'}), 400
        
        # 转换为DataFrame
        new_data = {
            '日期': [data['date']],
            '收盘': [str(data['close'])],
            '开盘': [str(data['open'])],
            '高': [str(data['high'])],
            '低': [str(data['low'])],
            '交易量': [f"{data['volume']/1000:.2f}K"],
            '涨跌幅': [f"{data.get('change_percent', 0):.2f}%"]
        }
        new_df = pd.DataFrame(new_data)
        
        # 更新模型
        model.train_or_update_model(new_df, is_initial_training=False)
        
        return jsonify({'message': 'Data added successfully'}), 200
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/predict', methods=['GET'])
def get_predictions():
    """获取预测结果"""
    try:
        days = request.args.get('days', 5, type=int)
        
        # 加载历史数据并预测
        df = pd.read_csv("../沪深300指数历史数据.csv", encoding='utf-8-sig')
        predictions = model.predict_future(df, days=days)
        
        # 转换为JSON格式
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
        
        return jsonify({'predictions': result}), 200
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

## 📈 监控和维护 / Monitoring and Maintenance

### 日志监控 / Log Monitoring

查看更新日志：
```bash
# 查看所有日志
cat daily_update.log

# 查看最新日志
tail -f daily_update.log

# 查看错误日志
grep ERROR daily_update.log

# 查看今天的日志
grep "$(date '+%Y-%m-%d')" daily_update.log
```

### 性能监控 / Performance Monitoring

检查模型性能：
```python
# 查看训练历史
model = IncrementalStockPredictionModel()
model.load_model()

print(f"训练会话数: {len(model.training_history)}")
for i, session in enumerate(model.training_history[-5:]):  # 最近5次
    print(f"Session {i}: {session['timestamp']} - {session['data_points']} points")
```

### 模型备份 / Model Backup

定期备份模型：
```bash
#!/bin/bash
# backup_model.sh

DATE=$(date +%Y%m%d)
BACKUP_DIR="/backup/stock_models"

mkdir -p $BACKUP_DIR
cp -r models/ "$BACKUP_DIR/models_$DATE"

# 保留最近30天的备份
find $BACKUP_DIR -name "models_*" -mtime +30 -exec rm -rf {} \;
```

## ⚠️ 注意事项 / Important Notes

### 数据质量 / Data Quality
- 确保新数据的准确性和完整性
- 检查数据格式是否符合要求
- 处理缺失值和异常值

### 模型维护 / Model Maintenance
- 定期检查模型性能
- 监控预测准确性
- 适时重新初始化训练

### 错误处理 / Error Handling
- 设置自动重试机制
- 备份重要数据和模型
- 建立告警机制

### 资源管理 / Resource Management
- 监控磁盘空间使用
- 清理旧的日志文件
- 优化模型文件大小

## 🔍 故障排除 / Troubleshooting

### 常见问题 / Common Issues

**问题1**: "Warning: Not enough data for training"
**解决**: 确保新数据至少有足够的记录进行增量训练

**问题2**: 模型文件损坏
**解决**: 从备份恢复或重新初始化训练

**问题3**: 内存不足
**解决**: 调整模型参数或增加系统内存

**问题4**: 数据格式错误
**解决**: 检查CSV文件格式和编码

### 调试模式 / Debug Mode

启用详细日志：
```python
import logging
logging.basicConfig(level=logging.DEBUG)
```

检查模型状态：
```python
model = IncrementalStockPredictionModel()
model.load_model()
print(f"Models loaded: {list(model.models.keys())}")
print(f"Scalers loaded: {list(model.scalers.keys())}")
```

## 📞 技术支持 / Technical Support

如果遇到问题，请检查：
1. 数据格式是否正确
2. 模型文件是否存在
3. 磁盘空间是否充足
4. 日志文件中的错误信息

For issues, please check:
1. Data format correctness
2. Model file existence
3. Sufficient disk space
4. Error messages in log files

---

**提示**: 这个增量学习系统是为了演示目的而设计的。在生产环境中，建议添加更多的错误处理、性能监控和安全措施。

**Note**: This incremental learning system is designed for demonstration purposes. In production environments, it's recommended to add more error handling, performance monitoring, and security measures.