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

public class VendorManagementContractTest {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Test
    public void vendorManagementLayout_hasOrganizerSelectionAndVendorDetailSurfaces() throws Exception {
        Document document = parse(layoutFile("fragment_vendor_management.xml"));

        assertEquals(1, countId(document, "toolbarVendorManagement"));
        assertEquals(1, countId(document, "rvVendorEvents"));
        assertEquals(1, countId(document, "rvVendorProposals"));
        assertEquals(1, countId(document, "cardVendorToggle"));
        assertEquals(1, countId(document, "btnVendorApproved"));
        assertEquals(1, countId(document, "btnVendorPending"));
        assertEquals(1, countId(document, "btnVendorRejected"));
        assertEquals(1, countId(document, "fabAddVendor"));
    }

    @Test
    public void adminDashboard_hasVendorRequestsEntryPoint() throws Exception {
        Document document = parse(layoutFile("fragment_home_admin.xml"));

        assertEquals(1, countId(document, "btnVendorRequests"));
        assertEquals(1, countId(document, "btnPendingApprovals"));
        assertEquals(1, countId(document, "btnRejectedEvents"));
    }

    @Test
    public void vendorProposalItem_hasAdminReviewActionsAndStatus() throws Exception {
        Document document = parse(layoutFile("item_vendor_proposal.xml"));

        assertEquals(1, countId(document, "tvVendorName"));
        assertEquals(1, countId(document, "chipVendorStatus"));
        assertEquals(1, countId(document, "layoutVendorActions"));
        assertEquals(1, countId(document, "btnApproveVendor"));
        assertEquals(1, countId(document, "btnRejectVendor"));
    }

    @Test
    public void mainActivity_replacesAdminOrganizerFavouritesWithVendors() throws Exception {
        String source = readUtf8(javaFile("MainActivity.java"));

        assertTrue(source.contains("favouritesItem.setTitle(R.string.vendors)"));
        assertTrue(source.contains("navigateTo(\"vendors\", R.id.nav_favourites)")
                || source.contains("navigateToFromBottomNav(\"vendors\", R.id.nav_favourites)"));
        assertTrue(source.contains("VendorManagementFragment.newInstance(currentRole)"));
    }

    @Test
    public void organizerBottomNavActionOpensCreateEventActivity() throws Exception {
        String source = readUtf8(javaFile("MainActivity.java"));

        assertTrue(source.contains("UserRoles.isOrganizer(currentRole)"));
        assertTrue(source.contains("actionItem.setIcon(R.drawable.ic_add)"));
        assertTrue(source.contains("actionItem.setTitle(R.string.create_event)"));
        assertTrue(source.contains("startActivity(new Intent(this, CreateEventActivity.class))"));
    }

    @Test
    public void organizerVendorFlow_startsOnEventSelectionThenOpensEventVendorDetail() throws Exception {
        String source = readUtf8(javaFile("ui/vendors/VendorManagementFragment.java"));

        assertTrue(source.contains("openOrganizerEventDetail(event)"));
        assertTrue(source.contains("showOrganizerEventSelection()"));
        assertTrue(source.contains("rvProposals.setVisibility(admin ? View.VISIBLE : View.GONE)"));
        assertTrue(source.contains("cardVendorToggle.setVisibility(admin ? View.VISIBLE : View.GONE)"));
        assertTrue(source.contains("fabAddVendor.setVisibility(View.GONE)"));
    }

    @Test
    public void adminHomeObservesUnreadVendorProposalCount() throws Exception {
        String source = readUtf8(javaFile("ui/home/HomeAdminFragment.java"));

        assertTrue(source.contains("observeUnreadPendingVendorProposalCount"));
        assertTrue(source.contains("vendor_requests_with_count"));
        assertTrue(source.contains("openVendorManagement()"));
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

    private static Path javaFile(String name) {
        Path fromRoot = Paths.get("app", "src", "main", "java", "com", "example", "CampusEventDiscovery", name);
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Paths.get("src", "main", "java", "com", "example", "CampusEventDiscovery", name);
    }

    private static String readUtf8(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
