# Приложение для переноса файлов в хранилище S3.

### При старте приложения указанная директория будет просканирована на наличие файлов, удовлетворяющих следующие условия:
### 1) имя файла (без расширения) является валидным номером мобильного телефона.
### 2) файл не пустой.
### 3) после последнего изменения файла прошло не менее 60 секунд.

### Настройки запуска:
    file: (указывается в случае чтения файлов с локального хранилища)
        root-dir: C:/.../
        dest-dir: C:/.../processed/

    s3: (настройки S3 хранилища)
        url: url хранилища
        bucket: имя корзины
        accessKey: accessKey
        secretKey: secretKey
        region: ap-southeast-1

    smb: (указывается в случае чтения файлов по протоколу SMB)
        enabled: указывается true в случае, если чтение происходит по протоколу SMB и false в случае чтения с локального хранилища
        protocol:
            version: версия SMB-протокола (одной цифрой)
        from-dir: smb://jenkins:jenkins@bkp01.it-spectrum.ru/smb-tst/smb/
        domain: домен
        username: имя пользователя
        password: пароль
        service-name: сервис
        dest-dir: путь папки для файлов, перенесенных в S3 хранилище (формат: smb://username:password@server.domain.ru/service/folder), например smb://jenkins:jenkins@bkp01.it-spectrum.ru/smb-tst/processed/)
    
    options: (указывается для настройки удаления/перемещения файлов, перемещенных в S3 хранилище)
        delete-files: true в случае, если после перемещения в S3 мы хотим удалять эти файлы, false в случае, если мы хотим их переносить в "dest-dir".

### Приложение с момента запуска крутится в цикле. В случае, если не найдено ни одного файла, подходящего по условиям, приложение на короткое время "зависает", после чего продолжает работу. Приложение не способно полностью завершить работу самостоятельно.


### Все успешные (info) и неуспешные (error) операции логируются в файлы info.log и error.log соответственно.
