(() => {
    const root = document.querySelector('[data-messenger]');
    if (!root) return;
    const status = root.querySelector('[data-sync-status]');
    const errorBox = root.querySelector('[data-chat-error]');
    const errorText = root.querySelector('[data-chat-error-text]');
    const login = root.querySelector('[data-chat-login]');
    const list = root.querySelector('[data-conversation-list]');
    const search = root.querySelector('[data-conversation-search]');
    const body = root.querySelector('[data-chat-body]');
    const form = root.querySelector('[data-message-form]');
    const input = form?.elements.content;
    const button = form?.querySelector('button[type="submit"]');
    const restriction = root.querySelector('[data-send-restriction]');
    const draftKey = `swipely:draft:${root.dataset.currentUser}:${root.dataset.peerId}`;
    let canSend = input ? !input.disabled : false;
    let sending = false;
    let syncing = false;
    let stopped = false;
    let timer;
    let signature = '';
    const controllers = new Set();
    const knownMessages = new Set([...root.querySelectorAll('[data-message-id]')].map(el => el.dataset.messageId));

    function draft(value) {
        try {
            if (value === undefined) return sessionStorage.getItem(draftKey) || '';
            if (value) sessionStorage.setItem(draftKey, value);
            else sessionStorage.removeItem(draftKey);
        } catch (_) { return ''; }
    }

    function controls() {
        if (!form) return;
        input.disabled = !canSend || sending;
        button.disabled = !canSend || sending;
        button.textContent = sending ? 'Отправляем…' : 'Отправить ↗';
        restriction.hidden = canSend;
        form.setAttribute('aria-busy', String(sending));
    }

    function failure(error) {
        errorBox.hidden = false;
        login.hidden = error.status !== 401;
        errorText.textContent = error.status === 401 ? 'Сессия завершилась. Войдите снова.' : error.message;
        status.textContent = 'Обновление приостановлено';
        if (error.status === 401) stopped = true;
        if ([401, 403, 404].includes(error.status) && form) {
            canSend = false;
            restriction.textContent = errorText.textContent;
            controls();
        }
    }

    async function request(url, options = {}) {
        const controller = new AbortController();
        controllers.add(controller);
        const timeout = setTimeout(() => controller.abort(), 10000);
        try {
            const response = await fetch(url, {
                ...options, credentials: 'same-origin', cache: 'no-store', signal: controller.signal,
                headers: {Accept: 'application/json', 'Accept-Language': 'ru', ...options.headers}
            });
            if (!response.ok) {
                const data = await response.json().catch(() => ({}));
                const error = new Error(data.message || `Не удалось выполнить запрос (${response.status}).`);
                error.status = response.status;
                throw error;
            }
            return await response.json();
        } catch (error) {
            if (error.status) throw error;
            throw new Error('Не удаётся связаться с сервером. Проверьте соединение и повторите попытку.');
        } finally {
            clearTimeout(timeout);
            controllers.delete(controller);
        }
    }

    function filter() {
        const term = search.value.trim().toLocaleLowerCase('ru');
        let visible = 0;
        for (const row of list.querySelectorAll('a')) {
            row.hidden = !row.querySelector('strong').textContent.toLocaleLowerCase('ru').includes(term);
            if (!row.hidden) visible++;
        }
        root.querySelector('[data-conversations-empty]').hidden = list.childElementCount > 0;
        root.querySelector('[data-search-empty]').hidden = !term || visible > 0 || list.childElementCount === 0;
    }

    function renderList(people) {
        const nextSignature = JSON.stringify(people);
        if (nextSignature === signature) return;
        signature = nextSignature;
        const fragment = document.createDocumentFragment();
        for (const person of people) {
            const row = document.createElement('a');
            row.className = 'conversation';
            const url = new URL(root.dataset.conversationUrl, location.href);
            url.searchParams.set('userId', person.userId);
            row.href = url.href;
            if (String(person.userId) === root.dataset.peerId) {
                row.classList.add('active');
                row.setAttribute('aria-current', 'page');
            }
            const avatar = document.createElement(person.avatarUrl ? 'img' : 'span');
            avatar.className = 'avatar';
            if (person.avatarUrl) {
                avatar.src = root.dataset.rootUrl.replace(/\/$/, '') + person.avatarUrl;
                avatar.alt = '';
            } else avatar.textContent = '♡';
            const text = document.createElement('span');
            const name = document.createElement('strong');
            name.textContent = person.displayName;
            const preview = document.createElement('small');
            preview.textContent = person.lastMessage || 'Начать разговор';
            text.append(name, preview);
            row.append(avatar, text);
            fragment.append(row);
        }
        list.replaceChildren(fragment);
        filter();
    }

    function renderState(state) {
        canSend = state.recipient.canSend;
        root.querySelector('[data-recipient-name]').textContent = state.recipient.displayName;
        controls();
        const nearBottom = body.scrollHeight - body.scrollTop - body.clientHeight < 90;
        let received = 0;
        for (const message of state.messages) {
            if (knownMessages.has(String(message.id))) continue;
            knownMessages.add(String(message.id));
            const bubble = document.createElement('p');
            bubble.className = `bubble ${message.senderId === state.currentUserId ? 'out' : 'in'}`;
            bubble.dataset.messageId = message.id;
            const content = document.createElement('span');
            content.className = 'message-content';
            content.textContent = message.content;
            const time = document.createElement('time');
            time.dateTime = message.sentAt;
            time.textContent = new Date(message.sentAt).toLocaleString('ru-RU', {day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'});
            bubble.append(content, document.createElement('br'), time);
            body.append(bubble);
            received++;
        }
        root.querySelector('[data-chat-empty]').hidden = state.messages.length > 0;
        if (nearBottom) body.scrollTop = body.scrollHeight;
        if (received) status.textContent = 'Есть новые сообщения';
    }

    async function sync() {
        clearTimeout(timer);
        if (syncing || sending || stopped || document.hidden) return;
        syncing = true;
        root.setAttribute('aria-busy', 'true');
        try {
            const people = await request(root.dataset.inboxUrl);
            renderList(people);
            if (root.dataset.stateUrl) renderState(await request(root.dataset.stateUrl));
            errorBox.hidden = true;
            status.textContent = 'Сообщения обновлены';
        } catch (error) {
            failure(error);
        } finally {
            syncing = false;
            root.setAttribute('aria-busy', 'false');
            if (!stopped) timer = setTimeout(sync, 3000);
        }
    }

    if (form) {
        input.value = draft();
        input.addEventListener('input', () => draft(input.value));
        input.addEventListener('keydown', event => {
            if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                event.preventDefault();
                if (!button.disabled) form.requestSubmit();
            }
        });
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (sending || !canSend || !input.value.trim()) return;
            const content = input.value;
            draft(content);
            sending = true;
            controls();
            clearTimeout(timer);
            try {
                await request(root.dataset.sendUrl, {method: 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({receiverId: Number(root.dataset.peerId), content})});
                input.value = '';
                draft('');
                errorBox.hidden = true;
                status.textContent = 'Сообщение отправлено';
            } catch (error) {
                if (!error.status) error.message += ' Текст сохранён. Перед повторной отправкой обновите историю: сообщение могло быть доставлено.';
                failure(error);
            } finally {
                sending = false;
                controls();
                if (!errorBox.hidden) timer = setTimeout(sync, 3000);
                else await sync();
                if (!input.disabled) input.focus();
            }
        });
        body.scrollTop = body.scrollHeight;
    }
    search.addEventListener('input', filter);
    root.querySelector('[data-chat-retry]').addEventListener('click', () => { stopped = false; sync(); });
    document.addEventListener('visibilitychange', () => { if (!document.hidden) sync(); });
    window.addEventListener('online', sync);
    window.addEventListener('pagehide', () => {
        clearTimeout(timer);
        for (const controller of controllers) controller.abort();
    });
    window.addEventListener('pageshow', sync);
    sync();
})();
