package qiwifiless3.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilService {

    private final ReaderService readerService;
    private final WriterService writerService;

    public String uploadFiles() {
        Set<File> allFiles = readerService.getAllFilesFromDir();

        writerService.uploadFilesToS3(allFiles);

        return "Все файлы успешно перемещены!";
    }


}
