(() => {
  let token = null, username = null, myUserId = null, stomp = null, connected = false;
  let reconnectAttempts = 0, reconnecting = false;
  let currentConv = { id: 'public', type: 'PUBLIC', name: 'Salon public' };

  // Logout / reconnect control
  let logoutRequested = false;
  let reconnectTimer = null;

  // convId -> { msgSub, reactSub, evtSub }
  const convSubs = new Map();
  const convMessages = new Map(); // convId -> messages (array)
  let myConversations = [];

  // Réactions sélectionnées (UI only)
  const mySelectedReactions = new Set(); // key = `${messageId}|${emoji}`
  const reactionKey = (messageId, emoji) => `${messageId}|${emoji}`;

  // ---- localStorage persistence (per user) ----
  const STORAGE_PREFIX = 'chat.selectedReactions.';

  // ---- Context menu state ----
  let ctxMenuEl = null;

  // ---- Edit/Delete modal state ----
  let editTarget = { convId: null, messageId: null };
  let deleteTarget = { convId: null, messageId: null };

  // ---- Reply state ----
  let replyTarget = null;
  // { convId, messageId, senderUsername, excerpt }

  function storageKeyForUser(user) {
    return `${STORAGE_PREFIX}${user || ''}`;
  }

  function loadSelectedReactionsForUser(user) {
    mySelectedReactions.clear();
    if (!user) return;

    try {
      const raw = localStorage.getItem(storageKeyForUser(user));
      if (!raw) return;

      const arr = JSON.parse(raw);
      if (Array.isArray(arr)) {
        arr.forEach(k => {
          if (typeof k === 'string' && k.includes('|')) mySelectedReactions.add(k);
        });
      }
    } catch {
      // ignore
    }
  }

  function persistSelectedReactionsForUser(user) {
    if (!user) return;
    try {
      localStorage.setItem(storageKeyForUser(user), JSON.stringify(Array.from(mySelectedReactions)));
    } catch {
      // ignore
    }
  }

  function clearSelectedReactionsForUser(user) {
    if (!user) return;
    try {
      localStorage.removeItem(storageKeyForUser(user));
    } catch {
      // ignore
    }
  }

  function makeExcerpt(text, max = 120) {
    const s = String(text || '').trim().replace(/\s+/g, ' ');
    return s.length > max ? s.slice(0, max) + '…' : s;
  }

  function clearReply() {
    replyTarget = null;
    if (els.replyBar) els.replyBar.classList.add('d-none');
  }

  function startReplyToMessage(convId, messageId) {
    const msg = (convMessages.get(convId) || []).find(m => m.id === messageId);
    if (!msg) return;
    if (msg.type && msg.type !== 'CHAT') return;

    replyTarget = {
      convId,
      messageId,
      senderUsername: msg.senderUsername || '—',
      excerpt: makeExcerpt(msg.content || '')
    };

    if (els.replyBar && els.replyToUser && els.replyToExcerpt) {
      els.replyToUser.textContent = replyTarget.senderUsername;
      els.replyToExcerpt.textContent = replyTarget.excerpt || '';
      els.replyBar.classList.remove('d-none');
    }

    els.msgInput?.focus();
  }

  // DOM elements
  const els = {
    statusBadge: document.querySelector('#status-badge'),
    themeToggle: document.querySelector('#theme-toggle'),
    toastContainer: document.querySelector('#toast-container'),

    loginSection: document.querySelector('#login-section'),
    authForm: document.querySelector('#auth-form'),
    usernameInput: document.querySelector('#username'),
    passwordInput: document.querySelector('#password'),
    btnLogin: document.querySelector('#btn-login'),
    loginError: document.querySelector('#login-error'),

    // Modal inscription
    registerModal: document.querySelector('#registerModal'),
    registerForm: document.querySelector('#register-form'),
    registerUsernameInput: document.querySelector('#register-username'),
    registerEmailInput: document.querySelector('#register-email'),
    registerPasswordInput: document.querySelector('#register-password'),
    registerError: document.querySelector('#register-error'),

    // Modal mot de passe oublié
    forgotModal: document.querySelector('#forgotModal'),
    forgotForm: document.querySelector('#forgot-form'),
    forgotEmailInput: document.querySelector('#forgot-email'),
    forgotCodeInput: document.querySelector('#forgot-code'),
    forgotPasswordInput: document.querySelector('#forgot-password'),
    forgotError: document.querySelector('#forgot-error'),
    forgotStep2: document.querySelector('#forgot-step2'),
    btnForgotCode: document.querySelector('#btn-forgot-code'),

    chatSection: document.querySelector('#chat-section'),
    messages: document.querySelector('#messages'),
    users: document.querySelector('#users'),
    userCount: document.querySelector('#user-count'),
    conversations: document.querySelector('#conversations'),
    currentConvLabel: document.querySelector('#current-conv-label'),
    msgForm: document.querySelector('#msg-form'),
    msgInput: document.querySelector('#msg-input'),
    sendBtn: document.querySelector('#send-btn'),
    btnLogout: document.querySelector('#btn-logout'),

    // Reply bar
    replyBar: document.querySelector('#reply-bar'),
    replyToUser: document.querySelector('#reply-to-user'),
    replyToExcerpt: document.querySelector('#reply-to-excerpt'),
    btnCancelReply: document.querySelector('#btn-cancel-reply'),

    // Modal edit message
    editMessageModal: document.querySelector('#editMessageModal'),
    editMessageForm: document.querySelector('#edit-message-form'),
    editMessageInput: document.querySelector('#edit-message-input'),
    editMessageError: document.querySelector('#edit-message-error'),

    // Modal delete message
    deleteMessageModal: document.querySelector('#deleteMessageModal'),
    deleteMessageError: document.querySelector('#delete-message-error'),
    btnConfirmDeleteMessage: document.querySelector('#btn-confirm-delete-message'),
  };

  // ---------- UI Helpers ----------
  function setStatus(text, state) {
    if (!els.statusBadge) return;
    els.statusBadge.textContent = text;
    els.statusBadge.classList.remove('text-bg-success', 'text-bg-danger', 'text-bg-secondary');
    if (state === true) els.statusBadge.classList.add('text-bg-success');
    else if (state === false) els.statusBadge.classList.add('text-bg-danger');
    else els.statusBadge.classList.add('text-bg-secondary');
  }

  function showChat(show) {
    if (!els.loginSection || !els.chatSection) return;
    els.loginSection.classList.toggle('d-none', !!show);
    els.chatSection.classList.toggle('d-none', !show);
    if (els.btnLogout) els.btnLogout.classList.toggle('d-none', !show);
  }

  function showToast(message, variant = 'warning') {
    if (!els.toastContainer) return;

    const div = document.createElement('div');
    div.className = `toast align-items-center text-bg-${variant} border-0 show`;

    const body = document.createElement('div');
    body.className = 'toast-body';
    body.textContent = message;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn-close btn-close-white me-2 m-auto';
    btn.setAttribute('data-bs-dismiss', 'toast');
    btn.onclick = () => div.remove();

    const dFlex = document.createElement('div');
    dFlex.className = 'd-flex';
    dFlex.append(body, btn);

    div.appendChild(dFlex);
    els.toastContainer.appendChild(div);

    setTimeout(() => div.remove(), 4000);
  }

  function isUnauthorizedStatus(status) {
    return status === 401 || status === 403;
  }

  function handleAuthExpired(source = 'REST/WS') {
    if (logoutRequested) return;
    showToast("Session expirée. Veuillez vous reconnecter.", "warning");
    performLogout({ reason: `expired:${source}`, clearStoredSelected: true });
  }

  // ---------- Context menu ----------
  function closeContextMenu() {
    if (ctxMenuEl) ctxMenuEl.remove();
    ctxMenuEl = null;
  }

  function openContextMenu({ x, y, messageId, convId, isMe }) {
    closeContextMenu();

    ctxMenuEl = document.createElement('div');
    ctxMenuEl.className = 'ctx-menu';
    ctxMenuEl.style.position = 'fixed';
    ctxMenuEl.style.left = `${x}px`;
    ctxMenuEl.style.top = `${y}px`;
    ctxMenuEl.style.zIndex = '9999';
    ctxMenuEl.style.minWidth = '180px';
    ctxMenuEl.style.background = 'var(--bs-body-bg)';
    ctxMenuEl.style.border = '1px solid var(--bs-border-color)';
    ctxMenuEl.style.borderRadius = '10px';
    ctxMenuEl.style.boxShadow = '0 10px 30px rgba(0,0,0,.15)';
    ctxMenuEl.style.padding = '6px';

    const mkItem = (label, onClick, danger = false) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.textContent = label;
      btn.style.width = '100%';
      btn.style.textAlign = 'left';
      btn.style.border = '0';
      btn.style.background = 'transparent';
      btn.style.padding = '10px 12px';
      btn.style.borderRadius = '8px';
      btn.style.cursor = 'pointer';
      btn.style.color = danger ? 'var(--bs-danger)' : 'inherit';
      btn.onmouseenter = () => btn.style.background = 'var(--bs-tertiary-bg)';
      btn.onmouseleave = () => btn.style.background = 'transparent';
      btn.onclick = (e) => {
        e.stopPropagation();
        closeContextMenu();
        onClick();
      };
      return btn;
    };

    // NEW: reply for me + peer
    ctxMenuEl.appendChild(mkItem('Répondre', () => startReplyToMessage(convId, messageId)));

    // edit/delete only for me
    if (isMe) {
      ctxMenuEl.appendChild(mkItem('Modifier', () => openEditModal(convId, messageId)));
      ctxMenuEl.appendChild(mkItem('Supprimer', () => openDeleteModal(convId, messageId), true));
    }

    document.body.appendChild(ctxMenuEl);

    // Ajustement si ça déborde à droite/bas
    const rect = ctxMenuEl.getBoundingClientRect();
    const pad = 8;
    let nx = x, ny = y;
    if (rect.right > window.innerWidth - pad) nx = Math.max(pad, window.innerWidth - rect.width - pad);
    if (rect.bottom > window.innerHeight - pad) ny = Math.max(pad, window.innerHeight - rect.height - pad);
    ctxMenuEl.style.left = `${nx}px`;
    ctxMenuEl.style.top = `${ny}px`;
  }

  // ---------- Edit/Delete modals ----------
  function wsEditDestination(convId) {
    return convId === 'public'
      ? '/app/chat.message.edit'
      : `/app/conv.${convId}.message.edit`;
  }

  function wsDeleteDestination(convId) {
    return convId === 'public'
      ? '/app/chat.message.delete'
      : `/app/conv.${convId}.message.delete`;
  }

  function openEditModal(convId, messageId) {
    if (!els.editMessageModal || !els.editMessageInput || !els.editMessageError) return;

    const msg = (convMessages.get(convId) || []).find(m => m.id === messageId);
    if (!msg) {
      showToast("Message introuvable", "danger");
      return;
    }
    if (msg.deletedAt) {
      showToast("Message déjà supprimé", "warning");
      return;
    }

    editTarget = { convId, messageId };
    els.editMessageError.textContent = '';
    els.editMessageInput.value = msg.content || '';

    if (window.bootstrap) {
      const modal = bootstrap.Modal.getOrCreateInstance(els.editMessageModal);
      modal.show();
      setTimeout(() => els.editMessageInput?.focus(), 0);
    }
  }

  function openDeleteModal(convId, messageId) {
    if (!els.deleteMessageModal || !els.deleteMessageError) return;

    const msg = (convMessages.get(convId) || []).find(m => m.id === messageId);
    if (!msg) {
      showToast("Message introuvable", "danger");
      return;
    }
    if (msg.deletedAt) {
      showToast("Message déjà supprimé", "warning");
      return;
    }

    deleteTarget = { convId, messageId };
    els.deleteMessageError.textContent = '';

    if (window.bootstrap) {
      bootstrap.Modal.getOrCreateInstance(els.deleteMessageModal).show();
    }
  }

  // ---------- Logout ----------
  function performLogout({ reason = 'manual', clearStoredSelected = true } = {}) {
    logoutRequested = true;

    closeContextMenu();
    clearReply();

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    reconnecting = false;
    reconnectAttempts = 0;

    const userToClear = username;
    if (clearStoredSelected) clearSelectedReactionsForUser(userToClear);

    try { cleanupConversationSubscriptions(); } catch {}
    try { if (stomp && connected) stomp.disconnect(() => {}); } catch {}

    stomp = null;
    connected = false;

    token = null;
    username = null;
    myUserId = null;

    currentConv = { id: 'public', type: 'PUBLIC', name: 'Salon public' };
    myConversations = [];
    convMessages.clear();
    mySelectedReactions.clear();

    setStatus("Déconnecté", false);
    if (els.messages) els.messages.innerHTML = '';
    if (els.users) els.users.innerHTML = '';
    if (els.userCount) els.userCount.textContent = '0';
    if (els.conversations) els.conversations.innerHTML = '';
    if (els.currentConvLabel) els.currentConvLabel.textContent = '# Salon public';
    if (els.msgInput) els.msgInput.value = '';

    // reset login fields
    if (els.usernameInput) els.usernameInput.value = '';
    if (els.passwordInput) els.passwordInput.value = '';
    if (els.loginError) els.loginError.textContent = '';

    // reset register fields
    if (els.registerUsernameInput) els.registerUsernameInput.value = '';
    if (els.registerEmailInput) els.registerEmailInput.value = '';
    if (els.registerPasswordInput) els.registerPasswordInput.value = '';
    if (els.registerError) els.registerError.textContent = '';

    // reset forgot fields
    if (els.forgotEmailInput) els.forgotEmailInput.value = '';
    if (els.forgotCodeInput) els.forgotCodeInput.value = '';
    if (els.forgotPasswordInput) els.forgotPasswordInput.value = '';
    if (els.forgotError) els.forgotError.textContent = '';
    if (els.forgotStep2) els.forgotStep2.classList.add('d-none');
    if (els.forgotEmailInput) els.forgotEmailInput.disabled = false;
    if (els.btnForgotCode) els.btnForgotCode.disabled = false;

    updateSendButtonState();
    showChat(false);
  }

  function logout() {
    showToast("Vous êtes déconnecté.", "info");
    performLogout({ reason: 'manual', clearStoredSelected: true });
  }

  // ---------- API ----------
  async function postJson(url, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body || {})
    });

    if (token && isUnauthorizedStatus(res.status)) {
      handleAuthExpired(`POST ${url}`);
      throw new Error('UNAUTHORIZED');
    }

    return res;
  }

  async function getJson(url) {
    const res = await fetch(url, { headers: { 'Authorization': 'Bearer ' + token } });

    if (isUnauthorizedStatus(res.status)) {
      handleAuthExpired(`GET ${url}`);
      throw new Error('UNAUTHORIZED');
    }

    if (!res.ok) throw new Error(`Erreur ${res.status}`);
    return res.json();
  }

  async function safeReadJson(res) {
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) return res.json();
    const text = await res.text();
    return { error: text || `Erreur ${res.status}` };
  }

  // ---------- AUTH : LOGIN ----------
  async function handleLogin() {
    if (!els.usernameInput || !els.passwordInput || !els.loginError) return;

    els.loginError.textContent = "";
    els.loginError.className = "text-danger small text-center mt-2";

    const userVal = els.usernameInput.value.trim();
    const passVal = els.passwordInput.value.trim();

    if (!userVal || !passVal) {
      els.loginError.textContent = "Veuillez remplir pseudo et mot de passe";
      return;
    }

    if (els.btnLogin) els.btnLogin.disabled = true;

    try {
      const res = await postJson('/api/v1/auth/login', { username: userVal, password: passVal });
      const data = await safeReadJson(res);

      if (!res.ok) throw new Error(data.error || "Identifiants invalides");

      token = data.token;
      username = data.username;
      myUserId = data.userId;

      logoutRequested = false;

      loadSelectedReactionsForUser(username);

      showToast("Ravi de vous revoir !", "success");
      await initChatAfterLogin();

    } catch (e) {
      if (!logoutRequested) els.loginError.textContent = e.message;
    } finally {
      if (els.btnLogin) els.btnLogin.disabled = false;
    }
  }

  // ---------- AUTH : REGISTER (MODAL) ----------
  async function handleRegister() {
    if (!els.registerUsernameInput || !els.registerEmailInput || !els.registerPasswordInput || !els.registerError) return;

    els.registerError.textContent = "";

    const userVal = els.registerUsernameInput.value.trim();
    const emailVal = els.registerEmailInput.value.trim();
    const passVal = els.registerPasswordInput.value.trim();

    if (!userVal || !emailVal || !passVal) {
      els.registerError.textContent = "Veuillez remplir tous les champs";
      return;
    }

    const submitBtn = els.registerForm?.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      const res = await postJson('/api/v1/auth/register', {
        username: userVal,
        email: emailVal,
        password: passVal
      });
      const data = await safeReadJson(res);

      if (!res.ok) throw new Error(data.error || "Erreur serveur");

      token = data.token;
      username = data.username;
      myUserId = data.userId;

      logoutRequested = false;

      loadSelectedReactionsForUser(username);

      if (els.registerModal && window.bootstrap) {
        const modal = bootstrap.Modal.getOrCreateInstance(els.registerModal);
        modal.hide();
      }

      els.registerUsernameInput.value = '';
      els.registerEmailInput.value = '';
      els.registerPasswordInput.value = '';
      els.registerError.textContent = '';

      showToast("Bienvenue ! Inscription réussie.", "success");
      await initChatAfterLogin();

    } catch (e) {
      if (!logoutRequested) els.registerError.textContent = e.message;
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  }

  // ---------- AUTH : RESET PASSWORD (FORGOT MODAL) ----------
  async function handleRequestResetCode() {
    if (!els.forgotEmailInput || !els.forgotError || !els.forgotStep2 || !els.btnForgotCode) return;

    els.forgotError.textContent = "";
    const emailVal = els.forgotEmailInput.value.trim();
    if (!emailVal) {
      els.forgotError.textContent = "Veuillez saisir votre adresse e-mail";
      return;
    }

    els.btnForgotCode.disabled = true;

    try {
      const res = await postJson('/api/v1/auth/reset-password/code', { email: emailVal });
      const data = await safeReadJson(res);

      if (!res.ok) throw new Error(data.error || "Impossible d'envoyer le code");

      showToast("Code envoyé à votre adresse e-mail", "success");

      els.forgotEmailInput.disabled = true;
      els.forgotStep2.classList.remove('d-none');

    } catch (e) {
      els.btnForgotCode.disabled = false;
      if (!logoutRequested) els.forgotError.textContent = e.message;
    }
  }

  async function handleResetPassword() {
    if (!els.forgotEmailInput || !els.forgotCodeInput || !els.forgotPasswordInput || !els.forgotError) return;

    els.forgotError.textContent = "";

    const emailVal = els.forgotEmailInput.value.trim();
    const codeVal = els.forgotCodeInput.value.trim();
    const passVal = els.forgotPasswordInput.value.trim();

    if (!emailVal || !codeVal || !passVal) {
      els.forgotError.textContent = "Veuillez remplir tous les champs";
      return;
    }

    const submitBtn = els.forgotForm?.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      const res = await postJson('/api/v1/auth/reset-password', {
        email: emailVal,
        code: codeVal,
        newPassword: passVal
      });
      const data = await safeReadJson(res);

      if (!res.ok) throw new Error(data.error || "Impossible de réinitialiser le mot de passe");

      if (els.forgotModal && window.bootstrap) {
        const modal = bootstrap.Modal.getOrCreateInstance(els.forgotModal);
        modal.hide();
      }

      if (els.passwordInput) els.passwordInput.value = '';
      if (els.loginError) els.loginError.textContent = '';

      els.forgotEmailInput.value = '';
      els.forgotEmailInput.disabled = false;
      els.forgotCodeInput.value = '';
      els.forgotPasswordInput.value = '';
      els.forgotError.textContent = '';
      els.forgotStep2.classList.add('d-none');
      els.btnForgotCode.disabled = false;

      showToast("Mot de passe mis à jour. Vous pouvez vous connecter.", "success");

    } catch (e) {
      if (!logoutRequested) els.forgotError.textContent = e.message;
    } finally {
      const submitBtn2 = els.forgotForm?.querySelector('button[type="submit"]');
      if (submitBtn2) submitBtn2.disabled = false;
    }
  }

  async function initChatAfterLogin() {
    try {
      const state = await getJson('/api/v1/state');

      convMessages.set('public', state.messages || []);
      renderUsers(state.users || []);

      const serverConvs = await getJson('/api/v1/conversations');
      myConversations = serverConvs || [];
      renderConversations();

      showChat(true);
      connectStomp();
      window.switchConv('public', 'PUBLIC', 'Salon public');
    } catch (e) {
      if (!logoutRequested) {
        console.error(e);
        showToast("Erreur lors du chargement initial", "danger");
      }
    }
  }

  // ---------- WS / STOMP ----------
  function isWsAuthError(err) {
    const s = (typeof err === 'string') ? err : JSON.stringify(err);
    return /401|403|unauthorized|forbidden|jwt|token/i.test(s);
  }

  function connectStomp() {
    if (logoutRequested) return;
    cleanupConversationSubscriptions();

    const socket = new WebSocket((location.protocol === 'https:' ? 'wss' : 'ws') + '://' + location.host + '/ws');
    stomp = Stomp.over(socket);
    stomp.debug = null;

    stomp.connect({ Authorization: 'Bearer ' + token }, () => {
      if (logoutRequested) return;

      connected = true;
      reconnectAttempts = 0;
      setStatus(`Connecté: ${username}`, true);
      updateSendButtonState();

      myConversations.forEach(c => ensureSubscribedToConversation(c.id));

      // ---- PUBLIC messages ----
      stomp.subscribe('/topic/public', (f) => {
        const m = JSON.parse(f.body);
        m.reactions = Array.isArray(m.reactions) ? m.reactions : [];
        const arr = convMessages.get('public') || [];
        arr.push(m);
        convMessages.set('public', arr);
        if (currentConv.id === 'public') renderInboundMessage(m);
      });

      // ---- PUBLIC reactions ----
      stomp.subscribe('/topic/public.reactions', (f) => {
        const r = JSON.parse(f.body);
        applyReactionToStore('public', r);
        renderReaction(r);
      });

      // ---- PUBLIC message events (updated/deleted) ----
      stomp.subscribe('/topic/public.events', (f) => {
        const evt = JSON.parse(f.body);
        applyMessageEvent('public', evt);
      });

      stomp.subscribe('/topic/users', (f) => renderUsers(JSON.parse(f.body)));

      stomp.subscribe('/user/queue/errors', (f) => {
        try {
          const errObj = JSON.parse(f.body);
          const msg = errObj?.message || 'Erreur';
          showToast(msg, 'danger');

          const status = errObj?.status;
          const code = errObj?.errorCode;
          const raw = JSON.stringify(errObj);

          if (status === 401 || status === 403 || code === 'UNAUTHORIZED' || code === 'TOKEN_EXPIRED' || /unauthorized|token|jwt/i.test(raw)) {
            handleAuthExpired('WS /user/queue/errors');
          }
        } catch (e) {
          const raw = String(f.body || 'Erreur');
          showToast(raw, 'danger');
          if (/401|403|unauthorized|token|jwt/i.test(raw)) {
            handleAuthExpired('WS /user/queue/errors');
          }
        }
      });

      stomp.subscribe('/user/queue/notifications', (f) => {
        const notif = JSON.parse(f.body);
        if (currentConv.id !== notif.conversationId) {
          if (notif.type === 'DM_MESSAGE') showToast(`${notif.from} : ${notif.preview}`, 'info');
          else if (notif.type === 'CONV_CREATED') showToast(`Nouvelle conversation avec ${notif.from}`, 'success');
        }
      });

    }, (err) => {
      connected = false;
      setStatus("Déconnecté", false);

      if (isWsAuthError(err)) {
        handleAuthExpired('WS CONNECT');
        return;
      }

      scheduleReconnect();
    });
  }

  function scheduleReconnect() {
    if (logoutRequested) return;
    if (reconnecting) return;
    reconnecting = true;

    reconnectAttempts++;
    const delay = Math.min(30000, 2000 * reconnectAttempts);

    reconnectTimer = setTimeout(() => {
      reconnecting = false;
      if (!logoutRequested) connectStomp();
    }, delay);
  }

  function cleanupConversationSubscriptions() {
    for (const { msgSub, reactSub, evtSub } of convSubs.values()) {
      try { msgSub?.unsubscribe(); } catch {}
      try { reactSub?.unsubscribe(); } catch {}
      try { evtSub?.unsubscribe(); } catch {}
    }
    convSubs.clear();
  }

  function ensureSubscribedToConversation(convId) {
    if (!stomp || !connected || convSubs.has(convId) || convId === 'public') return;

    const msgSub = stomp.subscribe(`/topic/conv.${convId}`, (f) => {
      const m = JSON.parse(f.body);
      m.reactions = Array.isArray(m.reactions) ? m.reactions : [];
      const arr = convMessages.get(convId) || [];
      arr.push(m);
      convMessages.set(convId, arr);
      if (currentConv.id === convId) renderInboundMessage(m);
    });

    const reactSub = stomp.subscribe(`/topic/conv.${convId}.reactions`, (f) => {
      const r = JSON.parse(f.body);
      applyReactionToStore(convId, r);
      renderReaction(r);
    });

    const evtSub = stomp.subscribe(`/topic/conv.${convId}.events`, (f) => {
      const evt = JSON.parse(f.body);
      applyMessageEvent(convId, evt);
    });

    convSubs.set(convId, { msgSub, reactSub, evtSub });
  }

  // ---------- STORE: persist reactions for re-render ----------
  function applyReactionToStore(convId, reaction) {
    const arr = convMessages.get(convId);
    if (!arr) return;

    const msg = arr.find(m => m.id === reaction.messageId);
    if (!msg) return;

    msg.reactions = Array.isArray(msg.reactions) ? msg.reactions : [];

    const delta = Number.isFinite(reaction.delta) ? reaction.delta : 1;

    if (delta > 0) {
      msg.reactions.push({ messageId: reaction.messageId, emoji: reaction.emoji, delta: 1 });
    } else if (delta < 0) {
      const idx = msg.reactions.findIndex(r => r.emoji === reaction.emoji);
      if (idx >= 0) msg.reactions.splice(idx, 1);
    }
  }

  // ---------- MESSAGE EVENTS (updated / deleted) ----------
  function applyMessageEvent(convId, evt) {
    if (!evt || !evt.type || !evt.messageId) return;

    if (evt.type === 'message.updated') {
      updateMessageInStore(convId, evt.messageId, { content: evt.content, editedAt: evt.editedAt });
      updateMessageInDom(evt.messageId, { content: evt.content, editedAt: evt.editedAt });
      return;
    }

    if (evt.type === 'message.deleted') {
      updateMessageInStore(convId, evt.messageId, { content: evt.content, deletedAt: evt.deletedAt });
      updateMessageInDom(evt.messageId, { content: evt.content, deletedAt: evt.deletedAt });
    }
  }

  function updateMessageInStore(convId, messageId, patch) {
    const arr = convMessages.get(convId);
    if (!arr) return;
    const m = arr.find(x => x.id === messageId);
    if (!m) return;
    Object.assign(m, patch);
  }

  function updateMessageInDom(messageId, patch) {
    const msgEl = document.querySelector(`[data-msg-id="${messageId}"]`);
    if (!msgEl) return;

    const contentEl = msgEl.querySelector('.content-text');
    if (contentEl && typeof patch.content === 'string') {
      contentEl.textContent = patch.content;
    }

    if (patch.deletedAt) msgEl.classList.add('msg-deleted');
    if (patch.editedAt) msgEl.classList.add('msg-edited');
  }

  // ---------- Rendering ----------
  function renderInboundMessage(m) {
    if (m.type === 'CHAT') {
      addMessageItem({
        id: m.id,
        metaTs: m.timestamp,
        senderUsername: m.senderUsername,
        senderUserId: m.senderUserId,
        content: m.content,
        replyTo: m.replyTo, // <-- NEW
        variant: 'chat',
        reactions: m.reactions
      });
    } else {
      const who = m.senderUsername || 'Quelqu’un';
      addMessageItem({
        metaTs: m.timestamp,
        content: `${who} ${m.type === 'JOIN' ? 'a rejoint' : 'est parti'}`,
        variant: 'system'
      });
    }
    autoScroll();
  }

  function addMessageItem({ id, metaTs, senderUsername, senderUserId, content, replyTo, variant, reactions }) {
    if (!els.messages) return;

    const li = document.createElement('li');
    li.className = 'list-group-item border-0 bg-transparent d-flex mb-3';
    if (id) li.setAttribute('data-msg-id', id);

    if (variant === 'system') {
      li.classList.add('msg-system', 'justify-content-center');

      const pill = document.createElement('div');
      pill.className = 'small bg-body-tertiary px-3 py-1 rounded-pill';

      const text = document.createElement('span');
      text.textContent = content;

      const meta = document.createElement('small');
      meta.className = 'msg-meta ms-1';
      meta.textContent = formatRelative(metaTs);

      pill.append(text, meta);
      li.appendChild(pill);

      els.messages.appendChild(li);
      return;
    }

    const isMe = (senderUserId && myUserId && senderUserId === myUserId);

    // Context menu for me + peer (Reply always available)
    if (id) {
      li.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        e.stopPropagation();
        openContextMenu({ x: e.clientX, y: e.clientY, messageId: id, convId: currentConv.id, isMe });
      });
    }

    li.classList.add(isMe ? 'msg-me' : 'msg-peer', isMe ? 'justify-content-end' : 'justify-content-start');

    const wrapper = document.createElement('div');
    wrapper.className = 'msg-wrapper';

    const reactMenu = document.createElement('div');
    reactMenu.className = 'react-menu-popover';
    [{c:'👍',l:'Like'}, {c:'❤️',l:'Love'}, {c:'😂',l:'Haha'}, {c:'😢',l:'Sad'}].forEach(e => {
      const s = document.createElement('span');
      s.textContent = e.c; s.title = e.l;
      s.onclick = (event) => {
        event.stopPropagation();
        sendReaction(id, e.c);
      };
      reactMenu.appendChild(s);
    });

    wrapper.innerHTML = `
      <div class="d-flex justify-content-between gap-3 mb-1">
          <small class="fw-bold sender-name"></small>
          <small class="msg-meta msg-time"></small>
      </div>
      <div class="text-break content-text"></div>
      <div class="reaction-list"></div>
    `;

    wrapper.querySelector('.sender-name').textContent = isMe ? 'Moi' : (senderUsername || '—');
    wrapper.querySelector('.msg-time').textContent = formatRelative(metaTs);

    const contentEl = wrapper.querySelector('.content-text');

    // Reply preview inserted above content
    if (replyTo && replyTo.messageId) {
      const preview = document.createElement('div');
      preview.className = 'reply-preview';
      preview.innerHTML = `
        <div class="reply-meta">Réponse à <strong></strong></div>
        <div class="reply-excerpt"></div>
      `;
      preview.querySelector('strong').textContent = replyTo.senderUsername || '—';
      preview.querySelector('.reply-excerpt').textContent = replyTo.excerpt || '';
      contentEl.parentNode.insertBefore(preview, contentEl);
    }

    preview.addEventListener('click', (e) => {
      e.stopPropagation();
      const ok = scrollToMessage(replyTo.messageId);
      if (!ok) showMessageNotFoundToast();
    });

    contentEl.textContent = content;

    wrapper.appendChild(reactMenu);

    const avatar = document.createElement('div');
    avatar.className = 'msg-avatar';
    avatar.textContent = (senderUsername || '?').charAt(0).toUpperCase();

    if (isMe) li.append(wrapper);
    else li.append(avatar, wrapper);

    els.messages.appendChild(li);

    if (Array.isArray(reactions) && reactions.length > 0) {
      reactions.forEach(r => renderReaction(r));
    }
  }

  function renderReaction(reaction) {
    const msgEl = document.querySelector(`[data-msg-id="${reaction.messageId}"]`);
    if (!msgEl) return;

    const container = msgEl.querySelector('.reaction-list');
    if (!container) return;

    const delta = Number.isFinite(reaction.delta) ? reaction.delta : 1;
    let badge = container.querySelector(`[data-emoji="${reaction.emoji}"]`);

    if (!badge) {
      if (delta < 0) return;

      badge = document.createElement('span');
      badge.className = 'badge rounded-pill bg-body-secondary text-body border emoji-animated px-2 py-1 me-1';
      badge.style.cursor = 'pointer';
      badge.setAttribute('data-emoji', reaction.emoji);

      badge.textContent = reaction.emoji + ' ';
      const count = document.createElement('span');
      count.className = 'count';
      count.textContent = '1';
      badge.appendChild(count);

      badge.onclick = () => sendReaction(reaction.messageId, reaction.emoji);
      container.appendChild(badge);

      const k = reactionKey(reaction.messageId, reaction.emoji);
      badge.classList.toggle('selected', mySelectedReactions.has(k));
      return;
    }

    const countEl = badge.querySelector('.count');
    const current = parseInt(countEl.textContent || '0', 10);
    const next = current + delta;

    if (next <= 0) {
      badge.remove();
      return;
    }

    countEl.textContent = String(next);

    badge.classList.remove('emoji-animated');
    void badge.offsetWidth;
    badge.classList.add('emoji-animated');

    const k = reactionKey(reaction.messageId, reaction.emoji);
    badge.classList.toggle('selected', mySelectedReactions.has(k));
  }

  function renderUsers(list) {
    if (!els.userCount || !els.users) return;

    els.userCount.textContent = (list || []).length;
    els.users.innerHTML = '';

    const sorted = [...(list || [])].sort((a, b) => a.localeCompare(b));

    for (const u of sorted) {
      const li = document.createElement('li');
      li.className = 'list-group-item d-flex justify-content-between align-items-center bg-transparent border-0 px-0';

      const left = document.createElement('span');
      left.innerHTML = `<span class="text-success me-2">●</span>`;
      const name = document.createElement('span');
      name.textContent = u;
      left.appendChild(name);

      li.appendChild(left);

      if (u !== username) {
        const btn = document.createElement('button');
        btn.className = 'btn btn-sm btn-outline-primary py-0';
        btn.textContent = 'DM';
        btn.addEventListener('click', () => window.startDM(u));
        li.appendChild(btn);
      } else {
        const me = document.createElement('small');
        me.className = 'text-muted';
        me.textContent = '(vous)';
        li.appendChild(me);
      }

      els.users.appendChild(li);
    }
  }

  function renderConversations() {
    if (!els.conversations) return;

    els.conversations.innerHTML = '';

    const publicLi = document.createElement('li');
    publicLi.className = `list-group-item list-group-item-action ${currentConv.id === 'public' ? 'active' : ''} border-0`;
    publicLi.style.cursor = 'pointer';
    publicLi.textContent = '# Salon public';
    publicLi.addEventListener('click', () => window.switchConv('public', 'PUBLIC', 'Salon public'));
    els.conversations.appendChild(publicLi);

    myConversations.forEach(c => {
      const active = currentConv.id === c.id ? 'active' : '';
      const label = c.type === 'DIRECT' ? `@ ${c.name}` : `# ${c.name}`;

      const li = document.createElement('li');
      li.className = `list-group-item list-group-item-action ${active} border-0`;
      li.style.cursor = 'pointer';
      li.textContent = label;
      li.addEventListener('click', () => window.switchConv(c.id, c.type, c.name));
      els.conversations.appendChild(li);
    });
  }

  // ---------- Actions ----------
  window.switchConv = async (id, type, name) => {
    currentConv = { id, type, name };
    if (els.currentConvLabel) els.currentConvLabel.textContent = type === 'DIRECT' ? `@ ${name}` : name;

    closeContextMenu();
    clearReply();

    if (id !== 'public' && !convMessages.has(id)) {
      try {
        const history = await getJson(`/api/v1/conversations/${id}/history`);
        convMessages.set(id, (history || []).reverse());
      } catch (e) {
        if (!logoutRequested) showToast("Impossible de charger l'historique", "danger");
      }
    }

    if (els.messages) els.messages.innerHTML = '';
    const messagesToRender = convMessages.get(id) || [];
    messagesToRender.forEach(renderInboundMessage);
    autoScroll();
    renderConversations();
  };

  window.startDM = async (peer) => {
    try {
      const res = await postJson('/api/v1/conversations/direct', { peer });
      const data = await safeReadJson(res);

      if (!res.ok) throw new Error(data.error || "Erreur lors de la création du DM");

      const convData = data;
      if (!myConversations.find(c => c.id === convData.id)) {
        myConversations.push(convData);
      }

      ensureSubscribedToConversation(convData.id);
      window.switchConv(convData.id, convData.type, convData.name);
    } catch (e) {
      if (!logoutRequested) showToast(e.message || "Erreur lors de la création du DM", "danger");
    }
  };

  function sendReaction(messageId, emoji) {
    if (!connected || !stomp) return;

    const k = reactionKey(messageId, emoji);

    if (mySelectedReactions.has(k)) mySelectedReactions.delete(k);
    else mySelectedReactions.add(k);

    persistSelectedReactionsForUser(username);

    const badge = document.querySelector(`[data-msg-id="${messageId}"] .reaction-list [data-emoji="${emoji}"]`);
    if (badge) badge.classList.toggle('selected', mySelectedReactions.has(k));

    const destReact = currentConv.id === 'public'
      ? '/app/chat.reaction'
      : `/app/conv.${currentConv.id}.reaction`;

    stomp.send(destReact, {}, JSON.stringify({ messageId, emoji }));
  }

  // ---------- Utils ----------
  function formatRelative(ts) {
    if (!ts) return "";
    let timestamp = ts;
    if (ts.toString().length === 10) timestamp = ts * 1000;
    const now = Date.now();
    const diffInSec = Math.floor((now - timestamp) / 1000);
    if (diffInSec < 20) return "à l'instant";
    return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function autoScroll() {
    if (!els.messages) return;
    els.messages.scrollTop = els.messages.scrollHeight;
  }

  function updateSendButtonState() {
    if (!els.sendBtn || !els.msgInput) return;
    els.sendBtn.disabled = !(connected && els.msgInput.value.trim());
  }

  function scrollToMessage(messageId) {
    if (!messageId) return false;

    const el = document.querySelector(`[data-msg-id="${messageId}"]`);
    if (!el) return false;

    // Scroll smooth
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });

    // Highlight (remove then re-add to retrigger animation)
    el.classList.remove('msg-highlight');
    void el.offsetWidth; // force reflow
    el.classList.add('msg-highlight');

    setTimeout(() => el.classList.remove('msg-highlight'), 1400);
    return true;
  }

  function showMessageNotFoundToast() {
    showToast("Message cité introuvable dans l'historique chargé.", "warning");
  }

  // ---------- Theme ----------
  if (els.themeToggle) {
    els.themeToggle.addEventListener('click', () => {
      const isDark = document.documentElement.getAttribute('data-bs-theme') === 'dark';
      const newTheme = isDark ? 'light' : 'dark';
      document.documentElement.setAttribute('data-bs-theme', newTheme);
      const icon = els.themeToggle.querySelector('#theme-icon');
      if (icon) icon.textContent = isDark ? '☀️' : '🌙';
    });
  }

  // ---------- Global listeners (context menu close) ----------
  document.addEventListener('pointerdown', (e) => {
    if (ctxMenuEl && ctxMenuEl.contains(e.target)) return;
    closeContextMenu();
  });
  document.addEventListener('scroll', () => closeContextMenu(), true);
  window.addEventListener('resize', () => closeContextMenu());
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeContextMenu();
  });

  // ---------- Modal listeners ----------
  if (els.editMessageForm) {
    els.editMessageForm.addEventListener('submit', (e) => {
      e.preventDefault();

      if (!connected || !stomp) {
        if (els.editMessageError) els.editMessageError.textContent = "Non connecté.";
        return;
      }

      const convId = editTarget.convId;
      const messageId = editTarget.messageId;
      const content = (els.editMessageInput?.value || '').trim();

      if (!convId || !messageId) {
        if (els.editMessageError) els.editMessageError.textContent = "Cible invalide.";
        return;
      }
      if (!content) {
        if (els.editMessageError) els.editMessageError.textContent = "Contenu vide.";
        return;
      }

      stomp.send(wsEditDestination(convId), {}, JSON.stringify({ messageId, content }));

      if (els.editMessageModal && window.bootstrap) {
        bootstrap.Modal.getOrCreateInstance(els.editMessageModal).hide();
      }
    });
  }

  if (els.btnConfirmDeleteMessage) {
    els.btnConfirmDeleteMessage.addEventListener('click', () => {
      if (!connected || !stomp) {
        if (els.deleteMessageError) els.deleteMessageError.textContent = "Non connecté.";
        return;
      }

      const convId = deleteTarget.convId;
      const messageId = deleteTarget.messageId;

      if (!convId || !messageId) {
        if (els.deleteMessageError) els.deleteMessageError.textContent = "Cible invalide.";
        return;
      }

      stomp.send(wsDeleteDestination(convId), {}, JSON.stringify({ messageId }));

      if (els.deleteMessageModal && window.bootstrap) {
        bootstrap.Modal.getOrCreateInstance(els.deleteMessageModal).hide();
      }
    });
  }

  if (els.btnCancelReply) els.btnCancelReply.addEventListener('click', clearReply);

  // ---------- Listeners ----------
  if (els.btnLogout) els.btnLogout.addEventListener('click', logout);

  if (els.authForm) {
    els.authForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleLogin();
    });
  }

  if (els.registerForm) {
    els.registerForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleRegister();
    });
  }

  if (els.btnForgotCode) {
    els.btnForgotCode.addEventListener('click', (e) => {
      e.preventDefault();
      handleRequestResetCode();
    });
  }

  if (els.forgotForm) {
    els.forgotForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleResetPassword();
    });
  }

  if (els.msgForm) {
    els.msgForm.addEventListener('submit', (e) => {
      e.preventDefault();
      if (!els.msgInput) return;

      const content = els.msgInput.value.trim();
      if (!content || !connected || !stomp) return;

      const dest = currentConv.id === 'public'
        ? '/app/chat.message'
        : `/app/conv.${currentConv.id}.message`;

      const body = { content };

      // NEW: attach replyTo if present
      if (replyTarget && replyTarget.convId === currentConv.id) {
        body.replyTo = {
          messageId: replyTarget.messageId,
          senderUsername: replyTarget.senderUsername,
          excerpt: replyTarget.excerpt
        };
      }

      stomp.send(dest, {}, JSON.stringify(body));

      clearReply();
      els.msgInput.value = '';
      updateSendButtonState();
    });
  }

  if (els.msgInput) els.msgInput.addEventListener('input', updateSendButtonState);

  // ---------- Cleanup ----------
  window.addEventListener('beforeunload', () => {
    try {
      cleanupConversationSubscriptions();
      if (stomp && connected) stomp.disconnect(() => {});
    } catch {}
  });

  window.addEventListener('pagehide', () => {
    try {
      cleanupConversationSubscriptions();
      if (stomp && connected) stomp.disconnect(() => {});
    } catch {}
  });

})();