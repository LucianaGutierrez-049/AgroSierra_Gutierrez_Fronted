package pe.edu.upeu.agrosierra_gutierrez.data.local
interface AlmacenSeguro {
    fun leer(): String?
    fun guardar(valor: String)
    fun borrar()
}
