package qiwifiless3.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import qiwifiless3.demo.service.QfFileTransferService;

@SpringBootApplication
@RequiredArgsConstructor
public class QFApplication implements CommandLineRunner {

    private final QfFileTransferService qfFileTransferService;

    public static void main(String[] args) {
        SpringApplication.run(QFApplication.class, args);
    }

    @Override
    public void run(String... args) {
        qfFileTransferService.uploadFiles();
    }

}
