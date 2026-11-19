package com.artbazaar.storage;

import com.artbazaar.config.AppProperties;
import com.artbazaar.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/** Saves uploads under app.storage.upload-dir; WebConfig serves that directory at /uploads/**. */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    static final String URL_PREFIX = "/uploads/";
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path root;

    public LocalFileStorageService(AppProperties props) {
        this.root = Path.of(props.storage().uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + root, e);
        }
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image must be 5 MB or smaller");
        }
        // Trust the file's bytes, not its name or the browser-supplied content type.
        ImageType type = detect(file);
        if (type == null) {
            throw new BadRequestException("Only JPEG, PNG or WebP images are allowed");
        }

        String name = UUID.randomUUID() + "." + type.extension;
        Path dir = root.resolve(folder).normalize();
        Path target = dir.resolve(name);
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid upload folder");
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(dir);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save uploaded image", e);
        }
        return URL_PREFIX + folder + "/" + name;
    }

    @Override
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(URL_PREFIX)) {
            return; // e.g. bundled seed images under /images/seed
        }
        Path file = root.resolve(publicUrl.substring(URL_PREFIX.length())).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete {}", file, e);
        }
    }

    private static ImageType detect(MultipartFile file) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < head.length) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return Arrays.stream(ImageType.values()).filter(t -> t.matches(head)).findFirst().orElse(null);
    }

    enum ImageType {
        JPEG("jpg"), PNG("png"), WEBP("webp");

        final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }

        boolean matches(byte[] h) {
            return switch (this) {
                case JPEG -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
                case PNG -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
                case WEBP -> h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            };
        }
    }
}
