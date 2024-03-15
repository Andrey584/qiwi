package qiwifiless3.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import qiwifiless3.demo.exception.ReaderException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class ReaderService {

    @Value(value = "${aws.path.from}")
    private String awsPathFrom;

    public ReaderService() {
    }

    public Set<File> getAllFilesFromDir() {
        File rootDir;
        try {
            rootDir = ResourceUtils.getFile(awsPathFrom);
            return getAllFilesFromDir(rootDir);
        } catch (FileNotFoundException e) {
            log.info("Указанная директория некорректна.");
            throw new ReaderException("Указанная директория некорректна.");
        }
    }

    private Set<File> getAllFilesFromDir(File rootDir) {
        Set<File> result = new HashSet<>();
        if (!rootDir.isDirectory()) {
            log.info("Указанный файл " + rootDir + " не является директорией.");
            throw new ReaderException("Указанный файл " + rootDir + " не является директорией.");
        }

        List<File> childFiles = List.of(Objects.requireNonNull(rootDir.listFiles()));
        for (File childFile : childFiles) {
            if (childFile.isFile() && childFile.length() != 0) {
                result.add(childFile);
            } else if (childFile.isDirectory()) {
                Set<File> filesFromChildDir = getAllFilesFromDir(childFile);
                result.addAll(filesFromChildDir);
            }
        }
        return result;
    }

}
