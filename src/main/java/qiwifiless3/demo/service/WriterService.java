package qiwifiless3.demo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Service
@Slf4j
public class WriterService {

    @Value(value = "${aws.bucket.name}")
    private String awsBucketName;
    @Value(value = "${aws.path.into}")
    private String awsPathInto;

    @Autowired
    private AmazonS3 amazonS3;

    public void uploadFilesToS3(Set<File> files) {
        for (File file : files) {
            PutObjectResult putObjectResult = amazonS3.putObject(new PutObjectRequest(awsBucketName, file.getName(), file));
            if (putObjectResult != null) {
                File destination = new File(awsPathInto);
                try {
                    FileUtils.moveFile(file, destination);
                } catch (IOException e) {
                    log.error("Файл с именем " + file.getName() + " не удалось переместить в папку processed. Файл не был добавлен в базу данных.");
                    throw new RuntimeException(e);
                }
                log.info("Файл с именем " + file.getName() + " успешно перемещен и добавлен в базу данных.");
            }
        }
    }
}

