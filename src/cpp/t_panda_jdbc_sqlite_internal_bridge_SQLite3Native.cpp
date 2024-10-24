#include <sqlite3.h>
#include <string.h>
#include "t_panda.jdbc.sqlite/t_panda_jdbc_sqlite_internal_bridge_SQLite3Native.h"

#define CLASS_SIG_SQLITE_RETURN_CODE "Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;"
#define CLASS_SIG_SQLITE_OUT "Lt_panda/jdbc/sqlite/internal/bridge/Out;"

jobject getSQLiteReturnCode(JNIEnv* env, int sqliteResultCode) {
    jclass enumClass = env->FindClass(CLASS_SIG_SQLITE_RETURN_CODE); 
    jfieldID enumField;
    switch (sqliteResultCode) {
        case SQLITE_OK: enumField = env->GetStaticFieldID(enumClass, "SQLITE_OK", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_ERROR: enumField = env->GetStaticFieldID(enumClass, "SQLITE_ERROR", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_INTERNAL: enumField = env->GetStaticFieldID(enumClass, "SQLITE_INTERNAL", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_PERM: enumField = env->GetStaticFieldID(enumClass, "SQLITE_PERM", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_ABORT: enumField = env->GetStaticFieldID(enumClass, "SQLITE_ABORT", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_BUSY: enumField = env->GetStaticFieldID(enumClass, "SQLITE_BUSY", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_LOCKED: enumField = env->GetStaticFieldID(enumClass, "SQLITE_LOCKED", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_NOMEM: enumField = env->GetStaticFieldID(enumClass, "SQLITE_NOMEM", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_READONLY: enumField = env->GetStaticFieldID(enumClass, "SQLITE_READONLY", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_INTERRUPT: enumField = env->GetStaticFieldID(enumClass, "SQLITE_INTERRUPT", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_IOERR: enumField = env->GetStaticFieldID(enumClass, "SQLITE_IOERR", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_CORRUPT: enumField = env->GetStaticFieldID(enumClass, "SQLITE_CORRUPT", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_NOTFOUND: enumField = env->GetStaticFieldID(enumClass, "SQLITE_NOTFOUND", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_FULL: enumField = env->GetStaticFieldID(enumClass, "SQLITE_FULL", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_CANTOPEN: enumField = env->GetStaticFieldID(enumClass, "SQLITE_CANTOPEN", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_PROTOCOL: enumField = env->GetStaticFieldID(enumClass, "SQLITE_PROTOCOL", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_EMPTY: enumField = env->GetStaticFieldID(enumClass, "SQLITE_EMPTY", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_SCHEMA: enumField = env->GetStaticFieldID(enumClass, "SQLITE_SCHEMA", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_TOOBIG: enumField = env->GetStaticFieldID(enumClass, "SQLITE_TOOBIG", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_CONSTRAINT: enumField = env->GetStaticFieldID(enumClass, "SQLITE_CONSTRAINT", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_MISMATCH: enumField = env->GetStaticFieldID(enumClass, "SQLITE_MISMATCH", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_MISUSE: enumField = env->GetStaticFieldID(enumClass, "SQLITE_MISUSE", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_NOLFS: enumField = env->GetStaticFieldID(enumClass, "SQLITE_NOLFS", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_AUTH: enumField = env->GetStaticFieldID(enumClass, "SQLITE_AUTH", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_FORMAT: enumField = env->GetStaticFieldID(enumClass, "SQLITE_FORMAT", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_RANGE: enumField = env->GetStaticFieldID(enumClass, "SQLITE_RANGE", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_NOTADB: enumField = env->GetStaticFieldID(enumClass, "SQLITE_NOTADB", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_ROW: enumField = env->GetStaticFieldID(enumClass, "SQLITE_ROW", CLASS_SIG_SQLITE_RETURN_CODE); break;
        case SQLITE_DONE: enumField = env->GetStaticFieldID(enumClass, "SQLITE_DONE", CLASS_SIG_SQLITE_RETURN_CODE); break;
        default: enumField = env->GetStaticFieldID(enumClass, "SQLITE_ERROR", CLASS_SIG_SQLITE_RETURN_CODE); break;
    }
    return env->GetStaticObjectField(enumClass, enumField);  
}

sqlite3* jbyteArray2SQLitePtr(JNIEnv* env, jbyteArray jba) {
    void *buffer;
    env->GetByteArrayRegion(jba, 0, sizeof(buffer), (jbyte*)&buffer);
    return (sqlite3*)buffer;
}

sqlite3_stmt* jbyteArray2StmtPtr(JNIEnv* env, jbyteArray jba) {
    void *buffer;
    env->GetByteArrayRegion(jba, 0, sizeof(buffer), (jbyte*)&buffer);
    return (sqlite3_stmt*)buffer;
}

jbyteArray ptr2jbyteArray(JNIEnv *env, void* ptr) {
    void *buffer = ptr;
    jbyteArray pointer = env->NewByteArray(sizeof(void*));
    env->SetByteArrayRegion(pointer, 0, sizeof(void*), (jbyte*)&buffer);
    return pointer;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_open
 * Signature: (Ljava/lang/String;Lt_panda/jdbc/sqlite/internal/bridge/Out;)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1open
  (JNIEnv *env, jclass sqlite3NativeClass, jstring jFilename, jobject dstDbConnPtr) {
    sqlite3* dbConn = nullptr;
    const char* filename = env->GetStringUTFChars(jFilename, JNI_FALSE);
    int result = sqlite3_open(filename, &dbConn);
    env->ReleaseStringUTFChars(jFilename, filename);

    jmethodID mid = env->GetMethodID(env->FindClass(CLASS_SIG_SQLITE_OUT), "set", "(Ljava/lang/Object;)V");
    env->CallVoidMethod(dstDbConnPtr, mid, ptr2jbyteArray(env, dbConn));
    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_close_v2
 * Signature: ([B)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1close_1v2
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray dbConnPtr) {
    sqlite3* dbConn = jbyteArray2SQLitePtr(env, dbConnPtr);
    int result = sqlite3_close_v2(dbConn);    

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_prepare_v2
 * Signature: ([BLjava/lang/String;Lt_panda/jdbc/sqlite/internal/bridge/Out;Lt_panda/jdbc/sqlite/internal/bridge/Out;)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1prepare_1v2
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray dbConnPtr, jstring jsql, jobject dstStmtPtr, jobject dstRestSql) {
    sqlite3* dbConn = jbyteArray2SQLitePtr(env, dbConnPtr);
    sqlite3_stmt* stmt = nullptr;

    char const* dstSql = nullptr;
    const char* sql = env->GetStringUTFChars(jsql, JNI_FALSE);
    int result = sqlite3_prepare_v2(dbConn, sql, -1, &stmt, &dstSql);
    jmethodID mid = env->GetMethodID(env->FindClass(CLASS_SIG_SQLITE_OUT), "set", "(Ljava/lang/Object;)V");
    env->CallVoidMethod(dstStmtPtr, mid, ptr2jbyteArray(env, stmt));
    if (dstRestSql != nullptr) env->CallVoidMethod(dstRestSql, mid, env->NewStringUTF(dstSql));
    env->ReleaseStringUTFChars(jsql, sql);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_extended_errcode
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1extended_1errcode
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray dbConnPtr) {
    sqlite3* dbConn = jbyteArray2SQLitePtr(env, dbConnPtr);

    return sqlite3_extended_errcode(dbConn);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_errmsg
 * Signature: ([B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1errmsg
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray dbConnPtr) {
    sqlite3* dbConn = jbyteArray2SQLitePtr(env, dbConnPtr);

    const char* errMsg = sqlite3_errmsg(dbConn);

    return env->NewStringUTF(errMsg);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_step
 * Signature: ([B)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1step
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_step(stmt);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_finalize
 * Signature: ([B)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1finalize
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_finalize(stmt);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_busy_timeout
 * Signature: ([BI)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1busy_1timeout
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray dbConnPtr, jint second) {
    sqlite3* dbConn = jbyteArray2SQLitePtr(env, dbConnPtr);

    int result = sqlite3_busy_timeout(dbConn, second * 1000);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_reset
 * Signature: ([B)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1reset
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_reset(stmt);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_int
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1int
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    return sqlite3_column_int(stmt, colIdx);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_name
 * Signature: ([BI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1name
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    const char* colName = sqlite3_column_name(stmt, colIdx);

    return env->NewStringUTF(colName);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_count
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1count
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    return sqlite3_column_count(stmt);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_text
 * Signature: ([BI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1text
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    const char* v = (const char*)sqlite3_column_text(stmt, colIdx);

    return env->NewStringUTF(v);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_int64
 * Signature: ([BI)J
 */
JNIEXPORT jlong JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1int64
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    sqlite_int64 v = sqlite3_column_int64(stmt, colIdx);

    return v;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_double
 * Signature: ([BI)D
 */
JNIEXPORT jdouble JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1double
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    double v = sqlite3_column_double(stmt, colIdx);

    return v;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_isNull
 * Signature: ([BI)Z
 */
JNIEXPORT jboolean JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1isNull
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint index) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    sqlite3_value* value = sqlite3_column_value(stmt, index);

    return sqlite3_value_type(value) == SQLITE_NULL;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_boolean
 * Signature: ([BI)Z
 */
JNIEXPORT jboolean JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1boolean
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    sqlite3_value* value = sqlite3_column_value(stmt, colIdx);
    int sqlite_type = sqlite3_value_type(value);
    if ( sqlite_type == SQLITE_NULL    ) return false; 
    if ( sqlite_type == SQLITE_INTEGER ) return sqlite3_value_int(value) != 0; 
    if ( sqlite_type == SQLITE_FLOAT   ) return sqlite3_value_double(value) != 0.0; 
    if ( sqlite_type == SQLITE_TEXT    ) return strcmp((const char*)sqlite3_value_text(value), "0") != 0; 
    if ( sqlite_type == SQLITE_BLOB    ) return true; 

    return false;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_bytes
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1bytes
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    jbyteArray arrj = nullptr;
    jbyte* arrjElmPtr = nullptr;
    size_t arrSize;

    sqlite3_value* value = sqlite3_column_value(stmt, colIdx);
    int sqlite_type = sqlite3_value_type(value);
    if (sqlite_type == SQLITE_NULL)
        return arrj;
    if (sqlite_type == SQLITE_INTEGER) {
        int iv = sqlite3_value_int(value);
        const unsigned char tmp[4] = { (unsigned char)((iv >> 24) & 0xff), (unsigned char)((iv >> 16) & 0xff), (unsigned char)((iv >> 8) & 0xff), (unsigned char)(iv & 0xff) };
        arrj = env->NewByteArray(4);
        arrjElmPtr = env->GetByteArrayElements(arrj, JNI_FALSE);
        arrjElmPtr[0] = tmp[0];
        arrjElmPtr[1] = tmp[1];
        arrjElmPtr[2] = tmp[2];
        arrjElmPtr[3] = tmp[3];
        env->ReleaseByteArrayElements(arrj, arrjElmPtr, 0);
        return arrj;
    }
    if (sqlite_type == SQLITE_FLOAT) {
        // TODO  変換 浮動小数 -> バイト列
        return arrj;
    }
    if (sqlite_type == SQLITE_TEXT) {
        const char* sv = (const char*)sqlite3_value_text(value);
        arrSize = sqlite3_column_bytes(stmt, colIdx);
        arrj = env->NewByteArray(arrSize);
        arrjElmPtr = env->GetByteArrayElements(arrj, JNI_FALSE);
        for (size_t i = 0; i < arrSize; i++)
            arrjElmPtr[i] = sv[i];
        env->ReleaseByteArrayElements(arrj, arrjElmPtr, 0);
        return arrj;
    }
    if (sqlite_type == SQLITE_BLOB) {
        const char* bv = (const char*)sqlite3_value_blob(value);
        arrSize = sqlite3_column_bytes(stmt, colIdx);
        arrj = env->NewByteArray(arrSize);
        arrjElmPtr = env->GetByteArrayElements(arrj, JNI_FALSE);
        for (size_t i = 0; i < arrSize; i++)
            arrjElmPtr[i] = bv[i];
        env->ReleaseByteArrayElements(arrj, arrjElmPtr, 0);
        return arrj;
    }

    return arrj;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_column_blob
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1column_1blob
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    const char* v = (const char*)sqlite3_column_blob(stmt, colIdx);    

    size_t arrSize = sqlite3_column_bytes(stmt, colIdx);
    jbyteArray arrj = env->NewByteArray(arrSize);
    jbyte* arrjElmPtr = env->GetByteArrayElements(arrj, JNI_FALSE);
    for (size_t i = 0; i < arrSize; i++)
        arrjElmPtr[i] = v[i];
    env->ReleaseByteArrayElements(arrj, arrjElmPtr, 0);

    return arrj;
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_bind_null
 * Signature: ([BI)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1bind_1null
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_bind_null(stmt, colIdx);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_bind_blob
 * Signature: ([BI[B)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1bind_1blob
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx, jbyteArray jValue) {
    // copy jbytearray -> char[]
    const jsize arrLen = env->GetArrayLength(jValue);
    char val[arrLen];
    jbyte* elm = env->GetByteArrayElements(jValue, JNI_FALSE);
    for (size_t i = 0; i < arrLen; i++)
        val[i] = elm[i];
    env->ReleaseByteArrayElements(jValue, elm, 0);
    // ==========================

    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_bind_blob(stmt, colIdx, val, -1, SQLITE_TRANSIENT);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_bind_double
 * Signature: ([BID)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1bind_1double
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx, jdouble jValue) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_bind_double(stmt, colIdx, jValue);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_bind_int
 * Signature: ([BII)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1bind_1int
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx, jint jValue) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_bind_int(stmt, colIdx, jValue);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_bind_int64
 * Signature: ([BIJ)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1bind_1int64
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx, jlong jValue) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_bind_int64(stmt, colIdx, jValue);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_bind_text
 * Signature: ([BILjava/lang/String;)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1bind_1text
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr, jint colIdx, jstring jValue) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    const char* val = env->GetStringUTFChars(jValue, JNI_FALSE);
    int result = sqlite3_bind_text(stmt, colIdx, val, -1, SQLITE_TRANSIENT);
    env->ReleaseStringUTFChars(jValue, val);

    return getSQLiteReturnCode(env, result);
}

/*
 * Class:     t_panda_jdbc_sqlite_internal_bridge_SQLite3Native
 * Method:    sqlite3_clear_bindings
 * Signature: ([B)Lt_panda/jdbc/sqlite/internal/SQLiteReturnCode;
 */
JNIEXPORT jobject JNICALL Java_t_1panda_jdbc_sqlite_internal_bridge_SQLite3Native_sqlite3_1clear_1bindings
  (JNIEnv *env, jclass sqlite3NativeClass, jbyteArray stmtPtr) {
    sqlite3_stmt* stmt = jbyteArray2StmtPtr(env, stmtPtr);

    int result = sqlite3_clear_bindings(stmt);

    return getSQLiteReturnCode(env, result);
}

