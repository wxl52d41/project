# 什么是FastDFS？

```markup
FastDFS是用C语言编写的一款开源的轻量级分布式文件系统。它对文件进行管理，功能包括：文件存
储、文件同步、文件访问（文件上传、文件下载）等，解决了大容量存储和负载均衡的问题。特别适合
以文件为载体的在线服务，如相册网站、视频网站等等。
FastDFS为互联网量身定制，充分考虑了冗余备份、负载均衡、线性扩容等机制，并注重高可用、高性
能等指标，使用FastDFS很容易搭建一套高性能的文件服务器集群提供文件上传、下载等服务。
```

# FastDFS特性

 - 分组存储，灵活简洁、对等结构，不存在单点 文件不分块存储，上传的文件和OS文件系统中的文件一一对应
 - 文件ID由FastDFS生成，作为文件访问凭证，FastDFS不需要传统的name server 和流行的web server无缝衔接，FastDFS已提供apache和nginx扩展模块
 - 中、小文件均可以很好支持，支持海量小文件存储
 - 支持多块磁盘，支持单盘数据恢复
 - 支持相同内容的文件只保存一份，节约磁盘空间
 - 支持在线扩容 支持主从文件
 - 存储服务器上可以保存文件属性（meta-data）V2.0网络通信采用libevent，支持大并发访问，整体性能更好
 - 下载文件支持多线程方式，支持断点续传

# 1.引入jar依赖

```xml
        <dependency>
            <groupId>com.github.tobato</groupId>
            <artifactId>fastdfs-client</artifactId>
            <version>1.26.6</version>
        </dependency>
```

# 2.配置application.yml

```yml
server:
  port: 10086

# 分布式文件系统FDFS配置
fdfs:
  connect-timeout: 600  # 连接超时时间
  so-timeout: 1500      # 读取超时时间
  tracker-list: 127.0.0.1:22122  # tracker服务配置地址列表，替换成自己服务的IP地址，支持多个
  pool:
    jmx-enabled: false
  thumb-image:
    height: 150       # 缩略图的宽高
    width: 150
```

# 3.上传下载工具类

```java
@Component
public class FastdfsUtils {

    public static final String DEFAULT_CHARSET = "UTF-8";

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    /**
     * 上传文件并返回文件路径
     *
     * @param file
     * @return 文件路径
     * @throws IOException
     */
    public StorePath upload(MultipartFile file) throws IOException {
        // 设置文件信息
        Set<MetaData> mataData = new HashSet<>();
        mataData.add(new MetaData("author", "fastdfs"));
        mataData.add(new MetaData("description", file.getOriginalFilename()));
        // 上传
        StorePath storePath = fastFileStorageClient.uploadFile(
                file.getInputStream(), file.getSize(),
                FilenameUtils.getExtension(file.getOriginalFilename()),
                null);
        return storePath;
    }

    /**
     * 根据全路径删除
     *
     * @param path "fullPath": "group1/M00/00/00/wKgAkGIfF2KAG7qSAAB-jaFjG-Q678.jpg"
     */
    public void delete(String path) {
        fastFileStorageClient.deleteFile(path);
    }

    /**
     * 根据组和路径删除
     * "group": "group1",
     * "path": "M00/00/00/wKgAkGIfF2KAG7qSAAB-jaFjG-Q678.jpg",
     * "fullPath": "group1/M00/00/00/wKgAkGIfF2KAG7qSAAB-jaFjG-Q678.jpg"
     *
     * @param group
     * @param path
     */
    public void delete(String group, String path) {
        fastFileStorageClient.deleteFile(group, path);
    }

    /**
     * 文件下载
     *
     * @param path     文件路径，例如：/group1/path=M00/00/00/itstyle.png
     * @param filename 下载的文件命名
     * @return
     */
    public void download(String path, String filename, HttpServletResponse response) throws IOException {
        // 获取文件
        StorePath storePath = StorePath.parseFromUrl(path);
        if (StringUtils.isBlank(filename)) {
            filename = FilenameUtils.getName(storePath.getPath());
        }
        byte[] bytes = fastFileStorageClient.downloadFile(storePath.getGroup(), storePath.getPath(), new DownloadByteArray());
        response.reset();

        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, DEFAULT_CHARSET));
        response.setCharacterEncoding(DEFAULT_CHARSET);
        // 设置强制下载不打开
//        response.setContentType("application/force-download");
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
```

# 4.控制器类

```java
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
```

# 5.使用postman 进行测试

## 5.1上传

```markup
http://localhost:10086/fastdfsUtils/upload
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/d9a0286bb3974ef5a7057d453af47915.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)

## 5.2 下载

```markup
http://localhost:10086/fastdfsUtils/download
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/fddb4035e09a44eaa4cfeef3dd66b37a.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)

