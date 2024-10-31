setlocal enabledelayedexpansion

@echo off

cd %~dp0

If %PROCESSOR_ARCHITECTURE:~-2%==64     set ARCHITECTURE=64
If not %PROCESSOR_ARCHITECTURE:~-2%==64 set ARCHITECTURE=32

rem コンパイル、アーカイブ化、ドキュメント生成の対象モジュール名
set mdl_name=t_panda.jdbc.sqlite

rem 一時フォルダ
set tmp_dir=.\tmp\
rem 出力フォルダ
set out_dir=.\out\
set dst_dir=.\dst\
set doc_dir=.\docs\
rem mavenリポジトリ作成フォルダ
set rep_dir=.\rep\
set rep_dir_uri=file:///%CD:\=/%%rep_dir:\=/%
rem 依存ライブラリ格納フォルダ
set lib_dir=.\lib\

rem java
set java_src_dir=.\src\java\
set jar_res_dir=.\res\
set java_src_enc=utf8
set classes_dir=%out_dir%classes\
set jar_name=t_panda.jdbc.sqlite.jar
set jar_pom_name=t_panda.jdbc.sqlite.jar.pom.xml

rem C++
set cpp_src_dir=.\src\cpp\
set cpp_include_dir=.\src\cpp\include\
set sqlite3_obj_name=sqlite.o
set sqlite3_obj_out_dir=%out_dir%
set dll_name=t_panda.jdbc.sqlite.dll
set dll_pom_name=t_panda.jdbc.sqlite_%ARCHITECTURE%.dll.pom.xml

rem エラーメッセージ
set error_msg=0

REM ========== 処理開始 ==========

if "%~1"=="all" (
echo.
    call :clean
echo.
    call :init
echo.
    call :dl-depend
echo.
    call :compile
echo.
    call :archive
echo.
    call :mvnrep
echo.
    call :javadoc
echo.
    goto :end
echo.
)

set help_disp_flg=false
if "%1"=="help"  set help_disp_flg=true
if "%1"=="/help" set help_disp_flg=true
if "%1"=="-help" set help_disp_flg=true
if "%1"=="-h"    set help_disp_flg=true
if "%1"==""      set help_disp_flg=true

if "%help_disp_flg%"=="true" (
    echo.

    echo %~nx0 clean
    echo     %lib_dir%, %out_dir%, %rep_dir%, %dst_dir%, %doc_dir%, %cpp_include_dir%, %tmp_dir% をクリーンします
    echo.
    echo %~nx0 dl-depend
    echo     %lib_dir% に %jar_pom_name% で指定された依存ファイルをダウンロードします
    echo     %lib_dir% に %dll_pom_name% で指定された依存ファイルをダウンロードします
    echo     %cpp_src_dir%, %cpp_include_dir% に sqlite3 のソースファイルをダウンロードします
    echo.
    echo %~nx0 compile
    echo     %java_src_dir% の内容をコンパイルします
    echo     出力先フォルダ %classes_dir%
    echo     %cpp_src_dir%sqlite3.c をコンパイルします
    echo     出力先ファイル %sqlite3_obj_out_dir%%sqlite3_obj_name%
    echo     %cpp_src_dir%*.cpp を %sqlite3_obj_out_dir%%sqlite3_obj_name% とリンクしてコンパイルします
    echo     出力先フォルダ %dst_dir%
    echo.
    echo %~nx0 archive
    echo     %classes_dir%, %java_src_dir%, %jar_res_dir% をアーカイブ化してjarを作成します
    echo     出力先ファイル名 %dst_dir%%jar_name%
    echo.
    echo %~nx0 mvnrep
    echo     %dst_dir%%jar_name%, %jar_pom_name% からmavenリポジトリの作成を行います
    echo     %dst_dir%%dll_name%, %dll_pom_name% からmavenリポジトリの作成を行います
    echo.
    echo %~nx0 javadoc
    echo     ドキュメントを生成します
    echo     出力先フォルダ %doc_dir%
    echo.
    echo %~nx0 all
    echo     clean -^> dl-depend -^> compile
    echo     -^> archive -^> mvnrep -^> javadoc の順で実行します
    echo.

    goto :end
    echo.
)

call :init
call :%1
goto :end

:init
    REM フォルダ作成
    If not exist %tmp_dir%      mkdir %tmp_dir%
    If not exist %out_dir%      mkdir %out_dir%
    If not exist %classes_dir%  mkdir %classes_dir%
    If not exist %dst_dir%      mkdir %dst_dir%
    If not exist %doc_dir%      mkdir %doc_dir%
    If not exist %java_src_dir% mkdir %java_src_dir%
    If not exist %lib_dir%      mkdir %lib_dir%
    If not exist %jar_res_dir%  mkdir %jar_res_dir%
    If not exist %rep_dir%      mkdir %rep_dir%
    If not exist %java_src_dir%t_panda.jdbc.sqlite mkdir %java_src_dir%t_panda.jdbc.sqlite

    If not exist %cpp_src_dir% mkdir %cpp_src_dir%
    If not exist %cpp_include_dir% mkdir %cpp_include_dir%
    copy %JAVA_HOME%\include\*.h %cpp_include_dir%
    copy %JAVA_HOME%\include\win32\*.h %cpp_include_dir%

    REM module-info.java 作成
    REM echo %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo module t_panda.jdbc.sqlite { > %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo     // requires /* transitive */ 依存する外部モジュール; >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo     // opens 外部モジュールからリフレクションでアクセスを許可するパッケージ; >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo     // exports 外部モジュールからアクセスを許可するパッケージ; >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo } >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
exit /b

:clean
    echo =============== クリーン開始 ===============
    del /s /q %tmp_dir%
    del /s /q %out_dir%
    del /s /q %dst_dir%
    del /s /q %doc_dir%
    del /s /q %lib_dir%
    del /s /q %rep_dir%
    del /s /q %cpp_include_dir%
    rmdir /s /q %tmp_dir%
    rmdir /s /q %out_dir%
    rmdir /s /q %dst_dir%
    rmdir /s /q %doc_dir%
    rmdir /s /q %lib_dir%
    rmdir /s /q %rep_dir%
    rmdir /s /q %cpp_include_dir%
    mkdir %tmp_dir%
    mkdir %out_dir%
    mkdir %dst_dir%
    mkdir %doc_dir%
    mkdir %lib_dir%
    mkdir %rep_dir%
    mkdir %cpp_include_dir%
    echo =============== クリーン終了 ===============
exit /b

:dl-depend
    echo =============== 依存ファイルダウンロード開始 ===============
    REM 依存ファイルをディレクトリを指定してダウンロード
    set mvnDlDependCmd=call mvn dependency:copy-dependencies -f %jar_pom_name% -DoutputDirectory=%lib_dir%
    echo %mvnDlDependCmd%
    %mvnDlDependCmd%
    if %errorlevel% neq 0 (
        set error_msg=依存ファイルダウンロードエラー
        goto :echo_error
    )
    REM 依存ファイルをディレクトリを指定してダウンロード
    set mvnDlDependCmd=call mvn dependency:copy-dependencies -f %dll_pom_name% -DoutputDirectory=%lib_dir%
    echo %mvnDlDependCmd%
    %mvnDlDependCmd%
    if %errorlevel% neq 0 (
        set error_msg=依存ファイルダウンロードエラー
        goto :echo_error
    )

    rem SQLite のソースファイルをダウンロードする
    set sqliteSourceZipDlCmd=curl -o %tmp_dir%sqlite-amalgamation-3460100.zip https://www.sqlite.org/2024/sqlite-amalgamation-3460100.zip
    set NEED_DL_SQLITE_SRC=FALSE
    If not exist %tmp_dir%sqlite-amalgamation-3460100\sqlite3.c set NEED_DL_SQLITE_SRC=TRUE
    If not exist %tmp_dir%sqlite-amalgamation-3460100\sqlite3.h set NEED_DL_SQLITE_SRC=TRUE
    If %NEED_DL_SQLITE_SRC% equ TRUE (
        echo %sqliteSourceZipDlCmd%
        %sqliteSourceZipDlCmd%
        if %errorlevel% neq 0 (
            set error_msg=依存ファイルダウンロードエラー
            goto :echo_error
        )
        powershell.exe -Command "Expand-Archive -Path '%tmp_dir%sqlite-amalgamation-3460100.zip' -DestinationPath '%tmp_dir%'"
        if %errorlevel% neq 0 (
            set error_msg=SQLite Source Zip 解凍エラー
            goto :echo_error
        )
        copy %tmp_dir%sqlite-amalgamation-3460100\sqlite3.c %cpp_src_dir%
        copy %tmp_dir%sqlite-amalgamation-3460100\sqlite3.h %cpp_include_dir%
    )
    echo =============== 依存ファイルダウンロード終了 ===============
exit /b

:compile
    echo =============== コンパイル開始 ===============
    set javacCmd=javac^
        -d %classes_dir%^
        -encoding %java_src_enc%^
        -parameters^
        -h %cpp_include_dir%^
        -Xlint:all^
        --module-source-path %java_src_dir%^
        --module %mdl_name%^
        --module-path %lib_dir%
    echo %javacCmd%
    %javacCmd%
    if %errorlevel% neq 0 (
        set error_msg=コンパイルエラー
        goto :echo_error
    )

    set ccCmd=gcc %cpp_src_dir%sqlite3.c -c -o %sqlite3_obj_out_dir%%sqlite3_obj_name%
    if not exist %sqlite3_obj_out_dir%%sqlite3_obj_name% (
        echo %ccCmd%
        %ccCmd%
        if %errorlevel% neq 0 (
            set error_msg=コンパイルエラー
            goto :echo_error
        )
    )

    set ccCmd=g++ -shared^
        src\cpp\*.cpp^
        %sqlite3_obj_out_dir%%sqlite3_obj_name%^
        -I%cpp_include_dir%^
        -Wl,--add-stdcall-alias^
        -o %dst_dir%%dll_name%
    echo %ccCmd%
    %ccCmd%
    if %errorlevel% neq 0 (
        set error_msg=コンパイルエラー
        goto :echo_error
    )
    echo =============== コンパイル終了 ===============
exit /b

:archive
    echo =============== アーカイブ化開始 ===============
    set jarCmd=jar^
        -cf %dst_dir%%jar_name%^
        -C %classes_dir%%mdl_name% .^
        -C %java_src_dir%%mdl_name% .^
        META-INF ^
        %jar_res_dir%
    echo %jarCmd%
    %jarCmd%
    if %errorlevel% neq 0 (
        set error_msg=アーカイブ化エラー
        goto :echo_error
    )
    echo =============== アーカイブ化終了 ===============
exit /b

:mvnrep
    echo =============== リポジトリ作成開始 ===============
    REM 指定ファイル(jarとpom)を指定ディレクトリにデプロイ
    set mvnDeployCmd=call mvn deploy:deploy-file -Dfile=%dst_dir%%jar_name% -Durl=%rep_dir_uri% -DpomFile=%jar_pom_name% -Dpackaging=jar
    echo %mvnDeployCmd%
    %mvnDeployCmd%
    if %errorlevel% neq 0 (
        set error_msg=リポジトリ作成エラー
        goto :echo_error
    )
    REM 指定ファイル(dllとpom)を指定ディレクトリにデプロイ
    set mvnDeployCmd=call mvn deploy:deploy-file -Dfile=%dst_dir%%dll_name% -Durl=%rep_dir_uri% -DpomFile=%dll_pom_name% -Dpackaging=dll
    echo %mvnDeployCmd%
    %mvnDeployCmd%
    if %errorlevel% neq 0 (
        set error_msg=リポジトリ作成エラー
        goto :echo_error
    )
    echo =============== リポジトリ作成終了 ===============
exit /b

:javadoc
    echo =============== ドキュメント生成開始 ===============
    set javadocCmd=javadoc ^
        --allow-script-in-comments ^
        -d %doc_dir% ^
        -encoding utf8^
        --module-source-path %java_src_dir%^
        --module %mdl_name%^
        --module-path %lib_dir%^
        -header "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.15.10/styles/vs.min.css'><script src='https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.15.10/highlight.min.js'></script><script>hljs.initHighlightingOnLoad();</script>"
    echo %javadocCmd%
    %javadocCmd%
    if %errorlevel% neq 0 (
        set error_msg=ドキュメント生成エラー
        goto :echo_error
    )
    echo =============== ドキュメント生成終了 ===============
exit /b

:echo_error
    echo %error_msg%
goto :end

:end
    ENDLOCAL
