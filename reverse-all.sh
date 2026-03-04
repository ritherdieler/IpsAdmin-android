#!/usr/bin/env bash

# Script para aplicar ADB reverse a todos los dispositivos conectados
# Funciona con emuladores (AVD, Genymotion) y dispositivos físicos

echo "🔄 Aplicando ADB reverse a todos los dispositivos conectados..."

# Puertos que necesitas redirigir
PORTS=(8080 3000 5173)

# Ruta completa de ADB
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"

# Verificar que ADB existe
if [ ! -f "$ADB_PATH" ]; then
    echo "❌ ADB no encontrado en: $ADB_PATH"
    echo "💡 Asegúrate de tener Android SDK instalado"
    exit 1
fi

# Obtener lista de dispositivos conectados
DEVICES=$($ADB_PATH devices | awk 'NR>1 && $2=="device"{print $1}')

if [ -z "$DEVICES" ]; then
    echo "❌ No se encontraron dispositivos conectados"
    echo "💡 Asegúrate de que tu dispositivo/emulador esté conectado y con USB debugging habilitado"
    exit 1
fi

echo "📱 Dispositivos encontrados:"
echo "$DEVICES"
echo ""

# Aplicar reverse a cada dispositivo
for serial in $DEVICES; do
    echo "🔧 Configurando dispositivo: $serial"
    
    for port in "${PORTS[@]}"; do
        if $ADB_PATH -s "$serial" reverse tcp:$port tcp:$port; then
            echo "  ✅ Puerto $port redirigido"
        else
            echo "  ⚠️  Error al redirigir puerto $port"
        fi
    done
    
    echo "✅ Reverse aplicado a $serial"
    echo ""
done

echo "🎉 Configuración completada!"
echo ""
echo "📋 Resumen:"
$ADB_PATH devices
echo ""
echo "🔗 Tu app puede usar: http://127.0.0.1:8080/"
echo "💡 Funciona en emuladores y dispositivos físicos"
