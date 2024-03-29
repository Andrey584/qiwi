package spectrum.qf.service;


import org.apache.commons.io.FileUtils;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(value = "smb.enabled", havingValue = "false")
public class QFFileServiceFSImpl extends QFFileService {

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
        Optional<List<File>> childFilesOptional = Optional.of(List.of(Objects.requireNonNull(directory.listFiles())));
        if (childFilesOptional.get().isEmpty()) {
            if (!pathFrom.equalsIgnoreCase(directory.getPath())) {
                String directoryPath = directory.getPath();
                String checkPathFrom = pathFrom.endsWith("\\") || pathFrom.endsWith("/") ? pathFrom.substring(0, pathFrom.length() - 1) : pathFrom;
                String checkDirectoryPath = directoryPath.endsWith("\\") || directoryPath.endsWith("/") ? directoryPath.substring(0, directoryPath.length() - 1) : directoryPath;

                if (!checkPathFrom.equalsIgnoreCase(checkDirectoryPath)) {
                    directory.delete();
                    return getAnyFile(directory.getParentFile());
                }
            }
        }
        List<File> childFiles = childFilesOptional.get();
        for (File childFile : childFiles) {
            if (checkFileForConditions(childFile)) {
                if (isNeedToValidatePhoneNumber) {
                    boolean isValidNumberPhone = isValidNumberPhone(childFile.getName());
                    if (isValidNumberPhone) {
                        return childFile;
                    }
                } else {
                    return childFile;
                }
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
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd/");
        String dateFolderName = dateFormat.format(new Date());

        pathTo = pathTo.endsWith("/") ? pathTo : pathTo + "/";

        File dateDestinationDir = new File(pathTo + dateFolderName);
        File fileDest = new File(getPathTo(dateDestinationDir) + "/" + fileName);

        if (deleteFiles) {
            boolean isDeleted = fileFrom.delete();
            if (!isDeleted)
                logger.error("Ошибка во время удаления файла с именем {}.", fileName);
            logger.info("Файл с именем {} был успешно удален после перемещения в S3 хранилище.", fileName);
        } else {
            try {
                FileUtils.moveFile(fileFrom, fileDest);
                logger.info("Файл с именем {} успешно перемещен в папку processed после копирования в S3 хранилище.", fileName);
            } catch (IOException e) {
                logger.info("Файл с именем {} уже существует в папке processed. Новый файл заменит уже существующий.", fileName);
                boolean isDeleted = fileDest.delete();
                if (!isDeleted) {
                    logger.error("Ошибка во время удаления файла с именем {}. Новый файл не заменит уже существующий.", fileName);
                }
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
        File newDir = new File(dir.getPath() + "/1/");
        newDir.mkdirs();
        return newDir;
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
        if (!subFoldersList.isEmpty()) {
            File actualFolder = subFoldersList.getFirst();
            return checkForMaxElementInDir(actualFolder);
        }
        return checkForMaxElementInDir(dir);
    }

    private File checkForMaxElementInDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        if (files.length < MAX_COUNT_FILES_IN_ONE_DIRECTORY) {
            return dir;
        }
        int dirNumberName = Integer.parseInt(dir.getName()) + 1;
        File newDirNumber = new File(dir.getParentFile().getPath() + "/" + dirNumberName + "/");
        newDirNumber.mkdirs();
        return newDirNumber;
    }

}
