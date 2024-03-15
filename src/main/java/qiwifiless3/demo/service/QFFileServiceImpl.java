package qiwifiless3.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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

    @Value(value = "${aws.path.from}")
    private String awsPathFrom;
    @Value(value = "${aws.path.into}")
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
            if (childFile.isFile() && childFile.length() != 0) {
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
        File fileTo = new File(awsPathTo + fileFrom.getName());
        try {
            FileUtils.moveFile(fileFrom, fileTo);
            log.info("Файл с именем " + fileFrom.getName() + " успешно перемещен в папку processed.");
        } catch (IOException e) {
            log.error("Не удалось переместить файл с именем " + fileFrom.getName() + " в папку processed.");
            try {
                File deleteFile = new File(awsPathTo + fileFrom.getName());
                FileUtils.delete(deleteFile);
            } catch (IOException ex) {
                //todo
            }
        }
    }
}
