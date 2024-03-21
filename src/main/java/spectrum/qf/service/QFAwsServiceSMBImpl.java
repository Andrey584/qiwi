package spectrum.qf.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@ConditionalOnProperty(value = "smb.enabled", havingValue = "true")
@RequiredArgsConstructor
@Service
public class QFAwsServiceSMBImpl implements QFAwsService {

    private static final Logger logger = LoggerFactory.getLogger(QFFileServiceFSImpl.class);

    @Value(value = "${s3.bucket}")
    private String awsBucketName;

    private final AmazonS3 amazonS3;

    @Override
    public void upload(QFFile file) {
        SmbFile smbFile = file.getSmbFile();
        long weight = 0;
        try {
            weight = smbFile.length();
        } catch (SmbException e) {
            logger.error("Не удалось вычислить длину файла с именем {}", smbFile.getName());
        }
        String pathFileTo = smbFile.getCanonicalPath();
        File fileTo = new File(pathFileTo);
        PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucketName, file.getSmbFile().getName(), fileTo);
        putObjectRequest.getRequestClientOptions().setReadLimit(1024 * 1024);
        amazonS3.putObject(putObjectRequest);
        logger.info("Файл с именем {} весом {} байт был успешно перемещен в S3 хранилище.", smbFile.getName(), weight);
    }
}

