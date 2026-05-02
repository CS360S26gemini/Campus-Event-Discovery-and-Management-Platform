package com.example.CampusEventDiscovery.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ThemeStyleContractTest {

    @Test
    public void sharedMaterial3Styles_exist() throws Exception {
        String themes = readUtf8(valuesFile("themes.xml"));

        assertTrue(themes.contains("name=\"AppMaterialCardView\""));
        assertTrue(themes.contains("name=\"AppButtonStyle\""));
        assertTrue(themes.contains("name=\"AppSecondaryButtonStyle\""));
        assertTrue(themes.contains("name=\"AppOutlinedButtonStyle\""));
        assertTrue(themes.contains("name=\"AppTextButtonStyle\""));
        assertTrue(themes.contains("name=\"AppDangerButtonStyle\""));
        assertTrue(themes.contains("name=\"AppTextInputBoxStyle\""));
        assertTrue(themes.contains("name=\"AppTextInputDropdownStyle\""));
        assertTrue(themes.contains("name=\"AppToolbarStyle\""));
        assertTrue(themes.contains("name=\"AppImageButtonStyle\""));
        assertTrue(themes.contains("name=\"colorControlHighlight\""));
        assertTrue(themes.contains("<item name=\"rippleColor\">?attr/colorControlHighlight</item>"));
    }

    @Test
    public void navigationStrings_exist() throws Exception {
        String strings = readUtf8(valuesFile("strings.xml"));

        assertTrue(strings.contains("name=\"organizer_tools\""));
        assertTrue(strings.contains("name=\"scan_tickets\""));
        assertTrue(strings.contains("name=\"open_sos_dashboard\""));
        assertTrue(strings.contains("name=\"view_payments\""));
        assertTrue(strings.contains("name=\"search_your_events\""));
        assertTrue(strings.contains("name=\"search_managed_events\""));
        assertTrue(strings.contains("name=\"event_context_label\""));
    }

    private static Path valuesFile(String name) {
        Path fromRoot = Paths.get("app", "src", "main", "res", "values", name);
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Paths.get("src", "main", "res", "values", name);
    }

    private static String readUtf8(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
