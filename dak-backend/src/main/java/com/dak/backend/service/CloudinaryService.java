package com.dak.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Uploads generated images to Cloudinary.
 *
 * Images were previously uploaded by hand and their URLs pasted into forms,
 * which works for a handful of rental photographs and does not for artwork
 * produced on every card render.
 */
@Service
public class CloudinaryService {

    private static final Logger log =
            LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final boolean enabled;

    public CloudinaryService(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret
    ) {

        this.enabled =
                !cloudName.isBlank()
                        && !apiKey.isBlank()
                        && !apiSecret.isBlank();

        this.cloudinary = enabled
                ? new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true
                ))
                : null;

        if (!enabled) {
            log.warn(
                    "Cloudinary is not configured; generated images will not be stored."
            );
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Stores an image and returns where it now lives.
     *
     * The folder groups by kind rather than by article, because artwork
     * accumulates on every render and a folder per article would produce
     * hundreds of folders holding one file each.
     */
    public StoredImage upload(
            byte[] imageBytes,
            String folder
    ) {

        if (!enabled) {
            throw new IllegalStateException(
                    "Cloudinary is not configured."
            );
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "format", "png"
                    )
            );

            return new StoredImage(
                    String.valueOf(result.get("secure_url")),
                    String.valueOf(result.get("public_id"))
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to upload image to Cloudinary.",
                    e
            );
        }
    }

    /**
     * Removes a stored image.
     *
     * Failure is logged rather than thrown: the caller has usually just
     * replaced the asset, and leaving an orphan behind is a smaller problem
     * than failing the request that produced its replacement.
     */
    public void delete(String publicId) {

        if (!enabled || publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );

        } catch (Exception e) {
            log.warn(
                    "Could not delete Cloudinary asset {}: {}",
                    publicId,
                    e.getMessage()
            );
        }
    }

    public record StoredImage(
            String imageUrl,
            String publicId
    ) {}
}