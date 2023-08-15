import matplotlib.pyplot as plt

# 衡量标准的名称
metrics = ['Run time', 'Total energy consumed', 'Total latency']

algorithm1_data = [1049, 28472, 2751]
algorithm2_data = [110, 220160, 3567]

# 创建一个2x2的子图布局
fig, axes = plt.subplots(nrows=1, ncols=3, figsize=(15, 8))

# 循环遍历每个子图并绘制数据
for i, ax in enumerate(axes.flat):
    bars = ax.bar(['ACO', 'Edge-ward'], [algorithm1_data[i], algorithm2_data[i]])
    ax.set_title(metrics[i])
    ax.set_ylabel('Value')

    # 在每个柱状图上添加具体数值标签
    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2.0, yval, round(yval, 2), va='bottom')

# 调整子图之间的间距
plt.tight_layout()

# 显示图表
plt.show()
