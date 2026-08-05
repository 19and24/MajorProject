// Minimal mock backend matching the app's API spec (§3-§5): ApiResponse envelope,
// PageResponse for lists, and the FridgeItem/Ingredient endpoints the app currently calls.
// Run: node mock-server/server.js   (listens on :8080, reachable from the emulator at 10.0.2.2:8080)
const http = require("http");

let nextFridgeItemId = 4;

const ingredients = [
  { id: 1, name: "닭가슴살", category: "육류", emoji: "🍗" },
  { id: 2, name: "우유", category: "유제품", emoji: "🥛" },
  { id: 3, name: "계란", category: "계란", emoji: "🥚" },
  { id: 4, name: "사과", category: "과일", emoji: "🍎" },
  { id: 5, name: "당근", category: "채소", emoji: "🥕" },
];

function addDays(n) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
}

const fridgeItems = [
  { id: 1, ingredient: ingredients[0], quantity: 1, unit: "개", deadline: addDays(-1), starred: false, createdAt: new Date().toISOString() },
  { id: 2, ingredient: ingredients[1], quantity: 1, unit: "L", deadline: addDays(3), starred: true, createdAt: new Date().toISOString() },
  { id: 3, ingredient: ingredients[2], quantity: 6, unit: "개", deadline: addDays(10), starred: false, createdAt: new Date().toISOString() },
];

function daysUntil(deadline) {
  if (!deadline) return null;
  const diff = (new Date(deadline) - new Date(new Date().toDateString())) / 86400000;
  return Math.round(diff);
}

function withComputed(item) {
  const days = daysUntil(item.deadline);
  return { ...item, daysUntilDeadline: days, expired: days !== null && days < 0 };
}

function send(res, status, body) {
  const json = JSON.stringify(body);
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(json);
}

function ok(res, data, status = 200) {
  send(res, status, { success: true, data, message: null });
}

function fail(res, status, errorCode, message) {
  send(res, status, { success: false, data: null, message, errorCode });
}

function page(items) {
  return { items, page: 1, size: items.length, totalElements: items.length, totalPages: 1 };
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = "";
    req.on("data", (chunk) => (raw += chunk));
    req.on("end", () => resolve(raw ? JSON.parse(raw) : {}));
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://localhost");
  const parts = url.pathname.replace(/^\/api\/v1\//, "").split("/").filter(Boolean);
  console.log(req.method, url.pathname);

  // GET /ingredients
  if (req.method === "GET" && parts[0] === "ingredients" && parts.length === 1) {
    const keyword = url.searchParams.get("keyword");
    let list = ingredients;
    if (keyword) list = list.filter((i) => i.name.includes(keyword));
    return ok(res, page(list));
  }

  // GET /fridge-items
  if (req.method === "GET" && parts[0] === "fridge-items" && parts.length === 1) {
    return ok(res, page(fridgeItems.map(withComputed)));
  }

  // POST /fridge-items
  if (req.method === "POST" && parts[0] === "fridge-items" && parts.length === 1) {
    const body = await readBody(req);
    const ingredient = ingredients.find((i) => i.id === body.ingredientId);
    if (!ingredient) return fail(res, 404, "INGREDIENT_NOT_FOUND", "존재하지 않는 식재료예요.");
    const item = {
      id: nextFridgeItemId++,
      ingredient,
      quantity: body.quantity ?? 1,
      unit: body.unit ?? null,
      deadline: body.deadline ?? null,
      starred: body.starred ?? false,
      createdAt: new Date().toISOString(),
    };
    fridgeItems.push(item);
    return ok(res, withComputed(item), 201);
  }

  // DELETE /fridge-items/{id}
  if (req.method === "DELETE" && parts[0] === "fridge-items" && parts.length === 2) {
    const id = Number(parts[1]);
    const idx = fridgeItems.findIndex((i) => i.id === id);
    if (idx === -1) return fail(res, 404, "FRIDGE_ITEM_NOT_FOUND", "존재하지 않는 항목이에요.");
    fridgeItems.splice(idx, 1);
    res.writeHead(204);
    return res.end();
  }

  // PATCH /fridge-items/{id}/star
  if (req.method === "PATCH" && parts[0] === "fridge-items" && parts[2] === "star") {
    const id = Number(parts[1]);
    const item = fridgeItems.find((i) => i.id === id);
    if (!item) return fail(res, 404, "FRIDGE_ITEM_NOT_FOUND", "존재하지 않는 항목이에요.");
    const body = await readBody(req);
    item.starred = !!body.starred;
    return ok(res, { id: item.id, starred: item.starred });
  }

  fail(res, 404, "UNKNOWN", `No mock route for ${req.method} ${url.pathname}`);
});

server.listen(8080, () => console.log("Mock backend listening on http://localhost:8080/api/v1"));
