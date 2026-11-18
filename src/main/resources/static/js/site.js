(() => {
    const normalize = path => path.replace(/\.html$/, '').replace(/\/$/, '') || '/';
    const current = normalize(location.pathname);
    for (const link of document.querySelectorAll('.app-nav a, .footer-links a')) {
        const target = normalize(new URL(link.href).pathname);
        const active = target === current || current.endsWith('/conversation') && target.endsWith('/messages');
        link.classList.toggle('active', active);
        if (active) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    }
    const notice = document.createElement('p');
    notice.className = 'notice';
    notice.setAttribute('role', 'status');
    notice.hidden = true;
    document.querySelector('main')?.prepend(notice);
    const reset = () => {
        for (const form of document.forms) {
            if (!form.dataset.pendingSubmit) continue;
            delete form.dataset.pendingSubmit;
            form.removeAttribute('aria-busy');
            form.querySelectorAll('[data-submit-disabled]').forEach(button => {
                button.disabled = false;
                delete button.dataset.submitDisabled;
            });
        }
        notice.hidden = true;
    };
    document.addEventListener('submit', event => {
        const form = event.target;
        if (form.matches('[data-message-form], [data-discover-form]')) return;
        if (!navigator.onLine) {
            event.preventDefault();
            notice.textContent = 'Нет подключения к сети. Введённые данные сохранены на странице. После подключения повторите отправку.';
            notice.hidden = false;
            return;
        }
        if (form.dataset.pendingSubmit) { event.preventDefault(); return; }
        form.dataset.pendingSubmit = 'true';
        form.setAttribute('aria-busy', 'true');
        notice.textContent = 'Отправляем данные…';
        notice.hidden = false;
        setTimeout(() => {
            form.querySelectorAll('button[type="submit"], button:not([type])').forEach(button => {
                if (!button.disabled) { button.dataset.submitDisabled = 'true'; button.disabled = true; }
            });
        }, 0);
    });
    window.addEventListener('pageshow', reset);
})();
