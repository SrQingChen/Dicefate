package com.srqingchen.dicefate.block;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.block.StationBlocks.AppraisalTableBlock;
import com.srqingchen.dicefate.block.StationBlocks.DieEngraverBlock;
import com.srqingchen.dicefate.block.StationBlocks.FateAltarBlock;
import com.srqingchen.dicefate.block.StationBlocks.FateLoomBlock;
import com.srqingchen.dicefate.block.StationBlocks.MerchantStallBlock;
import com.srqingchen.dicefate.block.StationBlocks.OfferingShrineBlock;
import com.srqingchen.dicefate.block.StationBlocks.RepairBenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

/**
 * 8 个功能方块（规划 5.4）。交互式实现：右键触发核心功能。
 * 26.1 的 BlockBehaviour.Properties 强制要求 setId —— 必须走 registerBlock
 * （register 的 Supplier 路径不会写入 id，会在构造时抛 "Block id not set"）。
 */
public final class FateBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(DiceFate.MODID);

    public static final DeferredBlock<FateAltarBlock> FATE_ALTAR =
        reg("fate_altar", FateAltarBlock::new, FateBlocks::stationProps);
    public static final DeferredBlock<DieEngraverBlock> DIE_ENGRAVER =
        reg("die_engraver", DieEngraverBlock::new, FateBlocks::stationProps);
    public static final DeferredBlock<AppraisalTableBlock> APPRAISAL_TABLE =
        reg("appraisal_table", AppraisalTableBlock::new, FateBlocks::stationProps);
    public static final DeferredBlock<RepairBenchBlock> REPAIR_BENCH =
        reg("repair_bench", RepairBenchBlock::new, FateBlocks::stationProps);
    public static final DeferredBlock<FateLoomBlock> FATE_LOOM =
        reg("fate_loom", FateLoomBlock::new, FateBlocks::stationProps);
    public static final DeferredBlock<OfferingShrineBlock> OFFERING_SHRINE =
        reg("offering_shrine", OfferingShrineBlock::new, FateBlocks::stationProps);
    public static final DeferredBlock<MerchantStallBlock> MERCHANT_STALL =
        reg("merchant_stall", MerchantStallBlock::new,
            p -> stationProps(p).mapColor(MapColor.WOOD));
    public static final DeferredBlock<Block> NULL_PORTAL =
        reg("null_portal", Block::new,
            p -> p.noOcclusion().strength(-1.0F, 3600000.0F).lightLevel(s -> 11));

    /** 工作站通用属性（作为 UnaryOperator 传给 registerBlock，由其负责 setId）。 */
    private static BlockBehaviour.Properties stationProps(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.METAL)
            .strength(2.5F, 6.0F)
            .sound(SoundType.DEEPSLATE)
            .requiresCorrectToolForDrops()
            .pushReaction(PushReaction.BLOCK);
    }

    private static <T extends Block> DeferredBlock<T> reg(
        String name, Function<BlockBehaviour.Properties, T> factory,
        java.util.function.UnaryOperator<BlockBehaviour.Properties> propsOp) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, factory, propsOp);
        com.srqingchen.dicefate.registry.ModBlocks.BLOCK_ITEMS.register(name,
            id -> new net.minecraft.world.item.BlockItem(block.get(),
                com.srqingchen.dicefate.registry.IdProps.item(id).stacksTo(64)));
        return block;
    }

    private FateBlocks() {}
}
