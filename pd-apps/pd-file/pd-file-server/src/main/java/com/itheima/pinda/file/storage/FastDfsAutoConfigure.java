package com.itheima.pinda.file.storage;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author：freeLee
 * @Date：2022/7/5 23:16
 * @Description：FastDfs策略配置类
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(FileServerProperties.class)
@ConditionalOnProperty(name = "pinda.file.type", value = "FAST_DFS")
public class FastDfsAutoConfigure {
    /**
     * FastDfs文件策略处理类
     */
    @Service
    public class FastDfsServiceImpl extends AbstractFileStrategy {

        // 注入操作FastDfs的客户端对象
        @Autowired
        private FastFileStorageClient storageClient;

        /**
         * 文件上传
         *
         * @param file
         * @param multipartFile
         * @return
         */
        @Override
        public void uploadFile(File file, MultipartFile multipartFile) throws Exception {
            // 调用FastDfs客户端对象将文件上传到FastDfs
            StorePath storePath = storageClient.uploadFile(multipartFile.getInputStream(), multipartFile.getSize(), file.getExt(), null);
            // 文件上传完成后需要设置上传文件的相关信息，用于保存到数据库
            file.setUrl(fileProperties.getUriPrefix() + storePath.getFullPath());
            file.setGroup(storePath.getGroup());
            file.setPath(storePath.getPath());
        }

        /**
         * 文件删除
         *
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            // 调用FastDfs客户端对象删除文件
            storageClient.deleteFile(fileDeleteDO.getGroup(), fileDeleteDO.getPath());
        }
    }

}
