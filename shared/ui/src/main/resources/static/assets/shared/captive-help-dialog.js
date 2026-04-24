class WmCaptiveHelpDialog extends HTMLElement {
  constructor() {
    super();
    this._lastActive = null;
    this._qrGenerated = false;
    this._onKeydown = (event) => {
      if (!this.hasAttribute("open")) return;
      if (event.key === "Escape") {
        event.preventDefault();
        this._close();
      }
    };
  }

  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;

    this.innerHTML = `
      <div class="wm-captive-help-dialog hidden fixed inset-0 z-[60]" hidden>
        <div class="absolute inset-0 bg-slate-900/50" data-role="backdrop"></div>
        <div class="relative flex min-h-full items-center justify-center p-4">
          <div class="w-full max-w-sm rounded-[1.75rem] border border-border bg-surface p-6 shadow-lg"
               role="dialog"
               aria-modal="true">
            <div class="text-lg font-semibold text-text" data-role="title"></div>
            <div class="mt-2 text-sm text-text-muted" data-role="instruction"></div>
            <div class="mt-5 flex flex-col items-center gap-4">
              <div class="rounded-2xl bg-white p-3" data-role="qr"></div>
              <a class="text-sm font-medium text-brand-700 break-all text-center" data-role="url" target="_blank" rel="noopener noreferrer"></a>
            </div>
            <div class="mt-4 flex items-start gap-3 rounded-xl bg-surface-muted px-4 py-3 text-sm text-text-muted">
              <svg xmlns="http://www.w3.org/2000/svg" class="mt-0.5 h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                <path d="M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0" />
                <path d="M12 16v.01" />
                <path d="M12 13a2 2 0 0 0 .914 -3.782a1.98 1.98 0 0 0 -2.414 .483" />
              </svg>
              <span data-role="mobile-data-note"></span>
            </div>
            <div class="mt-5 flex justify-end">
              <button type="button" class="btn btn-secondary" data-role="close"></button>
            </div>
          </div>
        </div>
      </div>
    `;

    this._root = this.querySelector(".wm-captive-help-dialog");
    this._panel = this.querySelector("[role='dialog']");
    this._titleEl = this.querySelector("[data-role='title']");
    this._instructionEl = this.querySelector("[data-role='instruction']");
    this._qrEl = this.querySelector("[data-role='qr']");
    this._urlEl = this.querySelector("[data-role='url']");
    this._mobileDataNoteEl = this.querySelector("span[data-role='mobile-data-note']");
    this._closeBtn = this.querySelector("[data-role='close']");

    this._syncLabels();

    this.removeAttribute("open");
    this._root.classList.add("hidden");
    this._root.hidden = true;

    this._closeBtn.addEventListener("click", () => this._close());
    this._root.addEventListener("click", (event) => {
      if (!this._panel || !this._panel.contains(event.target)) {
        this._close();
      }
    });
  }

  static get observedAttributes() {
    return ["title-text", "instruction-text", "mobile-data-text", "close-text", "portal-url"];
  }

  attributeChangedCallback() {
    this._syncLabels();
  }

  _syncLabels() {
    const titleText = this.getAttribute("title-text") || "";
    const instructionText = this.getAttribute("instruction-text") || "";
    const mobileDataText = this.getAttribute("mobile-data-text") || "";
    const closeText = this.getAttribute("close-text") || "Close";
    const portalUrl = this.getAttribute("portal-url") || "";
    if (this._titleEl) this._titleEl.textContent = titleText;
    if (this._instructionEl) this._instructionEl.innerHTML = instructionText;
    if (this._mobileDataNoteEl) this._mobileDataNoteEl.textContent = mobileDataText;
    if (this._closeBtn) this._closeBtn.textContent = closeText;
    if (this._urlEl) {
      this._urlEl.textContent = portalUrl;
      this._urlEl.href = portalUrl;
    }
  }

  open() {
    const portalUrl = this.getAttribute("portal-url") || "";
    this._lastActive = document.activeElement;
    this.setAttribute("open", "");
    this._root.classList.remove("hidden");
    this._root.hidden = false;
    document.addEventListener("keydown", this._onKeydown);

    if (portalUrl && this._qrEl && !this._qrGenerated && typeof QRCode !== "undefined") {
      new QRCode(this._qrEl, {
        text: portalUrl,
        width: 192,
        height: 192,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M,
      });
      this._qrGenerated = true;
    }

    try {
      this._closeBtn.focus();
    } catch (_) {}
  }

  _close() {
    this.removeAttribute("open");
    this._root.classList.add("hidden");
    this._root.hidden = true;
    document.removeEventListener("keydown", this._onKeydown);
    if (this._lastActive && typeof this._lastActive.focus === "function") {
      try {
        this._lastActive.focus();
      } catch (_) {}
    }
    this._lastActive = null;
  }
}

customElements.define("wm-captive-help-dialog", WmCaptiveHelpDialog);
