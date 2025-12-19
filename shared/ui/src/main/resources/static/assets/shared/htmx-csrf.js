(() => {
  function getMetaContent(name) {
    const meta = document.querySelector(`meta[name="${name}"]`);
    return meta ? meta.getAttribute("content") : null;
  }

  document.addEventListener("htmx:configRequest", (event) => {
    const csrfHeader = getMetaContent("_csrf_header");
    const csrfToken = getMetaContent("_csrf");

    if (!csrfHeader || !csrfToken) return;

    event.detail.headers[csrfHeader] = csrfToken;
  });
})();

