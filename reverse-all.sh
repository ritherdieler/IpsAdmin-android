#!/usr/bin/env bash

# Script para aplicar ADB reverse a todos los dispositivos conectados
# Funciona con emuladores (AVD, Genymotion) y dispositivos fÃ­sicos

echo "ðŸ”„ Aplicando ADB reverse a todos los dispositivos conectados..."

# Puertos que necesitas redirigir
PORTS=(8080 3000 5173)

# Ruta completa de ADB
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"

# Verificar que ADB existe
if [ ! -f "$ADB_PATH" ]; then
    echo "âŒ ADB no encontrado en: $ADB_PATH"
    echo "ðŸ’¡ AsegÃºrate de tener Android SDK instalado"
    exit 1
fi

# Obtener lista de dispositivos conectados
DEVICES=$($ADB_PATH devices | awk 'NR>1 && $2=="device"{print $1}')

if [ -z "$DEVICES" ]; then
    echo "âŒ No se encontraron dispositivos conectados"
    echo "ðŸ’¡ AsegÃºrate de que tu dispositivo/emulador estÃ© conectado y con USB debugging habilitado"
    exit 1
fi

echo "ðŸ“± Dispositivos encontrados:"
echo "$DEVICES"
echo ""

# Aplicar reverse a cada dispositivo
for serial in $DEVICES; do
    echo "ðŸ”§ Configurando dispositivo: $serial"
    
    for port in "${PORTS[@]}"; do
        if $ADB_PATH -s "$serial" reverse tcp:$port tcp:$port; then
            echo "  âœ… Puerto $port redirigido"
        else
            echo "  âš ï¸  Error al redirigir puerto $port"
        fi
    done
    
    echo "âœ… Reverse aplicado a $serial"
    echo ""
done

echo "ðŸŽ‰ ConfiguraciÃ³n completada!"
echo ""
echo "ðŸ“‹ Resumen:"
$ADB_PATH devices
echo ""
echo "ðŸ”— Tu app puede usar: http://127.0.0.1:8080/"
echo "ðŸ’¡ Funciona en emuladores y dispositivos fÃ­sicos"
