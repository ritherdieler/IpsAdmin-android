# 🚀 Configuración Unificada de Desarrollo

## 📋 Resumen

Esta configuración permite usar **la misma URL** (`http://127.0.0.1:8080/`) tanto en **emuladores** como en **dispositivos físicos**, eliminando la necesidad de cambiar IPs o configuraciones según el target.

## ✅ **Implementación Completada**

### **1. Configuración de URLs Unificada**
- **URL única**: `http://127.0.0.1:8080/`
- **Funciona en**: Emuladores (AVD, Genymotion) y dispositivos físicos
- **Configurado en**: `build.gradle` → flavor `dev`

### **2. AndroidManifest para Debug**
- **Archivo**: `src/debug/AndroidManifest.xml`
- **Permisos**: HTTP cleartext habilitado
- **Seguridad**: Configuración específica para debug

### **3. Configuración de Seguridad de Red**
- **Archivo**: `src/debug/res/xml/debug_network_security_config.xml`
- **Dominios permitidos**: `127.0.0.1`, `localhost`, `10.0.2.2`, `192.168.1.6`

### **4. Script ADB Reverse Automático**
- **Archivo**: `reverse-all.sh`
- **Funcionalidad**: Aplica reverse a todos los dispositivos conectados
- **Puertos**: 8080, 3000, 5173

### **5. Tareas Gradle Automatizadas**
- **`adbReverseAll`**: Aplica ADB reverse
- **`installDevDebugWithReverse`**: Instala app + configura reverse

## 🎯 **Cómo Usar**

### **Opción 1: Script Manual**
```bash
# Aplicar ADB reverse a todos los dispositivos
./reverse-all.sh

# Instalar app
./gradlew installDevDebug
```

### **Opción 2: Tarea Gradle Automatizada**
```bash
# Instala app y configura ADB reverse automáticamente
./gradlew installDevDebugWithReverse
```

### **Opción 3: Tareas Separadas**
```bash
# Solo ADB reverse
./gradlew adbReverseAll

# Solo instalar
./gradlew installDevDebug
```

## 🔧 **Flujo de Trabajo Recomendado**

### **1. Iniciar Backend**
```bash
cd /path/to/backend
./run-dev.sh
```

### **2. Configurar ADB Reverse**
```bash
cd /path/to/android
./reverse-all.sh
```

### **3. Instalar App**
```bash
./gradlew installDevDebug
```

### **4. ¡Listo!**
- Tu app se conecta a `http://127.0.0.1:8080/`
- Funciona en cualquier dispositivo/emulador
- No necesitas cambiar IPs

## 📱 **Dispositivos Soportados**

### **Emuladores**
- ✅ Android Virtual Device (AVD)
- ✅ Genymotion
- ✅ Otros emuladores Android

### **Dispositivos Físicos**
- ✅ Android 5.0+ (API 21+)
- ✅ Con USB debugging habilitado
- ✅ Conectados via USB o WiFi

## 🔄 **ADB Reverse - Cómo Funciona**

```
Dispositivo/Emulador          Tu Máquina
┌─────────────────┐         ┌─────────────────┐
│ 127.0.0.1:8080  │ ──────► │ localhost:8080  │
│                 │         │ (Backend)       │
└─────────────────┘         └─────────────────┘
```

## 🚨 **Notas Importantes**

### **ADB Reverse se Pierde Cuando:**
- Reinicias el dispositivo/emulador
- Desconectas y reconectas el dispositivo
- Cambias de red WiFi

### **Solución:**
Ejecuta `./reverse-all.sh` nuevamente o usa la tarea Gradle.

### **Verificación:**
```bash
# Ver dispositivos conectados
~/Library/Android/sdk/platform-tools/adb devices

# Ver reverse ports activos
~/Library/Android/sdk/platform-tools/adb reverse --list
```

## 🎉 **Beneficios**

1. **Una sola URL**: No más cambios de IP
2. **Funciona en todo**: Emuladores y dispositivos físicos
3. **Automático**: Scripts y tareas Gradle
4. **Seguro**: Solo HTTP en debug
5. **Flexible**: Múltiples puertos soportados

## 🔧 **Configuración en IntelliJ IDEA**

### **Before Launch (Opcional)**
1. **Run** → **Edit Configurations**
2. Seleccionar tu configuración de Android
3. **Before Launch** → **Add** → **External Tool**
4. **Program**: `bash`
5. **Arguments**: `-c "cd /path/to/project && ./reverse-all.sh"`

Con esto, ADB reverse se aplicará automáticamente antes de cada ejecución.

## 📊 **Estructura de Archivos**

```
IpsAdmin/
├── reverse-all.sh                           # Script ADB reverse
├── presentation/
│   ├── src/
│   │   ├── debug/
│   │   │   ├── AndroidManifest.xml          # Manifest para debug
│   │   │   └── res/xml/
│   │   │       └── debug_network_security_config.xml
│   │   └── main/
│   │       └── AndroidManifest.xml          # Manifest principal
│   └── build.gradle                         # Tareas Gradle
└── UNIFIED_DEVELOPMENT_README.md            # Esta documentación
```

¡Ya tienes una configuración unificada que funciona perfectamente en cualquier dispositivo!
