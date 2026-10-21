// Alternative to the plugin for browsers that block Penpot from loading localhost (Penpot cannot
// fetch the plugin). 1) Open http://localhost:8766/penpot/ui.html?modo=exportar and press Importar:
// it saves penpot/diseno.json. 2) Copy that file to the clipboard (macOS: pbcopy < penpot/diseno.json).
// 3) Paste this script in the console of the Penpot file tab, press «Pegar diseño» and paste (Cmd/Ctrl + V).
// It runs plugin.js with Penpot's own plugin API (window.ɵcontext). Generated from plugin.js.
(() => {
  let handler = null;
  const errors = [];
  const ui = {
    open: () => {
      // Reading the clipboard from script needs a permission some browsers deny; a real paste
      // (Cmd/Ctrl + V) into this field does not.
      const field = document.createElement("textarea");
      field.placeholder = "Pega aquí penpot/diseno.json (Cmd/Ctrl + V)";
      field.style.cssText = "position:fixed;z-index:99999;right:16px;bottom:64px;width:320px;height:64px;padding:8px;border-radius:8px;font:12px system-ui;display:none";
      const button = document.createElement("button");
      button.textContent = "Pegar diseño";
      button.style.cssText = "position:fixed;z-index:99999;right:16px;bottom:16px;padding:10px 16px;border:0;border-radius:8px;background:#31efb8;color:#10142b;font:600 14px system-ui;cursor:pointer";
      button.onclick = () => { field.style.display = "block"; field.focus(); };
      const run = async (steps) => {
        for (let i = 0; i < steps.length; i++) {
          button.textContent = "Creando " + (i + 1) + " de " + steps.length + "…";
          await handler({ ...steps[i], id: i });
        }
        button.textContent = errors.length ? "Listo con " + errors.length + " errores (ver consola)" : "Listo";
      };
      field.addEventListener("paste", async (event) => {
        event.preventDefault();
        event.stopPropagation();
        field.style.display = "none";
        await run(JSON.parse(event.clipboardData.getData("text/plain")));
      });
      window.__isp = { run, errors };
      document.body.append(field, button);
    },
    sendMessage: (message) => {
      if (message.type === "error") { errors.push(message.text); console.error("[importar]", message.text); }
      else if (message.text) console.log("[importar]", message.text);
    },
    onMessage: (callback) => { handler = callback; },
  };
  const penpot = new Proxy(window.ɵcontext, {
    get: (target, key) => (key === "ui" ? ui : typeof target[key] === "function" ? target[key].bind(target) : target[key]),
  });

