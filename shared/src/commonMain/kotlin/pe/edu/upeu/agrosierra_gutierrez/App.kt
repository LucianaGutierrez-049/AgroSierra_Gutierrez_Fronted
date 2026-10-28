package pe.edu.upeu.agrosierra_gutierrez

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.agrosierra_gutierrez.presentation.*
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
import pe.edu.upeu.agrosierra_gutierrez.domain.usecase.puedeEditarLabor

private val Verde=Color(0xFF245B3D)
private val Fondo=Color(0xFFF4F6EF)
@Composable fun App() {
    val vm: AgroViewModel=koinViewModel()
    val s by vm.estado.collectAsState()
    MaterialTheme(colorScheme=lightColorScheme(primary=Verde,secondary=Color(0xFF8A5E24),background=Fondo)) {
        Surface(Modifier.fillMaxSize(),color=Fondo) {
            Column(Modifier.fillMaxSize().safeContentPadding().padding(horizontal=20.dp,vertical=12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AGROSIERRA",style=MaterialTheme.typography.labelLarge,color=Verde,fontWeight=FontWeight.Bold)
                        Text("AgroCampo",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                    }
                    if(s.sesion!=null) TextButton(onClick=vm::perfil) { Text("Mi perfil") }
                }
                Spacer(Modifier.height(12.dp))
                if(s.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(s.mensaje.isNotBlank()) {
                    Card(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.mensaje)
                            Row { TextButton(onClick=vm::limpiarMensaje) { Text("Entendido") }
                                if(s.sesion!=null && s.reintentar) TextButton(onClick=vm::sincronizar,enabled=!s.cargando) { Text("Reintentar sincronización") } }
                        }
                    }
                }
                when {
                    s.sesion==null -> Login(vm,s)
                    s.perfil -> Perfil(vm,s)
                    s.formulario -> Formulario(vm,s)
                    s.seleccionado!=null -> Detalle(vm,s)
                    else -> Listado(vm,s)
                }
            }
        }
    }
}
@Composable private fun Login(vm: AgroViewModel,s: UiState) {
    var usuario by remember { mutableStateOf("") }; var clave by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Tu campo, conectado",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
        Text("Registra las labores de tu campaña y conserva tu trabajo incluso sin señal.",color=Color.DarkGray)
        OutlinedTextField(usuario,{usuario=it},label={Text("Usuario")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(clave,{clave=it},label={Text("Contraseña")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        Button(onClick={vm.login(usuario,clave)},enabled=!s.cargando,modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("Ingresar") }
        Text("Cooperativa AgroSierra · Gestión de labores",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun Listado(vm: AgroViewModel,s: UiState) {
    Text("Hola, ${s.sesion?.nombre}",style=MaterialTheme.typography.titleMedium)
    val edad=s.ultimaSync?.let { ((vm.plataforma.ahora()-it)/60000).coerceAtLeast(0) }
    Text(if(edad==null) "Nunca sincronizado" else if(edad==0L) "Actualizado hace un momento" else "Actualizado hace $edad min",
        style=MaterialTheme.typography.bodySmall,color=Verde)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(s.busqueda,{vm.filtrar(busqueda=it)},label={Text("Buscar por nombre o código")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        listOf("","Mantaro","Urubamba","Colca").forEach { valle ->
            FilterChip(selected=s.valle==valle,onClick={vm.filtrar(valle=valle)},label={Text(valle.ifBlank { "Todos" })})
        }
    }
    Button(onClick=vm::sincronizar,enabled=!s.cargando,modifier=Modifier.fillMaxWidth()) { Text("Sincronizar") }
    Text("Padrón de lotes · ${s.lotes.size}",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(vertical=12.dp))
    if(s.lotes.isEmpty()) Text(if(s.cargando) "Cargando lotes…" else "No hay lotes guardados para este filtro. Sincroniza cuando tengas conexión.")
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        items(s.lotes,key={it.id}) { lote ->
            Card(Modifier.fillMaxWidth().clickable { vm.seleccionar(lote) },colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(lote.codigo+" · "+lote.valle,style=MaterialTheme.typography.labelMedium,color=Verde)
                    Text(lote.nombre,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                    Text("${lote.cultivo} · ${lote.hectareas} ha")
                    Text(if(lote.campaniaCerrada) "Campaña cerrada" else "Campaña abierta",color=if(lote.campaniaCerrada) Color.Gray else Verde)
                }
            }
        }
    }
}
@Composable private fun Detalle(vm: AgroViewModel,s: UiState) {
    val lote=s.seleccionado ?: return
    val permisos=s.sesion?.privilegios.orEmpty()
    var confirmarCierre by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf<Labor?>(null) }
    TextButton(onClick=vm::volver) { Text("‹ Volver al padrón") }
    Text(lote.nombre,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    Text("${lote.codigo} · ${lote.valle} · ${lote.cultivo} · ${lote.hectareas} ha")
    Text(if(lote.campaniaCerrada) "Campaña cerrada" else "Campaña abierta",color=Verde)
    if("LABOR_CREAR" in permisos && !lote.campaniaCerrada) Button(onClick={vm.formulario()},enabled=!s.cargando,modifier=Modifier.fillMaxWidth()) { Text("Registrar labor") }
    if("CAMPANIA_CERRAR" in permisos && !lote.campaniaCerrada) OutlinedButton(onClick={confirmarCierre=true},enabled=!s.cargando,modifier=Modifier.fillMaxWidth()) { Text("Cerrar campaña") }
    Text("Labores de campo",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(vertical=12.dp))
    if(s.labores.isEmpty()) Text("Todavía no hay labores guardadas para este lote.")
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        items(s.labores,key={it.id}) { labor ->
            Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${labor.tipo} · ${labor.fecha}",fontWeight=FontWeight.Bold)
                    Text("${labor.responsable} · Cantidad: ${labor.cantidad}")
                    if(labor.observacion.isNotBlank()) Text(labor.observacion)
                    if(labor.pendiente) Text("Pendiente de sincronizar",color=Color(0xFF8A5E24),fontWeight=FontWeight.Bold)
                    if(labor.error.isNotBlank()) Text(labor.error,color=MaterialTheme.colorScheme.error)
                    Row {
                        if(s.sesion?.let { puedeEditarLabor(it,labor,vm.plataforma) } == true && !lote.campaniaCerrada) TextButton(onClick={vm.formulario(labor)},enabled=!s.cargando) { Text("Corregir") }
                        if("LABOR_ELIMINAR" in permisos) TextButton(onClick={borrar=labor},enabled=!s.cargando) { Text("Eliminar") }
                    }
                }
            }
        }
    }
    if(confirmarCierre) AlertDialog(onDismissRequest={confirmarCierre=false},title={Text("¿Cerrar la campaña?")},
        text={Text("Después del cierre no se podrán registrar ni corregir labores. Se necesita una cosecha registrada.")},
        confirmButton={TextButton(onClick={confirmarCierre=false;vm.cerrarCampania()}) { Text("Cerrar campaña") }},
        dismissButton={TextButton(onClick={confirmarCierre=false}) { Text("Cancelar") }})
    borrar?.let { labor -> AlertDialog(onDismissRequest={borrar=null},title={Text("¿Eliminar esta labor?")},text={Text("${labor.tipo} del ${labor.fecha}")},
        confirmButton={TextButton(onClick={borrar=null;vm.eliminarLabor(labor.id)}) { Text("Eliminar") }},
        dismissButton={TextButton(onClick={borrar=null}) { Text("Cancelar") }}) }
}
@Composable private fun Formulario(vm: AgroViewModel,s: UiState) {
    val original=s.editar
    var tipo by remember(original?.id) { mutableStateOf(original?.tipo ?: TipoLabor.RIEGO) }
    var fecha by remember(original?.id) { mutableStateOf(original?.fecha ?: vm.plataforma.fechaHoy()) }
    var cantidad by remember(original?.id) { mutableStateOf(original?.cantidad?.toString() ?: "") }
    var observacion by remember(original?.id) { mutableStateOf(original?.observacion ?: "") }
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=vm::cancelarFormulario) { Text("‹ Volver al lote") }
        Text(if(original==null) "Registrar labor" else "Corregir labor",style=MaterialTheme.typography.headlineSmall)
        Text(s.seleccionado?.nombre.orEmpty(),color=Verde)
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { TipoLabor.entries.forEach { t -> FilterChip(tipo==t,{tipo=t},label={Text(t.name.lowercase().replaceFirstChar { it.uppercase() })}) } }
        Campo(fecha,{fecha=it},"Fecha (AAAA-MM-DD)",s.campos["fecha"])
        Campo(cantidad,{cantidad=it},"Cantidad aplicada",s.campos["cantidad"],true)
        Campo(observacion,{observacion=it},"Observación",s.campos["observacion"])
        Text("Sin conexión, la labor se guarda en este dispositivo y queda pendiente de envío.",style=MaterialTheme.typography.bodySmall)
        Button(onClick={vm.guardarLabor(tipo,fecha,cantidad,observacion)},enabled=!s.cargando,modifier=Modifier.fillMaxWidth()) { Text("Guardar labor") }
    }
}
@Composable private fun Campo(valor: String,cambio: (String)->Unit,label: String,error: String?,numero: Boolean=false) {
    OutlinedTextField(valor,cambio,label={Text(label)},isError=error!=null,
        supportingText={if(error!=null) Text(error)},modifier=Modifier.fillMaxWidth(),
        keyboardOptions=KeyboardOptions(keyboardType=if(numero) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable private fun Perfil(vm: AgroViewModel,s: UiState) {
    val usuario=s.sesion ?: return
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=vm::volver) { Text("‹ Volver al padrón") }
        Text(usuario.nombre,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("${usuario.usuario} · ${usuario.rol}")
        Text("Tus privilegios",style=MaterialTheme.typography.titleMedium)
        usuario.privilegios.sorted().forEach { Text("• $it") }
        Text("Los pendientes se conservan asociados a tu usuario. Al volver a ingresar podrás sincronizarlos.",style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick=vm::salir,modifier=Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}
