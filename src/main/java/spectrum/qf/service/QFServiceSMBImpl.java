package spectrum.qf.service;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@ConditionalOnProperty(value = "smb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class QFServiceSMBImpl implements QFFileService {

    private final NtlmPasswordAuthentication auth;
    private static final Logger logger = LoggerFactory.getLogger(QFServiceSMBImpl.class);
    private static final int MIN_LENGTH_PHONE_NUMBER = 8;
    private static final int MAX_LENGTH_PHONE_NUMBER = 15;
    private static final int MAX_COUNT_IN_ONE_DIRECTORY = 2;
    private static final long MILLISECONDS_IN_ONE_MINUTE = 6;

    @Value(value = "${smb.from-dir}")
    private String smbPathFrom;
    @Value(value = "${smb.dest-dir}")
    private String smbPathTo;
    @Value(value = "${options.delete-files}")
    private Boolean deleteFiles;

    @Override
    public QFFile getFile() {
        try {
            SmbFile smbFile = new SmbFile(smbPathFrom, auth);
            return QFFile.of(getAnyFile(smbFile));
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла.");
        }
        return null;
    }

    @Override
    public void move(QFFile qfFile) {
        SmbFile fileFrom = qfFile.getSmbFile();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd/");
        String dateFolderName = dateFormat.format(new Date());

        String fileName = fileFrom.getName();

        smbPathTo = smbPathTo.endsWith("/") ? smbPathTo : smbPathTo + "/";

        SmbFile dateDestinationDir = null;
        try {
            dateDestinationDir = new SmbFile(smbPathTo + dateFolderName, auth);
        } catch (MalformedURLException ignored) {
            logger.error("Возникла ошибка во время попытки перемещения файла.");
        }

        SmbFile fileTo2 = null;
        try {
            fileTo2 = new SmbFile(getPathTo(dateDestinationDir) + "/" + fileName, auth);
        } catch (MalformedURLException | SmbException e) {
            logger.error("Возникла ошибка во время попытки перемещения файла.");
        }

        if (deleteFiles) {
            try {
                fileFrom.delete();
                logger.info("Файл с именем " + fileName + " весом " + fileFrom.length() + " был успешно удален после копирования в S3 хранилище.");
            } catch (SmbException e) {
                logger.error("Файл с именем " + fileName + " весом " + " не получилось удалить после перемещения в S3 хранилище.");
            }
        } else {
            try {
                fileFrom.renameTo(fileTo2);
                logger.info("Файл с именем " + fileName + " весом " + " байт успешно перемещен в папку processed после копирования в S3 хранилище.");
            } catch (SmbException e) {
                logger.error("Файл с именем " + fileFrom.getName() + " уже существует в папке processed. Новый файл заменит уже существующий.");
                try {
                    fileFrom.delete();
                    logger.info("Файл с именем " + fileFrom.getName() + " был успешно перезаписан в папку processed после копирования в S3 хранилище.");
                } catch (SmbException ex) {
                    logger.error("Файл с именем " + fileFrom.getName() + " не удалось удалить из папки processed. Новый файл не заменит уже существующий.");
                }
            }
        }
    }

    private SmbFile getAnyFile(SmbFile dir) throws SmbException {
        List<SmbFile> smbFiles = List.of(Objects.requireNonNull(dir.listFiles()));
        for (SmbFile childFile : smbFiles) {
            String fileName = childFile.getName();
            if (childFile.isFile()
                    && childFile.length() != 0
                    && fileName.length() >= MIN_LENGTH_PHONE_NUMBER
                    && fileName.length() <= MAX_LENGTH_PHONE_NUMBER
                    && System.currentTimeMillis() - childFile.lastModified() > MILLISECONDS_IN_ONE_MINUTE) {
                return childFile;
            }
        }
        for (SmbFile childFile : smbFiles) {
            if (childFile.isDirectory()) {
                return getAnyFile(childFile);
            }
        }
        return null;
    }

    private SmbFile getPathTo(SmbFile dir) throws SmbException, MalformedURLException {
        SmbFile newDir = checkForExistence(dir);
        return getActualFolderInDir(newDir);
    }

    private SmbFile createFirstFolder(SmbFile dir) throws MalformedURLException, SmbException {
        SmbFile newDir = new SmbFile(dir.getPath() + "1", auth);
        newDir.mkdirs();
        return newDir;
    }

    private SmbFile checkForExistence(SmbFile dir) throws SmbException, MalformedURLException {
        if (!dir.exists()) {
            dir.mkdirs();
            return createFirstFolder(dir);
        }
        return dir;
    }

    private SmbFile getActualFolderInDir(SmbFile dir) throws SmbException, MalformedURLException {
        SmbFile[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        List<SmbFile> subFolders = List.of(Objects.requireNonNull(files));
        List<SmbFile> list = subFolders.stream()
                .sorted((f1, f2) -> {
                    Integer f1Name = Integer.parseInt(f1.getName().substring(0, f1.getName().length() - 1));
                    Integer f2Name = Integer.parseInt(f2.getName().substring(0, f2.getName().length() - 1));
                    return f2Name.compareTo(f1Name);
                })
                .toList();
        if (!list.isEmpty()) {
            SmbFile actualFolder = list.getFirst();
            return checkForMaxElementInDir(actualFolder);
        }
        return checkForMaxElementInDir(dir);
    }

    private SmbFile checkForMaxElementInDir(SmbFile dir) throws SmbException, MalformedURLException {
        SmbFile[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        if (files.length < MAX_COUNT_IN_ONE_DIRECTORY) {
            return dir;
        }
        String countDir = dir.getName().substring(0, dir.getName().length() - 1);
        int dirNumberName = Integer.parseInt(countDir) + 1;
        SmbFile newDirNumber = new SmbFile(dir.getParent() + "/" + dirNumberName + "/", auth);
        newDirNumber.mkdirs();
        return newDirNumber;
    }

}


