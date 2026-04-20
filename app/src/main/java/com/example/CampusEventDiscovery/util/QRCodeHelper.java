package com.example.CampusEventDiscovery.util;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * QRCodeHelper.java
 *
 * Utility class to generate QR code Bitmaps.
 */
public class QRCodeHelper {

    /**
     * Generates a QR code Bitmap from the given content.
     *
     * @param content  The string content to encode in the QR code.
     * @param widthPx  The width of the QR code in pixels.
     * @param heightPx The height of the QR code in pixels.
     * @return A Bitmap representing the QR code.
     */
    public static Bitmap generateQRCode(String content, int widthPx, int heightPx) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, widthPx, heightPx);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
