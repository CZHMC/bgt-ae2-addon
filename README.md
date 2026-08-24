# Building Gadgets 2 AE2 Addon

独立的 NeoForge 1.21.1 附属模组，为 Building Gadgets 2 的放置流程接入 Applied Energistics 2 自动合成。

## 使用

1. 将 Building Gadgets 2 小帮手绑定到 AE2 Wireless Access Point（无线访问点）。绑定对象必须是在线且已供电的无线访问点。
2. 在 AE2 网络中准备能产出目标方块物品的可用合成样板和足够的合成 CPU/能源。
3. 使用小帮手正常建筑、交换或复制粘贴。缺少材料时，附属模组会先扫描当前 BGT 队列中所有尚未处理的位置，按材料类型汇总整个剩余建筑所需的缺料数量，然后打开 AE2 原生的合成数量窗口。数量默认是整条剩余队列的缺少数量；多种材料会逐项确认，确认完成后进入 AE2 原生合成计划队列。用户在原生计划窗口点击开始后，AE2 会按队列逐项提交合成任务；所有材料计划提交完成前，BGT 队列保持暂停，不会出现请求一个方块、放置一个方块后再重新请求的循环。关闭或取消窗口不会提交当前批次的合成任务。

## 支持范围

- 建筑小帮手（BUILD）
- 交换小帮手（EXCHANGE）
- 复制粘贴小帮手的普通粘贴/替换阶段（BGT 的 BUILD/EXCHANGE 队列）

以下流程保持 Building Gadgets 2 原行为，不会提交自动合成：

- 剪切粘贴小帮手（CUT）
- 破坏小帮手（DESTROY）
- 撤销破坏流程（UNDO_DESTROY）
- `needItems=false` 的队列、流体材料和创造模式玩家

## 版本与限制

- Minecraft 1.21.1
- NeoForge 21.1.248
- Applied Energistics 2 API 19.2.17 以上且小于 20
- Building Gadgets 2 1.3.9

这是对 BGT 1.3.9 `ServerTickHandler` 的 Mixin，目标是 `build` 和 `exchange` 方法，并在 `statePosList.remove(0)` 前检查材料。因此未来 BGT 版本如果改变这些方法签名、队列字段或处理顺序，需要重新验证并可能更新本附属模组。

附属模组复用 AE2 原生 `CraftAmountMenu`、`CraftConfirmMenu`、`CraftAmountScreen` 和 `CraftConfirmScreen`，不自定义合成计划界面。数量确认后由 AE2 原生菜单队列执行计划计算，用户点击每个原生计划窗口的“开始”后才提交任务；多种材料按顺序逐项提交，避免多个计划同时使用同一份网络库存快照。整批计划提交成功且材料回到网络后，Building Gadgets 2 才负责最终的 MODULATE 材料提取。

开发运行还需要 AE2 要求的 GuideME 21.1.1 或更高版本；附属工程已将其加入 `runtimeOnly` 依赖。
