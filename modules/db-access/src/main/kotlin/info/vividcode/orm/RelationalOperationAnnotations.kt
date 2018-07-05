package info.vividcode.orm

@Target(AnnotationTarget.FUNCTION)
annotation class Insert(
    val returnGeneratedKeys: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
annotation class Update()

@Target(AnnotationTarget.FUNCTION)
annotation class Delete()
