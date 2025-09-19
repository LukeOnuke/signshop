package com.lukeonuke.model;

import com.lukeonuke.service.TextService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Getter
@AllArgsConstructor
public class MessageModel {
    private String message;
    private boolean success;

    /**
     * Get as fully formatted text ready for player feedback.
     * */
    public Text getAsTextMessage(){
        MutableText text = Text.literal(message);
        if(success){
            text.formatted(Formatting.GREEN);
        }else {
            text.formatted(Formatting.RED);
        }
        return TextService.addPrefix(text);
    }
}
