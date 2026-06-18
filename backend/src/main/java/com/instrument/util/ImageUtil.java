package com.instrument.util;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ImageUtil {

    public static final String DEFAULT_IMAGE_URL = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik02NCA0MEw1MiA1Nkw0MCA0NlYyOC41QzQwIDI3LjEyOSA0MC44OTUgMjYgNDIuNSAyNkg4NS41Qzg3LjEwNSAyNiA4OCAyNy4xMjkgODggMjguNVY0Nkw3NiA1Nkw2NCA0MFoiIGZpbGw9IiNDMEM0Q0MiLz4KPHBhdGggZD0iTTI4IDQ4SDk2VjEwMEgyOFY0OFoiIGZpbGw9IiNGRkZGRkYiLz4KPHBhdGggZD0iTTI4IDQ4SDk2VjEwMEgyOFY0OFoiIHN0cm9rZT0iI0UwRTBFNSIgc3Ryb2tlLXdpZHRoPSIyIi8+CjxjaXJjbGUgY3g9IjYyIiBjeT0iNzQiIHI9IjE0IiBmaWxsPSIjRTBFMEE1Ii8+CjxwYXRoIGQ9Ik0zNiA5Nkw0OCA4NEw2MiA5Nkw3NiA3Nkw4OCA5MEw5NiA4MlY5NkgzNloiIGZpbGw9IiNDMEM0Q0MiLz4KPC9zdmc+Cg==";

    public static final int DEFAULT_IMAGE_WIDTH = 128;
    public static final int DEFAULT_IMAGE_HEIGHT = 128;
    public static final long DEFAULT_IMAGE_SIZE = 1024L;

    public static final int MAX_IMAGE_WIDTH = 1920;
    public static final int MAX_IMAGE_HEIGHT = 1920;
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;

    private static final Map<String, String> TYPE_DEFAULT_IMAGES = new HashMap<>();

    static {
        TYPE_DEFAULT_IMAGES.put("string", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRkNGNkU4Ii8+CjxsaW5lIHgxPSIyMCIgeTE9IjMyIiB4Mj0iMTA4IiB5Mj0iMzIiIHN0cm9rZT0iI0Q5QTgiNDIiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxsaW5lIHgxPSIyMCIgeTE9IjQ4IiB4Mj0iMTA4IiB5Mj0iNDgiIHN0cm9rZT0iI0Q5QTgiNDIiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxsaW5lIHgxPSIyMCIgeTE9IjY0IiB4Mj0iMTA4IiB5Mj0iNjQiIHN0cm9rZT0iI0Q5QTgiNDIiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxsaW5lIHgxPSIyMCIgeTE9IjgwIiB4Mj0iMTA4IiB5Mj0iODAiIHN0cm9rZT0iI0Q5QTgiNDIiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxsaW5lIHgxPSIyMCIgeTE9Ijk2IiB4Mj0iMTA4IiB5Mj0iOTYiIHN0cm9rZT0iI0Q5QTgiNDIiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxlbGxpcHNlIGN4PSI2NCIgY3k9IjEwMCIgcng9IjM4IiByeT0iMTIiIGZpbGw9IiNFOEM2NTciLz4KPC9zdmc+Cg==");
        TYPE_DEFAULT_IMAGES.put("bow", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRjBGNUZCIi8+CjxwYXRoIGQ9Ik0yNCAxMDRMMTA0IDI0IiBzdHJva2U9IiM3NDRBRDIiIHN0cm9rZS13aWR0aD0iNCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxjaXJjbGUgY3g9IjI0IiBjeT0iMTA0IiByPSI4IiBmaWxsPSIjN2M0YWQyIi8+CjxwYXRoIGQ9Ik0xMDQgMjRMMTE2IDEyIiBzdHJva2U9IiM3NDRBRDIiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+CjxwYXRoIGQ9Ik0zNiA5Mkw5MiAzNiIgc3Ryb2tlPSIjQTc4QkRBIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1kYXNoYXJyYXk9IjQgNCIvPgo8L3N2Zz4K");
        TYPE_DEFAULT_IMAGES.put("pick", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRkZGRUYwIi8+CjxwYXRoIGQ9Ik02NCAyNkMzNiAyNiAyNCA0OCAyNCA2NkMyNCA4NiA0NCAxMDQgNjQgMTA4Qzg0IDEwNCAxMDQgODYgMTA0IDY2QzEwNCA0OCA5MiAyNiA2NCAyNloiIGZpbGw9IiNGN0JFQTciLz4KPHBhdGggZD0iTTY0IDI2QzM2IDI2IDI0IDQ4IDI0IDY2QzI0IDg2IDQ0IDEwNCA2NCAxMDhDODQgMTA0IDEwNCA4NiAxMDQgNjZDMTA0IDQ4IDkyIDI2IDY0IDI2WiIgc3Ryb2tlPSIjRDk3NzU2IiBzdHJva2Utd2lkdGg9IjIiLz4KPHBhdGggZD0iTTU2IDY2TDcyIDY2IiBzdHJva2U9IiNEOUM3QTciIHN0cm9rZS13aWR0aD0iMiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+Cjwvc3ZnPgo=");
        TYPE_DEFAULT_IMAGES.put("rosin", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRjNGNEYzIi8+CjxyZWN0IHg9IjM2IiB5PSI0NCIgd2lkdGg9IjU2IiBoZWlnaHQ9IjQ4IiByeD0iNCIgZmlsbD0iI0Q5Qzk4QSIvPgo8cmVjdCB4PSIzNiIgeT0iNDAiIHdpZHRoPSI1NiIgaGVpZ2h0PSIxMiIgcng9IjIiIGZpbGw9IiNCNUEwNjAiLz4KPHJlY3QgeD0iNDQiIHk9IjU4IiB3aWR0aD0iNDAiIGhlaWdodD0iMTYiIHJ4PSIyIiBmaWxsPSIjRjBFNkQxIi8+Cjwvc3ZnPgo=");
        TYPE_DEFAULT_IMAGES.put("capo", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRjBGNUZCIi8+CjxyZWN0IHg9IjI0IiB5PSI0MCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjEyIiByeD0iNiIgZmlsbD0iIzQwOUVGRiIvPgo8cmVjdCB4PSIzMiIgeT0iNTIiIHdpZHRoPSI2NCIgaGVpZ2h0PSIzNiIgcng9IjQiIGZpbGw9IiM5MEM1RlgiLz4KPHJlY3QgeD0iNDQiIHk9Ijg4IiB3aWR0aD0iNDAiIGhlaWdodD0iMTYiIHJ4PSI4IiBmaWxsPSIjNDA5RUZGLz4KPC9zdmc+Cg==");
        TYPE_DEFAULT_IMAGES.put("strap", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRjVGNUY1Ii8+CjxyZWN0IHg9IjQ0IiB5PSIyMCIgd2lkdGg9IjQwIiBoZWlnaHQ9Ijg4IiByeD0iOCIgZmlsbD0iIzk5NTZENSIvPgo8cmVjdCB4PSI0NCIgeT0iMjAiIHdpZHRoPSI0MCIgaGVpZ2h0PSIyMCIgcng9IjgiIGZpbGw9IiM3QjNEQjgiLz4KPHJlY3QgeD0iNDQiIHk9Ijg4IiB3aWR0aD0iNDAiIGhlaWdodD0iMjAiIHJ4PSI4IiBmaWxsPSIjN0IzREI4Ii8+Cjwvc3ZnPgo=");
        TYPE_DEFAULT_IMAGES.put("cleaner", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgdmlld0JveD0iMCAwIDEyOCAxMjgiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiBmaWxsPSIjRjBGOUZCIi8+CjxyZWN0IHg9IjQ0IiB5PSIzMiIgd2lkdGg9IjQwIiBoZWlnaHQ9IjY0IiByeD0iNCIgZmlsbD0iIzY3QzIzQSIvPgo8cmVjdCB4PSI0NCIgeT0iMjAiIHdpZHRoPSI0MCIgaGVpZ2h0PSIxNiIgcng9IjQiIGZpbGw9IiM1MkFCMjkiLz4KPHJlY3QgeD0iNTIiIHk9IjQ4IiB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHJ4PSIyIiBmaWxsPSIjRjBGOUZCIi8+Cjx0ZXh0IHg9IjY0IiB5PSI3OCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iI0ZBRkZGRSIgZm9udC1zaXplPSIxMCIgZm9udC1mYW1pbHk9IkFyaWFsIj5DTFVFTjwvdGV4dD4KPC9zdmc+Cg==");
        TYPE_DEFAULT_IMAGES.put("other", DEFAULT_IMAGE_URL);
    }

    private ImageUtil() {
    }

    public static boolean isValidImageUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:image/");
    }

    public static String getDefaultImageUrl(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return DEFAULT_IMAGE_URL;
        }
        return TYPE_DEFAULT_IMAGES.getOrDefault(typeCode, DEFAULT_IMAGE_URL);
    }

    public static String resolveImageUrl(String imageUrl, String typeCode) {
        if (isValidImageUrl(imageUrl)) {
            return imageUrl;
        }
        return getDefaultImageUrl(typeCode);
    }

    public static Integer resolveImageWidth(Integer width) {
        if (width == null || width <= 0) {
            return DEFAULT_IMAGE_WIDTH;
        }
        return Math.min(width, MAX_IMAGE_WIDTH);
    }

    public static Integer resolveImageHeight(Integer height) {
        if (height == null || height <= 0) {
            return DEFAULT_IMAGE_HEIGHT;
        }
        return Math.min(height, MAX_IMAGE_HEIGHT);
    }

    public static Long resolveImageSize(Long size) {
        if (size == null || size <= 0) {
            return DEFAULT_IMAGE_SIZE;
        }
        return Math.min(size, MAX_IMAGE_SIZE);
    }

    public static boolean validateImageDimension(Integer width, Integer height) {
        if (width == null || height == null) {
            return true;
        }
        return width > 0 && width <= MAX_IMAGE_WIDTH
                && height > 0 && height <= MAX_IMAGE_HEIGHT;
    }

    public static boolean validateImageSize(Long size) {
        if (size == null) {
            return true;
        }
        return size > 0 && size <= MAX_IMAGE_SIZE;
    }

    public static String formatImageSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
