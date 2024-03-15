package qiwifiless3.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import qiwifiless3.demo.service.UtilService;

import java.io.FileNotFoundException;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class Controller {

    private final UtilService utilService;

    @GetMapping("/uploadFiles")
    public ResponseEntity<String> uploadFiles() {
        return ResponseEntity.ok(utilService.uploadFiles());
    }

}
