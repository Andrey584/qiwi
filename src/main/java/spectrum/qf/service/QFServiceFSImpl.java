package spectrum.qf.service;


import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
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
    private static final int MAX_COUNT_FILES_IN_ONE_DIRECTORY = 40000;
    private static final long MILLISECONDS_IN_ONE_MINUTE = 60;

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
            if (childFile.isFile()
                    && childFile.length() != 0
                    && System.currentTimeMillis() - childFile.lastModified() > MILLISECONDS_IN_ONE_MINUTE) {
                String fileName = childFile.getName();
                boolean isValidNumberPhone = isValidNumberPhone(fileName);
                if (isValidNumberPhone) {
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

    private boolean isValidNumberPhone(String name) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        int index = name.indexOf(".");
        String fileName = name;
        if (index != -1) {
            fileName = name.substring(0, name.indexOf("."));
        } else {
            return false;
        }
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(fileName, null);
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            logger.error("Не удалось проверить номер телефона файла с именем + {} на валидность.", name);
            return false;
        }
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
            boolean isDeleted = fileFrom.delete();
            if (!isDeleted)
                logger.error("Ошибка во время удаления файла с именем {}.", fileFrom.getName());
            logger.info("Файл с именем {} весом {} байт был успешно удален после перемещения в S3 хранилище.", fileName, fileWights);
        } else {
            try {
                FileUtils.moveFile(fileFrom, fileDest);
                logger.info("Файл с именем {} весом {} байт успешно перемещен в папку processed после копирования в S3 хранилище.",fileName, fileWights );
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
        if (files.length < MAX_COUNT_FILES_IN_ONE_DIRECTORY) {
            return dir;
        }
        int dirNumberName = Integer.parseInt(dir.getName()) + 1;
        File newDirNumber = new File(dir.getParentFile().getPath() + "/" + dirNumberName + "/");
        newDirNumber.mkdirs();
        return newDirNumber;
    }

}
