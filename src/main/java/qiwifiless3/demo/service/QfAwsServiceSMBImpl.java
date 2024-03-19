package qiwifiless3.demo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jcifs.smb.SmbFile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import qiwifiless3.demo.bean.QFFile;

import java.io.IOException;
import java.io.InputStream;

@ConditionalOnProperty(value = "smb.enabled", havingValue = "true")
@RequiredArgsConstructor
@Service
public class QfAwsServiceSMBImpl implements QFAwsService {

    private static final Logger logger = LoggerFactory.getLogger(QFLocalFileServiceImpl.class);

    @Value(value = "${s3.bucket}")
    private String awsBucketName;

    private final AmazonS3 amazonS3;

    @Override
    public void upload(QFFile file) {
        SmbFile smbFile = file.getSmbFile();
        try (InputStream inputStream = smbFile.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(awsBucketName, smbFile.getName(), inputStream, null));

        } catch (IOException e) {
            logger.error("Файл с именем " + smbFile.getName() + " не удалось переместить в S3 хранилище.");
        }
    }
}
