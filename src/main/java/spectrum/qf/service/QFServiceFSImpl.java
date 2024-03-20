package spectrum.qf.service;


import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import spectrum.qf.bean.QFFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(value = "smb.enabled", havingValue = "false")
public class QFServiceFSImpl implements QFFileService {
    private static final Logger logger = LoggerFactory.getLogger(QFServiceFSImpl.class);
    private static final int MIN_LENGTH_PHONE_NUMBER = 8;
    private static final int MAX_LENGTH_PHONE_NUMBER = 15;
    private static final int MAX_COUNT_IN_ONE_DIRECTORY = 40000;
    private static final long MILLISECONDS_IN_ONE_MINUTE = 60000;

    @Value(value = "${file.root-dir}")
    private String pathFrom;
    @Value(value = "${file.dest-dir}")
    private String pathTo;
    @Value(value = "${options.delete-files}")
    private boolean deleteFiles;

    public QFServiceFSImpl() {
    }

    @Override
    public QFFile getFile() {
        try {
            File rootDir = ResourceUtils.getFile(pathFrom);
            return QFFile.of(getAnyFile(rootDir));
        } catch (FileNotFoundException e) {
            logger.error("Указанный путь не является директорией. Приложение остановлено. Проверьте параметры запуска.");
            throw new RuntimeException(e);
        }
    }

    private File getAnyFile(File directory) {
        List<File> childFiles = List.of(Objects.requireNonNull(directory.listFiles()));
        for (File childFile : childFiles) {
            String fileName = childFile.getName();
            if (childFile.isFile()
                    && childFile.length() != 0
                    && fileName.length() >= MIN_LENGTH_PHONE_NUMBER
                    && fileName.length() <= MAX_LENGTH_PHONE_NUMBER
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
    public void move(QFFile qfFile) {
        File fileFrom = qfFile.getFile();
        String fileName = fileFrom.getName();
        long fileWights = fileFrom.length();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd/");
        String dateFolderName = dateFormat.format(new Date());

        pathTo = pathTo.endsWith("/") ? pathTo : pathTo + "/";

        File dateDestinationDir = new File(pathTo + dateFolderName);
        File fileDest = new File(getPathTo(dateDestinationDir) + "/" + fileName);

        if (deleteFiles) {
            try {
                FileUtils.delete(fileFrom);
                logger.info("Файл с именем " + fileName + " весом " + fileWights + " байт был успешно удален после перемещения в S3 хранилище.");
            } catch (IOException e) {
                logger.error("Ошибка при удалении файла с именем " + fileName + ". Файл не был удален.");
            }
        } else {
            try {
                FileUtils.moveFile(fileFrom, fileDest);
                logger.info("Файл с именем " + fileName + " весом " + fileWights + " байт успешно перемещен в папку processed.");
            } catch (IOException e) {
                fileDest.delete();
                logger.info("Файл с именем " + fileName + " уже существует в папке processed. Новый файл заменит уже существующий.");
            }
        }
    }

    private File getPathTo(File dir) {
        File newDir = checkForExistence(dir);
        return getActualFolderInDir(newDir);
    }

    private File checkForExistence(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
            return createFirstFolder(dir);
        }
        return dir;
    }

    private File createFirstFolder(File dir) {
        return new File(dir.getPath() + "/1");
    }

    private File getActualFolderInDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        List<File> subFolders = List.of(Objects.requireNonNull(files));
        List<File> subFoldersList = subFolders.stream()
                .sorted((f1, f2) -> {
                    Integer f1Name = Integer.parseInt(f1.getName());
                    Integer f2Name = Integer.parseInt(f2.getName());
                    return f1Name.compareTo(f2Name);
                })
                .collect(Collectors.toList())
                .reversed();
        File actualFolder = subFoldersList.getFirst();
        return checkForMaxElementInDir(actualFolder);
    }

    private File checkForMaxElementInDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        if (files.length < MAX_COUNT_IN_ONE_DIRECTORY) {
            return dir;
        }
        int dirNumberName = Integer.parseInt(dir.getName()) + 1;
        File newDirNumber = new File(dir.getParentFile().getPath() + "/" + dirNumberName + "/");
        newDirNumber.mkdirs();
        return newDirNumber;
    }

}
