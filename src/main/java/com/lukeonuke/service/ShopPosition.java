package com.lukeonuke.service;

import com.lukeonuke.SignShop;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@Getter
public class ShopPosition {
    private String world;
    private int x;
    private int y;
    private int z;

    public ShopPosition(World world, BlockPos blockPos) {
        this.world = world.getRegistryKey().getValue().toString();
        this.x = blockPos.getX();
        this.y = blockPos.getY();
        this.z = blockPos.getZ();
    }

    @Override
    public String toString() {
        return world + "/" + x + "/" + y + "/" + z;
    }

    @Nullable
    public static ShopPosition fromString(String enc){
        String[] encoded = enc.split("/");
        if (encoded.length < 4) {
            SignShop.LOGGER.error("Tried to decode malformed ShopPosition! Value: {}", enc);
            return null;
        }
        return new ShopPosition(encoded[0], Integer.parseInt(encoded[1]), Integer.parseInt(encoded[2]), Integer.parseInt(encoded[3]));
    }

    public RegistryKey<World> getWorldAsRegistryKey(){
        Identifier id = Identifier.of(getWorld());
        return RegistryKey.of(RegistryKeys.WORLD, id);
    }

    public BlockPos getBlockPos(){
        return new BlockPos(x, y, z);
    }
}
