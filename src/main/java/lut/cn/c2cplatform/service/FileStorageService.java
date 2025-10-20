package lut.cn.c2cplatform.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file) throws Exception;
    void deleteFile(String objectName) throws Exception;
}
