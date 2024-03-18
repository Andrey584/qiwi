# Приложение для переноса файлов из локального хранилища в хранилище S3. 

### Необходимо установить настройки (данные) в файле application.yml.

### При старте приложения указанная директория будет просканирована на наличие файлов, удовлетворяющих следующие условия:
### 1) имя файла состоит из номера телефона (+расширение) - не мнее 8 символов и не более 15
### 2) файл не пустой
### 3) последние изменения файла были сделаны не раньше, чем 60 секунд назад.


## После переноса файла из локального хранилище в хранилище S3 исходный файл будет перемещен в папку 'processed'.

## Приложение с момента запуска крутится в цикле. В случае, если не найдено ни одного файла, подходящего по условиям, приложение на короткое время "зависает", после чего продолжает работу. Приложение не способно полностью завершить работу самостоятельно.


## Все успешные (info) и неуспешные (error) операции логируются в файлы info.log и error.log соответственно.
