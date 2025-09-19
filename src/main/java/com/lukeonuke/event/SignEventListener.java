package com.lukeonuke.event;

import com.lukeonuke.model.MessageModel;
import com.lukeonuke.model.ShopModel;
import com.lukeonuke.service.*;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SignEventListener implements AttackBlockCallback, UseBlockCallback, PlayerBlockBreakEvents.Before {

    // attack
    @Override
    public ActionResult interact(PlayerEntity playerEntity, World world, Hand hand, BlockPos blockPos, Direction direction) {
        if (world.isClient) return ActionResult.PASS;
        final MinecraftServer server = playerEntity.getServer();
        if (server == null) return ActionResult.PASS;

        if (InventoryUtil.isHoldingRedstone(playerEntity)) {
            final ShopCreationService scs = ShopCreationService.getInstance();
            final BlockEntity block = world.getBlockEntity(blockPos);
            if (block instanceof ChestBlockEntity) {
                scs.setStorage(playerEntity, new ShopPosition(world, blockPos));
                return ActionResult.FAIL;
            } else if (block instanceof SignBlockEntity) {
                final SignBlockEntity sign = ShopUtil.getSignIfQualifiesAsShop(world, blockPos);
                if (sign == null) return ActionResult.PASS;

                scs.setSignPos(playerEntity, new ShopPosition(world, blockPos));
                if (scs.getShopCreationModel(playerEntity).isValid()) {
                    new Thread(() -> {
                        final DatabaseService ds = DatabaseService.getInstance();
                        final ShopModel shop = ds.getShopByPosition(new ShopPosition(world, blockPos));

                        server.executeSync(() -> {
                            if (shop != null) return;
                            MessageModel message = scs.createShop(playerEntity);
                            playerEntity.sendMessage(message.getAsTextMessage(), false);
                            scs.reset(playerEntity);
                            if(message.isSuccess()) ShopUtil.formatShop(sign, true);
                        });
                    }).start();

                    return ActionResult.FAIL;
                }
            } else {
                scs.reset(playerEntity);
                playerEntity.sendMessage(TextService.addPrefix("Reset creation storage."), false);
                return ActionResult.FAIL;
            }
        } else {
            final SignBlockEntity sign = ShopUtil.getSignIfQualifiesAsShop(world, blockPos);
            if (sign == null) return ActionResult.PASS;

            final DatabaseService ds = DatabaseService.getInstance();
            final ShopModel shop = ds.getShopByPosition(new ShopPosition(world, blockPos));
            if (shop == null) return ActionResult.PASS;
            playerEntity.sendMessage(
                    TextService.addPrefix(
                            Text.literal("Sells ").append(TextService.formatShopOffer(shop, world))
                    )
                    , false
            );
            if (shop.getOwner().equals(playerEntity.getUuid())) return ActionResult.PASS;
            return ActionResult.FAIL;
        }

        return ActionResult.FAIL;
    }

    // use
    @Override
    public ActionResult interact(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        if (world.isClient) return ActionResult.PASS;

        final BlockPos blockPos = blockHitResult.getBlockPos();
        final SignBlockEntity sign = ShopUtil.getSignIfQualifiesAsShop(world, blockPos);
        if (sign == null) return ActionResult.PASS;

        final DatabaseService ds = DatabaseService.getInstance();

        ShopModel model = ds.getShopByPosition(new ShopPosition(world, blockPos));
        if (model != null) {
            // Shop exists
            MessageModel message = ShopUtil.beginTransaction(model, playerEntity, sign);
            if(message.isSuccess()){
                playerEntity.sendMessage(
                        Text.literal("Bought ")
                            .append(TextService.formatShopOffer(model, world)),
                        true
                );
            }else {
                playerEntity.sendMessage(message.getAsTextMessage(), false);
                ShopUtil.formatShop(sign, false);
            }
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity playerEntity, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null) return true;
        SignBlockEntity signBlock = ShopUtil.getSignIfQualifiesAsShop(blockEntity);
        if (signBlock == null) return true;

        final DatabaseService ds = DatabaseService.getInstance();
        ShopModel shop = ds.getShopByPosition(new ShopPosition(world, blockPos));
        if (shop == null) return true;
        if (shop.getOwner().equals(playerEntity.getUuid())) {
            ds.softDeleteShopById(shop.getId());
            playerEntity.sendMessage(TextService.addPrefix(TextService.successFormat("Shop has been removed!")), true);
            return true;
        }
        return false;
    }
}
