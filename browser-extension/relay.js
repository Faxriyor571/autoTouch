(() => {
  "use strict";
  const BRIDGE = "http://127.0.0.1:17321";
  let sessionToken = "";

  async function refreshSession() {
    try {
      const response = await fetch(`${BRIDGE}/session`, {
        cache: "no-store",
        credentials: "omit"
      });
      if (response.ok) sessionToken = (await response.text()).trim();
    } catch (_) {
      sessionToken = "";
    }
  }

  async function sendObservation(detail) {
    if (!sessionToken) await refreshSession();
    if (!sessionToken) return;
    const form = new URLSearchParams();
    for (const key of [
      "source", "method", "path", "status", "durationMs",
      "observedAtEpochMs", "candidateKey", "candidateValue",
      "candidateEpochMs", "candidateScore"
    ]) {
      form.set(key, String(detail[key] ?? ""));
    }
    try {
      const response = await fetch(`${BRIDGE}/event`, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          "X-AutoTouch-Session": sessionToken
        },
        body: form.toString(),
        cache: "no-store",
        credentials: "omit"
      });
      if (response.status === 401) sessionToken = "";
    } catch (_) {}
  }

  async function sendClock(detail) {
    if (!sessionToken) await refreshSession();
    if (!sessionToken) return;
    const form = new URLSearchParams();
    form.set("text", String(detail.text || ""));
    form.set("observedAtEpochMs", String(detail.observedAtEpochMs || Date.now()));
    form.set("path", String(detail.path || ""));
    try {
      const response = await fetch(`${BRIDGE}/clock`, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          "X-AutoTouch-Session": sessionToken
        },
        body: form.toString(),
        cache: "no-store",
        credentials: "omit"
      });
      if (response.status === 401) sessionToken = "";
    } catch (_) {}
  }

  window.addEventListener("autotouch-uzex-result", event => {
    const detail = event.detail;
    if (!detail || typeof detail !== "object") return;
    if (!String(detail.path || "").startsWith("/")) return;
    sendObservation(detail);
  });

  window.addEventListener("autotouch-uzex-clock", event => {
    const detail = event.detail;
    if (!detail || typeof detail !== "object") return;
    if (!String(detail.path || "").startsWith("/")) return;
    sendClock(detail);
  });

  refreshSession();
  setInterval(refreshSession, 10_000);
})();
