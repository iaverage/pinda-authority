package com.itheima.pinda.file.storage;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import com.itheima.pinda.utils.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.itheima.pinda.utils.DateUtils.DEFAULT_MONTH_FORMAT_SLASH;

/**
 * @Author：freeLee
 * @Date：2022/7/6 23:16
 * @Description：阿里云OSS策略配置类
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(AbstractFileStrategy.class)
@ConditionalOnProperty(name = "pinda.file.type", value = "ALI")
public class AliOssAutoConfigure {

    /**
     * 阿里云OSS文件策略处理类
     */
    @Service
    public class AliServiceImpl extends AbstractFileStrategy {

        /**
         * 构建阿里云OSS客户端
         *
         * @return
         */
        private OSS buildClient() {
            properties = fileProperties.getAli();
            return new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
        }

        /**
         * 动态判断是否是安全协议Https
         *
         * @return
         */
        @Override
        protected String getUriPrefix() {
            if (StringUtils.isNoneEmpty(properties.getUriPrefix())) {
                return properties.getUriPrefix();
            } else {
                String prefix = properties.getEndpoint().contains("https://") ? "https://" : "http://";
                return prefix + properties.getBucketName() + StrPool.DOT + properties.getEndpoint().replaceFirst(prefix, "");
            }
        }

        /**
         * 文件上传
         *
         * @param file
         * @param multipartFile
         * @return
         */
        @Override
        public void uploadFile(File file, MultipartFile multipartFile) throws Exception {
            OSS client = buildClient();
            // 获取OSS空间名称
            String bucketName = properties.getBucketName();
            if (!client.doesBucketExist(bucketName)) {
                // 如果不存在,则创建存储空间
                client.createBucket(bucketName);
            }
            // 生成文件名
            String fileName = UUID.randomUUID().toString() + StrPool.DOT + file.getExt();
            // 日期目录
            String relativePath = Paths.get(LocalDate.now().format(DateTimeFormatter.ofPattern(DEFAULT_MONTH_FORMAT_SLASH))).toString();
            // 上传文件存储的相对路径
            String relativeFileName = relativePath + StrPool.SLASH + fileName;
            // 替换\为/
            StringUtils.replace(relativeFileName, "\\\\", StrPool.SLASH);
            StringUtils.replace(relativeFileName, "\\", StrPool.SLASH);
            // 对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentDisposition("attachment;fileName=" + file.getSubmittedFileName());
            metadata.setContentType(file.getContextType());
            // 上传请求对象
            PutObjectRequest request = new PutObjectRequest(bucketName, relativeFileName, multipartFile.getInputStream(), metadata);
            // 上传文件到阿里云OSS存储空间
            PutObjectResult result = client.putObject(request);
            // 文件上传完成后需要设置上传文件的相关信息，用于保存到数据库
            log.info("result={}", JSONObject.toJSONString(result));
            String url = getUriPrefix() + StrPool.SLASH + relativeFileName;
            url = StrUtil.replace(url, "\\\\", StrPool.SLASH);
            url = StrUtil.replace(url, "\\", StrPool.SLASH);
            // 写入文件表
            file.setUrl(url);
            file.setFilename(fileName);
            file.setRelativePath(relativePath);
            file.setGroup(result.getETag());
            file.setPath(result.getRequestId());
            // 关闭阿里云OSS客户端
            client.shutdown();
        }

        /**
         * 文件删除
         *
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            // 构建OSS客户端对象
            OSS client = buildClient();
            // 获取OSS空间名称
            String bucketName = properties.getBucketName();
            // 删除文件
            client.deleteObject(bucketName, fileDeleteDO.getRelativePath() + StrPool.SLASH + fileDeleteDO.getFileName());
            // 关闭阿里云OSS客户端
            client.shutdown();
        }
    }
}
