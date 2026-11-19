package com.artbazaar.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores user-uploaded images. The local-disk implementation can be swapped for S3 or
 * Cloudinary without touching callers.
 */
public interface FileStorageService {

    /**
     * Validates and stores an image.
     *
     * @param folder logical folder, e.g. "artworks"
     * @return the public URL path of the stored file, e.g. "/uploads/artworks/abc.jpg"
     */
    String storeImage(MultipartFile file, String folder);

    /** Deletes a file previously returned by {@link #storeImage}; ignores URLs it does not own. */
    void delete(String publicUrl);
}
