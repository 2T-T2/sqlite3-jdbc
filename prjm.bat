setlocal enabledelayedexpansion

@echo off

cd %~dp0

If %PROCESSOR_ARCHITECTURE:~-2%==64     set ARCHITECTURE=64
If not %PROCESSOR_ARCHITECTURE:~-2%==64 set ARCHITECTURE=32

rem �R���p�C���A�A�[�J�C�u���A�h�L�������g�����̑Ώۃ��W���[����
set mdl_name=t_panda.jdbc.sqlite

rem �ꎞ�t�H���_
set tmp_dir=.\tmp\
rem �o�̓t�H���_
set out_dir=.\out\
set dst_dir=.\dst\
set doc_dir=.\docs\
rem maven���|�W�g���쐬�t�H���_
set rep_dir=.\rep\
set rep_dir_uri=file:///%CD:\=/%%rep_dir:\=/%
rem �ˑ����C�u�����i�[�t�H���_
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

rem �G���[���b�Z�[�W
set error_msg=0

REM ========== �����J�n ==========

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
    echo     %lib_dir%, %out_dir%, %rep_dir%, %dst_dir%, %doc_dir%, %cpp_include_dir%, %tmp_dir% ���N���[�����܂�
    echo.
    echo %~nx0 dl-depend
    echo     %lib_dir% �� %jar_pom_name% �Ŏw�肳�ꂽ�ˑ��t�@�C�����_�E�����[�h���܂�
    echo     %lib_dir% �� %dll_pom_name% �Ŏw�肳�ꂽ�ˑ��t�@�C�����_�E�����[�h���܂�
    echo     %cpp_src_dir%, %cpp_include_dir% �� sqlite3 �̃\�[�X�t�@�C�����_�E�����[�h���܂�
    echo.
    echo %~nx0 compile
    echo     %java_src_dir% �̓��e���R���p�C�����܂�
    echo     �o�͐�t�H���_ %classes_dir%
    echo     %cpp_src_dir%sqlite3.c ���R���p�C�����܂�
    echo     �o�͐�t�@�C�� %sqlite3_obj_out_dir%%sqlite3_obj_name%
    echo     %cpp_src_dir%*.cpp �� %sqlite3_obj_out_dir%%sqlite3_obj_name% �ƃ����N���ăR���p�C�����܂�
    echo     �o�͐�t�H���_ %dst_dir%
    echo.
    echo %~nx0 archive
    echo     %classes_dir%, %java_src_dir%, %jar_res_dir% ���A�[�J�C�u������jar���쐬���܂�
    echo     �o�͐�t�@�C���� %dst_dir%%jar_name%
    echo.
    echo %~nx0 mvnrep
    echo     %dst_dir%%jar_name%, %jar_pom_name% ����maven���|�W�g���̍쐬���s���܂�
    echo     %dst_dir%%dll_name%, %dll_pom_name% ����maven���|�W�g���̍쐬���s���܂�
    echo.
    echo %~nx0 javadoc
    echo     �h�L�������g�𐶐����܂�
    echo     �o�͐�t�H���_ %doc_dir%
    echo.
    echo %~nx0 all
    echo     clean -^> dl-depend -^> compile
    echo     -^> archive -^> mvnrep -^> javadoc �̏��Ŏ��s���܂�
    echo.

    goto :end
    echo.
)

call :init
call :%1
goto :end

:init
    REM �t�H���_�쐬
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

    REM module-info.java �쐬
    REM echo %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo module t_panda.jdbc.sqlite { > %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo     // requires /* transitive */ �ˑ�����O�����W���[��; >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo     // opens �O�����W���[�����烊�t���N�V�����ŃA�N�Z�X��������p�b�P�[�W; >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo     // exports �O�����W���[������A�N�Z�X��������p�b�P�[�W; >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
    REM echo } >> %java_src_dir%t_panda.jdbc.sqlite\module-info.java
exit /b

:clean
    echo =============== �N���[���J�n ===============
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
    echo =============== �N���[���I�� ===============
exit /b

:dl-depend
    echo =============== �ˑ��t�@�C���_�E�����[�h�J�n ===============
    REM �ˑ��t�@�C�����f�B���N�g�����w�肵�ă_�E�����[�h
    set mvnDlDependCmd=call mvn dependency:copy-dependencies -f %jar_pom_name% -DoutputDirectory=%lib_dir%
    echo %mvnDlDependCmd%
    %mvnDlDependCmd%
    if %errorlevel% neq 0 (
        set error_msg=�ˑ��t�@�C���_�E�����[�h�G���[
        goto :echo_error
    )
    REM �ˑ��t�@�C�����f�B���N�g�����w�肵�ă_�E�����[�h
    set mvnDlDependCmd=call mvn dependency:copy-dependencies -f %dll_pom_name% -DoutputDirectory=%lib_dir%
    echo %mvnDlDependCmd%
    %mvnDlDependCmd%
    if %errorlevel% neq 0 (
        set error_msg=�ˑ��t�@�C���_�E�����[�h�G���[
        goto :echo_error
    )

    rem SQLite �̃\�[�X�t�@�C�����_�E�����[�h����
    set sqliteSourceZipDlCmd=curl -o %tmp_dir%sqlite-amalgamation-3460100.zip https://www.sqlite.org/2024/sqlite-amalgamation-3460100.zip
    set NEED_DL_SQLITE_SRC=FALSE
    If not exist %tmp_dir%sqlite-amalgamation-3460100\sqlite3.c set NEED_DL_SQLITE_SRC=TRUE
    If not exist %tmp_dir%sqlite-amalgamation-3460100\sqlite3.h set NEED_DL_SQLITE_SRC=TRUE
    If %NEED_DL_SQLITE_SRC% equ TRUE (
        echo %sqliteSourceZipDlCmd%
        %sqliteSourceZipDlCmd%
        if %errorlevel% neq 0 (
            set error_msg=�ˑ��t�@�C���_�E�����[�h�G���[
            goto :echo_error
        )
        powershell.exe -Command "Expand-Archive -Path '%tmp_dir%sqlite-amalgamation-3460100.zip' -DestinationPath '%tmp_dir%'"
        if %errorlevel% neq 0 (
            set error_msg=SQLite Source Zip �𓀃G���[
            goto :echo_error
        )
        copy %tmp_dir%sqlite-amalgamation-3460100\sqlite3.c %cpp_src_dir%
        copy %tmp_dir%sqlite-amalgamation-3460100\sqlite3.h %cpp_include_dir%
    )
    echo =============== �ˑ��t�@�C���_�E�����[�h�I�� ===============
exit /b

:compile
    echo =============== �R���p�C���J�n ===============
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
        set error_msg=�R���p�C���G���[
        goto :echo_error
    )

    set ccCmd=gcc %cpp_src_dir%sqlite3.c -c -o %sqlite3_obj_out_dir%%sqlite3_obj_name%
    if not exist %sqlite3_obj_out_dir%%sqlite3_obj_name% (
        echo %ccCmd%
        %ccCmd%
        if %errorlevel% neq 0 (
            set error_msg=�R���p�C���G���[
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
        set error_msg=�R���p�C���G���[
        goto :echo_error
    )
    echo =============== �R���p�C���I�� ===============
exit /b

:archive
    echo =============== �A�[�J�C�u���J�n ===============
    set jarCmd=jar^
        -cf %dst_dir%%jar_name%^
        -C %classes_dir%%mdl_name% .^
        -C %java_src_dir%%mdl_name% .^
        META-INF ^
        %jar_res_dir%
    echo %jarCmd%
    %jarCmd%
    if %errorlevel% neq 0 (
        set error_msg=�A�[�J�C�u���G���[
        goto :echo_error
    )
    echo =============== �A�[�J�C�u���I�� ===============
exit /b

:mvnrep
    echo =============== ���|�W�g���쐬�J�n ===============
    REM �w��t�@�C��(jar��pom)���w��f�B���N�g���Ƀf�v���C
    set mvnDeployCmd=call mvn deploy:deploy-file -Dfile=%dst_dir%%jar_name% -Durl=%rep_dir_uri% -DpomFile=%jar_pom_name% -Dpackaging=jar
    echo %mvnDeployCmd%
    %mvnDeployCmd%
    if %errorlevel% neq 0 (
        set error_msg=���|�W�g���쐬�G���[
        goto :echo_error
    )
    REM �w��t�@�C��(dll��pom)���w��f�B���N�g���Ƀf�v���C
    set mvnDeployCmd=call mvn deploy:deploy-file -Dfile=%dst_dir%%dll_name% -Durl=%rep_dir_uri% -DpomFile=%dll_pom_name% -Dpackaging=dll
    echo %mvnDeployCmd%
    %mvnDeployCmd%
    if %errorlevel% neq 0 (
        set error_msg=���|�W�g���쐬�G���[
        goto :echo_error
    )
    echo =============== ���|�W�g���쐬�I�� ===============
exit /b

:javadoc
    echo =============== �h�L�������g�����J�n ===============
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
        set error_msg=�h�L�������g�����G���[
        goto :echo_error
    )
    echo =============== �h�L�������g�����I�� ===============
exit /b

:echo_error
    echo %error_msg%
goto :end

:end
    ENDLOCAL
