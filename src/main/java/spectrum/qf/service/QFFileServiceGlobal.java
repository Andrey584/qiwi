package spectrum.qf.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;


public abstract class QFFileServiceGlobal {

    @Value(value = "${options.delete-files}")
    protected Boolean deleteFiles;
    protected static final Logger logger = LoggerFactory.getLogger(QFFileServiceGlobal.class);
    protected static final long MAX_COUNT_FILES_IN_ONE_DIRECTORY = 40000;
    protected static final long MILLISECONDS_IN_ONE_MINUTE = 60000;


    public boolean isValidNumberPhone(String name) {
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
            logger.error("Не удалось проверить номер телефона файла с именем {} на валидность.", name);
            return false;
        }
    }
}
