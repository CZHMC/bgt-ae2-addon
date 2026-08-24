# Building Gadgets 2 AE2 Addon

[English documentation](README.md)

一个为 Building Gadgets 2 接入 Applied Energistics 2 自动合成流程的附属模组。当建筑队列缺少材料时，本模组会调用 AE2 原生的数量窗口和合成计划窗口，请求网络中存在合成样板的材料。

> Building Gadgets 2 和 Applied Energistics 2。

## 功能

- 为 Building Gadgets 2 的 `BUILD` 和 `EXCHANGE` 队列接入 AE2 自动合成。
- 支持复制粘贴小帮手使用 BGT `BUILD`/`EXCHANGE` 队列的普通粘贴和替换粘贴阶段。
- 在请求材料前扫描整个剩余 BGT 队列。
- 按 AE2 物品键合并缺少数量，重复方块材料会合并为一个批次。
- 按 BGT 原本的顺序检查已有材料：AE2 存储网络、绑定容器、Curios、玩家背包。
- 使用 AE2 原生数量窗口和合成计划窗口，不增加自定义合成界面。
- 在原生合成批次提交完成且材料可供 BGT 提取前，保持 BGT 队列暂停。

本模组不会在后台静默提交合成任务。玩家需要在 AE2 原生数量窗口确认数量，在原生计划窗口查看计划，并点击 **开始** 后才会提交任务。

## 支持范围

支持：

- 建筑小帮手（`BUILD`）
- 交换小帮手（`EXCHANGE`）
- 复制粘贴小帮手使用 BGT `BUILD`/`EXCHANGE` 队列的普通粘贴和替换粘贴阶段

本模组不处理：

- 剪切粘贴小帮手（`CUT`）
- 破坏小帮手（`DESTROY`）
- 破坏撤销流程（`UNDO_DESTROY`）
- `needItems=false` 的队列
- 流体材料
- 创造模式玩家

不支持的队列会继续使用 Building Gadgets 2 原本的行为。

## 运行要求

| 组件 | 版本 |
| --- | --- |
| Applied Energistics 2 | 19.2.17 或更高 |
| Building Gadgets 2 | 1.3.9 或更高 |

本模组针对 BGT 1.3.9 的 `ServerTickHandler` 中 `build` 和 `exchange` 方法。如果未来 BGT 修改这些方法、队列字段或处理顺序，可能需要更新本模组。

## 工作流程

1. 将 Building Gadgets 2 小帮手绑定到在线且已供电的 AE2 Wireless Access Point。
2. 确保 AE2 网络中存在能够合成目标方块物品的样板。
3. 开始受支持的 BGT 建筑或交换操作。
4. 当剩余队列缺少材料时，本模组扫描所有剩余位置并汇总缺少数量。
5. 在 AE2 原生数量窗口确认数量。
6. 查看并开始 AE2 原生合成计划。
7. 整批任务提交且材料重新可用后，BGT 恢复原本的提取和放置流程。

AE2 的单个合成计划一次只能请求一种物品。缺少多种不同材料时，AE2 原生队列会按顺序展示并提交这些计划；在整个顺序完成前，本模组会保持 BGT 整批队列暂停。

## 开发

```text
./gradlew test
./gradlew build
```

开发运行还需要 `build.gradle` 中声明的依赖，其中包括 AE2 运行时依赖链需要的 GuideME。

## 问题反馈

如果你有bug问题、更好的优化或功能建议,欢迎提出issue和pr

## 许可证

本仓库使用 [MIT License](LICENSE) 授权。

## 致谢

- [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
- [Building Gadgets 2](https://github.com/Direwolf20-MC/BuildingGadgets2)
