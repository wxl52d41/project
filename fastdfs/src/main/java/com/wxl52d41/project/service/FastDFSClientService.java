package com.wxl52d41.project.service;

import java.io.IOException;

public interface FastDFSClientService {

    String uploadFile(byte[] bytes, long fileSize, String extension);


     byte[] downloadFile(String fileUrl) throws IOException;
}
