package com.czhmc.bgt_ae2_addon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizationResourceTest {
    private static final String QUANTITY_TOO_LOW_KEY =
            "\"message.bgt_ae2_addon.crafting_quantity_too_low\"";

    @Test
    void quantityValidationMessageIsAvailableInEnglishAndChinese() throws IOException {
        String english = Files.readString(Path.of(
                "src/main/resources/assets/bgt_ae2_addon/lang/en_us.json"));
        String chinese = Files.readString(Path.of(
                "src/main/resources/assets/bgt_ae2_addon/lang/zh_cn.json"));

        assertTrue(english.contains(QUANTITY_TOO_LOW_KEY));
        assertTrue(chinese.contains(QUANTITY_TOO_LOW_KEY));
    }
}
