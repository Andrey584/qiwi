package qiwifiless3.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import qiwifiless3.demo.service.QfFileTransferService;

@SpringBootApplication
public class QFApplication implements CommandLineRunner {

    private final QfFileTransferService qfFileTransferService;

    @Autowired
    public QFApplication(QfFileTransferService qfFileTransferService) {
        this.qfFileTransferService = qfFileTransferService;
    }

    public static void main(String[] args) {
        SpringApplication.run(QFApplication.class, args);
    }

    @Override
    public void run(String... args) {
        qfFileTransferService.uploadFiles();
    }
}
