class WmConfirmDialog extends HTMLElement {
  constructor() {
    super();
    this._onConfirm = null;
    this._onCancel = null;
    this._lastActive = null;
    this._onKeydown = (event) => {
      if (!this.hasAttribute("open")) return;
      if (event.key === "Escape") {
        event.preventDefault();
        this._cancel();
      }
    };
  }

  connectedCallback() {
    if (this._initialized) return;
    this._initialized = true;
    this.innerHTML = `
      <div class="wm-confirm-dialog hidden fixed inset-0 z-[60]" hidden>
        <div class="absolute inset-0 bg-slate-900/50" data-role="backdrop"></div>
          <div class="relative flex min-h-full items-center justify-center p-4">
          <div class="w-full max-w-md rounded-xl border border-border bg-surface p-6 shadow-lg"
               role="dialog"
               aria-modal="true"
               aria-live="assertive">
            <div class="text-lg font-semibold text-text" data-role="title">Confirm action</div>
            <div class="mt-2 text-sm text-text-muted" data-role="message">Are you sure?</div>
            <div class="mt-5 flex justify-end gap-3">
              <button type="button" class="btn btn-secondary" data-role="cancel">Cancel</button>
              <button type="button" class="btn btn-danger" data-role="confirm">Confirm</button>
            </div>
          </div>
        </div>
      </div>
    `;

    this._root = this.querySelector(".wm-confirm-dialog");
    this._titleEl = this.querySelector("[data-role='title']");
    this._messageEl = this.querySelector("[data-role='message']");
    this._confirmBtn = this.querySelector("[data-role='confirm']");
    this._cancelBtn = this.querySelector("[data-role='cancel']");
    this._backdrop = this.querySelector("[data-role='backdrop']");
    this._panel = this.querySelector("[role='dialog']");
    this.removeAttribute("open");
    this._root.classList.add("hidden");
    this._root.hidden = true;

    this._syncLabels();

    this._confirmBtn.addEventListener("click", () => this._confirm());
    this._cancelBtn.addEventListener("click", () => this._cancel());
    this._root.addEventListener("click", (event) => {
      if (!this._panel || !this._panel.contains(event.target)) {
        this._cancel();
      }
    });
  }

  static get observedAttributes() {
    return ["confirm-text", "cancel-text", "title-text"];
  }

  attributeChangedCallback() {
    this._syncLabels();
  }

  _syncLabels() {
    const title = this.getAttribute("title-text") || "Please confirm";
    const confirmText = this.getAttribute("confirm-text") || "Confirm";
    const cancelText = this.getAttribute("cancel-text") || "Cancel";
    if (this._titleEl) this._titleEl.textContent = title;
    if (this._confirmBtn) this._confirmBtn.textContent = confirmText;
    if (this._cancelBtn) this._cancelBtn.textContent = cancelText;
  }

  open({ message, onConfirm, onCancel }) {
    this._lastActive = document.activeElement;
    this._messageEl.textContent = message || "Are you sure?";
    this._onConfirm = onConfirm || null;
    this._onCancel = onCancel || null;
    this.setAttribute("open", "");
    this._root.classList.remove("hidden");
    this._root.hidden = false;
    document.addEventListener("keydown", this._onKeydown);
    try {
      this._confirmBtn.focus();
    } catch (_) {
      this._confirmBtn.blur?.();
    }
  }

  _confirm() {
    this.removeAttribute("open");
    this._root.classList.add("hidden");
    this._root.hidden = true;
    document.removeEventListener("keydown", this._onKeydown);
    if (typeof this._onConfirm === "function") this._onConfirm();
    this._restoreFocus();
  }

  _cancel() {
    this.removeAttribute("open");
    this._root.classList.add("hidden");
    this._root.hidden = true;
    document.removeEventListener("keydown", this._onKeydown);
    if (typeof this._onCancel === "function") this._onCancel();
    this._restoreFocus();
  }

  _restoreFocus() {
    if (this._lastActive && typeof this._lastActive.focus === "function") {
      try {
        this._lastActive.focus();
      } catch (_) {
        this._lastActive.blur?.();
      }
    }
    this._lastActive = null;
  }
}

customElements.define("wm-confirm-dialog", WmConfirmDialog);

document.body.addEventListener("htmx:confirm", (event) => {
  const dialog = document.querySelector("wm-confirm-dialog");
  if (!dialog) return;
  const elt = event.detail?.elt;
  if (!elt || typeof elt.closest !== "function") return;
  const source = elt.closest("[hx-confirm],[data-hx-confirm]");
  if (!source) return;
  const question =
    source.getAttribute("hx-confirm") || source.getAttribute("data-hx-confirm") || "";
  if (String(question).trim().length === 0) return;
  event.preventDefault();
  dialog.open({
    message: question,
    onConfirm: () => event.detail.issueRequest(true),
  });
});
