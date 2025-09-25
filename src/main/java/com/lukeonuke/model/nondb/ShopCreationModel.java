package com.lukeonuke.model.nondb;

import com.lukeonuke.service.ShopPosition;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ShopCreationModel {
    private ShopPosition itemPos;
    private ShopPosition pricePos;
    private ShopPosition signPos;

    public boolean isValid() {
        return Objects.nonNull(itemPos) && Objects.nonNull(pricePos) && Objects.nonNull(signPos);
    }

    public boolean containsData() {
        return Objects.nonNull(itemPos) || Objects.nonNull(pricePos) || Objects.nonNull(signPos);
    }

    @Override
    public String toString() {
        return "ShopCreationModel{" +
                "itemPos=" + itemPos +
                ", pricePos=" + pricePos +
                ", signPos=" + signPos +
                ", isValid()=" + isValid() +
                " }";
    }
}
