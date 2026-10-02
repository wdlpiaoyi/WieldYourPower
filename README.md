# WieldYourPower / 力量掌控

Forge 1.20.1 模组。作者：wdlpiaoyi、deepseek。

## 设计意图

- **方便玩家更精确地操控角色**：把速度、跳跃、跨越、挖掘、属性等改造成可量化的限制项，玩家可以按自己的需要精细调节，而不是被模组或装备的数值牵着走。
- **避免拆家**：提供 OP 级控制工具（增强击杀、实体冻结、创造模式防护），并让玩家能主动收住自己的破坏行为，减少误伤建筑与存档。
- **方便整合包作者控制终局内容强度**：通过「作者庇护」标签与各项配置，让终局 Boss / OP 物品的强度可控，避免被一件超模装备瞬间抹平。

## 功能

### 自身限制（每玩家，随玩家数据同步）
- **动量上限（绝对值，格/刻）**：行走水平、飞行水平、飞行竖直；跳跃动量。`-1` 关闭。
  - 原版参考：行走 ≈ `0.216`，疾跑 ≈ `0.281`，创造飞行水平 ≈ `0.545`，跳跃 ≈ `0.42`。
- **跨越高度**：原版 `0.6` 的系数（`-1` 不限，`1` 原版，`0` 无法跨越）。
- **挖掘**：破坏方块所需最少刻数（`-1` 禁止挖掘）与两次破坏间的冷却刻数。
- **自定义 attribute 上限**：每玩家一组 `attribute:<属性id>:<上限>`，服务端每 tick 用临时修饰符钳制（不改底层数值）。
- **友军防护**：**按攻击者判定**（只查攻击方——玩家本人，或其宠物/召唤物/投射物的主人——自己的表），命中即拦。默认还覆盖主从关系：玩家打不了自家宠物/召唤物，同一主人的宠物/召唤物也互不伤害（`allyProtectsOwned` 可关）。owner 溯源走 `OwnableEntity`/驯服动物 + 投射物/药水云 + 召唤物关键字反射兜底；自残默认放行（`allyBlocksSelfHarm` 可开）。按 `tag:` / `type:` / `uuid:` 匹配（默认 `type:touhou_little_maid:maid`）。
- 打开面板：按键（默认未绑定，需自行设置）或 `/wyp panel`；使用 Cloth Config 界面。

### 击杀
- `/wyp kill`：增强击杀，无视无敌、图腾、事件取消、`discard`，对硬扛者强制移除，并清除残留血条；对玩家可强制重生，补刀绕过复活拦截。
- **永久移除**：对"移除后靠 `addFreshEntity` 自我复活"的怪物，会拒绝其重新加入世界，击杀真正落地（按实体实例判定，不受 UUID 改动影响）。
- `/wyp kill [目标] honor`：只走普通死亡流程的变体（不做强制移除）。
- **武德** `killHonor`：命中的目标只走一次普通死亡，交给其它模组接管（默认末影龙、凋灵、`tag:odamaneFinalDeath`）。
- **Boss 反清场兼容**（`bossDespawnCompat`，默认关闭）：关键字反射调用目标的清理逻辑，带字节码安全扫描，仅信任整合包时开启。

### 创造模式防护
- 大部分其它模组无法杀死创造/旁观玩家（本模组的 `/wyp kill` 可穿透）。
- 不显示死亡界面；拦截 `hurt` / `setHealth`（含负数，钳制到 `[1, 上限]`）/ 直接伤害 / 强制死亡 / 掉包 / 强制重生 / 无敌帧链路。
- 每 tick 维持血量与血上限、回满饱食、清除负面效果、原地复活。
- 可选**移除受击判定**（`creativeHitboxMode`，默认下蹲且着地时恢复）。

### 作者庇护（供整合包作者）
- 用**过滤列表**决定哪些**非玩家实体**获得保护，条目为逗号分隔的匹配项：`tag,<标签>` / `type,<实体id>` / `uuid,<uuid>`（id 内自带的 `:` 保留）。默认 `tag,authorsfavor`。
- 只拦**不走原版伤害链路**的代码：直接 `setHealth(X)`（`X` 低于当前血量）只让 `(当前血量 - X) × 系数` 这部分变化生效（`新血量 = floor(当前血量 - (当前血量 - X) × 系数)`）；**负数（含 `-Inf`）等价于 `setHealth(0)`**，同样按变化量算；直接 `die()` 也会被拦截回血。走 `hurt` / `actualHurt` 的伤害（普通武器、OP 武器、生物攻击）一律不动。
- 另外抗 `discard` / 强制移除 / 异常瞬移（位置与重力会还原），对绕过原版接口的"深度移除"也会把实体重新加回世界（不针对任何特定模组）；对生命上限削减按历史最大值软化；并持续显示同名（无实际效果）提示 buff。对"血量被 0"的兜底复活只在**原版 `DATA_HEALTH_ID` 也为 0/负**时生效——若某实体把 `getHealth()` 换成了自己的字段（原版字段仍为正），则交给它自己的死亡流程，不去复活。
- **所有系数默认「等效不生效」**，需整合包作者按需调整。
- **该加给谁**：只加给「**不希望被 OP 物品/代码抹除、且自身没有脚本化自删行为**」的实体。命中庇护的实体在**活着时"移除抗性"始终生效（与系数无关）**——既会拦住它自己的 `discard`/`remove`，也会在深移除后把它加回世界；因此带脚本阶段、会自行移除的 Boss/演出实体挂庇护会互相打架，这类实体请改在自身模组里做防御，不要挂庇护。

### 实体冻结
- `/wyp freeze <目标> [刻数] [半径]`：真正冻结实体（取消其 tick）。仅对非玩家生效；省略参数时使用配置默认值。

### 实体查看器
- 物品「实体查看器」（创造模式「操作员实用物品」标签页）：右键实体查看名称 / 类型 / UUID / 坐标 / 血量，并提供快捷指令按钮：击杀此实体、批量击杀该种类、附近击杀、添加 / 移除庇护标签。
- 自带射线检测（不依赖原版触及属性），支持多部件实体（如末影龙）与掉落物。

### 创造击破与方块破坏器
- **创造击破**：创造玩家左键即可破坏那些通常难以破坏的方块；本模组的挖掘自我限制仍然生效（`mineSpeed`/`mineInterval` 有设置时不介入）；与原版一致，手持不能破坏方块的物品（如剑）不会触发。配置项 `creativeBreaksProtectedBlocks`。
- **创造放置**：创造玩家可以放置被其他模组拦截的方块（放行被取消的放置事件）；并且可**忽略目标格被实体占用**（原版 `Level.isUnobstructed` 会拦生物/船等），即能把方块放进生物所在的格子。配置项 `creativePlacesBlocks` / `creativePlacesThroughEntities`。
- **抑制更新（放置/破坏）**：新增按键（默认空绑定，分类「力量掌控」，默认**仅创造模式**可用，生存玩家需先获授权）。激活时**你自己的**放置/破坏（含方块物品、流体桶/细雪桶，以及方块破坏器、创造强制击破）与**右键方块交互**（骨粉、打火石、斧/锄/铲、门/活板门、按钮/拉杆、蜡烛、营火、TNT、重生锚等）跳过邻居通知与邻居形状更新——**包括红石火把、红石线、中继器、比较器、拉杆这类在 `onPlace`/`onRemove` 里自己发通知的方块**（因此按住快捷键破坏红石火把不会再让活塞收回）——观察者、活塞、红石不响应；**幽匿振动**同样被吞掉：幽匿传感器 / 尖啸体 / 催化体 / 监守者只通过游戏事件感知世界，激活期间你自己的破坏/放置/右键交互产生的这些事件不再派发，因此传感器不会听到你、也不会输出红石；方块本身照常同步、其它玩家不受影响。切换时动作栏会提示开/关。激活方式由配置 `noUpdatePlacementMode` 决定：`HOLD` 按住 / `TOGGLE` 切换 / `INVERTED_HOLD` 反向按住（默认 `HOLD`）。
- **生存玩家授权**：生存玩家默认不能用该按键，需先获得「抑制更新」权限，三种来源互不干扰——API `setSurvivalNoUpdateAccess`、戴 `noUpdateCurioTag` 标签的饰品、以及管理指令 `/wyp access grant noupdate [玩家]`（`/wyp access revoke noupdate [玩家]` 撤销，权限等级 2）。指令授权**按玩家持久保存**（死亡重生、重登、重启都保留），撤销后立刻失效；是否已授权可用 `/wyp status [玩家]` 查看。
- **方块破坏器**（管理员物品，创造模式「操作员实用物品」标签页）：右键方块强制移除，并**屏蔽该方块自身的右键交互**（自带的交互界面、放置等），无视自我限制。配置项 `blockBreakerEnabled`。
- 二者依赖一个兼容处理开关 `blockProtectionBypass`（默认开）来让移除在个别特殊方块上生效；该处理是关键词式的，不针对任何特定模组，找不到时自动退回普通移除。
- 破坏遵循原版创造行为：**容器内容物掉落、方块物品不掉落**。

### API（供 KubeJS / 其他模组）
类：`net.wieldyourpower.api.WieldYourPowerApi`。方块操作是**服务端**静态方法（客户端调用返回 `false`；返回 `true` 表示方块确实变了）；`setSurvivalNoUpdateAccess` 也是**服务端**调用（会同步给该玩家客户端）。

| 方法 | 作用 |
|---|---|
| `breakBlock(level, pos, player)` / `(…, drop)` | 强制移除方块（同管理员方块破坏器：跑方块自身 destroy 钩子、关键词式绕过其它模组的方块保护）；`drop` 控制是否先掉资源 |
| `breakBlockNoUpdate(level, pos, player)` / `(…, drop)` | 同上，但**不触发邻居更新**（观察者/活塞/红石不响应；方块本身照常同步） |
| `placeBlockNoUpdate(level, pos, state, player)` | 放置 `state` 且**不触发邻居更新**（同 `BlockItem.placeBlock` 的写入方式） |
| `setSurvivalNoUpdateAccess(player, allowed)` | 授予/撤销该**生存**玩家的**不更新模式权限**：让他们也能用「不更新」快捷键/抑制。服务端在**装备**时传 `true`、**卸下**时传 `false`（只对正确栏位调用）；服务器会把开关**同步给该玩家的客户端**（客户端据此做本地预测），撤销时立刻同步失效。创造玩家无需调用，此接口**不影响创造** |

KubeJS 例：
```js
const WYP = Java.loadClass('net.wieldyourpower.api.WieldYourPowerApi')

// 1) 右键工具：不更新地破坏方块
WYP.breakBlockNoUpdate(level, pos, player, true)

// 2) curio 饰品：装备解锁「抑制更新」快捷键（服务端 equip/unequip，仅正确栏位）
WYP.setSurvivalNoUpdateAccess(player, true)   // 装备时
WYP.setSurvivalNoUpdateAccess(player, false)  // 卸下时
```

**饰品自动授予（需要 Curios，软依赖）**：给饰品物品打上 `noUpdateCurioTag`（默认 `wieldyourpower:no_update_curio`）标签后，戴着它的生存玩家**自动**获得抑制更新（服务端只在**装备/卸下/登录**时检测并同步客户端，**不每 tick 扫描**；卸下自动失效；创造不受影响）。配置：`noUpdateCurioEnabled` / `noUpdateCurioTag`。
```js
// KubeJS：把你的 curio 物品挂到标签上（curio 槽位本身用 curios 的标签声明）
ServerEvents.tags('item', e => {
  e.add('wieldyourpower:no_update_curio', 'kubejs:my_amulet')
})
```

### 其他
- 所有过滤列表（友军 / 武德 / 作者庇护 / attribute）在 Cloth 界面里都有**快捷添加行**（下拉选列表与类型 + 填值 + 添加）。
- 死亡界面可按 `/` 或 `T` 直接敲指令（便于脱困）。
- Cloth Config 为**强依赖**，所有配置都在其界面里，且每项都有说明 tooltip。

## 指令

权限等级 2 的为管理指令；省略目标时默认为自己。

```
/wyp panel
/wyp config
/wyp kill [目标] [honor]
/wyp freeze <目标> [刻数] [半径]
/wyp favor add|remove [目标]
/wyp access grant|revoke noupdate [玩家]
/wyp status [玩家]
```

自身限制（行走/飞行速度、挖掘速度与间隔、跳跃、跨越、attribute 列表、重置）只在 `/wyp panel` 的 Cloth 界面里编辑，不提供指令；`honor` 是可选尾缀，走普通死亡流程。

## 作者庇护用法

用 KubeJS 等工具给目标实体加上标签，或把目标按 `type,` / `uuid,` 写进 `[authorsFavor] filter`。也可用指令快捷添加 / 移除默认标签：

```
/wyp favor add [实体]
/wyp favor remove [实体]
```

配置项（`config/wieldyourpower-common.toml`，`[authorsFavor]`，或直接在 Cloth 界面里改）：

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用庇护 |
| `filter` | `["tag,authorsfavor"]` | 逗号分隔的匹配项：`tag,<标签>` / `type,<实体id>` / `uuid,<uuid>` |
| `damageCoefficient` | `1` | 变化量系数（0–1）：非原版链路的 `setHealth(X)`（`X` 低于当前血量）只让 `(当前血量 - X) × 系数` 这部分变化生效，即 `新血量 = floor(当前血量 - (当前血量 - X) × 系数)`；负数按 0 算。`1` = 变化全额生效（不保护），`0` = 变化全被吸收（血量不变）；原版链路伤害不受影响 |
| `maxHealthCoefficient` | `0` | 生命上限削减的硬地板（相对历史最大值） |
| `maxHealthChangeCoefficient` | `1` | 生命上限变化量的系数：`1` 全额生效，`0` 忽略削减 |

> 本模组的 `/wyp kill` 始终穿透庇护（`KillUtil.isForceKilling` 后门）。

## 构建

- 需要 JDK 17、Forge `1.20.1-47.3.10`、Gradle 8.8 wrapper。
- `.\gradlew.bat clean build`
- 产物：`build/libs/wieldyourpower-1.20.1-forge-<版本>.jar`
- 依赖从 `modpackModsDir` 读取（`build.gradle` 中定义，默认项目相对 `libs/`；可用 `-PmodpackModsDir=...` 覆盖）：
  - **Cloth Config 必需**（缺 `cloth-config-*.jar` 编译失败，运行时也要求）。

## 许可

MIT
