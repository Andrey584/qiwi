package qiwifiless3.demo.bean;

import jcifs.smb.SmbFile;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class QFFile {
    private File file;
    private SmbFile smbFile;

    private QFFile() {
    }

    public QFFile(File file) {
        this.file = file;
    }

    public QFFile(SmbFile smbFile) {
        this.smbFile = smbFile;
    }

    public static QFFile of(File file) {
        return new QFFile(file);
    }

    public static QFFile of(SmbFile smbFile) {
        return new QFFile(smbFile);
    }

}
