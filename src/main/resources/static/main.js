const apiBase = '';

function log(message) {
    const logEl = document.getElementById('log-output');
    const time = new Date().toISOString();
    logEl.textContent += `[${time}] ${message}\n`;
    logEl.scrollTop = logEl.scrollHeight;
}

function prettyJson(obj) {
    try {
        return JSON.stringify(obj, null, 2);
    } catch (e) {
        return String(obj);
    }
}

async function doFetch(path, options = {}, resultElementId) {
    const url = apiBase + path;
    const opts = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        }
    };
    const start = performance.now();
    try {
        const res = await fetch(url, opts);
        const text = await res.text();
        let parsed;
        try {
            parsed = text ? JSON.parse(text) : null;
        } catch {
            parsed = text;
        }
        const ms = (performance.now() - start).toFixed(0);
        log(`${opts.method || 'GET'} ${path} -> ${res.status} (${ms} ms)`);
        if (resultElementId) {
            const el = document.getElementById(resultElementId);
            el.textContent = `HTTP ${res.status}\n\n` + prettyJson(parsed);
        }
        return {status: res.status, body: parsed};
    } catch (e) {
        const ms = (performance.now() - start).toFixed(0);
        log(`ERROR ${opts.method || 'GET'} ${path} (${ms} ms): ${e}`);
        if (resultElementId) {
            const el = document.getElementById(resultElementId);
            el.textContent = `ERROR: ${e}`;
        }
        throw e;
    }
}

function toIsoLocal(datetimeInput) {
    if (!datetimeInput) return null;
    const value = datetimeInput.value;
    if (!value) return null;
    const d = new Date(value);
    return d.toISOString();
}

function setupCreate() {
    const form = document.getElementById('create-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const initiator = document.getElementById('create-initiator').value;
        const author = document.getElementById('create-author').value;
        const title = document.getElementById('create-title').value;
        await doFetch('/api/documents', {
            method: 'POST',
            body: JSON.stringify({initiator, author, title})
        }, 'create-result');
    });
}

function setupGetOne() {
    const form = document.getElementById('get-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('get-id').value;
        const includeHistory = document.getElementById('get-with-history').checked;
        if (!id) return;
        const path = `/api/documents/${id}?includeHistory=${includeHistory}`;
        await doFetch(path, {}, 'get-result');
    });
}

function setupSearchAndList() {
    const form = document.getElementById('search-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const status = document.getElementById('search-status').value;
        const author = document.getElementById('search-author').value;
        const page = document.getElementById('search-page').value || '0';
        const size = document.getElementById('search-size').value || '10';
        const sort = document.getElementById('search-sort').value || 'id,desc';
        const fromIso = toIsoLocal(document.getElementById('search-from'));
        const toIso = toIsoLocal(document.getElementById('search-to'));

        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (author) params.append('author', author);
        if (fromIso) params.append('from', fromIso);
        if (toIso) params.append('to', toIso);
        params.append('page', page);
        params.append('size', size);
        if (sort) params.append('sort', sort);

        const path = `/api/documents/search?` + params.toString();
        await doFetch(path, {}, 'search-result');
    });

    const listBtn = document.getElementById('list-all-btn');
    listBtn.addEventListener('click', async () => {
        const page = document.getElementById('search-page').value || '0';
        const size = document.getElementById('search-size').value || '10';
        const sort = document.getElementById('search-sort').value || 'id,desc';
        const params = new URLSearchParams({page, size, sort});
        const path = `/api/documents?` + params.toString();
        await doFetch(path, {}, 'search-result');
    });
}

function parseIds(value) {
    return value.split(',')
        .map(v => v.trim())
        .filter(v => v.length > 0)
        .map(v => Number(v))
        .filter(v => !isNaN(v));
}

function setupBatch() {
    const submitBtn = document.getElementById('batch-submit-btn');
    const approveBtn = document.getElementById('batch-approve-btn');

    async function send(path) {
        const idsValue = document.getElementById('batch-ids').value;
        const ids = parseIds(idsValue);
        const initiator = document.getElementById('batch-initiator').value || 'batch-user';
        const comment = document.getElementById('batch-comment').value || null;
        if (!ids.length) {
            alert('Введите хотя бы один ID');
            return;
        }
        await doFetch(path, {
            method: 'POST',
            body: JSON.stringify({initiator, ids, comment})
        }, 'batch-result');
    }

    submitBtn.addEventListener('click', () => send('/api/documents/submit'));
    approveBtn.addEventListener('click', () => send('/api/documents/approve'));
}

function setupConcurrent() {
    const form = document.getElementById('concurrent-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const initiator = document.getElementById('concurrent-initiator').value || 'concurrent-tester';
        const documentId = Number(document.getElementById('concurrent-doc-id').value);
        const threads = Number(document.getElementById('concurrent-threads').value || 8);
        const attempts = Number(document.getElementById('concurrent-attempts').value || 50);
        if (!documentId) {
            alert('Укажите ID документа');
            return;
        }
        await doFetch('/api/documents/test-concurrent-approve', {
            method: 'POST',
            body: JSON.stringify({initiator, documentId, threads, attempts})
        }, 'concurrent-result');
    });
}

function setupLog() {
    const clearBtn = document.getElementById('log-clear-btn');
    clearBtn.addEventListener('click', () => {
        document.getElementById('log-output').textContent = '';
    });
}

window.addEventListener('DOMContentLoaded', () => {
    setupCreate();
    setupGetOne();
    setupSearchAndList();
    setupBatch();
    setupConcurrent();
    setupLog();
    log('UI initialized. Backend base URL: ' + (apiBase || window.location.origin));
});

