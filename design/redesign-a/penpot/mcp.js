// Library loaded into the Penpot MCP `storage` (execute_code) to build the boards produced by
// compactar.py. Same drawing rules as plugin.js; the data arrives as compact arrays because the
// MCP plugin cannot read files from this machine.
storage.T = storage.T || { styles: [], colors: [], icons: [], images: [], shadows: [], radii: [] };
storage.boards = storage.boards || {};
storage.hotspots = storage.hotspots || [];
storage.imageData = storage.imageData || {};
storage.fonts = storage.fonts || {};

storage.font = (family) => {
  if (!(family in storage.fonts)) storage.fonts[family] = penpot.fonts.findByName(family) || penpot.fonts.findByName("Roboto") || null;
  return storage.fonts[family];
};

storage.variant = (font, weight, italic) => {
  const style = italic ? "italic" : "normal";
  return font.variants.find((v) => String(v.fontWeight) === String(weight) && v.fontStyle === style) ||
    font.variants.find((v) => String(v.fontWeight) === String(weight)) || font.variants[0];
};

storage.fill = (index) => {
  if (index === null || index === undefined) return [];
  const [color, opacity] = storage.T.colors[index];
  return [{ fillColor: color.toUpperCase(), fillOpacity: opacity }];
};

// style: [family, weight, italic, size, lineHeight, letterSpacing, color, transform, decoration, align]
storage.applyStyle = (target, index, isRange) => {
  const [family, weight, italic, size, lineHeight, letterSpacing, color, transform, decoration, align] = storage.T.styles[index];
  const font = storage.font(family);
  if (font) {
    const variant = storage.variant(font, weight, italic);
    if (isRange) font.applyToRange(target, variant); else font.applyToText(target, variant);
  }
  target.fontSize = String(size);
  target.letterSpacing = String(letterSpacing);
  if (!isRange) { target.lineHeight = String(lineHeight); target.align = align; }
  target.fills = storage.fill(color);
  if (transform) target.textTransform = transform;
  if (decoration) target.textDecoration = decoration;
};

storage.at = (board, shape, x, y) => {
  board.appendChild(shape);
  shape.x = board.x + x;
  shape.y = board.y + y;
};

storage.rect = (board, [, x, y, w, h, fill, stroke, radius, shadow], name) => {
  const shape = penpot.createRectangle();
  shape.name = name || "Rectángulo";
  shape.resize(Math.max(w, 0.5), Math.max(h, 0.5));
  storage.at(board, shape, x, y);
  shape.fills = storage.fill(fill);
  if (stroke) {
    const [color, opacity] = storage.T.colors[stroke[0]];
    shape.strokes = [{ strokeColor: color.toUpperCase(), strokeOpacity: opacity, strokeWidth: stroke[1], strokeAlignment: "inner", strokeStyle: "solid" }];
  }
  if (radius !== null && radius !== undefined) {
    const r = storage.T.radii[radius];
    shape.borderRadiusTopLeft = r[0]; shape.borderRadiusTopRight = r[1];
    shape.borderRadiusBottomRight = r[2]; shape.borderRadiusBottomLeft = r[3];
  }
  if (shadow !== null && shadow !== undefined) {
    shape.shadows = storage.T.shadows[shadow].map(([inset, sx, sy, blur, spread, color, opacity]) => ({
      style: inset ? "inner-shadow" : "drop-shadow", offsetX: sx, offsetY: sy, blur, spread,
      color: { color: color.toUpperCase(), opacity },
    }));
  }
  return shape;
};

storage.text = (board, [, x, y, w, h, characters, style, flags, segments]) => {
  const shape = penpot.createText(characters);
  if (!shape) return null;
  board.appendChild(shape);
  storage.applyStyle(shape, style, false);
  for (const [start, end, segStyle] of segments || []) storage.applyStyle(shape.getRange(start, end), segStyle, true);
  if (flags) {
    shape.growType = "fixed";
    shape.resize(w, h);
    shape.growType = "auto-height";
  } else {
    shape.growType = "auto-width";
  }
  shape.x = board.x + x;
  shape.y = board.y + y;
  return shape;
};

storage.svg = (board, [, x, y, w, h, icon]) => {
  const group = penpot.createShapeFromSvg(storage.T.icons[icon]);
  if (!group) return null;
  group.name = "Ícono";
  storage.at(board, group, x, y);
  if (Math.abs(group.width - w) > 0.5 || Math.abs(group.height - h) > 0.5) group.resize(w, h);
  return group;
};

storage.image = async (board, node) => {
  const index = node[5];
  if (!storage.imageData[index]) {
    const [mime, base64] = storage.T.images[index];
    const raw = atob(base64);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
    storage.imageData[index] = await penpot.uploadMediaData("imagen-" + index, bytes, mime);
  }
  const shape = storage.rect(board, ["r", node[1], node[2], node[3], node[4], null, null, null, null], "Imagen");
  if (node[6]) {
    const r = node[6];
    shape.borderRadiusTopLeft = r[0]; shape.borderRadiusTopRight = r[1];
    shape.borderRadiusBottomRight = r[2]; shape.borderRadiusBottomLeft = r[3];
  }
  shape.fills = [{ fillOpacity: 1, fillImage: storage.imageData[index] }];
  return shape;
};

storage.board = async (step) => {
  const [x, y, w, h] = step.box;
  const board = penpot.createBoard();
  board.name = step.title;
  board.resize(w, h);
  board.x = x;
  board.y = y;
  board.fills = storage.fill(step.fill);
  board.clipContent = true;
  for (const node of step.nodes) {
    if (node[0] === "r") storage.rect(board, node, null);
    else if (node[0] === "t") storage.text(board, node);
    else if (node[0] === "s") storage.svg(board, node);
    else if (node[0] === "i") await storage.image(board, node);
  }
  for (const [lx, ly, lw, lh, target] of step.links) {
    const spot = storage.rect(board, ["r", lx, ly, lw, lh, null, null, null, null], "Enlace → " + target);
    storage.hotspots.push({ id: spot.id, target });
  }
  storage.boards[step.screen] = board.id;
  return step.title + " · " + step.nodes.length + " capas";
};

storage.page = (name) => {
  const empty = penpot.currentPage.root.children.length === 0;
  const page = empty ? penpot.currentPage : penpot.createPage();
  page.name = name;
  if (!empty) penpot.openPage(page, false);
  return page.name;
};

storage.title = (step) => {
  const shape = penpot.createText(step.text);
  const font = storage.font("Roboto Flex");
  if (font) font.applyToText(shape, storage.variant(font, "700", false));
  shape.fontSize = String(step.size);
  shape.fills = [{ fillColor: "#10142B", fillOpacity: 1 }];
  shape.growType = "auto-width";
  shape.x = step.x;
  shape.y = step.y;
  return step.text;
};

storage.run = async (steps) => {
  const done = [];
  for (const step of steps) {
    if (step.type === "page") done.push("página " + storage.page(step.name));
    else if (step.type === "title") done.push("título " + storage.title(step));
    else if (step.type === "board") done.push(await storage.board(step));
  }
  return done;
};

storage.link = () => {
  let count = 0;
  for (const spot of storage.hotspots) {
    const shape = penpotUtils.findShapeById(spot.id);
    const destination = storage.boards[spot.target] && penpotUtils.findShapeById(storage.boards[spot.target]);
    if (!shape || !destination) continue;
    shape.addInteraction("click", { type: "navigate-to", destination });
    count += 1;
  }
  return count;
};

storage.flows = (list) => {
  const created = [];
  for (const flow of list) {
    const start = storage.boards[flow.start] && penpotUtils.findShapeById(storage.boards[flow.start]);
    if (!start) continue;
    penpot.currentPage.createFlow(flow.name, start);
    created.push(flow.name);
  }
  return created.length;
};
