package com.itheima.pinda.file.storage;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import com.itheima.pinda.utils.DateUtils;
import com.itheima.pinda.utils.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @Author：freeLee
 * @Date：2022/7/4 22:47
 * @Description：本地上传策略配置类
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(FileServerProperties.class)
@ConditionalOnProperty(name = "pinda.file.type", havingValue = "LOCAL")
public class LocalAutoConfigure {
    /**
     * 本地文件策略处理类
     */
    @Service
    public class LocalServiceImpl extends AbstractFileStrategy {

        private void buildClient() {
            properties = fileProperties.getLocal();
        }

        /**
         * 文件上传
         *
         * @param file
         * @param multipartFile
         * @return
         */
        @Override
        public void uploadFile(File file, MultipartFile multipartFile) throws Exception{
            buildClient();
            String endpoint = properties.getEndpoint();
            String bucketName = properties.getBucketName();
            String uriPrefix = properties.getUriPrefix();
            // 使用UUID为文件生成新文件名
            String fileName = UUID.randomUUID().toString() + StrPool.DOT + file.getExt();
            // 日期目录，例:2022/07/04
            // Paths.get()动态支持不同系统目录层级分隔符
            String relativePath = Paths.get(LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_DAY_FORMAT_SLASH))).toString();
            // 上传文件存储的绝对路径，例:/home/uploadFiles/oss-file-service/2022/07/04
            String absolutePath = Paths.get(endpoint, bucketName, relativePath).toString();
            // 目标输出文件，例:/home/uploadFiles/oss-file-service/2022/07/04/xx.doc
            java.io.File outFile = new java.io.File(Paths.get(absolutePath, fileName).toString());
            // 向目标文件写入数据
            FileUtils.writeByteArrayToFile(outFile, multipartFile.getBytes());
            // 文件上传完成后需要设置File对象的属性(url,fileName,relativePath)，用于保存到数据库
            String url = new StringBuilder(getUriPrefix())
                    .append(StrPool.SLASH)
                    .append(properties.getBucketName())
                    .append(StrPool.SLASH)
                    .append(relativePath)
                    .append(StrPool.SLASH)
                    .append(fileName)
                    .toString();
            // 替换windows环境的\路径
            url = StrUtil.replace(url, "\\\\", StrPool.SLASH);
            url = StrUtil.replace(url, "\\", StrPool.SLASH);
            file.setUrl(url);
            file.setFilename(fileName);
            file.setRelativePath(relativePath);
        }

        /**
         * 文件删除
         *
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            // 拼接要删除的文件的绝对路径
            String filePath = Paths.get(properties.getEndpoint(), properties.getBucketName(),
                    fileDeleteDO.getRelativePath(), fileDeleteDO.getFileName()).toString();
            java.io.File file = new java.io.File(filePath);
            // 使用工具类进行删除
            FileUtils.deleteQuietly(file);
        }
    }
}
