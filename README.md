# sqlite3-jdbc
SQLiteのJDBC

## セットアップ方法
### mavenプロジェクトに追加する方法
pom.xmlに下記を記載
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <repositories>
    <repository>
      <id>nakigao_rep</id>
      <url>https://raw.githubusercontent.com/2T-T2/nakigao-maven-repository/main/</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>t_panda.jdbc.sqlite</groupId>
      <artifactId>t_panda.jdbc.sqlite.jar</artifactId>
      <version>0.0.0.0</version>
      <scope>runtime</scope>
    </dependency>
  </dependencies>
</project>
```
### ソースからJarを生成する方法
#### 前提条件
- windows 64bit 環境であること
- 環境変数 PATH に JDKのbinフォルダを設定してあること
- 環境変数 PATH に Mavenのbinフォルダを設定してあること

ソースダウンロードする
```bat
curl -L -O "https://github.com/2T-T2/sqlite3-jdbc/archive/refs/heads/main.zip"
```
ダウンロードしたzip解凍後にprmj.batのあるフォルダに移動し下記実行
```bat
prjm.bat all
```
上記実行後に、同フォルダに<br>
 <b>rep\t_panda\compiler\t_panda.jdbc.sqlite.jar\0.0.0.0\t_panda.jdbc.sqlite.jar-0.0.0.0.jar</b> <br>
 <b>rep\t_panda\compiler\t_panda.jdbc.sqlite.jar\0.0.0.0\t_panda.jdbc.sqlite64.dll-0.0.0.0.dll</b> が生成される。<br>
<div><b><i>※pom.xml が存在しますが、mavenでのビルドは出来ません。。。</i></b></div>
生成された jar はクラスパスに追加して使用してください。
生成された dll は生成されたjarと同ディレクトリに配置して使用してください。

### 使用例
```java
import java.sql.*;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("t_panda.jdbc.sqlite.SQLiteDriver");

        try (
            Connection conn = DriverManager.getConnection("jdbc:sqlite://test2.db", null, null);
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)
        ) {
            conn.setAutoCommit(false);

            // 外部キーの有効化
            enableForeignKeys(conn);

            try {
                stmt.execute("""
                        create table class (
                           name			varchar(255)	not null,
                           constraint PK_class primary key (name)
                        );
                        create table student (
                           id			int				not null,
                           last_name	varchar(255)	not null,
                           first_name	varchar(255)	not null,
                           age			int				not null	default 0,
                           class_name   varchar(255)    not null,
                           constraint PK_student primary key (id),
                           constraint FK_student foreign key (class_name) references class(name)
                        );
                        """);
                while (stmt.getMoreResults()) {
                }

                try (ResultSet resultSet = stmt.executeQuery("select type, name, sql from sqlite_master");) {
                    while (resultSet.next()) {
                        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                            String cNm = resultSet.getMetaData().getColumnName(i);
                            System.out.print(cNm + ": " + resultSet.getString(cNm) + ", ");
                        }
                        System.out.println("");
                    }
                }

                try (PreparedStatement insStmt = conn.prepareStatement("insert into class values ( ? )")) {
                    for (var name : new String[]{"red", "blue", "yellow"}) {
                        insStmt.setString(1, name);
                        insStmt.addBatch();
                    }
                    insStmt.executeBatch();
                }

                try (PreparedStatement insStmt = conn.prepareStatement("insert into student values ( ?, ?, ?, ?, ? )")) {
                    int id = 0;
                    Object[][] insData = new Object[][]{
                            new Object[]{++id, "sasaki" , "kojirou", 20, "red"   },
                            new Object[]{++id, "suzuki" , "tarou"  , 22, "red"   },
                            new Object[]{++id, "fujiki" , "ichirou", 18, "red"   },
                            new Object[]{++id, "satou"  , "masasi" , 35, "blue"  },
                            new Object[]{++id, "yamada" , "satomi" , 10, "blue"  },
                            new Object[]{++id, "tasiro" , "naoko"  , 15, "blue"  },
                            new Object[]{++id, "torita" , "osamu"  , 42, "yellow"},
                            new Object[]{++id, "kumada" , "rumiko" ,  6, "yellow"},
                            new Object[]{++id, "nekota" , "hanako" , 12, "yellow"},
                    };

                    for (Object[] insDataRow : insData) {
                        int prmIdx = 0;
                        insStmt.setInt(++prmIdx, (int) insDataRow[0]);
                        insStmt.setString(++prmIdx, (String) insDataRow[1]);
                        insStmt.setString(++prmIdx, (String) insDataRow[2]);
                        insStmt.setInt(++prmIdx, (int) insDataRow[3]);
                        insStmt.setString(++prmIdx, (String) insDataRow[4]);
                        insStmt.addBatch();
                    }
                    insStmt.executeBatch();
                }

                try (ResultSet resultSet = stmt.executeQuery("select student.last_name, class.name from student, class where student.class_name = class.name and class.name = 'red'");) {
                    while (resultSet.next()) {
                        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                            String cNm = resultSet.getMetaData().getColumnName(i);
                            System.out.print(cNm + ": " + resultSet.getString(cNm) + ", ");
                        }
                        System.out.println("");
                    }
                }

                try (PreparedStatement insStmt = conn.prepareStatement("select student.last_name, class.name from student, class where student.class_name = class.name and class.name = ?")) {
                    insStmt.setString(1, "blue");
                    try (ResultSet resultSet = insStmt.executeQuery()) {
                        while (resultSet.next()) {
                            for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                                String cNm = resultSet.getMetaData().getColumnName(i);
                                System.out.print(cNm + ": " + resultSet.getString(cNm) + ", ");
                            }
                            System.out.println("");
                        }
                    }
                }

                // ↓ expect throw Exception "SQLITE_CONSTRAINT FOREIGN KEY constraint failed"
                stmt.executeUpdate("insert into student values ( 100, 'kawasima', 'masato', 70, 'black' )");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.out.println(e + "\n  " + String.join("\n  ", Stream.of(e.getStackTrace()).map(Object::toString).toList()));
        }
    }

    private static void enableForeignKeys(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            boolean b = conn.getAutoCommit();
            conn.setAutoCommit(false);
            stmt.executeUpdate("commit;");  // トランザクション中は外部キー有効化が出来ないため。
            stmt.executeUpdate("pragma foreign_keys = ON;");
            stmt.executeUpdate("begin;");
            conn.setAutoCommit(b);
        }
    }
}
```

### ドキュメント
[https://2t-t2.github.io/sqlite3-jdbc/](https://2t-t2.github.io/sqlite3-jdbc/)

### 補足
<div><b><i>※pom.xml が存在しますが、mavenでのビルドは出来ません。。。</i></b></div>
ビルドはprjm.batを使用して行ってください。

```bat
prjm.bat help

prjm.bat clean
    .\lib\, .\out\, .\rep\, .\dst\, .\docs\, .\src\cpp\include\, .\tmp\ をクリーンします

prjm.bat dl-depend
    .\lib\ に t_panda.jdbc.sqlite.jar.pom.xml で指定された依存ファイルをダウンロードします
    .\lib\ に t_panda.jdbc.sqlite_64.dll.pom.xml で指定された依存ファイルをダウンロードします
    .\src\cpp\, .\src\cpp\include\ に sqlite3 のソースファイルをダウンロードします

prjm.bat compile
    .\src\java\ の内容をコンパイルします
    出力先フォルダ .\out\classes\
    .\src\cpp\sqlite3.c をコンパイルします
    出力先ファイル .\out\sqlite.o
    .\src\cpp\*.cpp を .\out\sqlite.o とリンクしてコンパイルします
    出力先フォルダ .\dst\

prjm.bat archive
    .\out\classes\, .\src\java\, .\res\ をアーカイブ化してjarを作成します
    出力先ファイル名 .\dst\t_panda.jdbc.sqlite.jar

prjm.bat mvnrep
    .\dst\t_panda.jdbc.sqlite.jar, t_panda.jdbc.sqlite.jar.pom.xml からmavenリポジトリの作成を行います
    .\dst\t_panda.jdbc.sqlite.dll, t_panda.jdbc.sqlite_64.dll.pom.xml からmavenリポジトリの作成を行います

prjm.bat javadoc
    ドキュメントを生成します
    出力先フォルダ .\docs\

prjm.bat all
    clean -> dl-depend -> compile
    -> archive -> mvnrep -> javadoc の順で実行します
```
