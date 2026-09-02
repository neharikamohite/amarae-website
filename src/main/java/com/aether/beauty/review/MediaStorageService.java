package com.aether.beauty.review;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Saves review photos/videos and hands back the public URL the browser
 * loads them from.
 *
 * Two backends, auto-selected:
 *  - Cloudinary, when a CLOUDINARY_URL env var is set (recommended — free
 *    tier, CDN-backed, survives redeploys).
 *  - Local disk, as a fallback so the feature still works before Cloudinary
 *    is configured. IMPORTANT: on Render's default (non-persistent-disk)
 *    web-service plans, local disk is wiped on every redeploy/restart —
 *    fine for testing, not for real customer photos. See amarae-website
 *    setup notes for how to get a CLOUDINARY_URL.
 */
@Service
public class MediaStorageService {
  private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);
  private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
  private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
  private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024; // 8 MB
  private static final long MAX_VIDEO_BYTES = 60L * 1024 * 1024; // 60 MB
  private static final int MAX_FILES_PER_REVIEW = 6;

  private final Cloudinary cloudinary; // null when CLOUDINARY_URL isn't set
  private final Path localStorageDir;
  private final String localPublicPath;

  public MediaStorageService(
    @Value("${CLOUDINARY_URL:}") String cloudinaryUrl,
    @Value("${aether.media.storage-dir:uploads/reviews}") String storageDir,
    @Value("${aether.media.public-path:/media/reviews}") String publicPath
  ) {
    this.cloudinary = buildCloudinary(cloudinaryUrl);
    this.localStorageDir = Path.of(storageDir);
    this.localPublicPath = publicPath;
    if (this.cloudinary == null) {
      try {
        Files.createDirectories(this.localStorageDir);
      } catch (IOException ex) {
        throw new UncheckedIOException("Could not create media storage directory: " + storageDir, ex);
      }
    }
  }

  /**
   * A malformed CLOUDINARY_URL used to throw straight out of the
   * constructor and take the whole Spring context down with it — the app
   * would fail to start on Render and the previous successful deploy would
   * keep serving instead. Now a bad value just falls back to local disk
   * storage and logs a clear warning, so a typo in an env var can never
   * block a deploy again.
   */
  private static Cloudinary buildCloudinary(String cloudinaryUrl) {
    if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
      return null;
    }
    try {
      return new Cloudinary(cloudinaryUrl.trim());
    } catch (RuntimeException ex) {
      log.warn(
        "CLOUDINARY_URL is set but could not be parsed ({}). Falling back to local disk storage for review media. "
          + "Expected format: cloudinary://<api_key>:<api_secret>@<cloud_name>",
        ex.getMessage()
      );
      return null;
    }
  }

  public boolean usingCloudStorage() {
    return cloudinary != null;
  }

  public int maxFilesPerReview() {
    return MAX_FILES_PER_REVIEW;
  }

  public StoredMedia store(MultipartFile file) {
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    MediaType mediaType;
    long maxBytes;
    if (IMAGE_TYPES.contains(contentType)) {
      mediaType = MediaType.IMAGE;
      maxBytes = MAX_IMAGE_BYTES;
    } else if (VIDEO_TYPES.contains(contentType)) {
      mediaType = MediaType.VIDEO;
      maxBytes = MAX_VIDEO_BYTES;
    } else {
      throw new IllegalArgumentException(
        "Unsupported file type: " + contentType + ". Please upload JPG/PNG/WEBP/GIF photos or MP4/WEBM/MOV videos."
      );
    }
    if (file.getSize() > maxBytes) {
      throw new IllegalArgumentException(
        mediaType == MediaType.IMAGE
          ? "Each photo must be 8 MB or smaller."
          : "Each video must be 60 MB or smaller."
      );
    }

    return cloudinary != null ? storeToCloudinary(file, mediaType) : storeToLocalDisk(file, mediaType, contentType);
  }

  @SuppressWarnings("unchecked")
  private StoredMedia storeToCloudinary(MultipartFile file, MediaType mediaType) {
    try {
      Map<String, Object> uploadResult = cloudinary
        .uploader()
        .upload(
          file.getBytes(),
          ObjectUtils.asMap(
            "folder", "amarae/reviews",
            "resource_type", mediaType == MediaType.VIDEO ? "video" : "image"
          )
        );
      String secureUrl = (String) uploadResult.get("secure_url");
      return new StoredMedia(mediaType, secureUrl);
    } catch (IOException ex) {
      throw new UncheckedIOException("Could not upload to Cloudinary", ex);
    }
  }

  private StoredMedia storeToLocalDisk(MultipartFile file, MediaType mediaType, String contentType) {
    String extension = extensionFor(contentType);
    String filename = UUID.randomUUID() + extension;
    Path target = localStorageDir.resolve(filename);
    try {
      file.transferTo(target);
    } catch (IOException ex) {
      throw new UncheckedIOException("Could not save uploaded file", ex);
    }
    return new StoredMedia(mediaType, localPublicPath + "/" + filename);
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      case "video/mp4" -> ".mp4";
      case "video/webm" -> ".webm";
      case "video/quicktime" -> ".mov";
      default -> "";
    };
  }

  public record StoredMedia(MediaType mediaType, String url) {}
}
