import pandas as pd
import numpy as np
import os
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout
from tensorflow.keras.callbacks import EarlyStopping

# 读取数据
data_path = os.path.join("..", "shareprice", "沪深300指数历史数据-full.csv")
df = pd.read_csv(data_path)

# 确保日期排序
df = df.sort_values(by="日期")

# 选择需要的列，并去掉千位分隔符，转换为浮点数
features = ["开盘", "收盘", "高", "低", "交易量"]
def parse_numeric(val):
    val = str(val).replace(",", "").strip()
    if val.endswith("K"):
        return float(val[:-1]) * 1_000
    elif val.endswith("M"):
        return float(val[:-1]) * 1_000_000
    elif val.endswith("B"):
        return float(val[:-1]) * 1_000_000_000
    else:
        return float(val)

for col in features:
    df[col] = df[col].apply(parse_numeric)
data = df[features].values

# 数据归一化
scaler = MinMaxScaler()
scaled_data = scaler.fit_transform(data)

# 创建时间序列数据
def create_dataset(dataset, look_back=30):
    X, y = [], []
    for i in range(len(dataset) - look_back):
        X.append(dataset[i:i+look_back])
        y.append(dataset[i+look_back])
    return np.array(X), np.array(y)

look_back = 30
X, y = create_dataset(scaled_data, look_back)

# 确保 X 是三维数组 (samples, timesteps, features)
if len(X.shape) != 3:
    X = np.array(X).reshape((X.shape[0], look_back, len(features)))

# 确保 y 是二维数组 (samples, features)
if len(y.shape) != 2:
    y = np.array(y).reshape((y.shape[0], len(features)))

# 划分训练集和测试集
train_size = int(len(X) * 0.9)

# 如果数据量太小，直接用全部数据训练，不做验证集
if train_size < 1:
    X_train, y_train = X, y
    X_test, y_test = None, None
else:
    X_train, X_test = X[:train_size], X[train_size:]
    y_train, y_test = y[:train_size], y[train_size:]

# 构建 LSTM 模型
model = Sequential()
model.add(LSTM(64, return_sequences=True, input_shape=(look_back, len(features))))
model.add(Dropout(0.2))
model.add(LSTM(64))
model.add(Dropout(0.2))
model.add(Dense(len(features)))
model.compile(optimizer="adam", loss="mse")

# 训练模型
es = EarlyStopping(monitor="val_loss", patience=10, restore_best_weights=True)

# 如果样本数小于批量大小，调整 batch_size 避免 math domain error
batch_size = min(32, len(X_train))

if X_test is None or y_test is None or len(X_test) == 0:
    model.fit(X_train, y_train, epochs=100, batch_size=batch_size, callbacks=[es], verbose=1)
else:
    val_batch_size = min(batch_size, len(X_test))
    model.fit(X_train, y_train, validation_data=(X_test, y_test), epochs=100, batch_size=batch_size, callbacks=[es], verbose=1)

# 预测未来5天
last_sequence = scaled_data[-look_back:]
predictions = []
current_seq = last_sequence.copy()

for _ in range(5):
    pred = model.predict(current_seq.reshape(1, look_back, len(features)), verbose=0)
    predictions.append(pred[0])
    current_seq = np.vstack([current_seq[1:], pred])

# 反归一化
predictions = scaler.inverse_transform(predictions)

# 生成预测日期
last_date = pd.to_datetime(df["日期"].iloc[-1])
future_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=5, freq="B")  # 只取工作日

# 保存预测结果
pred_df = pd.DataFrame(predictions, columns=features)
pred_df.insert(0, "日期", future_dates.strftime("%Y-%m-%d"))
pred_df.to_csv("predict.csv", index=False, encoding="utf-8-sig")

print("预测完成，结果已保存到 predict.csv，并包含预测日期")