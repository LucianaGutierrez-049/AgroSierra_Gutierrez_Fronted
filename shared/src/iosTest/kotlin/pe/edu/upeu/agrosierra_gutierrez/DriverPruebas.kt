package pe.edu.upeu.agrosierra_gutierrez
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pe.edu.upeu.agrosierra_gutierrez.db.AgroDb
actual fun driverPruebas(): SqlDriver=NativeSqliteDriver(AgroDb.Schema,":memory:")
