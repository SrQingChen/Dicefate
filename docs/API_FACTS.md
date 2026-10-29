# NeoForge 26.1.2 / MC 26.1.2 API 事实清单

> 本文件全部来自本地反编译源码（NFRT decompile jar, 6882 java）与 `neoforge-26.1.2.87-universal.jar` (javap)。
> 编码前必须以本清单为准；与 1.21.x 记忆不符处一律以本清单为准。发现新事实随时补充。

## 已核实（2026-10-22）

### 命名重构（与 1.21.x 相比的最大变化）
- `ResourceLocation` → **`net.minecraft.resources.Identifier`**
  - 工厂：`Identifier.parse("ns:path")`、`Identifier.fromNamespaceAndPath(ns, path)`、`Identifier.withDefaultNamespace(path)`
  - `STREAM_CODEC` 存在（STRING_UTF8 map）
- `GuiGraphics` → **`net.minecraft.client.gui.GuiGraphicsExtractor`**
  - `pose()` 返回 `Matrix3x2fStack`；`fill/blit/blitSprite(RenderPipeline, Identifier, x, y, w, h[, color|alpha])`
- 原版注册表键类位于 `net.minecraft.core.registries.Registries`（注意包名含 s）
  - `ITEM, BLOCK, ENTITY_TYPE, MENU, SOUND_EVENT, PARTICLE_TYPE, VILLAGER_PROFESSION, RECIPE_SERIALIZER, DATA_COMPONENT_TYPE, BLOCK_ENTITY_TYPE, CREATIVE_MODE_TAB, VILLAGER_TRADE`（26.1 村民交易数据驱动化）
- `ItemInstance` 是新类（ItemStack 的无数量抽象）；运行时仍用 `ItemStack`，配方/序列化可用 `ItemStackTemplate`
- `EntityRenderer<T extends Entity, S extends EntityRenderState>` 双泛型 + `createRenderState()`
- 渲染状态：`LivingEntityRenderState`、`HumanoidRenderState`；`MobRenderer<T, S extends LivingEntityRenderState, M extends EntityModel<? super S>>`；`HumanoidMobRenderer<T, S extends HumanoidRenderState, M extends HumanoidModel<S>>`；`HumanoidModel.createMesh(CubeDeformation, float)`

### 伤害事件（拼点唯一入口）
- `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent`：**存在**，`ICancellableEvent`
  - `getContainer()` → `DamageContainer`；`getSource()`；`getAmount()/setAmount(float)`；`getOriginalAmount()`；`addReductionModifier(...)`；`setInvulnerabilityTicks(int)`
- `LivingDamageEvent$Post`：`getInflictedDamage()/getHealthDamage()/getBlockedDamage()`
- `LivingDamageEvent$Pre`（与 Incoming 并存，注意区分；我们用 Incoming）
- `DamageSource.getEntity()/getDirectEntity()/getWeaponItem()` 均在
- `Entity.invulnerableTime` 是 **public int**，可直接置 0
- `LivingEntity.hurt` 流程中 `invulnerableTime=20`；`DamageTypeTags.BYPASSES_COOLDOWN` 可绕过

### 属性与效果
- `AttributeModifier` 是 record：`(Identifier id, double amount, Operation operation)`
  - Operation 枚举：`ADD_VALUE("add_value",0)`、`ADD_MULTIPLIED_BASE(...,1)`、`ADD_MULTIPLIED_TOTAL(...,2)`
  - 属性本体经 `BuiltInRegistries.ATTRIBUTE` 持有（Holder 引用）
- MobEffect 体系仍在 `net.minecraft.world.effect`

### 物品/组件
- `ItemStack(ItemLike[, count])` 公开构造仍可用（内部走 `builtInRegistryHolder()`）
- 组件读写：`stack.set(DataComponentType<T>, T)`、`get/getOrDefault/remove/update/applyComponents(DataComponentPatch)`
- `ItemStack.getPrototype()/getComponentsPatch()/immutableComponents()`
- `Item.Properties`：`stacksTo(int)`, `durability(int)`, `rarity(Rarity)`, `food(...)`, `useCooldown(float)`, `equippable(EquipmentSlot)`, `craftRemainder`, `fireResistant()`, `tool(...)` 等
- `DataComponentType.Builder`：`.persistent(Codec)`（旧名）→ 以实际反编译为准：`networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf,T>)`；builder 方法含 codec/streamCodec（见反编译 47-71 行）

### 注册
- `DeferredRegister.create(Registry<T>|ResourceKey<Registry<T>>|Identifier, modid)`；`createItems/createBlocks/createEntities/createDataComponents` 便捷工厂
- `register(String, Supplier)` → `DeferredHolder`；`createTagKey("x")`
- `NeoForgeRegistries.ATTACHMENT_TYPES`（附件注册表）
- `BuildCreativeModeTabContentsEvent`（往标签页塞物品）；自定义标签页走 `Registries.CREATIVE_MODE_TAB` DeferredRegister + `CreativeModeTabRegistry`（细节后补）

### 附件（Attachment）
- `AttachmentType.builder(Supplier<T>|Function<IAttachmentHolder,T>)`
  - `.serialize(MapCodec<T>)` / `.serialize(IAttachmentSerializer)` / `.copyOnDeath()` / `.sync(StreamCodec<RegistryFriendlyByteBuf,T>)` / `.sync(BiPredicate<IAttachmentHolder,ServerPlayer>, StreamCodec)` → `.build()`

### 网络
- `RegisterPayloadHandlersEvent`（IModBusEvent）→ `event.registrar("1")` 返回 `PayloadRegistrar`
- `registrar.playToClient/playToServer/playBidirectional(CustomPacketPayload.Type<T>, StreamCodec<? super RegistryFriendlyByteBuf, T>, IPayloadHandler<T>)`
- `CustomPacketPayload.Type<T>` + `StreamCodec` 组合注册

### 客户端 GUI/HUD
- `RegisterGuiLayersEvent`（IModBusEvent）：`registerAboveAll/belowAll(Identifier, GuiLayer)`、`registerAbove/Below(anchor, id, GuiLayer)`、`replaceLayer`、`wrapLayer`
- `GuiLayer` 接口：`void render(GuiGraphicsExtractor, DeltaTracker)`
- `VanillaGuiLayers`（neoforge.client.gui）常量锚点
- 关键事件：`RegisterKeyMappingsEvent`、`ScreenEvent.Init.Post`、`RenderLivingEvent.Pre/Post`、`RenderNameTagEvent$CanRender/DoRender`、`ContainerScreenEvent`、`ClientTickEvent.Post`、`RegisterMenuScreensEvent`
- `RenderGuiLayerEvent.Post` 可叠加绘制

### 菜单
- `IMenuTypeExtension.create(IContainerFactory<T>)` → `MenuType<T>`；`Registries.MENU` 注册
- Screen 注册：`RegisterMenuScreensEvent`

### 模型（自定义 3D 骰子网格）
- `ModelEvent$RegisterLoaders`（IModBusEvent）：`register(Identifier, UnbakedModelLoader<?>)`
- 26.1 物品模型新事件：`RegisterItemModelsEvent`、`RegisterBlockModelsEvent`、`RegisterBlockStateModels`（代替旧 item_model 定义生成，细节后补）

### 数据包注册表
- `net.neoforged.neoforge.registries.DataPackRegistryEvent$NewRegistry`（IModBusEvent）

### Tick
- `ServerTickEvent$Pre/Post`：`getServer()/hasTime()`
- `PlayerTickEvent`（Pre/Post 子类）

### Curios 15.0.0（26.1.2）
- `CuriosApi.registerCurio(Item, ICurioItem)`；`CuriosApi.getCuriosInventory(LivingEntity)`；`CuriosDataComponents`（组件式槽位声明）
- `CuriosSlotTypes` / `CuriosSlotTypes$Preset`

### 村民
- `Registries.VILLAGER_PROFESSION` + `Registries.VILLAGER_TRADE`（26.1 交易数据驱动）

### 其他已核实
- `LivingEntity.java` L1186：无敌帧判定 `invulnerableTime > 10.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)`
- GameTest：`RegisterGameTestsEvent`、`GameTestHelperExtension`
- 模组元数据：`neoforge.mods.toml`（模板已配）

## 待核实（进入对应模块前补）
- [ ] Recipe datagen provider 具体类名（RecipeProvider/RecipeOutput）
- [ ] Advancement datagen（AdvancementProvider）
- [ ] LootTable datagen + `LivingDropsEvent`（自定义掉落改走代码+datapack）
- [ ] StructureType/结构 JSON 字段（26.1 worldgen）
- [ ] 自定义 UnbakedModelLoader→BakedModel 管线细节（mesh 三角形支持）
- [ ] KeyMapping 构造与注册细节
- [ ] ParticleTypes 注册 + `ServerLevel.sendParticles`
- [ ] Config：`ModConfigSpec` 包名
- [ ] 村民交易 JSON 格式（VILLAGER_TRADE registry）
- [ ] 声音事件注册 + sounds.json
- [ ] Boss 血条（BossEvent/BossBarColor 服务端 API）
- [ ] SpawnPlacements（仅档案，不改生成——可能不需要）

## 运行时实测补充（2026-10-22，runServer 验证）

以下三条在编译期完全通过、但启动时才炸，属 26.1 硬性运行时约束：

### 1. Block / Item 的 Properties 必须注入 id
- `BlockBehaviour.Properties` 构造时 `effectiveDrops()` 会 `Objects.requireNonNull(id)` → 未设 id 抛 `NullPointerException: Block id not set`
- `Item.Properties` 同理 → `Item id not set`
- **`DeferredRegister.register(String, Supplier)` 不会写 id**；必须用：
  - 方块：`BLOCKS.registerBlock(name, Function<Properties,B>, UnaryOperator<Properties>)`
  - 物品：`ITEMS.registerItem(name, Function<Properties,I>, UnaryOperator<Properties>)`
  - 或手动 `props.setId(ResourceKey.create(Registries.ITEM/BLOCK, id))`（见 `registry/IdProps`）
- `registerSimpleItem(...)` / `registerSimpleBlockItem(...)` 已自动注入 id，安全

### 2. structure JSON 必填 `spawn_overrides`
`Structure.StructureSettings.CODEC` 的 `spawn_overrides` 是 required field（无默认值）：
```json
{"type":"minecraft:jigsaw", "biomes":"#...", "step":"surface_structures",
 "spawn_overrides": {}, "terrain_adaptation":"beard_thin", ...}
```
缺失 → `IllegalStateException: No key spawn_overrides` → 整个注册表回滚。

### 3. 配方 ingredient 格式
`Ingredient.CODEC` 现在是 `HolderSet<Item>`：
- ✅ 单物品：`"minecraft:bone"`（裸 id 字符串）
- ✅ 多物品：`["minecraft:bone","minecraft:stick"]`
- ✅ 标签：`"#minecraft:planks"`
- ❌ 旧格式 `{"item":"minecraft:bone"}` 已失效
- `result` 用 `ItemStackTemplate.CODEC`：`{"id":"ns:item","count":n}` 或裸 id 字符串

### 4. neoforge.mods.toml 的值必须带引号
Gradle `expand` 后若写成 `modId=dicefate` / `version=0.1.0`，TOML 会把 `0.1.0` 当数字解析失败：
`Invalid sequence ... in number...` → `is not a valid mod file`。
模板中必须写 `modId="${mod_id}"`、`version="${mod_version}"`。

### 实测基线（26.1.2.87 dev runServer）
- `Loaded 1560 recipes` = 原版 1515 + 本模组 45
- `Loaded 1650 advancements` = 原版 1617 + 本模组 33
- `Done (2.096s)!` 零 ERROR / 零 dicefate 相关 WARN
