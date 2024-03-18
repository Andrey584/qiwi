package qiwifiless3.demo.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class QfFileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(QfFileTransferService.class);
    private volatile boolean isContinue = true;

    private final QFFileService qfFileService;
    private final QFAwsService qfAwsService;

    public void uploadFiles() {
        while (isContinue) {
            File file = qfFileService.getFile();
            if (file != null) {
                qfAwsService.upload(file);
                qfFileService.move(file);
            } else {
                try {
                    logger.info("Нет подходящих файлов.");
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        logger.info("Программа завершила свою работу.");
    }

    @PreDestroy
    public void stop() {
        isContinue = false;
    }

}

