package info.vividcode.orm.db.mapper.internal

import info.vividcode.orm.db.mapper.MapperTree
import info.vividcode.orm.db.mapper.ResultSetMapper
import java.sql.ResultSet
import java.sql.SQLException

/**
 * 返り値の型に対応する {@link RowGeneratorMethod} を探し、それを使って返り値のオブジェクトを生成するクラス。
 * DB から取得した単一の値を Java のオブジェクトに変換する処理には {@link ColumnValueMapper} を使用する。
 *
 * {@link RowGeneratorMethod} を探す処理を表す {@link RowGeneratorMethodFinder} オブジェクトと
 * {@link ColumnValueMapper} オブジェクトはインスタンス生成時に指定できる。
 *
 * デフォルトの挙動をする {@link RowGeneratorMethodResultSetMapper} オブジェクトを生成するために
 * {@link RowGeneratorMethodResultSetMapper.DefaultFactory} を使用できる。
 *
 * @param <T> DB から取得したテーブルの行に対応する Java の型。
 */
class RowGeneratorMethodResultSetMapper<T : Any>(
    private val mapperTree: MapperTree<T>
) : ResultSetMapper<T> {

    @Throws(SQLException::class)
    override fun map(r: ResultSet): T = mapperTree.map(r)

}