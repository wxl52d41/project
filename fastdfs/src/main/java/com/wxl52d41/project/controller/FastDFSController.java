package com.wxl52d41.project.controller;


import com.wxl52d41.project.service.FastDFSClientService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/fastdfs")
public class FastDFSController {
    @Autowired
    private FastDFSClientService fastDFSClientService;

    @PostMapping("/upload")
    public String uploadFile(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String originalFileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String fileName = file.getName();
        long fileSize = file.getSize();
        System.out.println(originalFileName + ":" + fileName + ":" + fileSize +
                ":" + extension + ":" + bytes.length);
        return fastDFSClientService.uploadFile(bytes, fileSize, extension);
        //group1/M00/00/00/wKgAkGIexaOAKdugAAB-jaFjG-Q904.jpg
    }

    @PostMapping("/download")
    public void downloadFile(String fileUrl, HttpServletResponse response)
            throws IOException {
        byte[] bytes = fastDFSClientService.downloadFile(fileUrl);
        response.setHeader("Content-disposition", "attachment;filename=" +
                URLEncoder.encode(fileUrl, "UTF-8"));
        response.setCharacterEncoding("UTF-8");
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}