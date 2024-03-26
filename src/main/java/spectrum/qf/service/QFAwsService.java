package spectrum.qf.service;

import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import spectrum.qf.bean.QFFile;

import java.sql.*;

@RequiredArgsConstructor
public abstract class QFAwsService {

    @Value(value = "${s3.bucket}")
    protected String awsBucketName;
    @Value(value = "${db.url}")
    protected String dbUrl;
    @Value(value = "${db.username}")
    protected String username;
    @Value(value = "${db.password}")
    protected String password;
    @Value(value = "${db.table-name}")
    protected String dbName;

    protected final AmazonS3 amazonS3;
    protected static final Logger logger = LoggerFactory.getLogger(QFAwsService.class);

    abstract void upload(QFFile qfFile);

    protected void createDatabaseLog(String fileName) {
        try (Connection connection = DriverManager.getConnection(dbUrl, username, password)) {
            String sql = "INSERT INTO " + dbName + " (name, create_time) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, fileName);
            Timestamp createTime = new Timestamp(System.currentTimeMillis());
            statement.setTimestamp(2, createTime);

            statement.executeUpdate();

        } catch (SQLException e) {
            logger.error("Не удалось записать лог в Базу Данных для файла с именем " + fileName);
        }
    }
}
