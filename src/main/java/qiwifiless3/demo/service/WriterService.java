package qiwifiless3.demo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import qiwifiless3.demo.exception.WriterException;

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
        if (files.isEmpty()) {
            log.info("Не найдено файлов, подходящих для переноса в S3 хранилище");
            throw new WriterException("Не найдено файлов, подходящих для переноса в S3 хранилище.");
        }
        for (File file : files) {
            amazonS3.putObject(new PutObjectRequest(awsBucketName, file.getName(), file));//перемещаем в 3 хранилище
            File fileFrom = new File(file.getPath());
            File fileTo = new File(awsPathInto + file.getName());
            try {
                FileUtils.moveFile(fileFrom, fileTo); //перемещаем файл из исходной папки в папку processed
                log.info("Файл с именем " + file.getName() + " успешно перемещен в папку processed и добавлен в базу данных.");
            } catch (IOException e) {
                log.error("Файл с именем " + file.getName() + " не удалось переместить в папку processed. Файл не был добавлен в S3 хранилище."
                        + " " + e.getMessage());
                throw new WriterException("Файл с именем " + file.getName() + " не удалось переместить в папку processed." +
                        "Файл не был добавлен в S3 хранилище." + " " + e.getMessage());
            }

        }
    }
}

