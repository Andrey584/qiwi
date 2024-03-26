package spectrum.qf.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

@ConditionalOnProperty(value = "smb.enabled", havingValue = "true")
@Service
public class QFAwsServiceSMBImpl extends QFAwsService {
    public QFAwsServiceSMBImpl(AmazonS3 amazonS3) {
        super(amazonS3);
    }

    public void upload(QFFile file) {
        SmbFile smbFile = file.getSmbFile();
        String smbFileName = file.getSmbFile().getName();
        long weight = 0;
        try {
            weight = smbFile.length();
        } catch (SmbException e) {
            logger.error("Не удалось вычислить длину файла с именем {}", smbFile.getName());
        }
        String pathFileTo = smbFile.getCanonicalPath();
        PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucketName, smbFileName, pathFileTo);
        putObjectRequest.getRequestClientOptions().setReadLimit(1024 * 1024);
        amazonS3.putObject(putObjectRequest);
        createDatabaseLog(smbFileName);
        logger.info("Файл с именем {} весом {} байт был успешно перемещен в S3 хранилище.", smbFileName, weight);
    }
}

