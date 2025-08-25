# Solution 2: Incremental Learning Model

## 概述 (Overview)
这个解决方案保存训练好的模型，并支持增量学习来更新模型，适合日常数据更新场景。

This solution saves trained models and supports incremental learning to update models, suitable for daily data update scenarios.

## 特点 (Features)
- 模型持久化存储，避免重复训练
- 支持增量学习，可以用新数据更新现有模型
- 维护训练历史记录
- 快速预测，无需重新训练
- 预测未来5天的：收盘价、开盘价、最高价、最低价、交易量

## 使用方法 (Usage)

### 1. 安装依赖
```bash
pip install pandas numpy scikit-learn joblib
```

### 2. 首次运行（初始训练）
```bash
cd solution2
python train_and_predict_incremental.py
```

### 3. 后续运行（增量更新）
当有新数据时，再次运行同样的命令：
```bash
python train_and_predict_incremental.py
```

程序会自动检测是否存在已训练的模型：
- 如果不存在：进行初始训练
- 如果存在：进行增量更新

### 4. 输出文件
- `predict.csv`: 包含未来5天预测结果的CSV文件
- `models/incremental_model.pkl`: 保存的模型文件

## 文件结构
```
solution2/
├── train_and_predict_incremental.py    # 主程序文件
├── models/                            # 模型存储目录
│   └── incremental_model.pkl          # 保存的模型和参数
├── predict.csv                       # 预测结果输出文件
└── README.md                         # 说明文档
```

## 模型参数
- **lookback_days**: 10 (使用过去10天的数据作为特征)
- **模型类型**: RandomForestRegressor
- **初始训练**: 100棵树，最大深度10
- **增量更新**: 50棵树，最大深度10

## 增量学习机制

### 初始训练
- 使用所有历史数据训练模型
- 保存模型、缩放器和训练历史

### 增量更新
- 加载已存在的模型和缩放器
- 使用新数据训练额外的模型
- 更新模型参数（目前采用替换策略）
- 保存更新后的模型

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
- 训练效率高，支持增量更新
- 模型持久化，避免重复训练
- 适合生产环境的日常更新
- 维护训练历史，便于模型管理

### 缺点  
- 随机森林本身不支持真正的在线学习
- 需要额外的存储空间保存模型
- 模型文件可能随时间增大

## 使用场景
1. **日常更新**: 每天新增一条数据后更新模型
2. **批量更新**: 定期用一批新数据更新模型
3. **生产部署**: 在生产环境中维护和更新模型

## 注意事项
- 确保有足够磁盘空间存储模型文件
- 首次运行需要较长时间进行初始训练
- 模型文件包含完整的训练状态，请妥善保管
- 可以手动删除 `models/` 目录来重新初始训练

## 每日增量更新使用方法 / Daily Incremental Update Usage

### 方法1：交互式更新 / Method 1: Interactive Update
使用交互式脚本添加新数据：
```bash
python3 daily_update.py
```
选择数据来源：
1. 模拟数据（演示用）
2. 从CSV文件加载
3. 从API获取（需要实现）

### 方法2：自动化更新 / Method 2: Automated Update

#### 准备新数据文件 / Prepare New Data File
创建包含新日数据的CSV文件，格式如下：
```csv
"日期","收盘","开盘","高","低","交易量","涨跌幅"
"2025-8-23","4,385.20","4,378.00","4,395.45","4,375.10","275.32K","0.16%"
```

#### 运行自动化更新 / Run Automated Update
```bash
# 使用默认配置
python3 automated_daily_update.py

# 指定新数据文件
python3 automated_daily_update.py --data-source file --new-data-file today_data.csv
```

### 方法3：生产环境自动化 / Method 3: Production Automation

#### 设置定时任务 / Setup Scheduled Task

**Linux/Mac (使用 cron):**
```bash
# 编辑 crontab
crontab -e

# 添加每日运行任务（每天早上9点）
0 9 * * * cd /path/to/solution2 && python3 automated_daily_update.py >> daily_update.log 2>&1
```

**Windows (使用任务计划程序):**
1. 打开"任务计划程序"
2. 创建基本任务
3. 设置每日触发器
4. 设置操作为运行 `python3 automated_daily_update.py`

#### API数据源集成 / API Data Source Integration
修改 `automated_daily_update.py` 中的 `get_new_data_from_api()` 函数：
```python
def get_new_data_from_api():
    # 示例：使用 Alpha Vantage API
    import requests
    
    api_key = "YOUR_API_KEY"
    url = f"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=000300.SZ&apikey={api_key}"
    
    response = requests.get(url)
    data = response.json()
    
    # 解析并转换为所需格式
    # ... 实现数据转换逻辑 ...
    
    return new_df
```

## 文件说明 / File Descriptions

### 新增文件 / New Files
- `daily_update.py`: 交互式日更新工具
- `automated_daily_update.py`: 自动化日更新脚本
- `new_daily_data_example.csv`: 新数据格式示例
- `daily_update.log`: 更新日志文件（自动生成）

### 工作流程 / Workflow

1. **数据准备**: 获取新的日数据（CSV文件或API）
2. **模型加载**: 加载已保存的模型和参数
3. **增量训练**: 用新数据更新模型
4. **预测生成**: 生成新的5天预测
5. **结果保存**: 保存更新后的预测结果
6. **日志记录**: 记录更新过程和结果

## 数据源集成示例 / Data Source Integration Examples

### Yahoo Finance
```python
import yfinance as yf

def get_data_from_yahoo(symbol="000300.SS", period="1d"):
    ticker = yf.Ticker(symbol)
    data = ticker.history(period=period)
    # 转换为所需格式
    return formatted_data
```

### Alpha Vantage
```python
import requests

def get_data_from_alphavantage(symbol, api_key):
    url = f"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol={symbol}&apikey={api_key}"
    response = requests.get(url)
    return process_alphavantage_data(response.json())
```

## 监控和维护 / Monitoring and Maintenance

### 日志监控 / Log Monitoring
检查 `daily_update.log` 文件以监控更新状态：
```bash
# 查看最新日志
tail -f daily_update.log

# 查看错误日志
grep ERROR daily_update.log
```

### 模型性能监控 / Model Performance Monitoring
程序会记录每次更新的性能指标，可以通过训练历史追踪模型表现：
```python
# 查看训练历史
model = IncrementalStockPredictionModel()
model.load_model()
for session in model.training_history:
    print(f"Date: {session['timestamp']}, Data points: {session['data_points']}")
```

## 高级用法 / Advanced Usage

### 手动添加新数据
可以通过程序接口添加新数据：
```python
model = IncrementalStockPredictionModel()
model.load_model()
model.add_new_data("new_data.csv")
```

### 批量更新
一次性添加多天数据：
```python
# 准备包含多天数据的CSV文件
multi_day_data = pd.read_csv("weekly_data.csv")
model.train_or_update_model(multi_day_data, is_initial_training=False)
```

### 查看训练历史
程序会显示所有训练会话的历史信息，包括时间戳、数据点数量和性能指标。