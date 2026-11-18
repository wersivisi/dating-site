(() => {
    const form = document.querySelector('[data-discover-form]');
    if (!form) return;
    let results = document.querySelector('[data-discover-results]');
    const status = document.querySelector('[data-discover-status]');
    const errorBox = document.querySelector('[data-discover-error]');
    const submit = form.querySelector('button');
    let controller;
    let pendingUrl = location.href;
    let pendingPush = false;

    async function load(url, push) {
        controller?.abort();
        const current = new AbortController();
        controller = current;
        pendingUrl = url;
        pendingPush = push;
        const timeout = setTimeout(() => current.abort(), 10000);
        results.setAttribute('aria-busy', 'true');
        submit.disabled = true;
        status.textContent = 'Загружаем анкеты…';
        errorBox.hidden = true;
        try {
            const response = await fetch(url, {signal: current.signal, credentials: 'same-origin', cache: 'no-store'});
            if (response.redirected) {
                location.assign(response.url);
                return;
            }
            if (!response.ok) throw new Error('Request failed');
            const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
            const next = doc.querySelector('[data-discover-results]');
            if (!next) throw new Error('Invalid page');
            results.replaceWith(next);
            results = next;
            const query = new URL(url, location.href).searchParams;
            for (const name of ['q', 'city', 'goal']) form.elements[name].value = query.get(name) || '';
            if (push && url !== location.href) history.pushState(null, '', url);
            status.textContent = `Найдено анкет: ${results.querySelectorAll('.person-card').length}`;
        } catch (_) {
            if (current !== controller) return;
            errorBox.hidden = false;
            status.textContent = 'Поиск не обновлён';
        } finally {
            clearTimeout(timeout);
            if (current === controller) {
                submit.disabled = false;
                results.setAttribute('aria-busy', 'false');
            }
        }
    }
    form.addEventListener('submit', event => {
        event.preventDefault();
        const url = new URL(form.action, location.href);
        url.search = new URLSearchParams(new FormData(form));
        load(url.href, true);
    });
    document.querySelector('[data-discover-retry]').addEventListener('click', () => load(pendingUrl, pendingPush));
    window.addEventListener('popstate', () => load(location.href, false));
    window.addEventListener('pagehide', () => controller?.abort());
})();
