package info.vividcode.orm.db

import java.sql.PreparedStatement

class SqlCommand(
    val sqlString: String,
    val sqlValueSetterList: List<PreparedStatement.(Int) -> Unit>
)
