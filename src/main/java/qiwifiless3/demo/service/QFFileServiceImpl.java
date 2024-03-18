package qiwifiless3.demo.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class QFFileServiceImpl implements QFFileService {
    private static final Logger logger = LoggerFactory.getLogger(QFFileServiceImpl.class);

    private static final int MIN_LENGTH_PHONE_NUMBER = 8;
    private static final int MAX_LENGTH_PHONE_NUMBER = 15;
    private static final long MILLISECONDS_IN_ONE_MINUTE = 60000;

    @Value(value = "${file.root-dir}")
    private String awsPathFrom;
    @Value(value = "${file.dest-dir}")
    private String awsPathTo;

    public QFFileServiceImpl() {
    }

    @Override
    public File getFile() {
        try {
            File rootDir = ResourceUtils.getFile(awsPathFrom);
            return getAnyFile(rootDir);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private File getAnyFile(File directory) {
        List<File> childFiles = List.of(Objects.requireNonNull(directory.listFiles()));
        for (File childFile : childFiles) {
            String fileName = childFile.getName();
            if (childFile.isFile()
                    && childFile.length() != 0
                    && fileName.length() >= MIN_LENGTH_PHONE_NUMBER && fileName.length() <= MAX_LENGTH_PHONE_NUMBER
                    && System.currentTimeMillis() - childFile.lastModified() > MILLISECONDS_IN_ONE_MINUTE) {
                return childFile;
            }
        }
        for (File childFile : childFiles) {
            if (childFile.isDirectory()) {
                return getAnyFile(childFile);
            }
        }
        return null;
    }

    @Override
    public void move(File fileFrom) {
        String fileName = fileFrom.getName();
        File fileTo = new File(awsPathTo + fileName);
        try {
            FileUtils.moveFile(fileFrom, fileTo);
            logger.info("Файл с именем " + fileName + " успешно перемещен в папку processed.");
        } catch (IOException e) {
            //log.error("Не удалось переместить файл с именем " + fileName + " в папку processed.");
            logger.error("Файл с именем " + fileName + " не удалось переместить в папку processed.");
        }
    }
}
