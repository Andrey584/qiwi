package spectrum.qf.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

import java.io.File;

@Service
@ConditionalOnProperty(value = "smb.enabled", havingValue = "false")
public class QFAwsServiceFSImpl extends QFAwsService {

    public QFAwsServiceFSImpl(AmazonS3 amazonS3) {
        super(amazonS3);
    }

    @Override
    public void upload(QFFile qfFile) {
        File file = qfFile.getFile();
        String fileName = file.getName();
        long fileWight = file.length();
        PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucketName, fileName, file);
        putObjectRequest.getRequestClientOptions().setReadLimit(1024 * 1024);
        amazonS3.putObject(new PutObjectRequest(awsBucketName, file.getName(), file));
        createDatabaseLog(fileName);
        logger.info("Файл с именем {} весом {} байт успешно загружен в S3 хранилище.", fileName, fileWight);
    }

}

