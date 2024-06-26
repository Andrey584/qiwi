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
import java.util.Optional;

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
            logger.error("Исходная папка по указанному пути не существует. Проверьте исходный каталог на наличие.");
        }
        return null;
    }

    @Override
    public void move(QFFile qfFile) {
        checkExistRootFolder();
        SmbFile fileFrom = qfFile.getSmbFile();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd/");
        String dateFolderName = dateFormat.format(new Date());

        String fileName = fileFrom.getName();

        smbPathTo = smbPathTo.endsWith("/") ? smbPathTo : smbPathTo + "/";

        SmbFile dateDestinationDir;
        SmbFile smbFile = null;
        try {
            dateDestinationDir = new SmbFile(smbPathTo + dateFolderName, auth);
            smbFile = new SmbFile(getPathTo(dateDestinationDir) + fileName, auth);
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
                logger.info("Файл с именем {} успешно перемещен в папку {} после копирования в S3 хранилище.", fileName, destDirFolderName);
            } catch (SmbException e) {
                logger.info("Файл с именем {} уже существует в папке {}. Новый файл заменит уже существующий.", fileName, destDirFolderName);
                try {
                    smbFile.delete();
                    fileFrom.renameTo(smbFile);
                    logger.info("Файл с именем {} был успешно перезаписан в папку {} после копирования в S3 хранилище.", fileName, destDirFolderName);
                } catch (SmbException ex) {
                    logger.error("Файл с именем {} не удалось удалить из папки {}. Новый файл не заменит уже существующий.", fileName, destDirFolderName);
                }
            }
        }
    }

    private SmbFile getAnyFile(SmbFile directory) throws SmbException, MalformedURLException {
        String dirPath = directory.getCanonicalPath().endsWith("/") ? directory.getCanonicalPath() : directory.getCanonicalPath() + "/";
        Optional<List<SmbFile>> smbFilesOptional = Optional.of(List.of(Objects.requireNonNull(new SmbFile(dirPath, auth).listFiles())));
        if (smbFilesOptional.get().isEmpty()) {
            smbPathFrom = smbPathFrom.endsWith("/") ? smbPathFrom : smbPathFrom + "/";
            if (!smbPathFrom.equalsIgnoreCase(dirPath)) {
                directory.delete();
                checkExistRootFolder();
                SmbFile smbFile = new SmbFile(directory.getParent() + "/", auth);
                return getAnyFile(smbFile);
            }
        }
        List<SmbFile> smbFiles = smbFilesOptional.get();
        for (SmbFile childFile : smbFiles) {
            if (checkSmbFileForConditions(childFile)) {
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

    private void checkExistRootFolder() {
        SmbFile smbFile;
        try {
            smbFile = new SmbFile(smbPathFrom, auth);
            if (!smbFile.exists()) {
                logger.error("Исходной папки по указанному пути не существует. Проверьте исходный каталог на наличие.");
                throw new RuntimeException("Исходной папки по указанному пути не существует. Проверьте исходный каталог на наличие");
            }
        } catch (SmbException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }
}


