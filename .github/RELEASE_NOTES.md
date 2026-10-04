# Leader Lite B2.1

Via 层升级与适配：

- Via 栈升级至 viaversion / viabackwards **5.12.0**（新增 MC 26.3 客户端支持）+ viarewind **4.2.0**
- 适配 viarewind 4.2.0 的 PlayerPositionTracker 重写（传送确认队列化），Grim 提前应答逻辑保持一致
- 新增 26.1 / 26.2 / 26.3 共 **153 个新物品**的模型与纹理（白杨木系列、硫磺/朱砂方块、彩色台阶楼梯、软垫、地图等），现代物品模型注册数 555 → **708**
- 副手 F 键交换：修复改键不持久的问题，并在原版按键设置中显示可翻译的 "Swap Offhand / 切换副手" 条目
- 包结构适配：cn.unfair.util.via → leader.util.via
- 新增 GitHub Actions：push 自动构建、tag 自动发布 Release
