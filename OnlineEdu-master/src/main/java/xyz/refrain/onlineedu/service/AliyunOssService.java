package xyz.refrain.onlineedu.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 本地文件存储服务 (替代 Aliyun Oss)
 *
 * @author Gemini CLI
 */
@Service
@Slf4j
public class AliyunOssService {

    @Value("${storage.local-path}")
    private String localPath;

    @Value("${storage.access-path}")
    private String accessPath;

    /**
     * 上传文件到本地
     *
     * @param file 文件
     * @return 文件访问路径，失败返回空字符串
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String mainName = FileNameUtil.mainName(originalFilename);
            String extName = FileNameUtil.extName(originalFilename);
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String newFileName = mainName + "-" + uuid + "." + extName;

            // 确保目录存在
            File destDir = new File(localPath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            // 保存文件
            File destFile = new File(destDir, newFileName);
            file.transferTo(destFile);
            
            log.info("文件上传至本地成功: {}", destFile.getAbsolutePath());
            return accessPath + newFileName;
        } catch (IOException e) {
            log.error("上传文件到本地失败", e);
        }
        return "";
    }

    /**
     * 根据 url 删除本地文件
     */
    public boolean delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return false;
        }
        try {
            String fileName = getOssFileKey(fileUrl);
            File file = new File(localPath, fileName);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            log.error("删除本地文件失败", e);
        }
        return false;
    }

    /**
     * 根据 url 获得文件名
     */
    public String getOssFileKey(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return "";
        }
        int i = fileUrl.lastIndexOf('/');
        if (i >= 0) {
            return fileUrl.substring(i + 1);
        }
        return fileUrl;
    }

}
