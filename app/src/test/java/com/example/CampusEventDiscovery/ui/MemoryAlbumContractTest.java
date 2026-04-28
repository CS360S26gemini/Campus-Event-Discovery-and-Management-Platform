package com.example.CampusEventDiscovery.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

public class MemoryAlbumContractTest {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Test
    public void memoryCard_hasViewAndAddPhotoActions() throws Exception {
        Document document = parse(layoutFile("item_memory.xml"));

        assertEquals(1, countId(document, "btnViewMemoryPhotos"));
        assertEquals(1, countId(document, "btnAddMemoryPhotos"));
    }

    @Test
    public void memoryAlbumScreen_hasToolbarGridAndEmptyState() throws Exception {
        Document document = parse(layoutFile("activity_memory_album.xml"));

        assertEquals(1, countId(document, "toolbarMemoryAlbum"));
        assertEquals(1, countId(document, "rvMemoryAlbumPhotos"));
        assertEquals(1, countId(document, "tvEmptyMemoryAlbum"));
    }

    @Test
    public void memoryAlbumActivity_isRegisteredAndUsesGridAdapter() throws Exception {
        String manifest = readManifest();
        String activity = readJava("ui/profile/MemoryAlbumActivity.java");
        String memories = readJava("ui/profile/MemoriesActivity.java");

        assertTrue(manifest.contains(".ui.profile.MemoryAlbumActivity"));
        assertTrue(activity.contains("new GridLayoutManager(this, 3)"));
        assertTrue(activity.contains("MemoryPhotoGridAdapter"));
        assertTrue(memories.contains("new Intent(this, MemoryAlbumActivity.class)"));
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

    private static Document parse(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file.toFile());
    }

    private static Path layoutFile(String name) {
        Path fromRoot = Paths.get("app", "src", "main", "res", "layout", name);
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Paths.get("src", "main", "res", "layout", name);
    }

    private static String readJava(String name) throws Exception {
        Path fromRoot = Paths.get("app", "src", "main", "java", "com", "example", "CampusEventDiscovery", name);
        if (Files.exists(fromRoot)) {
            return readUtf8(fromRoot);
        }
        return readUtf8(Paths.get("src", "main", "java", "com", "example", "CampusEventDiscovery", name));
    }

    private static String readManifest() throws Exception {
        Path fromRoot = Paths.get("app", "src", "main", "AndroidManifest.xml");
        if (Files.exists(fromRoot)) {
            return readUtf8(fromRoot);
        }
        return readUtf8(Paths.get("src", "main", "AndroidManifest.xml"));
    }

    private static String readUtf8(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
