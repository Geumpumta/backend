package com.gpt.geumpumtabackend.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.image.dto.response.ImageUploadSuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService {

    private final Cloudinary cloudinary;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    public ImageUploadSuccessResponse uploadImage(MultipartFile image, String folderPath){

        String uploadedImagePublicId = "";
        ImageUploadSuccessResponse response = null;

        try {
            if (image == null || image.isEmpty())
                throw new BusinessException(ExceptionType.INVALID_IMAGE_FILE);

            if (image.getSize() > 1024 * 1024 * 10)
                throw new BusinessException(ExceptionType.IMAGE_SIZE_EXCEEDED);

            String contentType = image.getContentType();
            if (!ALLOWED_CONTENT_TYPES.contains(contentType))
                throw new BusinessException(ExceptionType.INVALID_IMAGE_FILE);

            Map<?, ?> result = cloudinary.uploader().upload(
                    image.getBytes(),
                    Map.of(
                            "folder", folderPath,
                            "public_id", UUID.randomUUID().toString()
                    )
            );


            if (result.get("secure_url") == null || result.get("public_id") == null)
                throw new BusinessException(ExceptionType.IMAGE_UPLOAD_FAILED);

            uploadedImagePublicId = result.get("public_id").toString();
            response = ImageUploadSuccessResponse.of(
                    result.get("secure_url").toString(),
                    result.get("public_id").toString()
            );

        }
        catch (BusinessException e) {
            rollbackImage(uploadedImagePublicId);
            throw e;
        }
        catch (Exception e) {
            rollbackImage(uploadedImagePublicId);
            throw new BusinessException(ExceptionType.IMAGE_UPLOAD_FAILED);
        }
        return response;
    }

    public void rollbackImage(String publicId){
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        catch (IOException e) {
            log.warn("이미지 롤백에 실패했습니다. publicId : {}", publicId);
        }
    }
}
