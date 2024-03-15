package qiwifiless3.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class QfFileTransferService {

    private final QFFileService qfFileService;
    private final QFAwsService qfAwsService;

    public void uploadFiles() {
        while (true) {
            File file = qfFileService.getFile();
            if (file != null) {
                qfAwsService.upload(file);
                qfFileService.move(file);
            } else {
                try {
                    log.info("Нет подходящих файлов.");
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
