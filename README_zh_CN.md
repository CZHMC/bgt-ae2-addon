# Building Gadgets 2 AE2 Addon

[English documentation](README.md)

一个为 Building Gadgets 2 接入 Applied Energistics 2 自动合成流程的附属模组。当建筑队列缺少材料时，本模组会调用 AE2 原生的数量窗口和合成计划窗口，请求网络中存在合成样板的材料。

> 需要 Minecraft 1.21.1、NeoForge、Building Gadgets 2 和 Applied Energistics 2。

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
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Applied Energistics 2 | 19.2.17 或更高，但低于 20 |
| Building Gadgets 2 | 1.3.9 或更高 |
| GuideME | AE2 开发运行环境需要 21.1.1 或更高版本 |

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

## 不提供公开 API

本项目是一个实现型附属模组，不是 API 库。本模组不向其他模组提供稳定的公开 API。`com.czhmc.bgt_ae2_addon` 下的类和包均属于内部实现细节，未来版本可能变更，不提供兼容性保证。

## 开发

```text
./gradlew test
./gradlew build
```

开发运行还需要 `build.gradle` 中声明的依赖，其中包括 AE2 运行时依赖链需要的 GuideME。

## 问题反馈

提交 Issue 时请附带：

- Minecraft、NeoForge、AE2 和 BGT 版本；
- 操作类型（`BUILD` 或 `EXCHANGE`）；
- 简洁的复现步骤；
- 相关 `latest.log` 片段，并删除个人信息。

目前暂不配置 Modrinth 和 CurseForge 发布。

## 许可证

本仓库中的附属模组源代码使用 [MIT License](LICENSE) 授权。

第三方依赖继续使用各自的许可证。许可证和署名边界见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 致谢

- [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
- [Building Gadgets 2](https://github.com/Direwolf20-MC/BuildingGadgets2)
- [GuideME](https://github.com/AppliedEnergistics/GuideME)
- [NeoForge](https://github.com/neoforged/NeoForge)
