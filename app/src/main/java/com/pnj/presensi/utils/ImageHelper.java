package com.pnj.presensi.utils;

import android.graphics.Bitmap;

import com.pnj.presensi.entity.azure.FaceRectangle;

import java.io.IOException;

public class ImageHelper {
    // The maximum side length of the image to detect, to keep the size of image less than 4MB.
    // Resize the image if its side length is larger than the maximum.
    private static final int IMAGE_MAX_SIDE_LENGTH = 1280;

    // Ratio to scale a detected face rectangle, the face rectangle scaled up looks more natural.
    private static final double FACE_RECT_SCALE_RATIO = 1.3;

    // Crop the face thumbnail out from the original image.
    // For better view for human, face rectangles are resized to the rate faceRectEnlargeRatio.
    public static Bitmap generateFaceThumbnail(
            Bitmap originalBitmap,
            FaceRectangle faceRectangle) throws IOException {
        FaceRectangle faceRect =
                calculateFaceRectangle(originalBitmap, faceRectangle, FACE_RECT_SCALE_RATIO);

        return Bitmap.createBitmap(
                originalBitmap, faceRect.getLeft(), faceRect.getTop(), faceRect.getWidth(), faceRect.getHeight());
    }

    // Resize face rectangle, for better view for human
    // To make the rectangle larger, faceRectEnlargeRatio should be larger than 1, recommend 1.3
    private static FaceRectangle calculateFaceRectangle(
            Bitmap bitmap, FaceRectangle faceRectangle, double faceRectEnlargeRatio) {
        // Get the resized side length of the face rectangle
        double sideLength = faceRectangle.getWidth() * faceRectEnlargeRatio;
        sideLength = Math.min(sideLength, bitmap.getWidth());
        sideLength = Math.min(sideLength, bitmap.getHeight());

        // Make the left edge to left more.
        double left = faceRectangle.getLeft()
                - faceRectangle.getWidth() * (faceRectEnlargeRatio - 1.0) * 0.5;
        left = Math.max(left, 0.0);
        left = Math.min(left, bitmap.getWidth() - sideLength);

        // Make the top edge to top more.
        double top = faceRectangle.getTop()
                - faceRectangle.getHeight() * (faceRectEnlargeRatio - 1.0) * 0.5;
        top = Math.max(top, 0.0);
        top = Math.min(top, bitmap.getHeight() - sideLength);

        // Shift the top edge to top more, for better view for human
        double shiftTop = faceRectEnlargeRatio - 1.0;
        shiftTop = Math.max(shiftTop, 0.0);
        shiftTop = Math.min(shiftTop, 1.0);
        top -= 0.15 * shiftTop * faceRectangle.getHeight();
        top = Math.max(top, 0.0);

        // Set the result.
        FaceRectangle result = new FaceRectangle(
                (int) top,
                (int) left,
                (int) sideLength,
                (int) sideLength
        );
        return result;
    }
}