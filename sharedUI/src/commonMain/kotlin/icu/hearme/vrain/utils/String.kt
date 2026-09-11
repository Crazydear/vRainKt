package icu.hearme.vrain.utils

import java.util.stream.Collectors

/** 字符串转字符串列表，主要处理生僻字 */
fun String.toListString(): List<String> {
    return this.codePoints()
        .mapToObj { String(Character.toChars(it)) }
        .collect(Collectors.toList())
}