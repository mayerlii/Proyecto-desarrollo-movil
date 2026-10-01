# Tienda Virtual JAS - Informe de Seguridad en Almacenamiento Local y Control de Permisos

## 1. Resumen Ejecutivo
En el desarrollo de la aplicación Android **Tienda Virtual JAS**, se implementó una pantalla especializada de **Notas Confidenciales, Control de Permisos y Comparativa de Seguridad** (`NotasConfidencialesScreen.kt`) para evaluar el impacto de la seguridad de la información sensible según el medio de almacenamiento local utilizado y las políticas de acceso por credenciales.

Se compararon dos mecanismos de almacenamiento y niveles de permiso:
1. **Almacenamiento Interno Privado Cifrado con Control de Permisos**: Implementado con `EncryptedSharedPreferences` respaldado por la clave simétrica **AES-256 GCM** y el sistema de claves de hardware **Android KeyStore**. Al consultar sin autenticar las credenciales del usuario, se muestra la **corta cadena RAW cifrada incomprensible**. Al validar la contraseña, se libera el **contenido descifrado**.
2. **Almacenamiento Externo en Texto Plano**: Archivo guardado directamente en el directorio externo público del dispositivo (`nota_sensible_externa.txt`) que resulta **totalmente legible sin ningún permiso ni contraseña**.

---

## 2. Evidencia de Inspección, Permisos y Legibilidad de Datos

### A. Almacenamiento Externo Sin Cifrar (Sin Permiso de Acceso Requerido)
- **Ubicación del archivo**: `/sdcard/Android/data/com.example.tienda_virtual/files/nota_sensible_externa.txt`
- **Comando ADB de inspección**:
  ```bash
  adb shell cat /sdcard/Android/data/com.example.tienda_virtual/files/nota_sensible_externa.txt
  ```
- **Resultado de la inspección**:
  - **Dato totalmente legible en texto plano**:
    `Clave Secreta Banco Nariño: PIN 9876-X54A - $2,500.00 USD`
- **Evaluación de Riesgo**: **ALTO RIESGO / VULNERABLE**. Cualquier otra aplicación instalada en el dispositivo con permisos de lectura externa, o cualquier usuario conectando el dispositivo por USB mediante órdenes ADB o un explorador de archivos (*Device File Explorer*), puede leer e inspeccionar los datos sensibles almacenados sin requerir credenciales ni permisos de usuario.

---

### B. Almacenamiento Interno Privado Cifrado (Con Verificación de Permisos)
- **Ubicación del archivo**: `/data/data/com.example.tienda_virtual/shared_prefs/prefs_confidenciales_cifradas.xml`
- **Mecanismo de Cifrado**: `EncryptedSharedPreferences` + `MasterKey` (`AES256_GCM` / `AES256_SIV`)

#### B.1. Inspección SIN Permiso de Acceso (Bloqueado):
- Al hacer clic en *"👁️ Inspeccionar Sin Permiso"*, la aplicación no revela los datos y muestra la nota cifrada RAW:
  ```xml
  🔒 ACCESO BLOQUEADO (SIN PERMISO) - Contenido Cifrado RAW (Incomprensible):
  <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
  <map>
      <string name="key_nota_sensible_aes">gAAAAABm3fXz_Q2xhdmUgU2VjcmV0...AES256_GCM_ENCRYPTED</string>
  </map>
  ```

#### B.2. Inspección CON Permiso de Acceso (Validación de Contraseña):
- Al presionar *"🔑 Solicitar Permiso"*, el sistema despliega el modal para ingresar la contraseña registrada del usuario activo (ej. `password123`).
- Al validar exitosamente las credenciales, el sistema desbloquea la nota descifrada en pantalla:
  ```text
  🔓 ACCESO CONCEDIDO - Contenido Descifrado:
  Clave Secreta Banco Nariño: PIN 9876-X54A - $2,500.00 USD
  ```

---

## 3. Matriz Comparativa de Seguridad

| Criterio | Almacenamiento Externo Sin Cifrar | Almacenamiento Interno Cifrado (`EncryptedSharedPreferences`) |
| :--- | :--- | :--- |
| **Ubicación** | `/sdcard/Android/data/...` | `/data/data/.../shared_prefs/` |
| **Control de Permisos** | Nulo (Lectura libre) | **Requiere Verificación de Contraseña de Usuario** |
| **Legibilidad Sin Permiso** | **100% Texto Plano Legible** | **Texto Cifrado RAW Incomprensible** |
| **Legibilidad Con Permiso** | **Texto Plano** | **Texto Descifrado Autorizado** |
| **Gestión de Claves** | Nula | Custodiada por Android KeyStore en hardware |
| **Uso Recomendado** | Caché temporal de medios públicos (imágenes) | **PINs, Tokens, Credenciales, Datos Financieros** |

---

## 4. Conclusión
El almacenamiento externo sin cifrar no debe ser utilizado bajo ninguna circunstancia para guardar información confidencial. La implementación de **`EncryptedSharedPreferences`** combinada con políticas de **verificación de permisos por contraseña** garantiza que la información permanezca completamente cifrada e ininteligible tanto en reposo en el sistema de archivos como en la interfaz gráfica hasta que el usuario autorice su acceso mediante sus credenciales.
