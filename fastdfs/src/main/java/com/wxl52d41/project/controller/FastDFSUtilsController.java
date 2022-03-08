package com.wxl52d41.project.controller;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.wxl52d41.project.util.FastdfsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author xlwang55
 * @date 2022/3/2 13:47
 */
@RestController
@RequestMapping("/fastdfsUtils")
public class FastDFSUtilsController {
    @Autowired
    private FastdfsUtils fastdfsUtils;

    @PostMapping("/upload")
    public String upload(MultipartFile file) throws IOException {
        StorePath storePath = fastdfsUtils.upload(file);
        String fullPath = storePath.getFullPath();
        System.out.println("fullPath = " + fullPath);
        //group1/M00/00/00/wKgAkGIfF2KAG7qSAAB-jaFjG-Q678.jpg
        return fullPath;
    }

    @GetMapping("/download")
    public void downloadFile(String fileUrl, HttpServletResponse response)
            throws IOException {
        fastdfsUtils.download(fileUrl, null, response);
    }

    @DeleteMapping("/delete")
    public void delete(String fileUrl) {
        fastdfsUtils.delete(fileUrl);
    }
}
