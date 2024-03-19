package qiwifiless3.demo.service;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import qiwifiless3.demo.bean.QFFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(value = "smb.enabled", havingValue = "true")
public class QFRemoteFileServiceImpl implements QFFileService {
    private static final Logger logger = LoggerFactory.getLogger(QFRemoteFileServiceImpl.class);
    private static final int MIN_LENGTH_PHONE_NUMBER = 8;
    private static final int MAX_LENGTH_PHONE_NUMBER = 15;
    private static final int MAX_COUNT_IN_ONE_DIRECTORY = 40000;
    private static final long MILLISECONDS_IN_ONE_MINUTE = 60000;

    @Value(value = "${smb.domain}")
    private String smbDomain;
    @Value(value = "${smb.username}")
    private String smbUsername;
    @Value(value = "${smb.password}")
    private String smbPassword;
    @Value(value = "${smb.url}")
    private String smbUrl;
    @Value(value = "${smb.dest-dir}")
    private String smbPathTo;
    @Value(value = "${options.delete-files}")
    private Boolean deleteFiles;

    @Override
    public QFFile getFile() {
        try {
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(smbDomain, smbUsername, smbPassword);
            SmbFile smbFile = new SmbFile(smbUrl, auth);
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
            dateDestinationDir = new SmbFile(smbPathTo + dateFolderName);
        } catch (MalformedURLException ignored) {
            logger.error("Ошибка при попытке указать путь файлу");
        }

        SmbFile fileTo2 = null;
        try {
            fileTo2 = new SmbFile(getPathTo(dateDestinationDir) + "/" + fileName);
        } catch (MalformedURLException | SmbException e) {
            logger.error("Ошибка при попытке указать путь файлу.");
        }

        if (deleteFiles) {
            try {
                fileFrom.delete();
                logger.info("Файл с именем " + fileName + " весом " + fileName.length() + " был успешно удален после перемещения в S3 хранилище.");
            } catch (SmbException e) {
                logger.error("Файл с именем " + fileName + " весом " + fileName.length() + " не получилось удалить после перемещения в S3 хранилище.");
            }
        } else {
            try {
                fileFrom.renameTo(fileTo2);
                logger.info("Файл с именем " + fileName + " весом " + fileName.length() + " байт успешно перемещен в папку processed.");
            } catch (IOException e) {
                try {
                    assert fileTo2 != null;
                    fileTo2.delete();
                } catch (SmbException ex) {
                    logger.error("Файл с именем " + fileName + " не получилось удалить из папки processed. Новый файл не заменит старый.");
                }
                logger.error("Файл с именем " + fileName + " уже существует в папке processed. Новый файл заменит уже существующий.");
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

    private SmbFile createFirstFolder(SmbFile dir) throws MalformedURLException {
        return new SmbFile(dir.getPath() + "/1");
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
                    Integer f1Name = Integer.parseInt(f1.getName());
                    Integer f2Name = Integer.parseInt(f2.getName());
                    return f1Name.compareTo(f2Name);
                })
                .collect(Collectors.toList())
                .reversed();
        SmbFile actualFolder = list.getFirst();
        return checkForMaxElementInDir(actualFolder);
    }

    private SmbFile checkForMaxElementInDir(SmbFile dir) throws SmbException, MalformedURLException {
        SmbFile[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        if (files.length < MAX_COUNT_IN_ONE_DIRECTORY) {
            return dir;
        }
        int dirNumberName = Integer.parseInt(dir.getName()) + 1;
        //todo проверить как работает без getPath() у родителя
        SmbFile newDirNumber = new SmbFile(dir.getParent() + "/" + dirNumberName + "/");
        newDirNumber.mkdirs();
        return newDirNumber;
    }

}


