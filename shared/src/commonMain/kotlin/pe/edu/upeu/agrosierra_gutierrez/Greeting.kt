package pe.edu.upeu.agrosierra_gutierrez

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}