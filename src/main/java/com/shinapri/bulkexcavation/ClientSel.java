package com.shinapri.bulkexcavation;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.math.BlockPos;

public final class ClientSel {
    public static BlockPos pos1, pos2;

    public static boolean push(BlockPos p) {
        if (pos1 == null) { pos1 = p; return false; }   // ยังไม่ครบ
        pos2 = p; return true;                           // ครบคู่แล้ว
    }
    public static Pair<BlockPos, BlockPos> consume() {
        Pair<BlockPos, BlockPos> pair = Pair.of(pos1, pos2);
        pos1 = pos2 = null;
        return pair;
    }
}
