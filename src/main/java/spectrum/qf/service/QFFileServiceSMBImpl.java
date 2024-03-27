package spectrum.qf.service;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import lombok.RequiredArgsConstructor;
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
public class QFFileServiceSMBImpl extends QFFileService {

    private final NtlmPasswordAuthentication auth;

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

        SmbFile dateDestinationDir;
        SmbFile smbFile = null;
        try {
            dateDestinationDir = new SmbFile(smbPathTo + dateFolderName, auth);
            smbFile = new SmbFile(getPathTo(dateDestinationDir) + "/" + fileName, auth);
        } catch (MalformedURLException | SmbException e) {
            logger.error("Возникла ошибка во время попытки перемещения файла.");
        }

        if (deleteFiles) {
            try {
                fileFrom.delete();
                logger.info("Файл с именем {} был успешно удален после копирования в S3 хранилище.", fileName);
            } catch (SmbException e) {
                logger.error("Файл с именем {} не получилось удалить после перемещения в S3 хранилище.", fileName);
            }
        } else {
            try {
                fileFrom.renameTo(smbFile);
                logger.info("Файл с именем {} успешно перемещен в папку processed после копирования в S3 хранилище.", fileName);
            } catch (SmbException e) {
                logger.info("Файл с именем {} уже существует в папке processed. Новый файл заменит уже существующий.", fileName);
                try {
                    fileFrom.delete();
                    logger.info("Файл с именем {} был успешно перезаписан в папку processed после копирования в S3 хранилище.", fileName);
                } catch (SmbException ex) {
                    logger.error("Файл с именем {} не удалось удалить из папки processed. Новый файл не заменит уже существующий.", fileName);
                }
            }
        }
    }

    private SmbFile getAnyFile(SmbFile dir) throws SmbException {
        List<SmbFile> smbFiles = List.of(Objects.requireNonNull(dir.listFiles()));
        for (SmbFile childFile : smbFiles) {
            if (childFile.isFile()
                    && childFile.length() != 0
                    && System.currentTimeMillis() - childFile.lastModified() > MILLISECONDS_IN_ONE_MINUTE) {
                if (needToValidatePhoneNumber) {
                    boolean isValidNumberPhone = isValidNumberPhone(childFile.getName());
                    if (isValidNumberPhone) {
                        return childFile;
                    }
                } else {
                    return childFile;

                }


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
        SmbFile newDir = new SmbFile(dir.getPath() + "1/", auth);
        newDir.mkdir();
        return newDir;
    }

    private SmbFile checkForExistence(SmbFile dir) throws SmbException, MalformedURLException {
        if (!dir.exists()) {
            dir.mkdir();
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
        List<SmbFile> subFoldersList = subFolders.stream()
                .sorted((f1, f2) -> {
                    Integer f1Name = Integer.parseInt(f1.getName().substring(0, f1.getName().length() - 1));
                    Integer f2Name = Integer.parseInt(f2.getName().substring(0, f2.getName().length() - 1));
                    return f2Name.compareTo(f1Name);
                })
                .toList();
        if (!subFoldersList.isEmpty()) {
            SmbFile actualFolder = subFoldersList.getFirst();
            return checkForMaxElementInDir(actualFolder);
        }
        return checkForMaxElementInDir(dir);
    }

    private SmbFile checkForMaxElementInDir(SmbFile dir) throws SmbException, MalformedURLException {
        SmbFile[] files = dir.listFiles();
        if (files == null) {
            return dir;
        }
        if (files.length < MAX_COUNT_FILES_IN_ONE_DIRECTORY) {
            return dir;
        }
        String countDir = dir.getName().substring(0, dir.getName().length() - 1);
        int dirNumberName = Integer.parseInt(countDir) + 1;
        SmbFile newDirNumber = new SmbFile(dir.getParent() + "/" + dirNumberName + "/", auth);
        newDirNumber.mkdirs();
        return newDirNumber;
    }

}


