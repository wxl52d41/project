package com.wxl52d41.project.util;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * fastdfs 文件上传工具类
 * 参考地址：https://blog.52itstyle.vip/archives/4837/
 * @author jianzhang11
 */
@Slf4j
@Component
public class FastdfsUtil {
    @Autowired
    private FastFileStorageClient storageClient;


    /**
     * 上传文件并返回文件路径
     * @param file
     * @return 文件路径
     * @throws IOException
     */
    public String upload(MultipartFile file) throws IOException {

        StorePath storePath = storageClient.uploadFile(
                file.getInputStream(), file.getSize(),
                FilenameUtils.getExtension(file.getOriginalFilename()),
                null);

        return storePath.getFullPath();
    }

    /**
     * 下载文件
     *
     * @param fileUrl 文件URL
     * @return 文件字节
     */
    public byte[] downloadFile(String fileUrl){
        String group = fileUrl.substring(0, fileUrl.indexOf("/"));
        String path = fileUrl.substring(fileUrl.indexOf("/") + 1);
        DownloadByteArray downloadByteArray = new DownloadByteArray();
        byte[] bytes = storageClient.downloadFile(group, path, downloadByteArray);
        return bytes;
    }

    /**
     * 删除文件
     * @param fileUrl
     */
    public void deleteFile(String fileUrl){
        String group = fileUrl.substring(0, fileUrl.indexOf("/"));
        String path = fileUrl.substring(fileUrl.indexOf("/") + 1);
        storageClient.deleteFile(group,path);
    }

}
