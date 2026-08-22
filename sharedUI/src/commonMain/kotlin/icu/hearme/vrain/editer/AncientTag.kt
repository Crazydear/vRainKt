package icu.hearme.vrain.editer

enum class AncientTag(
    val label: String,
    val startTag: String,
    val endTag: String,
    val shortcutHint: String? = null
) {
    COMMENT("批注", "【", "】", "Ctrl+Shift+C"),
    BOOK_LINE("书名", "《", "》", "Ctrl+Shift+B"),
    RECT("圆角方框", "〔", "〕", "Ctrl+Shift+R"),
    CIRCLE("圆框", "〈", "〉", "Ctrl+Shift+O"),
    ZOOM("缩放", "（", "）", "Ctrl+Shift+Z"),
    FOCUS_CIRCLE("圈注", "｛", "｝", "Ctrl+1"),
    FOCUS_POINT("点注", "＜", "＞", "Ctrl+2"),
    FOCUS_LINE("线注", "［", "］", "Ctrl+3"),
    NEW_PAGE("分页", "%", "", "Ctrl+Shift+Enter"),
    HALF_PAGE("半页", "$", "", "Alt+Shift+Enter"),
    LAST_COL("末列", "&", ""),
    SPACE("空格", "@", ""),
    RAISED_HEAD("顶格", "T", ""),
    NEW_ROW("分栏", "^", "")
}