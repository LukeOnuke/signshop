package com.lukeonuke.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lukeonuke.SignShop;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for manipulating inventories in the context of signshops.
 * Most dangerous class in the whole plugin. Also took the longest...
 */
public class InventoryUtil {
    /**
     * Count the occurrences of an item inside an inventory.
     *
     * @return Occurrences of the item. Goes over 64!
     */
    public static int countItems(Inventory inventory, ItemStack item) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if(ItemStack.areItemsAndComponentsEqual(stack, item)){
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Get the first item in the inventory, if the inventory is empty it will return null.
     * Doesn't modify inventory state.
     */
    @Nullable
    public static ItemStack getFirstItem(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                return stack.copyWithCount(1);
            }
        }
        return null;
    }

    /**
     * Can the inventory store said item in that amount.
     * Doesn't modify inventory state.
     */
    public static boolean canStore(Inventory inventory, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                remaining -= item.getMaxCount();
                continue;
            }
            if (stack.isOf(item)) {
                remaining -= (stack.getMaxCount() - stack.getCount());
            }
        }

        return remaining <= 0;
    }

    /**
     * Add items to an inventory. Modifies inventory state!
     */
    public static void addItems(Inventory inventory, ItemStack item, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) { //If slot is empty add max stack size or remaining.
                int stackAmount = Math.min(remaining, item.getMaxCount());
                inventory.setStack(i, item.copyWithCount(stackAmount));
                remaining -= stackAmount;
                continue;
            }
            // If last condition is not met, calculate space in current stack;
            int space = item.getMaxCount() - stack.getCount();
            if (ItemStack.areItemsAndComponentsEqual(stack, item) && space > 0) {
                int stackAmount = Math.min(space, remaining); // Get stack size, either remaining or maximum left.
                inventory.setStack(i, item.copyWithCount(stackAmount + stack.getCount()));
                remaining -= stackAmount;
            }

            if (remaining <= 0) break;
        }

        inventory.markDirty();
    }

    /**
     * Remove items from an inventory. Modifies inventory state!
     */
    public static void removeItems(Inventory inventory, ItemStack item, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (ItemStack.areItemsAndComponentsEqual(stack, item)) {
                if (remaining >= stack.getMaxCount()) {
                    inventory.setStack(i, ItemStack.EMPTY);
                    remaining -= stack.getMaxCount();
                } else {
                    stack.setCount(stack.getCount() - remaining);
                    //inventory.setStack(i, stack);
                    remaining = 0;
                }
            }
        }
        inventory.markDirty();
    }

    @Nullable
    public static String itemToNbt(ItemStack stack, World world) {
        final Gson gson = new Gson();
        try{
            RegistryWrapper.WrapperLookup registries = world.getRegistryManager();
            DynamicOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);

            JsonElement json = ItemStack.CODEC.encodeStart(ops, stack)
                    .getOrThrow();

            return gson.toJson(json);
        }catch (Exception e){
            SignShop.LOGGER.error("Couldn't serialise item: {} {}", e.getClass() , e.getMessage());
        }
        return null;
    }

    @Nullable
    public static ItemStack fromJson(String jsonString, World world) {
        try{
            RegistryWrapper.WrapperLookup registries = world.getRegistryManager();
            DynamicOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);

            JsonElement element = JsonParser.parseString(jsonString);
            return ItemStack.CODEC.parse(ops, element).getOrThrow();
        }catch (Exception e){
            SignShop.LOGGER.error("Couldn't deserialize item: {} {}", e.getClass() , e.getMessage());
        }

        return null;
    }

    public static boolean isHoldingRedstone(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        return !stack.isEmpty() && stack.getItem() == Items.REDSTONE;
    }
}
