package pe.edu.upeu.agrosierra_gutierrez
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import pe.edu.upeu.agrosierra_gutierrez.data.local.AlmacenSeguro
class AlmacenSeguroAndroid(context: Context): AlmacenSeguro {
    private val preferencias=context.getSharedPreferences("sesion_segura",Context.MODE_PRIVATE)
    private val alias="agrocampo_sesion_aes"
    private fun clave(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun guardar(valor: String) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,clave()) }
        val cifrado=cipher.doFinal(valor.toByteArray(Charsets.UTF_8))
        check(preferencias.edit().putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP))
            .putString("datos",Base64.encodeToString(cifrado,Base64.NO_WRAP)).commit())
    }
    override fun leer(): String? = try {
        val datos=preferencias.getString("datos",null)
        val iv=preferencias.getString("iv",null)
        if(datos==null || iv==null) null else Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE,clave(),GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)))
            String(doFinal(Base64.decode(datos,Base64.NO_WRAP)),Charsets.UTF_8)
        }
    } catch (_: Exception) { borrar(); null }
    override fun borrar() { preferencias.edit().clear().commit() }
}
