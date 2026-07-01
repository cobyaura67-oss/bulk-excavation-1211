package com.shinapri.bulkexcavation.network;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SetRegionPayload(BlockPos pos1, BlockPos pos2) implements CustomPayload {
    public static final Id<SetRegionPayload> ID =
            new Id<>(Identifier.of("bulk-excavation", "set_region"));

    public static final PacketCodec<RegistryByteBuf, SetRegionPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC, SetRegionPayload::pos1,
                    BlockPos.PACKET_CODEC, SetRegionPayload::pos2,
                    SetRegionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
