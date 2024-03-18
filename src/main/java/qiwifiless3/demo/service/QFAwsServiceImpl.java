package qiwifiless3.demo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class QFAwsServiceImpl implements QFAwsService {

    private static final Logger logger = LoggerFactory.getLogger(QFAwsServiceImpl.class);

    @Value(value = "${s3.bucket}")
    private String awsBucketName;

    @Autowired
    private AmazonS3 amazonS3;

    @Override
    public void upload(File file) {
        long fileWight = file.length();
        amazonS3.putObject(new PutObjectRequest(awsBucketName, file.getName(), file));
        logger.info("Файл " + file.getName() + " весом " + fileWight +  " байт успешно загружен в S3 хранилище.");
    }

}

