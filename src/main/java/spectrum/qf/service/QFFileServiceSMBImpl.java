package spectrum.qf.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
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
public class QFFileServiceSMBImpl implements QFFileService {

    private final NtlmPasswordAuthentication auth;
    private static final Logger logger = LoggerFactory.getLogger(QFFileServiceSMBImpl.class);
    private static final int MAX_COUNT_IN_ONE_DIRECTORY = 40000;
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

        SmbFile smbFile = null;
        try {
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
                String fileName = childFile.getName();
                boolean isValidPhoneNumber = isValidNumberPhone(fileName);
                if (isValidPhoneNumber) {
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
            logger.error("Не удалось проверить номер телефона файла с именем {} на валидность.", name);
            return false;
        }
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


