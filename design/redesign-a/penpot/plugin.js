// Penpot side of the import: receives each board already measured by ui.html and creates it
// with native layers, then the "navigate to" interactions and the happy path flows.
const UI_URL = "http://localhost:8766/penpot/ui.html";

penpot.ui.open("IspAdmin · Importar diseño", UI_URL, { width: 380, height: 520 });

const boards = {};
const hotspots = [];
const fonts = {};

function reply(message) {
  penpot.ui.sendMessage(message);
}

function findFont(family) {
  if (!(family in fonts)) {
    fonts[family] = penpot.fonts.findByName(family) || penpot.fonts.findByName("Roboto") || null;
  }
  return fonts[family];
}

function applyFont(text, node) {
  const font = findFont(node.family);
  if (!font) return;
  const style = node.italic ? "italic" : "normal";
  const weight = String(node.weight);
  const variant =
    font.variants.find((v) => String(v.fontWeight) === weight && v.fontStyle === style) ||
    font.variants.find((v) => String(v.fontWeight) === weight) ||
    font.variants.find((v) => v.fontStyle === style) ||
    font.variants[0];
  font.applyToText(text, variant);
}

function place(board, shape, node) {
  board.appendChild(shape);
  shape.x = board.x + node.x;
  shape.y = board.y + node.y;
}

function rectangle(board, node) {
  const shape = penpot.createRectangle();
  shape.name = node.name || "Rectángulo";
  shape.resize(Math.max(node.w, 0.5), Math.max(node.h, 0.5));
  place(board, shape, node);
  shape.fills = node.fill ? [{ fillColor: node.fill.color, fillOpacity: node.fill.opacity }] : [];
  if (node.stroke) {
    shape.strokes = [{ strokeColor: node.stroke.color, strokeOpacity: node.stroke.opacity, strokeWidth: node.stroke.width, strokeAlignment: "inner", strokeStyle: "solid" }];
  }
  if (node.radius) {
    shape.borderRadiusTopLeft = node.radius[0];
    shape.borderRadiusTopRight = node.radius[1];
    shape.borderRadiusBottomRight = node.radius[2];
    shape.borderRadiusBottomLeft = node.radius[3];
  }
  if (node.shadows && node.shadows.length) {
    shape.shadows = node.shadows.map((s) => ({
      style: s.inset ? "inner-shadow" : "drop-shadow",
      offsetX: s.x, offsetY: s.y, blur: s.blur, spread: s.spread,
      color: { color: s.color, opacity: s.opacity },
    }));
  }
  return shape;
}

function text(board, node) {
  const shape = penpot.createText(node.text);
  if (!shape) return null;
  board.appendChild(shape);
  applyFont(shape, node);
  shape.fontSize = String(node.size);
  shape.lineHeight = String(node.lineHeight);
  shape.letterSpacing = String(node.letterSpacing);
  if (node.transform) shape.textTransform = node.transform;
  if (node.decoration) shape.textDecoration = node.decoration;
  shape.align = node.align;
  shape.fills = [{ fillColor: node.color, fillOpacity: node.opacity }];
  for (const seg of node.segments || []) styleRange(shape.getRange(seg.start, seg.end), seg);
  if (node.wrap || node.fixedWidth) {
    shape.growType = "fixed";
    shape.resize(node.w, node.h);
    shape.growType = "auto-height";
  } else {
    shape.growType = "auto-width";
  }
  shape.x = board.x + node.x;
  shape.y = board.y + node.y;
  return shape;
}

// Bold, color or font changes inside one paragraph.
function styleRange(range, seg) {
  const font = findFont(seg.family);
  if (font) {
    const style = seg.italic ? "italic" : "normal";
    const weight = String(seg.weight);
    const variant =
      font.variants.find((v) => String(v.fontWeight) === weight && v.fontStyle === style) ||
      font.variants.find((v) => String(v.fontWeight) === weight) ||
      font.variants[0];
    font.applyToRange(range, variant);
  }
  range.fontSize = String(seg.size);
  range.letterSpacing = String(seg.letterSpacing);
  range.fills = [{ fillColor: seg.color, fillOpacity: seg.opacity }];
  if (seg.decoration) range.textDecoration = seg.decoration;
  if (seg.transform) range.textTransform = seg.transform;
}

function svg(board, node) {
  const group = penpot.createShapeFromSvg(node.svg);
  if (!group) return null;
  group.name = node.name || "Ícono";
  place(board, group, node);
  if (Math.abs(group.width - node.w) > 0.5 || Math.abs(group.height - node.h) > 0.5) group.resize(node.w, node.h);
  return group;
}

async function image(board, node) {
  const data = await penpot.uploadMediaData(node.name || "imagen", new Uint8Array(node.bytes), node.mime);
  const shape = rectangle(board, { ...node, fill: null });
  shape.fills = [{ fillOpacity: 1, fillImage: data }];
  return shape;
}

async function createBoard(message) {
  const board = penpot.createBoard();
  board.name = message.title;
  board.resize(message.w, message.h);
  board.x = message.x;
  board.y = message.y;
  board.fills = [{ fillColor: message.fill.color, fillOpacity: message.fill.opacity }];
  board.clipContent = true;
  for (const node of message.nodes) {
    if (node.type === "rect") rectangle(board, node);
    else if (node.type === "text") text(board, node);
    else if (node.type === "svg") svg(board, node);
    else if (node.type === "image") await image(board, node);
  }
  for (const spot of message.hotspots) {
    const shape = rectangle(board, { ...spot, name: "Enlace → " + spot.target, fill: null });
    hotspots.push({ shape, target: spot.target });
  }
  boards[message.screen] = board;
  return message.nodes.length;
}

function openPage(name) {
  const page = penpot.createPage();
  page.name = name;
  penpot.openPage(page, false);
}

function title(message) {
  const shape = penpot.createText(message.text);
  if (!shape) return;
  applyFont(shape, { family: "Roboto Flex", weight: 700 });
  shape.fontSize = String(message.size);
  shape.fills = [{ fillColor: "#10142b", fillOpacity: 1 }];
  shape.growType = "auto-width";
  shape.x = message.x;
  shape.y = message.y;
}

function link() {
  let count = 0;
  for (const spot of hotspots) {
    const destination = boards[spot.target];
    if (!destination) continue;
    spot.shape.addInteraction("click", { type: "navigate-to", destination });
    count += 1;
  }
  return count;
}

function flows(list) {
  const page = penpot.currentPage;
  let count = 0;
  for (const flow of list) {
    const start = boards[flow.start];
    if (!page || !start) continue;
    try {
      page.createFlow(flow.name, start);
      count += 1;
    } catch (error) {
      reply({ type: "log", text: "Flujo «" + flow.name + "»: " + error });
    }
  }
  return count;
}

penpot.ui.onMessage(async (message) => {
  try {
    if (message.type === "page") {
      openPage(message.name);
      reply({ type: "ok", id: message.id });
    } else if (message.type === "title") {
      title(message);
      reply({ type: "ok", id: message.id });
    } else if (message.type === "board") {
      const count = await createBoard(message);
      reply({ type: "ok", id: message.id, text: message.title + " · " + count + " capas" });
    } else if (message.type === "links") {
      reply({ type: "ok", id: message.id, text: link() + " enlaces" });
    } else if (message.type === "flows") {
      reply({ type: "ok", id: message.id, text: flows(message.flows) + " happy paths" });
    }
  } catch (error) {
    reply({ type: "error", id: message.id, text: String(error && error.message ? error.message : error) });
  }
});
