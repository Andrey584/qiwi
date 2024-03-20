package spectrum.qf.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spectrum.qf.bean.QFFile;

@Service
@RequiredArgsConstructor
public class QfFileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(QfFileTransferService.class);
    private volatile boolean isContinue = true;

    private final QFFileService qfFileService;
    private final QFAwsService qfAwsService;

    public void uploadFiles() {
        while (isContinue) {
            QFFile file = qfFileService.getFile();
            if (file == null) {
                logger.info("Нет подходящих файлов.");
                sleep();
            } else {
                if (file.getFile() != null || file.getSmbFile() != null) {
                    qfAwsService.upload(file);
                    qfFileService.move(file);
                } else {
                    logger.info("Нет подходящих файлов.");
                    sleep();
                }
            }
        }
        logger.info("Программа завершила свою работу.");
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void stop() {
        isContinue = false;
    }

}
