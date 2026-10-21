// Prototype navigation for the exported screens (copied to pantallas/ by exportar.py).
// - Elements with data-destino navigate like links; clicking where there is no link flashes the links.
// - With ?flujo=<id>&paso=<n> it shows the happy path panel and highlights what to tap.
(function () {
  "use strict";

  var params = new URLSearchParams(location.search);
  var flows = window.FLUJOS || [];
  var flow = flows.find(function (f) { return f.id === params.get("flujo"); });
  var step = flow ? Number(params.get("paso") || 0) : -1;
  var screen = decodeURIComponent(location.pathname.split("/").pop().replace(/\.html$/, ""));

  // Same label rule as exportar.py: aria-label, else the visible text with tags read as spaces.
  function label(el) {
    var aria = el.getAttribute("aria-label");
    if (aria) return aria.trim();
    var parts = [];
    var walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, {
      acceptNode: function (node) { return node.parentElement.closest("svg") ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT; }
    });
    while (walker.nextNode()) parts.push(walker.currentNode.nodeValue);
    return parts.join(" ").replace(/\s+/g, " ").trim();
  }

  function matches(text, key) {
    return key.endsWith("*") ? text.startsWith(key.slice(0, -1)) : text === key;
  }

  function target(el) {
    if (el.dataset && el.dataset.destino) return el.dataset.destino;
    var href = el.getAttribute && el.getAttribute("href");
    return href && href !== "#" ? href.split("?")[0] : null;
  }

  function linkTo(file) {
    if (!flow) return file;
    var next = flow.pasos[step + 1];
    return next && file === next.pantalla + ".html" ? flowUrl(step + 1) : file;
  }

  function flowUrl(index) {
    return flow.pasos[index].pantalla + ".html?flujo=" + encodeURIComponent(flow.id) + "&paso=" + index;
  }

  var style = document.createElement("style");
  style.textContent =
    "[data-destino]{cursor:pointer}" +
    ".nav-flash{outline:2px solid rgba(47,74,208,.85)!important;outline-offset:2px;background-color:rgba(47,74,208,.08)!important}" +
    ".nav-toca{outline:3px solid #ff6b00!important;outline-offset:3px;animation:nav-pulso 1.2s ease-in-out infinite}" +
    "@keyframes nav-pulso{50%{outline-color:rgba(255,107,0,.25)}}" +
    "#nav-panel{position:fixed;z-index:9999;font:14px/20px system-ui,sans-serif;color:#10142b;background:#fff;" +
    "border-radius:14px;box-shadow:0 6px 24px rgba(16,20,43,.22);padding:14px 16px;display:flex;flex-direction:column;gap:8px}" +
    "#nav-panel b{font-size:15px}#nav-panel small{color:#4a5068}" +
    "#nav-panel .fila{display:flex;gap:8px}#nav-panel a{flex:1;text-align:center;padding:8px 10px;border-radius:999px;" +
    "text-decoration:none;font-weight:600;background:#dfe3ff;color:#141d48}#nav-panel a.principal{background:#141d48;color:#fff}" +
    "#nav-panel a[aria-disabled=true]{opacity:.4;pointer-events:none}";
  document.head.appendChild(style);

  document.addEventListener("click", function (event) {
    var el = event.target.closest("a[href], [data-destino]");
    var file = el ? target(el) : null;
    if (!file && document.body.dataset.destino && !event.target.closest("a, button")) file = document.body.dataset.destino;
    if (file) {
      event.preventDefault();
      location.href = linkTo(file);
      return;
    }
    if (el) event.preventDefault();
    var links = document.querySelectorAll("a[href]:not([href='#']), [data-destino]");
    links.forEach(function (link) { link.classList.add("nav-flash"); });
    setTimeout(function () { links.forEach(function (link) { link.classList.remove("nav-flash"); }); }, 700);
  }, true);

  if (!flow || !flow.pasos[step] || flow.pasos[step].pantalla !== screen) return;

  var current = flow.pasos[step];
  if (current.toca && current.toca !== "*") {
    var hit = Array.prototype.find.call(
      document.querySelectorAll("a, button, [data-destino]"),
      function (el) { return matches(label(el), current.toca); }
    );
    if (hit) {
      hit.classList.add("nav-toca");
      hit.scrollIntoView({ block: "nearest" });
    }
  }

  var board = document.body.firstElementChild;
  var width = board ? board.getBoundingClientRect().width : 390;
  var panel = document.createElement("div");
  panel.id = "nav-panel";
  if (innerWidth >= width + 340) {
    panel.style.left = width + 24 + "px";
    panel.style.top = "16px";
    panel.style.width = "280px";
  } else {
    panel.style.left = panel.style.right = panel.style.bottom = "12px";
  }
  var last = step === flow.pasos.length - 1;
  var what = current.toca === "*" ? "Toca la pantalla" : current.toca ? "Toca «" + current.toca.replace(/\*$/, "…") + "»" : "";
  panel.innerHTML =
    "<small>Happy path · " + (flow.rol || "") + "</small><b></b>" +
    "<small>Paso " + (step + 1) + " de " + flow.pasos.length + "</small>" +
    (what ? "<div class='que'></div>" : "") + (current.nota ? "<small class='nota'></small>" : "") +
    (last ? "<div>Fin del recorrido.</div>" : "") +
    "<div class='fila'><a class='ant'>← Anterior</a><a class='sig principal'>" + (last ? "Ver todos" : "Siguiente →") + "</a></div>" +
    "<div class='fila'><a href='flujos.html'>Happy paths</a><a href='index.html'>Pantallas</a></div>";
  panel.querySelector("b").textContent = flow.nombre;
  if (what) panel.querySelector(".que").textContent = what;
  if (current.nota) panel.querySelector(".nota").textContent = current.nota;
  var previous = panel.querySelector(".ant");
  var next = panel.querySelector(".sig");
  if (step > 0) previous.href = flowUrl(step - 1); else previous.setAttribute("aria-disabled", "true");
  next.href = last ? "flujos.html" : flowUrl(step + 1);
  document.body.appendChild(panel);

  document.addEventListener("keydown", function (event) {
    if (event.key === "ArrowRight") location.href = next.href;
    if (event.key === "ArrowLeft" && step > 0) location.href = previous.href;
  });
})();
