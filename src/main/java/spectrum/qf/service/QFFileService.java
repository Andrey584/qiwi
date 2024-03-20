package spectrum.qf.service;

import spectrum.qf.bean.QFFile;

public interface QFFileService {

    QFFile getFile();

    void move(QFFile file);

}
