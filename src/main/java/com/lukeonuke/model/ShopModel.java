package com.lukeonuke.model;

import com.lukeonuke.service.InventoryUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "shops", indexes = {
        @Index(name = "idx_position", columnList = "position") // secondary key
})
public class ShopModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    @Setter
    private String position; // secondary key

    @Column(nullable = false)
    @Setter
    private UUID owner;

    @Column(nullable = false, length = 1024)
    @Setter
    private String item;
    @Column(nullable = false)
    @Setter
    private int itemAmount;

    @Column(nullable = false, length = 1024)
    @Setter
    private String price;
    @Column(nullable = false)
    @Setter
    private int priceAmount;

    @Column(nullable = false)
    @Setter
    private String storagePricePos;
    @Column(nullable = false)
    @Setter
    private String storageItemPos;

    @CreationTimestamp
    @Column(updatable = false)
    @Getter
    private Instant createdAt;

    @Column(nullable = true)
    @Setter
    private Instant deletedAt;

    public ItemStack getPriceAsItemStack(World world){
        return InventoryUtil.fromJson(getPrice(), world);
    }

    public ItemStack getItemAsItemStack(World world){
        return InventoryUtil.fromJson(getItem(), world);
    }

    @Override
    public String toString() {
        return "ShopModel{" +
                "id=" + id +
                ", position='" + position + '\'' +
                ", owner=" + owner +
                ", item='" + item + '\'' +
                ", itemAmount=" + itemAmount +
                ", price='" + price + '\'' +
                ", priceAmount=" + priceAmount +
                ", storagePricePos='" + storagePricePos + '\'' +
                ", storageItemPos='" + storageItemPos + '\'' +
                ", createdAt=" + createdAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
