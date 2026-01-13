package com.gpt.geumpumtabackend.image.dto.response;

public record ImageUploadSuccessResponse(
        String imageUrl,
        String publicId
) {
    public static ImageUploadSuccessResponse of(String imageUrl, String publicId) {
        return new ImageUploadSuccessResponse(imageUrl, publicId);
    }
}
