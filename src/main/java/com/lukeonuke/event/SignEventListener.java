package com.lukeonuke.event;

import com.lukeonuke.model.nondb.MessageModel;
import com.lukeonuke.model.ShopModel;
import com.lukeonuke.service.*;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
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
        // Initialise and check prerequisites
        if (world.isClient) return ActionResult.PASS;
        final MinecraftServer server = playerEntity.getServer();
        if (server == null) return ActionResult.PASS;

        if (InventoryUtil.isHoldingRedstone(playerEntity)) {
            // Shop creation
            final ShopCreationService scs = ShopCreationService.getInstance();
            final BlockEntity block = world.getBlockEntity(blockPos);
            if (block instanceof Inventory) {
                // Set item/price storage
                scs.setStorage(playerEntity, new ShopPosition(world, blockPos));
                return ActionResult.FAIL;
            } else if (block instanceof SignBlockEntity) {
                // Create shop.
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
                // If player clicked on a non chest/sign block, reset creation storage.
                if(scs.getShopCreationModel(playerEntity).containsData()){
                    scs.reset(playerEntity);
                    playerEntity.sendMessage(TextService.addPrefix(TextService.successFormat("Cleared shop creation storage.")), false);
                }
                return ActionResult.PASS; // As of 0.0.3-ALPHA, resetting shop creation storage lets the player interact with the block below.
            }
        } else {
            // Get shop information and send to player.
            final SignBlockEntity sign = ShopUtil.getSignIfQualifiesAsShop(world, blockPos);
            if (sign == null) return ActionResult.PASS;

            final DatabaseService ds = DatabaseService.getInstance();
            final ShopModel shop = ds.getShopByPosition(new ShopPosition(world, blockPos));
            if (shop == null) return ActionResult.PASS;
            if (shop.getOwner().equals(playerEntity.getUuid())) {
                // If it's the owner of the sign.
                if(playerEntity.isSneaking()){
                    // Let them break the sign and update the shop.
                    return ActionResult.PASS;
                } else {
                    // Update shop sign, don't let them break the sign. Not shifting, nuh uh.
                    ShopUtil.formatShop(sign, ShopUtil.shopHasStock(shop, server));
                    playerEntity.sendMessage(TextService.addPrefix(TextService.tipFormat("To break the shop, sneak whilst breaking the sign.")), false);
                    sendShopOffer(playerEntity, shop, world);
                    playerEntity.sendMessage(TextService.addPrefix(TextService.successFormat("Updated shop.")), true);
                    return ActionResult.FAIL;
                }

            }
            sendShopOffer(playerEntity, shop, world);
            return ActionResult.FAIL;
        }

        return ActionResult.FAIL;
    }

    // use
    @Override
    public ActionResult interact(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        // Prerequisite
        if (world.isClient) return ActionResult.PASS;

        final BlockPos blockPos = blockHitResult.getBlockPos();
        final SignBlockEntity sign = ShopUtil.getSignIfQualifiesAsShop(world, blockPos);
        if (sign == null) return ActionResult.PASS;

        final DatabaseService ds = DatabaseService.getInstance();

        ShopModel model = ds.getShopByPosition(new ShopPosition(world, blockPos));
        if (model != null) {
            // Buy from shop if shop exists.
            MessageModel message = ShopUtil.beginTransaction(model, playerEntity, sign);
            if(message.isSuccess()){
                // If transaction is successful say what player bought.
                playerEntity.sendMessage(
                        Text.literal("Bought ")
                            .append(TextService.formatShopOffer(model, world)),
                        true
                );
            }else {
                // If transaction failed return why and format shop.
                playerEntity.sendMessage(message.getAsTextMessage(), false);
                if (!message.isPlayersFault()) ShopUtil.formatShop(sign, false);
            }
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity playerEntity, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity) {
        // Prerequisite
        if (blockEntity == null) return true;
        SignBlockEntity signBlock = ShopUtil.getSignIfQualifiesAsShop(blockEntity);
        if (signBlock == null) return true;

        final DatabaseService ds = DatabaseService.getInstance();
        ShopModel shop = ds.getShopByPosition(new ShopPosition(world, blockPos));
        if (shop == null) return true;
        // If shop exists AND the owner is the player AND player is sneaking, then allow breaking it.
        if (shop.getOwner().equals(playerEntity.getUuid()) && playerEntity.isSneaking()) {
            ds.softDeleteShopById(shop.getId());
            playerEntity.sendMessage(TextService.addPrefix(TextService.successFormat("Shop has been removed!")), true);
            return true;
        }
        // Otherwise return false
        return false;
    }

    private void sendShopOffer(PlayerEntity pe, ShopModel shop, World world){
        pe.sendMessage(
                TextService.addPrefix(
                        Text.literal("Sells ").append(TextService.formatShopOffer(shop, world))
                )
                , false
        );
    }
}
