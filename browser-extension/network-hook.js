(() => {
  "use strict";
  if (window.__autoTouchUzexObserverInstalled) return;
  window.__autoTouchUzexObserverInstalled = true;

  const EVENT_NAME = "autotouch-uzex-result";
  const CLOCK_EVENT_NAME = "autotouch-uzex-clock";
  const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);
  const STRONG_KEY = /(accept|register|create|order|trade|deal|transaction|auction|request|application)/i;
  const TIME_KEY = /(time|date|timestamp|moment|at$)/i;
  const RESULT_KEYWORDS = /(accept|accepted|registered|confirmed|success|successfully|order|trade|deal|qabul|buyurtma|bitim|tasdiq|yuborildi|qabul qilindi)/i;
  const seenSignals = new Map();

  function pageDate() {
    const text = document.querySelector("#clock-label")?.textContent || "";
    const match = text.match(/(\d{2})\.(\d{2})\.(\d{4})/);
    return match ? `${match[3]}-${match[2]}-${match[1]}` : null;
  }

  function toEpochMillis(value) {
    if (typeof value === "number") {
      if (value >= 1_000_000_000_000 && value < 10_000_000_000_000) return Math.trunc(value);
      if (value >= 1_000_000_000 && value < 10_000_000_000) return Math.trunc(value * 1000);
      return null;
    }
    if (typeof value !== "string") return null;
    const text = value.trim();
    if (!text) return null;

    if (/^\d{2}:\d{2}:\d{2}\.\d{3}$/.test(text)) {
      const date = pageDate();
      if (!date) return null;
      const parsed = Date.parse(`${date}T${text}+05:00`);
      return Number.isFinite(parsed) ? parsed : null;
    }

    if (/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}/.test(text)) {
      const normalized = text.includes("T") ? text : text.replace(" ", "T");
      const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
      const parsed = Date.parse(hasZone ? normalized : `${normalized}+05:00`);
      return Number.isFinite(parsed) ? parsed : null;
    }
    return null;
  }

  function candidateScore(key, value) {
    let score = 0;
    if (TIME_KEY.test(key)) score += 2;
    if (STRONG_KEY.test(key)) score += 3;
    if (typeof value === "string" && value.includes(".")) score += 1;
    return score;
  }

  function findBestCandidate(text) {
    let best = null;
    let visited = 0;

    function consider(key, value) {
      const epochMillis = toEpochMillis(value);
      if (epochMillis == null) return;
      const score = candidateScore(String(key || "value"), value);
      if (!best || score > best.score) {
        best = {
          key: String(key || "value").slice(0, 120),
          value: String(value).slice(0, 120),
          epochMillis,
          score
        };
      }
    }

    function walk(value, path, depth) {
      if (visited++ > 300 || depth > 7 || value == null) return;
      if (typeof value !== "object") {
        consider(path, value);
        return;
      }
      if (Array.isArray(value)) {
        for (let i = 0; i < Math.min(value.length, 30); i++) {
          walk(value[i], `${path}[${i}]`, depth + 1);
        }
        return;
      }
      for (const [key, child] of Object.entries(value)) {
        walk(child, path ? `${path}.${key}` : key, depth + 1);
      }
    }

    try {
      walk(JSON.parse(text), "", 0);
    } catch (_) {
      const iso = text.match(/\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d{1,7})?(?:Z|[+-]\d{2}:?\d{2})?/);
      const clock = text.match(/\b\d{2}:\d{2}:\d{2}\.\d{3}\b/);
      if (iso) consider("response.timestamp", iso[0]);
      if (clock) consider("response.time", clock[0]);
    }
    return best;
  }

  function safePath(url) {
    try {
      return new URL(url, location.href).pathname.slice(0, 300);
    } catch (_) {
      return "unknown";
    }
  }

  function inspectResponse(meta, text) {
    if (!MUTATING_METHODS.has(meta.method)
        && meta.source !== "websocket"
        && meta.source !== "dom") return;
    const candidate = findBestCandidate(String(text || "").slice(0, 500_000));
    const bodyText = String(text || "");
    const isResultLike = RESULT_KEYWORDS.test(bodyText);
    const minScore = meta.source === "dom" ? 4 : 3;
    if (!candidate || candidate.score < minScore) return;
    if (meta.source === "dom" && !isResultLike) return;

    const signalKey = [
      meta.source,
      meta.method,
      safePath(meta.url),
      candidate.key,
      candidate.value,
      candidate.epochMillis
    ].join("|");
    const now = performance.now();
    const previous = seenSignals.get(signalKey);
    if (previous && now - previous < 200) return;
    seenSignals.set(signalKey, now);

    window.dispatchEvent(new CustomEvent(EVENT_NAME, {
      detail: {
        source: meta.source,
        method: meta.method,
        path: safePath(meta.url),
        status: Number(meta.status || 0),
        durationMs: Number(meta.durationMs || 0),
        observedAtEpochMs: Date.now(),
        candidateKey: candidate?.key || "",
        candidateValue: candidate?.value || "",
        candidateEpochMs: candidate?.epochMillis || 0,
        candidateScore: candidate?.score || 0
      }
    }));
  }

  const originalFetch = window.fetch;
  window.fetch = async function(input, init) {
    const method = String(init?.method || input?.method || "GET").toUpperCase();
    const url = String(input?.url || input);
    const started = performance.now();
    const response = await originalFetch.apply(this, arguments);
    if (MUTATING_METHODS.has(method)) {
      response.clone().text().then(text => inspectResponse({
        source: "fetch",
        method,
        url,
        status: response.status,
        durationMs: performance.now() - started
      }, text)).catch(() => {});
    }
    return response;
  };

  const originalOpen = XMLHttpRequest.prototype.open;
  const originalSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(method, url) {
    this.__autoTouchMeta = {
      method: String(method || "GET").toUpperCase(),
      url: String(url || ""),
      started: 0
    };
    return originalOpen.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function() {
    const xhr = this;
    const meta = xhr.__autoTouchMeta || { method: "GET", url: "", started: 0 };
    meta.started = performance.now();
    if (MUTATING_METHODS.has(meta.method)) {
      xhr.addEventListener("loadend", () => {
        let text = "";
        try {
          if (!xhr.responseType || xhr.responseType === "text") text = xhr.responseText || "";
          else if (xhr.responseType === "json") text = JSON.stringify(xhr.response);
        } catch (_) {}
        inspectResponse({
          source: "xhr",
          method: meta.method,
          url: meta.url,
          status: xhr.status,
          durationMs: performance.now() - meta.started
        }, text);
      }, { once: true });
    }
    return originalSend.apply(this, arguments);
  };

  const NativeWebSocket = window.WebSocket;
  function ObservedWebSocket(url, protocols) {
    const socket = protocols === undefined
      ? new NativeWebSocket(url)
      : new NativeWebSocket(url, protocols);
    const openedAt = performance.now();
    socket.addEventListener("message", event => {
      const inspect = text => inspectResponse({
        source: "websocket",
        method: "WS",
        url: String(url),
        status: 200,
        durationMs: performance.now() - openedAt
      }, text);
      if (typeof event.data === "string") inspect(event.data);
      else if (event.data instanceof Blob) event.data.text().then(inspect).catch(() => {});
    });
    return socket;
  }
  ObservedWebSocket.prototype = NativeWebSocket.prototype;
  Object.defineProperties(ObservedWebSocket, {
    CONNECTING: { value: NativeWebSocket.CONNECTING },
    OPEN: { value: NativeWebSocket.OPEN },
    CLOSING: { value: NativeWebSocket.CLOSING },
    CLOSED: { value: NativeWebSocket.CLOSED }
  });
  window.WebSocket = ObservedWebSocket;

  function installDomObserver() {
    if (!document.documentElement) return;
    const resultWords = /(accept|accepted|register|order|trade|deal|qabul|buyurtma|bitim|tasdiq|yuborildi|success|confirmed|qabul qilindi)/i;
    let lastClockText = "";

    const emitClock = () => {
      const text = String(document.querySelector("#clock-label")?.textContent || "").trim();
      if (!text || text === lastClockText) return;
      lastClockText = text;
      window.dispatchEvent(new CustomEvent(CLOCK_EVENT_NAME, {
        detail: {
          text,
          observedAtEpochMs: Date.now(),
          path: location.pathname.slice(0, 300)
        }
      }));
    };

    const observer = new MutationObserver(mutations => {
      let checked = 0;
      for (const mutation of mutations) {
        for (const node of mutation.addedNodes) {
          if (checked++ > 20) return;
          const text = String(node.textContent || "").trim().slice(0, 5_000);
          if (!resultWords.test(text)) continue;
          inspectResponse({
            source: "dom",
            method: "DOM",
            url: location.href,
            status: 200,
            durationMs: 0
          }, text);
        }
      }
      emitClock();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
    emitClock();
    setInterval(emitClock, 500);
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", installDomObserver, { once: true });
  } else {
    installDomObserver();
  }
})();
