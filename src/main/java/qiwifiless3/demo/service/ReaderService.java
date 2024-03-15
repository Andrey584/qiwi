package qiwifiless3.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ReaderService {

    @Value(value = "${aws.path.from}")
    private String awsPathFrom;

    public ReaderService() {
    }

    public Set<File> getAllFiles() throws FileNotFoundException {
        File rootDir = ResourceUtils.getFile(awsPathFrom);
        File[] childFiles = rootDir.listFiles();

        return getAllFiles(rootDir);

    }


    private Set<File> getAllFiles(File rootDir) {
        Set<File> result = new HashSet<>();
        if (!rootDir.isDirectory()) {
            log.warn("Директория " + rootDir.getName() + " пустая.");
            throw new RuntimeException();
        }

        List<File> childFiles = List.of(rootDir.listFiles());
        for (File childFile : childFiles) {
            if (childFile.isFile()) {
                result.add(childFile);
            } else if (childFile.isDirectory()) {
                Set<File> filesFromChildDir = getAllFiles(childFile);
                result.addAll(filesFromChildDir);
            }
        }
        return result;
    }

}
