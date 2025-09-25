package com.lukeonuke.service;

import com.lukeonuke.model.ShopModel;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.KnowledgeBookItem;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.UUID;

public class TextService {
    public static String addPrefixString(String s) {
        final ConfigurationService cs = ConfigurationService.getInstance();
        return cs.getPrefix() + s;
    }

    public static MutableText addPrefix(Text text) {
        final ConfigurationService cs = ConfigurationService.getInstance();
        return cs.getPrefixAsText().append(text);
    }

    public static MutableText addPrefix(String text) {
        return addPrefix(Text.of(text));
    }

    public static MutableText successFormat(String text) {
        return Text.literal(text).formatted(Formatting.GREEN);
    }

    public static MutableText failureFormat(String text) {
        return Text.literal(text).formatted(Formatting.RED);
    }

    public static MutableText tipFormat(String text) {
        return Text.literal(text).formatted(Formatting.GRAY, Formatting.ITALIC);
    }

    public static MutableText formatShopOffer(ShopModel shop, World world) {
        return Text.literal(shop.getItemAmount() + "x ")
                .append(getItemNameWithEnchantments(shop.getItemAsItemStack(world)))
                .append(" for " + shop.getPriceAmount() + "x ")
                .append(getItemNameWithEnchantments(shop.getPriceAsItemStack(world)));
    }

    public static MutableText getItemNameWithEnchantments(ItemStack item){
        MutableText text =
                // Why so fucked up? So that the formatting only applies to the name
                Text.empty().append(
                    Text.empty().append(item.getFormattedName()).formatted(Formatting.BOLD)
                );

        StringBuilder sb = new StringBuilder(" (  ");
        if(item.hasEnchantments()){
            ItemEnchantmentsComponent itemEnchantmentsComponent = item.getEnchantments();
            itemEnchantmentsComponent.getEnchantments().forEach(ere -> {
                String id = ere.getIdAsString();
                id = id.substring(id.indexOf(":") + 1);
                sb.append(id.replace("_", " "));
                sb.append(" ");
                sb.append(itemEnchantmentsComponent.getLevel(ere));
                sb.append("  ");
            });
            sb.append(")");
        } else return text;

        return text.append(Text.literal(sb.toString()).formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
