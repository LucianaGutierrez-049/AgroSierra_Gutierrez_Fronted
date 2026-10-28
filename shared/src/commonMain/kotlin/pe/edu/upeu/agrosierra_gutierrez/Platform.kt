package pe.edu.upeu.agrosierra_gutierrez

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform