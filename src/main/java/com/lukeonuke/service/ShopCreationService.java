package com.lukeonuke.service;

import com.lukeonuke.SignShop;
import com.lukeonuke.model.nondb.MessageModel;
import com.lukeonuke.model.nondb.ShopCreationModel;
import com.lukeonuke.model.ShopModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * In memory storage for shop creation state.
 */
public class ShopCreationService {
    private final HashMap<PlayerEntity, ShopCreationModel> storage = new HashMap<>();
    ;

    private ShopCreationService() {
    }

    private static ShopCreationService instance = null;

    public static ShopCreationService getInstance() {
        if (instance == null) instance = new ShopCreationService();
        return instance;
    }

    private void insureEntryExists(PlayerEntity pe) {
        storage.computeIfAbsent(pe, playerEntity -> new ShopCreationModel());
    }

    public void setShopCreationModel(PlayerEntity pe, @Nullable ShopPosition itemPos, @Nullable ShopPosition pricePos, @Nullable ShopPosition signPos) {
        insureEntryExists(pe);
        ShopCreationModel model = storage.get(pe);
        if (itemPos != null) model.setItemPos(itemPos);
        if (pricePos != null) model.setPricePos(pricePos);
        if (signPos != null) model.setSignPos(signPos);
        storage.put(pe, model);
    }

    public void setItemPos(PlayerEntity pe, ShopPosition itemPos) {
        setShopCreationModel(pe, itemPos, null, null);
    }

    public void setPricePos(PlayerEntity pe, ShopPosition pricePos) {
        setShopCreationModel(pe, null, pricePos, null);
    }

    public void setSignPos(PlayerEntity pe, ShopPosition signPos) {
        setShopCreationModel(pe, null, null, signPos);
        pe.sendMessage(Text.of("Set sign pos"), true);
    }

    public ShopCreationModel getShopCreationModel(PlayerEntity pe) {
        insureEntryExists(pe);
        return storage.get(pe);
    }

    public void setStorage(PlayerEntity pe, ShopPosition pos){
        ShopCreationModel model = getShopCreationModel(pe);
        if(model.getItemPos() == null){
            model.setItemPos(pos);
            pe.sendMessage(TextService.addPrefix("Set item storage position."), true);
        } else if (model.getPricePos() == null) {
            model.setPricePos(pos);
            pe.sendMessage(TextService.addPrefix("Set price storage position."), true);
        }
    }

    public void reset(PlayerEntity pe){
        storage.remove(pe);
        insureEntryExists(pe);
    }

    public MessageModel createShop(PlayerEntity pe) {
        ShopCreationModel shopCreation = getShopCreationModel(pe);
        if (!shopCreation.isValid()) return new MessageModel("Shop not built!", false);
        ShopModel databaseModel = new ShopModel();
        databaseModel.setStorageItemPos(shopCreation.getItemPos().toString());
        databaseModel.setStoragePricePos(shopCreation.getPricePos().toString());
        databaseModel.setPosition(shopCreation.getSignPos().toString());
        databaseModel.setOwner(pe.getUuid());

        final MinecraftServer server = pe.getServer();
        if (server == null){
            SignShop.LOGGER.error("Server was null whilst creating shop!");
            return new MessageModel("Internal server error! ShopCreationService#createShop server==null", false);
        }

        ShopPosition itemPos = shopCreation.getItemPos();
        ShopPosition pricePos = shopCreation.getPricePos();

        // Set and validate positions in database model;
        MessageModel returnedMessage = setItemOrPrice(databaseModel, itemPos, server, true);
        if(!returnedMessage.isSuccess()) return returnedMessage;
        returnedMessage = setItemOrPrice(databaseModel, pricePos, server, false);
        if(!returnedMessage.isSuccess()) return returnedMessage;

        // Insert shop model into the database.
        DatabaseService.getInstance().insertShop(databaseModel);
        return new MessageModel("Shop created!", true);
    }

    private MessageModel setItemOrPrice(ShopModel reference, ShopPosition itemPos, MinecraftServer server, boolean isItem){
        // Set message prefix so players know if it's the price or item.
        String loggingPrefix = "Price";
        if(isItem) loggingPrefix = "Item";

        // Check itemPos
        if(itemPos == null){
            SignShop.LOGGER.error("{} pos was null whilst creating shop!", loggingPrefix);
            return new MessageModel("Internal server error! item/pricePos==null", false);
        }
        // Get world
        World itemWorld = server.getWorld(itemPos.getWorldAsRegistryKey());
        // Check if world is null
        if(itemWorld == null){
            SignShop.LOGGER.error("World was null whilst creating shop!");
            return new MessageModel("Internal server error! itemWorld==null", false);
        }

        // Get storage and check if it exists
        Inventory storage = ShopUtil.getStorage(itemPos, server);
        if(storage == null) return new MessageModel(loggingPrefix + " storage doesn't exist!", false);

        // Get item(stack) and validate
        ItemStack item = InventoryUtil.getFirstItem(storage);
        if(item == null) return new MessageModel(loggingPrefix + " storage is empty! Can't read what the item is if it doesn't exist.", false);
        int itemAmount = InventoryUtil.countItems(storage, item);
        if(itemAmount <= 0) return new MessageModel(loggingPrefix + " cant be 0 or negative!", false);

        // Serialise item(stack) and validate serialisation
        String itemNBT = InventoryUtil.itemToNbt(item, itemWorld);
        if(itemNBT == null) return new MessageModel("Internal server error! ItemNBT cant be null!", false);

        if(itemNBT.length() > 1024) return new MessageModel("Item contains too much data!", false);

        if(isItem){
            reference.setItem(itemNBT);
            reference.setItemAmount(itemAmount);
        }else {
            reference.setPrice(itemNBT);
            reference.setPriceAmount(itemAmount);
        }
        return new MessageModel("Set values!", true);
    }
}
