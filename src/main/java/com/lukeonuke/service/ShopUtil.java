package com.lukeonuke.service;

import com.lukeonuke.model.nondb.MessageModel;
import com.lukeonuke.model.ShopModel;
import com.lukeonuke.model.TransactionModel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShopUtil {

    public static boolean qualifiesAsPossibleShop(BlockEntity block){
        return getSignIfQualifiesAsShop(block) != null;
    }

    @Nullable
    public static SignBlockEntity getSignIfQualifiesAsShop(BlockEntity block){
        // Is sign?
        if(!(block instanceof SignBlockEntity sign)) return null;
        final SignText signText = sign.getText(true);
        // Does the first line of the sign contain trade?
        if(signText.getMessage(0, true).getString().equalsIgnoreCase("[trade]")){
            return sign;
        }
        return null;
    }

    @Nullable
    public static SignBlockEntity getSignIfQualifiesAsShop(World world, BlockPos blockPos){
        final BlockEntity block = world.getBlockEntity(blockPos);
        return getSignIfQualifiesAsShop(block);
    }

    public static MessageModel beginTransaction(ShopModel shop, PlayerEntity player, SignBlockEntity sign){
        PlayerInventory playerInventory = player.getInventory();
        World world = player.getWorld();

        ItemStack item = shop.getItemAsItemStack(world);
        ItemStack price = shop.getPriceAsItemStack(world);

        // Verify if user can even afford and store the items
        int availablePrice = InventoryUtil.countItems(playerInventory, price);
        if(availablePrice < shop.getPriceAmount()) return new MessageModel("You can't afford this!", false, true);
        if(!InventoryUtil.canStore(playerInventory, item.getItem(), shop.getItemAmount())) return new MessageModel("Not enough inventory space to buy!", false, true);

        // Get and verify the chests
        ShopPosition storagePricePos = ShopPosition.fromString(shop.getStoragePricePos());
        ShopPosition storageItemPos = ShopPosition.fromString(shop.getStorageItemPos());
        MinecraftServer server = player.getServer();
        if(server == null) return new MessageModel("Internal server error! server==null", false);
        if(storagePricePos == null || storageItemPos == null) return new MessageModel("Internal server error! Shop is corrupted! Cant read storage pos.", false);

        Inventory storagePrice = getStorage(storagePricePos, server);
        if(storagePrice == null) return new MessageModel("Price storage is missing!", false);
        Inventory storageItem = getStorage(storageItemPos, server);
        if(storageItem == null) return new MessageModel("Item storage is missing!", false);

        // Verify if the chests have the item and store the results of the transaction
        if(!InventoryUtil.canStore(storagePrice, price.getItem(), shop.getPriceAmount())) return new MessageModel("Shops currency storage is full!", false);
        if(InventoryUtil.countItems(storageItem, item) < shop.getItemAmount()) return new MessageModel("Shop doesn't have the stock!", false);

        // Transaction
        InventoryUtil.removeItems(playerInventory, price, shop.getPriceAmount());
        InventoryUtil.removeItems(storageItem, item, shop.getItemAmount());

        InventoryUtil.addItems(storagePrice, price, shop.getPriceAmount());
        InventoryUtil.addItems(playerInventory, item, shop.getItemAmount());

        // Log transaction
        final TransactionModel transactionModel = new TransactionModel();
        transactionModel.setBuyer(player.getUuid());
        transactionModel.setShop(shop);
        new Thread(() -> {
            DatabaseService.getInstance().insertTransaction(transactionModel);
        }).start();
        sendTransactionNotification(shop, server, player);
        formatShop(sign, shopHasStock(shop, storageItem, storagePrice, item, price));

        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.BLOCKS, 0.5f, 1.5f);

        return new MessageModel("Bought " + shop.getItemAmount() + "x " + item.getName().getString() + " for " + shop.getPriceAmount() + "x " + price.getName().getString(), true);
    }

    /**
     * Fetches the Inventory instance of a storage block at a ShopPosition.
     * @param pos ShopPosition dictating where the storage is.
     * @param server Server that holds the shop position.
     * @return Inventory instance regarding that storage block if it exists, or NULL if it doesn't exist.
     */
    @Nullable
    public static Inventory getStorage(ShopPosition pos, MinecraftServer server){
        ServerWorld world = server.getWorld(pos.getWorldAsRegistryKey());
        if (world == null) return null;
        //return world.getBlockEntity(pos.getBlockPos(), BlockEntityType.CHEST).orElse(null);
        final BlockPos blockPos = pos.getBlockPos();
        BlockEntity inventoryCandidate = world.getBlockEntity(blockPos);
        BlockState state = world.getBlockState(blockPos);
        Block block = state.getBlock();

        // Check if it's a doublechest.
        if(block instanceof ChestBlock chestBlock){
            Inventory inventory = ChestBlock.getInventory(chestBlock, state, world, blockPos, false);
            if (inventory != null) return inventory;
        }

        if (inventoryCandidate instanceof Inventory inventory){
            return inventory;
        }
        return null;
    }

    /**
     * Formats shop trade sign depending on is valid. It will set either be <font color="red">[trade]</font> if
     * isValid=false, or <font color="green">[trade]</font> if isValid=true.
     * @param sign The sign to be modified.
     * @param isValid Boolean toggle.
     */
    public static void formatShop(SignBlockEntity sign, boolean isValid){
        SignText old = sign.getFrontText();
        Text[] texts = sign.getFrontText().getMessages(true);

        Formatting formatting = Formatting.RED;
        if(isValid) formatting = Formatting.GREEN;
        texts[0] = Text.literal(texts[0].getString()).formatted(formatting, Formatting.BOLD);
        sign.setText(new SignText(texts, old.getMessages(true), old.getColor(), old.isGlowing()), true);
        sign.markDirty();
    }

    /**
     * Send notification of shop transaction to owner of shop.
     * @param shop Shop that the transaction was acted in.
     * @param server Server that houses the shop.
     * @param purchaser The player that initiated the transaction.
     */
    public static void sendTransactionNotification(ShopModel shop, MinecraftServer server, PlayerEntity purchaser){
        UUID ownerUUID = shop.getOwner();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerUUID);
        if(player == null) return;
        player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.5f);
        player.sendMessage(TextService.addPrefix(Text.empty().append(purchaser.getDisplayName()).append(" bought ").append(TextService.formatShopOffer(shop, server.getOverworld()))));
    }

    /**
     * Calculate if shop has stock.
     * @param shop Shop model referring to aforementioned shop.
     * @param storageItem Storage for shop item.
     * @param storagePrice Storage for shop price.
     * @param item Shop item.
     * @param price Shop price.
     * @return Does shop has enough stock to complete at least one transaction.
     */
    public static boolean shopHasStock(ShopModel shop, Inventory storageItem, Inventory storagePrice, ItemStack item, ItemStack price){
        if(!InventoryUtil.canStore(storagePrice, price.getItem(), shop.getPriceAmount())) return false;
        return InventoryUtil.countItems(storageItem, item) >= shop.getItemAmount();
    }

    /**
     * Calculate if shop has stock.
     * @param shop Shop model referring to aforementioned shop.
     * @param server Server that contains the shop.
     * @return Does shop has enough stock to complete at least one transaction.
     */
    public static boolean shopHasStock(ShopModel shop, MinecraftServer server){
        ItemStack item = shop.getItemAsItemStack(server.getOverworld());
        if(item == null) return false;
        ItemStack price = shop.getPriceAsItemStack(server.getOverworld());
        if(price == null) return false;

        ShopPosition storagePricePos = ShopPosition.fromString(shop.getStoragePricePos());
        ShopPosition storageItemPos = ShopPosition.fromString(shop.getStorageItemPos());
        if(storagePricePos == null || storageItemPos == null) return false;

        Inventory storagePrice = getStorage(storagePricePos, server);
        if(storagePrice == null) return false;
        Inventory storageItem = getStorage(storageItemPos, server);
        if(storageItem == null) return false;

        // Verify if the chests have the item and store the results of the transaction
        if(!InventoryUtil.canStore(storagePrice, price.getItem(), shop.getPriceAmount())) return false;
        return InventoryUtil.countItems(storageItem, item) >= shop.getItemAmount();
    }
}
