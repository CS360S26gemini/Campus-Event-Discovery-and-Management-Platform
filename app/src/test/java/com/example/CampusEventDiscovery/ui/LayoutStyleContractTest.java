package com.example.CampusEventDiscovery.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

public class LayoutStyleContractTest {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Test
    public void layouts_doNotUseLegacyOrNativeWidgets() throws Exception {
        List<String> failures = new ArrayList<>();

        for (File file : layoutFiles()) {
            Document document = parse(file);
            collectElements(document.getDocumentElement(), element -> {
                String tag = element.getTagName();
                if ("androidx.cardview.widget.CardView".equals(tag)
                        || "androidx.appcompat.widget.Toolbar".equals(tag)
                        || "Button".equals(tag)
                        || "EditText".equals(tag)) {
                    failures.add(file.getName() + " uses legacy/native widget <" + tag + ">");
                }
            });
        }

        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    @Test
    public void m3Controls_useSharedAppStyles() throws Exception {
        List<String> failures = new ArrayList<>();

        for (File file : layoutFiles()) {
            Document document = parse(file);
            collectElements(document.getDocumentElement(), element -> {
                String tag = element.getTagName();
                String style = element.getAttribute("style");

                if ("com.google.android.material.textfield.TextInputLayout".equals(tag)
                        && !style.startsWith("@style/AppTextInput")) {
                    failures.add(file.getName() + " TextInputLayout missing AppTextInput style");
                }

                if ("com.google.android.material.appbar.MaterialToolbar".equals(tag)
                        && !"@style/AppToolbarStyle".equals(style)) {
                    failures.add(file.getName() + " MaterialToolbar missing AppToolbarStyle");
                }

                if ("com.google.android.material.button.MaterialButton".equals(tag)
                        && !style.matches("@style/App.*ButtonStyle")) {
                    failures.add(file.getName() + " MaterialButton missing App button style");
                }

                if ("ImageButton".equals(tag)
                        && !"@style/AppImageButtonStyle".equals(style)) {
                    failures.add(file.getName() + " ImageButton missing AppImageButtonStyle");
                }
            });
        }

        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    @Test
    public void layouts_doNotHardcodeHexColors() throws Exception {
        List<String> failures = new ArrayList<>();

        for (File file : layoutFiles()) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).matches(".*#[0-9A-Fa-f]{6,8}.*")) {
                    failures.add(file.getName() + ":" + (i + 1) + " hardcodes a color");
                }
            }
        }

        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    @Test
    public void layouts_useThemeTypographyInsteadOfRawTextSizing() throws Exception {
        List<String> failures = new ArrayList<>();

        for (File file : layoutFiles()) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("android:textSize=") || line.contains("android:textStyle=\"bold\"")) {
                    failures.add(file.getName() + ":" + (i + 1) + " hardcodes typography instead of using textAppearance");
                }
            }
        }

        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    @Test
    public void clickableCards_useSharedHighlightRipple() throws Exception {
        List<String> failures = new ArrayList<>();

        for (File file : layoutFiles()) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("app:rippleColor=\"?attr/colorPrimary\"")) {
                    failures.add(file.getName() + ":" + (i + 1) + " uses direct primary ripple instead of colorControlHighlight");
                }
            }
        }

        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    @Test
    public void mainNavigation_hasBottomNavigationCreateActionSlot() throws Exception {
        Document document = parse(layoutFile("activity_main.xml"));
        Document menu = parse(menuFile("bottom_nav_menu.xml"));

        assertEquals(1, countId(document, "bottomNavigationView"));
        assertEquals(1, countId(menu, "nav_action"));
    }

    @Test
    public void organizerNavigationSurfaces_arePresent() throws Exception {
        Document organizerHome = parse(layoutFile("fragment_home_organizer.xml"));
        assertEquals(1, countId(organizerHome, "btnCreateEvent"));
        assertEquals(1, countId(organizerHome, "btnManageEvents"));
        assertEquals(1, countId(organizerHome, "btnScanTickets"));
        assertEquals(1, countId(organizerHome, "btnSosDashboard"));

        Document profile = parse(layoutFile("fragment_profile.xml"));
        assertEquals(1, countId(profile, "rowCreateEvent"));
        assertEquals(1, countId(profile, "rowScanTickets"));
        assertEquals(1, countId(profile, "rowSosDashboard"));

        Document organizerDetail = parse(layoutFile("activity_organizer_event_detail.xml"));
        assertEquals(1, countId(organizerDetail, "btnPayments"));
    }

    private interface ElementVisitor {
        void visit(Element element);
    }

    private static void collectElements(Element element, ElementVisitor visitor) {
        visitor.visit(element);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                collectElements((Element) child, visitor);
            }
        }
    }

    private static int countId(Document document, String idName) {
        final int[] count = {0};
        collectElements(document.getDocumentElement(), element -> {
            String id = element.getAttributeNS(ANDROID_NS, "id");
            if (("@+id/" + idName).equals(id) || ("@id/" + idName).equals(id)) {
                count[0]++;
            }
        });
        return count[0];
    }

    private static Document parse(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file);
    }

    private static List<File> layoutFiles() throws Exception {
        File[] files = layoutDir().toFile().listFiles((dir, name) -> name.endsWith(".xml"));
        List<File> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                result.add(file);
            }
        }
        return result;
    }

    private static File layoutFile(String name) {
        return layoutDir().resolve(name).toFile();
    }

    private static File menuFile(String name) {
        Path fromRoot = Paths.get("app", "src", "main", "res", "menu", name);
        if (Files.exists(fromRoot)) {
            return fromRoot.toFile();
        }
        return Paths.get("src", "main", "res", "menu", name).toFile();
    }

    private static Path layoutDir() {
        Path fromRoot = Paths.get("app", "src", "main", "res", "layout");
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Paths.get("src", "main", "res", "layout");
    }
}
