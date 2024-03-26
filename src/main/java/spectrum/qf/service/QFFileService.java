package spectrum.qf.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import spectrum.qf.bean.QFFile;


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
    protected static final Logger logger = LoggerFactory.getLogger(QFFileService.class);
    protected static final long MAX_COUNT_FILES_IN_ONE_DIRECTORY = 40000;
    protected static final long MILLISECONDS_IN_ONE_MINUTE = 60000;


    protected boolean isValidNumberPhone(String name) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        int index = name.indexOf(".");
        String fileName;
        if (index != -1) {
            fileName = name.substring(0, name.indexOf("."));
        } else {
            return false;
        }
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(fileName, null);
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            logger.error("Номер телефона из имени файла {} невалидный. Переименуйте файл в корректный формат.", name);
            return false;
        }
    }

    public abstract QFFile getFile();

    public abstract void move(QFFile file);
}
