package qiwifiless3.demo.service;

import jcifs.smb.SmbException;
import qiwifiless3.demo.bean.QFFile;

import java.io.File;
import java.net.MalformedURLException;

public interface QFFileService {

    QFFile getFile();

    void move(QFFile file);

}
