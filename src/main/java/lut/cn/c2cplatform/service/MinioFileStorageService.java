package lut.cn.c2cplatform.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lut.cn.c2cplatform.config.MinioProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class MinioFileStorageService implements FileStorageService {
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while creating MinIO bucket", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        String objectName = sdf.format(new Date()) + UUID.randomUUID() + fileExtension;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
    }

    @Override
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }
}
