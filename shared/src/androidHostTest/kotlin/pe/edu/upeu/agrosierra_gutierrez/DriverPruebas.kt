package pe.edu.upeu.agrosierra_gutierrez
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.agrosierra_gutierrez.db.AgroDb
actual fun driverPruebas(): SqlDriver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AgroDb.Schema.create(it) }
