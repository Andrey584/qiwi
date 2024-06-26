package spectrum.qf.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

@Service
@RequiredArgsConstructor
public class QFFileTransferService {

    @Value(value = "${options.cycle}")
    private Boolean inCycle;

    private static final long SLEEP_IN_MILLISECONDS = 180000;
    private static final Logger logger = LoggerFactory.getLogger(QFFileTransferService.class);
    private volatile boolean isContinue = true;

    private final QFFileService qfFileService;
    private final QFAwsService qfAwsService;

    public void uploadFiles() {
        while (isContinue) {
            QFFile file = qfFileService.getFile();
            if (file == null || (file.getFile() == null && file.getSmbFile() == null)) {
                logger.info("Нет подходящих файлов.");
                if (inCycle) {
                    sleep();
                } else {
                    logger.info("Приложение завершило свою работу.");
                    System.exit(0);
                }
            } else {
                qfAwsService.upload(file);
                qfFileService.move(file);
            }
        }
        logger.info("Программа завершила свою работу.");
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEP_IN_MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        isContinue = false;
    }

}

