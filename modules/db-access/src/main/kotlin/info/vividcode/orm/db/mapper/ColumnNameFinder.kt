package info.vividcode.orm.db.mapper

import kotlin.reflect.KParameter

/**
 * カラム名として期待する文字列を探す処理を表すインターフェイス。
 */
internal interface ColumnNameFinder {

    /**
     * 指定の型を生成するための [RowGeneratorMethod] のパラメータを受け取り、そこから対応するカラム名を探して返す。
     *
     * @param parameter カラムに対応するパラメータ。
     * @return 対応するカラム名。
     */
    fun findExpectedColumnName(parameter: KParameter): String

}
