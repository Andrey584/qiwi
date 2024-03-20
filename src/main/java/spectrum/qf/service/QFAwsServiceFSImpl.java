package spectrum.qf.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

import java.io.File;

@Service
@ConditionalOnProperty(value = "smb.enabled", havingValue = "false")
@RequiredArgsConstructor
public class QFAwsServiceFSImpl implements QFAwsService {

    private final AmazonS3 amazonS3;
    private static final Logger logger = LoggerFactory.getLogger(QFAwsServiceFSImpl.class);

    @Value(value = "${s3.bucket}")
    private String awsBucketName;

    @Override
    public void upload(QFFile qfFile) {
        File file = qfFile.getFile();
        long fileWight = file.length();
        amazonS3.putObject(new PutObjectRequest(awsBucketName, file.getName(), file));
        logger.info("Файл " + file.getName() + " весом " + fileWight + " байт успешно загружен в S3 хранилище.");
    }

}

