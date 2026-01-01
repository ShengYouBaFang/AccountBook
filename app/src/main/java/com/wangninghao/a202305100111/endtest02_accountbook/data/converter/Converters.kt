package com.wangninghao.a202305100111.endtest02_accountbook.data.converter

import androidx.room.TypeConverter
import com.wangninghao.a202305100111.endtest02_accountbook.data.entity.RecordType

/**
 * Room类型转换器
 * 用于将枚举类型转换为数据库可存储的类型
 */
class Converters {

    @TypeConverter
    fun fromRecordType(type: RecordType): String {
        return type.name
    }

    @TypeConverter
    fun toRecordType(value: String): RecordType {
        return RecordType.valueOf(value)
    }
}
