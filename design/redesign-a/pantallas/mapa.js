// Navigation map (copied to pantallas/ by exportar.py): lays out the boards and draws a line
// from every link to the board it opens, like a prototype view. Needs the pages served over
// http so the map can read the links inside each board.
(function () {
  "use strict";

  var DATA = window.MAPA;
  var FLOWS = window.FLUJOS || [];
  var BACK = /^(Atrás|Cerrar|Volver|Cerrar registro)$/;
  var GAP = 160;
  var DEFS = '<defs><marker id="flecha" markerUnits="userSpaceOnUse" markerWidth="18" markerHeight="18" refX="16" refY="9" orient="auto">' +
    '<path d="M0,0 L18,9 L0,18 z" fill="#0d99ff"/></marker></defs>';

  var stage = document.getElementById("stage");
  var inner = document.getElementById("inner");
  var svg = document.getElementById("lines");
  var pageSelect = document.getElementById("modulo");
  var flowSelect = document.getElementById("flujo");
  var showBack = document.getElementById("volver");
  var showNav = document.getElementById("barra");
  var zoomLabel = document.getElementById("zoom");
  var zoom = 0.5;
  var frames = [];

  function label(el) {
    var aria = el.getAttribute("aria-label");
    if (aria) return aria.trim();
    var parts = [];
    var walker = el.ownerDocument.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    while (walker.nextNode()) if (!walker.currentNode.parentElement.closest("svg")) parts.push(walker.currentNode.nodeValue);
    return parts.join(" ").replace(/\s+/g, " ").trim();
  }

  function matches(text, key) {
    return key.endsWith("*") ? text.startsWith(key.slice(0, -1)) : text === key;
  }

  function targetOf(el) {
    var file = el.dataset.destino || el.getAttribute("href") || "";
    file = file.split("?")[0].split("#")[0];
    return /^a-.+\.html$/.test(file) ? file.replace(/\.html$/, "") : null;
  }

  function fill(select, options) {
    options.forEach(function (option) {
      var item = document.createElement("option");
      item.value = option[0];
      item.textContent = option[1];
      select.appendChild(item);
    });
  }

  fill(pageSelect, DATA.pages.map(function (page) { return [page.id, page.name]; }));
  fill(flowSelect, [["", "Todas las pantallas del módulo"]].concat(FLOWS.map(function (flow) {
    return [flow.id, (flow.clave ? "★ " : "") + flow.nombre];
  })));

  function readHash() {
    var hash = new URLSearchParams(location.hash.slice(1));
    flowSelect.value = hash.get("flujo") || "";
    pageSelect.value = hash.get("modulo") || DATA.pages[1].id;
  }

  function writeHash() {
    location.hash = flowSelect.value ? "flujo=" + flowSelect.value : "modulo=" + pageSelect.value;
  }

  function layout() {
    var flow = FLOWS.find(function (f) { return f.id === flowSelect.value; });
    pageSelect.disabled = !!flow;
    if (flow) {
      var x = 0;
      return flow.pasos.map(function (step, index) {
        var board = DATA.boards[step.pantalla];
        var frame = { screen: step.pantalla, board: board, x: x, y: 60, step: step, index: index };
        x += board.w + GAP + 60;
        return frame;
      });
    }
    return DATA.order.filter(function (screen) { return DATA.boards[screen].page === pageSelect.value; })
      .map(function (screen) {
        var board = DATA.boards[screen];
        return { screen: screen, board: board, x: board.x, y: board.y + 60 };
      });
  }

  function render() {
    inner.querySelectorAll(".frame, .fuera, .toque").forEach(function (node) { node.remove(); });
    svg.innerHTML = DEFS;
    frames = layout();
    var minY = Math.min.apply(null, frames.map(function (f) { return f.y; }));
    var width = 0;
    var height = 0;
    frames.forEach(function (frame) {
      frame.y -= minY - 60;
      width = Math.max(width, frame.x + frame.board.w);
      height = Math.max(height, frame.y + frame.board.h);
      var node = document.createElement("a");
      node.className = "frame";
      node.href = frame.screen + ".html";
      node.target = "_blank";
      node.style.cssText = "left:" + frame.x + "px;top:" + frame.y + "px;width:" + frame.board.w + "px;height:" + frame.board.h + "px";
      var title = frame.step ? frame.index + 1 + ". " + frame.board.title : frame.board.title;
      node.innerHTML = "<span class='titulo'></span><iframe scrolling='no' tabindex='-1'></iframe>";
      node.querySelector(".titulo").textContent = title;
      if (frame.step && frame.step.nota) {
        var note = document.createElement("span");
        note.className = "nota";
        note.textContent = frame.step.nota;
        node.appendChild(note);
      }
      var iframe = node.querySelector("iframe");
      iframe.style.cssText = "width:" + frame.board.w + "px;height:" + frame.board.h + "px";
      iframe.addEventListener("load", function () { frame.doc = iframe.contentDocument; draw(); });
      iframe.src = frame.screen + ".html";
      node.addEventListener("mouseenter", function () { focus(frame.screen); });
      node.addEventListener("mouseleave", function () { focus(null); });
      inner.appendChild(node);
      frame.node = node;
    });
    inner.style.width = width + 420 + "px";
    inner.style.height = height + 120 + "px";
    applyZoom();
  }

  function applyZoom() {
    inner.style.transform = "scale(" + zoom + ")";
    stage.style.width = parseFloat(inner.style.width) * zoom + "px";
    stage.style.height = parseFloat(inner.style.height) * zoom + "px";
    zoomLabel.textContent = Math.round(zoom * 100) + " %";
  }

  function links(frame) {
    if (!frame.doc) return [];
    var found = [];
    var flowStep = frame.step;
    if (flowStep) {
      var next = frames[frame.index + 1];
      if (!next) return [];
      var source = null;
      if (flowStep.toca && flowStep.toca !== "*") {
        source = Array.prototype.find.call(frame.doc.querySelectorAll("a, button, [data-destino]"), function (el) {
          return matches(label(el), flowStep.toca);
        });
      }
      return [{ el: source, target: next, dashed: !flowStep.toca }];
    }
    frame.doc.querySelectorAll("a[href], [data-destino]").forEach(function (el) {
      if (el === frame.doc.body) return;
      var target = targetOf(el);
      if (!target || target === frame.screen) return;
      var text = label(el);
      if (!showBack.checked && BACK.test(text)) return;
      if (!showNav.checked && el.closest("nav")) return;
      found.push({ el: el, target: target });
    });
    var whole = frame.doc.body.dataset.destino;
    if (whole) found.push({ el: null, target: whole.replace(/\.html$/, "") });
    return found;
  }

  function anchorOf(frame, el) {
    if (!el) return { left: frame.x, right: frame.x + frame.board.w, top: frame.y, bottom: frame.y + 40 };
    var rect = el.getBoundingClientRect();
    return { left: frame.x + rect.left, right: frame.x + rect.right, top: frame.y + rect.top, bottom: frame.y + rect.bottom };
  }

  function path(from, to, dashed, screen) {
    var line = document.createElementNS("http://www.w3.org/2000/svg", "path");
    var bend = Math.max(80, Math.abs(to.x - from.x) / 2);
    var c1x = from.x + (to.x >= from.x ? bend : -bend);
    var c2x = to.x + (to.x >= from.x ? -bend : bend);
    line.setAttribute("d", "M" + from.x + "," + from.y + " C" + c1x + "," + from.y + " " + c2x + "," + to.y + " " + to.x + "," + to.y);
    line.setAttribute("class", "enlace" + (dashed ? " auto" : ""));
    line.dataset.from = screen;
    svg.appendChild(line);
    var dot = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    dot.setAttribute("cx", from.x);
    dot.setAttribute("cy", from.y);
    dot.setAttribute("r", 6);
    dot.setAttribute("class", "origen");
    dot.dataset.from = screen;
    svg.appendChild(dot);
  }

  function outside(frame, rect, target) {
    var board = DATA.boards[target];
    var page = DATA.pages.find(function (p) { return p.id === board.page; });
    var from = { x: rect.right, y: (rect.top + rect.bottom) / 2 };
    var to = { x: frame.x + frame.board.w + 40, y: from.y };
    path(from, to, false, frame.screen);
    var chip = document.createElement("button");
    chip.className = "fuera";
    chip.style.cssText = "left:" + (to.x + 6) + "px;top:" + (to.y - 18) + "px";
    chip.textContent = "→ " + board.title + " · " + page.name.replace(/^A · /, "");
    chip.dataset.from = frame.screen;
    chip.addEventListener("click", function () { flowSelect.value = ""; pageSelect.value = board.page; writeHash(); });
    inner.appendChild(chip);
  }

  function draw() {
    svg.innerHTML = DEFS;
    inner.querySelectorAll(".fuera, .toque").forEach(function (node) { node.remove(); });
    svg.setAttribute("width", inner.style.width);
    svg.setAttribute("height", inner.style.height);
    frames.forEach(function (frame) {
      links(frame).forEach(function (link) {
        var rect = anchorOf(frame, link.el);
        if (link.el) {
          var mark = document.createElement("span");
          mark.className = "toque";
          mark.style.cssText = "left:" + rect.left + "px;top:" + rect.top + "px;width:" + (rect.right - rect.left) + "px;height:" + (rect.bottom - rect.top) + "px";
          inner.appendChild(mark);
        }
        var target = typeof link.target === "string"
          ? frames.find(function (f) { return f.screen === link.target; })
          : link.target;
        if (!target) {
          outside(frame, rect, link.target);
          return;
        }
        var goesRight = target.x + target.board.w / 2 >= (rect.left + rect.right) / 2;
        var from = { x: goesRight ? rect.right : rect.left, y: (rect.top + rect.bottom) / 2 };
        var to = { x: goesRight ? target.x : target.x + target.board.w, y: target.y + 70 };
        path(from, to, link.dashed, frame.screen);
      });
    });
  }

  function focus(screen) {
    inner.classList.toggle("enfocado", !!screen);
    inner.querySelectorAll("[data-from]").forEach(function (node) {
      node.classList.toggle("activo", node.dataset.from === screen);
    });
  }

  document.getElementById("mas").addEventListener("click", function () { zoom = Math.min(1, zoom + 0.1); applyZoom(); });
  document.getElementById("menos").addEventListener("click", function () { zoom = Math.max(0.2, zoom - 0.1); applyZoom(); });
  pageSelect.addEventListener("change", function () { flowSelect.value = ""; writeHash(); });
  flowSelect.addEventListener("change", writeHash);
  showBack.addEventListener("change", draw);
  showNav.addEventListener("change", draw);
  window.addEventListener("hashchange", function () { readHash(); render(); });
  readHash();
  render();
  setTimeout(draw, 1500);
})();
