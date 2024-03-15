package qiwifiless3.demo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class QFAwsServiceImpl implements QFAwsService {

    @Value(value = "${aws.bucket.name}")
    private String awsBucketName;
    @Value(value = "${aws.path.into}")
    private String awsPathInto;

    @Autowired
    private AmazonS3 amazonS3;

    @Override
    public void upload(File file) {
        amazonS3.putObject(new PutObjectRequest(awsBucketName, file.getName(), file));
        log.info("Файл " + file.getName() + " успешно загружен в S3 хранилище.");
    }

}

