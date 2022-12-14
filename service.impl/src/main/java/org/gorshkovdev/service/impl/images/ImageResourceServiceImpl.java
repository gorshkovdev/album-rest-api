package org.gorshkovdev.service.impl.images;

import java.io.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gorshkovdev.service.configuration.ImageConfiguration;
import org.gorshkovdev.service.exception.*;
import org.gorshkovdev.service.images.*;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

/**
 * @author gorshkovdev
 * @version 1.0-SNAPSHOT
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ImageResourceServiceImpl implements ImageResourceService {

  private final ImageConfiguration configuration;
  private final ImageCrudService crudService;

  private final Map<String, String> base64Cache = new HashMap<>();

  @Override
  public ImageResourceServiceResponse<Resource> getResource(long id) {
    Image image = crudService.getImageById(id);

    String filename = image.getFilename();

    return new ImageResourceServiceResponse<>(image, getResourceUnsafe(filename));
  }

  @Override
  public ImageResourceServiceResponse<String> getBase64(long id) {
    Image image = crudService.getImageById(id);

    String filename = image.getFilename();

    String base64 = base64Cache.computeIfAbsent(filename, (key) -> {
      try (InputStream inputStream = getResourceUnsafe(filename).getInputStream()) {
        byte[] allBytes = inputStream.readAllBytes();

        return Base64Utils.encodeToString(allBytes);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    return new ImageResourceServiceResponse<>(image, base64);
  }

  @Override
  public Image create(String filename, String contentType, Resource body) {
    checkBadContentType(contentType);

    try (InputStream inputStream = body.getInputStream()) {
      String parentDirectories = configuration.getParentDirectories();
      FileUtils.createParentDirectories(new File(parentDirectories));

      File file = !StringUtils.isBlank(parentDirectories)
          ? new File(parentDirectories + "/" + filename)
          : new File(filename);

      FileUtils.copyInputStreamToFile(inputStream, file);
      return crudService.createImage(filename, contentType, body.contentLength());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(long id) {
    Image image = crudService.deleteImageById(id);

    String filename = image.getFilename();

    boolean filenameIsUnused = crudService.countImagesByFilename(filename) == 0;
    if (filenameIsUnused) {

      base64Cache.remove(filename);

      Resource resource = getResource(filename);
      if (resource.exists()) {
        try {

          File file = resource.getFile();
          if (!file.delete()) {
            String absolutePath = file.getAbsolutePath();
            log.error("can't delete {}", absolutePath);
            throw new IOException();
          }

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private Resource getResourceUnsafe(String filename) {
    Resource resource = getResource(filename);
    if (resource.exists()) {
      return resource;
    }
    throw new ResourceNotExistsException(filename);
  }

  private Resource getResource(String filename) {
    String parentDirectories = configuration.getParentDirectories();

    File file = !StringUtils.isBlank(parentDirectories)
        ? new File(parentDirectories + "/" + filename)
        : new File(filename);

    return new PathResource(file.getPath());
  }

  private void checkBadContentType(String contentType) {
    boolean isBad = !StringUtils.equalsAny(contentType, configuration.getContentTypes());
    if (isBad) {
      throw new BadContentTypeException(contentType);
    }
  }
}
