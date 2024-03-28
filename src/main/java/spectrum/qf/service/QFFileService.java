package spectrum.qf.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.annotation.PostConstruct;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import spectrum.qf.bean.QFFile;

import java.io.File;


public abstract class QFFileService {

    @Value(value = "${file.root-dir}")
    protected String pathFrom;
    @Value(value = "${file.dest-dir}")
    protected String pathTo;
    @Value(value = "${smb.from-dir}")
    protected String smbPathFrom;
    @Value(value = "${smb.dest-dir}")
    protected String smbPathTo;
    @Value(value = "${options.delete-files}")
    protected Boolean deleteFiles;
    @Value(value = "${options.validation-phone-number}")
    protected Boolean isNeedToValidatePhoneNumber;

    protected static final Logger logger = LoggerFactory.getLogger(QFFileService.class);
    protected static final long MAX_COUNT_FILES_IN_ONE_DIRECTORY = 50000;
    protected static final long MILLISECONDS_IN_ONE_MINUTE = 60000;

    public abstract QFFile getFile();

    public abstract void move(QFFile file);

    protected boolean isValidNumberPhone(String fileName) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String phoneNumber = getPhoneNumberFromFileName(fileName);
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            logger.error("Номер телефона из имени файла {} невалидный. Переименуйте файл в корректный формат.", fileName);
            return false;
        }
    }

    protected boolean checkFileForConditions(File file) {
        return file.isFile()
                && file.length() != 0
                && System.currentTimeMillis() - file.lastModified() > MILLISECONDS_IN_ONE_MINUTE;
    }

    protected boolean checkSmbFileForConditions(SmbFile smbFile) throws SmbException {
        return smbFile.isFile()
                && smbFile.length() != 0
                && System.currentTimeMillis() - smbFile.lastModified() > MILLISECONDS_IN_ONE_MINUTE;
    }

    private String getPhoneNumberFromFileName(String fileName) {
        String filePhoneNumberFromFileName = fileName.replaceAll("[^0-9]", "");
        filePhoneNumberFromFileName = filePhoneNumberFromFileName.startsWith("8") ? filePhoneNumberFromFileName.replaceFirst("8", "+7") : filePhoneNumberFromFileName;
        return filePhoneNumberFromFileName.startsWith("+") ? filePhoneNumberFromFileName : "+" + filePhoneNumberFromFileName;
    }
}
