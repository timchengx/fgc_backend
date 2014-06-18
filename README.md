fgc_backend
===========
Install instruction

1. 請先確認伺服器上有安裝Java Virtual Machine，backend需要JVM才能正常運行
2. 首先需完成web端的安裝設定才能進行backend端的安裝
3. 使用在web端的資料庫管理工具(phpMyAdmin、MySQL WorkBench)在伺服器的資料庫系統中開啟新帳號以及設定以下密碼 (帳號 fgcbackend 、密碼 backend)，並且在給予此帳號對於fgc schema有著以下權限：
DELETE、INSERT、LOCK TABLES、SELECT、UPDATE
4. 前往此下載backend端主程式 https://github.com/gnehcmit/fgc_backend/releases/tag/1.0b
5. 在Terminal(Linux)或是命令提示字元下(Windows)執行以下指令，啟動backend server

```java -server -jar ./fgcbackend.jar```
