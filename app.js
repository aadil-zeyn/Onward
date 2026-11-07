/* Onward — a calm, beautiful todo list. No dependencies, no build step. */
(() => {
  'use strict';

  /* ------------------------------------------------------------------------
     Constants & helpers
     ------------------------------------------------------------------------ */

  const KEYS = { tasks: 'onward:tasks', theme: 'onward:theme', filter: 'onward:filter' };
  const PRIORITIES = ['none', 'low', 'medium', 'high'];
  const PRIORITY_LABEL = { none: 'No priority', low: 'Low', medium: 'Medium', high: 'High' };
  const FILTERS = ['all', 'active', 'completed'];
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)');

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

  const storage = {
    get(key, fallback = null) {
      try {
        const raw = localStorage.getItem(key);
        return raw == null ? fallback : JSON.parse(raw);
      } catch {
        return fallback;
      }
    },
    set(key, value) {
      try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* private mode, quota */ }
    },
  };

  const uid = () =>
    (window.crypto && crypto.randomUUID)
      ? crypto.randomUUID()
      : Date.now().toString(36) + Math.random().toString(36).slice(2, 10);

  const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;

  /* Dates are stored as local "YYYY-MM-DD" strings. */
  const pad = (n) => String(n).padStart(2, '0');
  const toISO = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const fromISO = (s) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); };
  const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
  const startOfToday = () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d; };
  const dayDiff = (iso) => Math.round((fromISO(iso) - startOfToday()) / 86400000);
  const nextMonday = () => { const d = startOfToday(); return addDays(d, ((8 - d.getDay()) % 7) || 7); };

  function describeDue(iso) {
    if (!iso) return null;
    const diff = dayDiff(iso);
    const date = fromISO(iso);
    let label;
    if (diff === 0) label = 'Today';
    else if (diff === 1) label = 'Tomorrow';
    else if (diff === -1) label = 'Yesterday';
    else if (diff > 1 && diff < 7) label = date.toLocaleDateString(undefined, { weekday: 'long' });
    else {
      const opts = { month: 'short', day: 'numeric' };
      if (date.getFullYear() !== new Date().getFullYear()) opts.year = 'numeric';
      label = date.toLocaleDateString(undefined, opts);
    }
    return { label, diff, state: diff < 0 ? 'overdue' : diff === 0 ? 'today' : 'upcoming' };
  }

  /* ------------------------------------------------------------------------
     State
     ------------------------------------------------------------------------ */

  function seedTasks() {
    const now = Date.now();
    const today = toISO(new Date());
    const tomorrow = toISO(addDays(new Date(), 1));
    return [
      { id: uid(), title: 'Welcome to Onward — tap the circle to complete a task', done: false, priority: 'none', due: '', createdAt: now },
      { id: uid(), title: 'Double-click a task (or use the pencil) to edit it', done: false, priority: 'medium', due: today, createdAt: now },
      { id: uid(), title: 'Drag the dotted handle to reorder your list', done: false, priority: 'low', due: tomorrow, createdAt: now },
      { id: uid(), title: 'Give important tasks a priority and a due date', done: false, priority: 'high', due: '', createdAt: now },
      { id: uid(), title: 'Open Onward for the first time', done: true, priority: 'none', due: '', createdAt: now, completedAt: now },
    ];
  }

  function normalize(list) {
    if (!Array.isArray(list)) return null;
    return list
      .filter((t) => t && t.id != null && typeof t.title === 'string')
      .map((t) => ({
        id: String(t.id),
        title: t.title.slice(0, 300),
        done: Boolean(t.done),
        priority: PRIORITIES.includes(t.priority) ? t.priority : 'none',
        due: /^\d{4}-\d{2}-\d{2}$/.test(t.due || '') ? t.due : '',
        createdAt: Number(t.createdAt) || Date.now(),
        completedAt: t.completedAt ? Number(t.completedAt) : null,
      }));
  }

  let tasks = normalize(storage.get(KEYS.tasks)) ?? seedTasks();
  let filter = FILTERS.includes(storage.get(KEYS.filter)) ? storage.get(KEYS.filter) : 'all';
  let editingId = null;
  let undoState = null;
  let pendingListRender = 0;

  const save = () => storage.set(KEYS.tasks, tasks);
  const findTask = (id) => tasks.find((t) => t.id === id);
  const matchesFilter = (t) => filter === 'all' || (filter === 'active' ? !t.done : t.done);

  /* ------------------------------------------------------------------------
     Elements
     ------------------------------------------------------------------------ */

  const root = document.documentElement;
  const el = {
    themeBtn: $('#theme-toggle'),
    themeMeta: $('meta[name="theme-color"]'),
    todayLabel: $('#today-label'),
    greeting: $('#greeting'),
    summary: $('#summary'),
    ring: $('#ring'),
    ringBar: $('#ring-bar'),
    ringLabel: $('#ring-label'),
    composer: $('#composer'),
    input: $('#new-title'),
    addBtn: $('#add-btn'),
    newPriority: $('#new-priority'),
    newDue: $('#new-due'),
    tabs: $('#tabs'),
    tabIndicator: $('#tab-indicator'),
    clearDone: $('#clear-done'),
    list: $('#list'),
    empty: $('#empty'),
    emptyTitle: $('#empty-title'),
    emptyText: $('#empty-text'),
    toast: $('#toast'),
    toastText: $('#toast-text'),
    toastUndo: $('#toast-undo'),
    menu: $('#date-menu'),
    template: $('#task-template'),
  };

  const nodes = new Map(); // task id -> <li>

  const RING_C = 2 * Math.PI * 27;
  el.ringBar.style.strokeDasharray = String(RING_C);
  el.ringBar.style.strokeDashoffset = String(RING_C);

  /* ------------------------------------------------------------------------
     Theme
     ------------------------------------------------------------------------ */

  const effectiveTheme = () => root.dataset.theme || (prefersDark.matches ? 'dark' : 'light');

  function syncThemeUI() {
    const mode = effectiveTheme();
    el.themeBtn.dataset.mode = mode;
    el.themeBtn.setAttribute('aria-label', mode === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
    el.themeMeta.setAttribute('content', getComputedStyle(root).getPropertyValue('--bg').trim() || '#f4f0e8');
  }

  el.themeBtn.addEventListener('click', () => {
    const next = effectiveTheme() === 'dark' ? 'light' : 'dark';
    root.dataset.theme = next;
    storage.set(KEYS.theme, next);
    syncThemeUI();
  });
  prefersDark.addEventListener?.('change', syncThemeUI);

  /* ------------------------------------------------------------------------
     Shared controls: priority picker + date field
     ------------------------------------------------------------------------ */

  function setPriority(picker, value) {
    picker.dataset.value = value;
    $$('.prio-option', picker).forEach((opt) => {
      const on = opt.dataset.priority === value;
      opt.setAttribute('aria-checked', String(on));
      opt.tabIndex = on ? 0 : -1;
    });
  }

  function setDate(field, iso) {
    const info = describeDue(iso);
    field.dataset.value = iso || '';
    field.dataset.state = info ? info.state : '';
    field.classList.toggle('has-value', Boolean(iso));
    $('.date-label', field).textContent = info ? info.label : 'Due date';
    $('.date-input', field).value = iso || '';
    $('.date-btn', field).setAttribute('aria-label', info ? `Due ${info.label}. Change due date` : 'Set a due date');
  }

  document.addEventListener('click', (e) => {
    const opt = e.target.closest('.prio-option');
    if (opt) { setPriority(opt.parentElement, opt.dataset.priority); return; }

    const dateBtn = e.target.closest('.date-btn');
    if (dateBtn) {
      const field = dateBtn.closest('.date-field');
      if (menuField === field && el.menu.classList.contains('open')) closeMenu(false);
      else openMenu(field);
      return;
    }

    const clear = e.target.closest('.date-clear');
    if (clear) { setDate(clear.closest('.date-field'), ''); focusFieldOwner(clear.closest('.date-field')); }
  });

  document.addEventListener('change', (e) => {
    if (e.target.matches('.date-input')) {
      const field = e.target.closest('.date-field');
      setDate(field, e.target.value);
      focusFieldOwner(field);
    }
  });

  // Arrow keys move through a radio group, like native radios.
  document.addEventListener('keydown', (e) => {
    const opt = e.target.closest?.('.prio-option');
    if (!opt) return;
    const dir = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[e.key];
    if (!dir) return;
    e.preventDefault();
    const opts = $$('.prio-option', opt.parentElement);
    const next = opts[(opts.indexOf(opt) + dir + opts.length) % opts.length];
    setPriority(opt.parentElement, next.dataset.priority);
    next.focus();
  });

  // After choosing a date, send focus back to where the user was typing.
  function focusFieldOwner(field) {
    if (field === el.newDue) el.input.focus();
    else $('.edit-input', field.closest('.task'))?.focus();
  }

  /* ------------------------------------------------------------------------
     Due-date menu
     ------------------------------------------------------------------------ */

  let menuField = null;

  function openMenu(field) {
    menuField = field;
    const fmt = (d) => d.toLocaleDateString(undefined, { weekday: 'short' });
    $('[data-when="today"] .menu-hint', el.menu).textContent = fmt(new Date());
    $('[data-when="tomorrow"] .menu-hint', el.menu).textContent = fmt(addDays(new Date(), 1));
    $('[data-when="next-week"] .menu-hint', el.menu).textContent =
      nextMonday().toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
    const hasValue = Boolean(field.dataset.value);
    $('[data-when="none"]', el.menu).hidden = !hasValue;
    $('[data-when-sep]', el.menu).hidden = !hasValue;

    const r = field.getBoundingClientRect();
    const mw = el.menu.offsetWidth;
    const mh = el.menu.offsetHeight;
    const left = Math.max(12, Math.min(r.left, window.innerWidth - mw - 12));
    let top = r.bottom + 6;
    const above = top + mh > window.innerHeight - 12 && r.top - mh - 6 > 12;
    if (above) top = r.top - mh - 6;
    el.menu.classList.toggle('above', above);
    el.menu.style.left = `${left}px`;
    el.menu.style.top = `${top}px`;
    el.menu.classList.add('open');
    $$('.date-btn').forEach((b) => b.setAttribute('aria-expanded', 'false'));
    $('.date-btn', field).setAttribute('aria-expanded', 'true');
    $('.menu-item', el.menu).focus({ preventScroll: true });
  }

  function closeMenu(restoreFocus) {
    if (!el.menu.classList.contains('open')) return;
    el.menu.classList.remove('open');
    const field = menuField;
    if (field) $('.date-btn', field).setAttribute('aria-expanded', 'false');
    if (restoreFocus && field) focusFieldOwner(field);
  }

  el.menu.addEventListener('click', (e) => {
    const item = e.target.closest('.menu-item');
    if (!item || !menuField) return;
    const field = menuField;
    const when = item.dataset.when;
    if (when === 'pick') {
      closeMenu(false);
      const input = $('.date-input', field);
      input.value = field.dataset.value || toISO(new Date());
      try { input.showPicker(); } catch { input.focus(); input.click(); }
      return;
    }
    const value = {
      today: toISO(new Date()),
      tomorrow: toISO(addDays(new Date(), 1)),
      'next-week': toISO(nextMonday()),
      none: '',
    }[when];
    setDate(field, value);
    closeMenu(true);
  });

  el.menu.addEventListener('keydown', (e) => {
    const items = $$('.menu-item', el.menu).filter((i) => !i.hidden);
    const i = items.indexOf(document.activeElement);
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      const d = e.key === 'ArrowDown' ? 1 : -1;
      items[(i + d + items.length) % items.length].focus();
    } else if (e.key === 'Tab') {
      closeMenu(false);
    }
  });

  document.addEventListener('pointerdown', (e) => {
    if (el.menu.classList.contains('open') && !e.target.closest('#date-menu, .date-btn')) closeMenu(false);
  });
  window.addEventListener('resize', () => closeMenu(false));
  window.addEventListener('scroll', () => closeMenu(false), { passive: true });

  /* ------------------------------------------------------------------------
     Rendering
     ------------------------------------------------------------------------ */

  function renderChrome() {
    const now = new Date();
    const total = tasks.length;
    const done = tasks.filter((t) => t.done).length;
    const active = total - done;
    const open = tasks.filter((t) => !t.done && t.due);
    const dueToday = open.filter((t) => dayDiff(t.due) === 0).length;
    const overdue = open.filter((t) => dayDiff(t.due) < 0).length;

    el.todayLabel.textContent = now.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
    const h = now.getHours();
    el.greeting.textContent =
      h < 5 ? 'Still up?' : h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : h < 22 ? 'Good evening' : 'Good night';

    el.summary.textContent = '';
    if (!total) {
      el.summary.textContent = 'A clean slate. What’s first?';
    } else if (!active) {
      el.summary.textContent = 'Everything’s done. Onward.';
    } else {
      const parts = [`${plural(active, 'task')} to go`];
      if (dueToday) parts.push(`${dueToday} due today`);
      el.summary.append(parts.join(' · '));
      if (overdue) {
        const warn = document.createElement('span');
        warn.className = 'warn';
        warn.textContent = `${overdue} overdue`;
        el.summary.append(' · ', warn);
      }
    }

    const pct = total ? done / total : 0;
    el.ringBar.style.strokeDashoffset = String(RING_C * (1 - pct));
    el.ringLabel.textContent = `${Math.round(pct * 100)}%`;
    el.ring.setAttribute('aria-label', total ? `${done} of ${total} tasks done` : 'No tasks yet');

    $('[data-count="all"]', el.tabs).textContent = total;
    $('[data-count="active"]', el.tabs).textContent = active;
    $('[data-count="completed"]', el.tabs).textContent = done;
    $$('.tab', el.tabs).forEach((tab) => tab.setAttribute('aria-selected', String(tab.dataset.filter === filter)));
    moveIndicator();

    el.clearDone.disabled = done === 0;
    document.title = active ? `(${active}) Onward` : 'Onward';
  }

  function moveIndicator(animate = true) {
    const tab = $('.tab[aria-selected="true"]', el.tabs);
    if (!tab) return;
    if (!animate) el.tabIndicator.style.transition = 'none';
    el.tabIndicator.style.width = `${tab.offsetWidth}px`;
    el.tabIndicator.style.transform = `translateX(${tab.offsetLeft}px)`;
    if (!animate) {
      void el.tabIndicator.offsetWidth;
      el.tabIndicator.style.transition = '';
    }
  }

  function createTaskEl(task) {
    const li = el.template.content.firstElementChild.cloneNode(true);
    li.dataset.id = task.id;
    li.refs = {
      check: $('.check', li),
      title: $('.task-title span', li),
      meta: $('.task-meta', li),
      due: $('.meta-due', li),
      dueText: $('.meta-due > span', li),
      prio: $('.meta-prio', li),
      prioText: $('.meta-prio > span', li),
      editInput: $('.edit-input', li),
      editPrio: $('.editor .prio-picker', li),
      editDate: $('.editor .date-field', li),
    };
    return li;
  }

  function updateTaskEl(li, task) {
    const r = li.refs;
    li.dataset.priority = task.priority;
    li.classList.toggle('done', task.done);
    r.check.setAttribute('aria-checked', String(task.done));
    r.check.setAttribute('aria-label', `${task.done ? 'Mark as not done' : 'Mark as done'}: ${task.title}`);
    if (r.title.textContent !== task.title) r.title.textContent = task.title;

    const due = describeDue(task.due);
    r.due.hidden = !due;
    if (due) {
      r.dueText.textContent = due.label;
      r.due.dataset.state = due.state;
    }
    r.prio.hidden = task.priority === 'none';
    r.prioText.textContent = PRIORITY_LABEL[task.priority];
    r.meta.hidden = !due && task.priority === 'none';
  }

  function animateIn(li, delay) {
    if (reduceMotion.matches) return;
    li.classList.add('entering');
    li.style.animationDelay = delay ? `${delay}ms` : '';
    const done = (e) => {
      if (e.target !== li) return;
      li.classList.remove('entering');
      li.style.animationDelay = '';
      li.removeEventListener('animationend', done);
    };
    li.addEventListener('animationend', done);
  }

  function animateOut(li, instant) {
    if (instant || reduceMotion.matches) { li.remove(); return; }
    li.style.height = `${li.offsetHeight}px`;
    li.classList.add('leaving');
    void li.offsetHeight;
    li.style.height = '0px';
    let removed = false;
    const remove = () => { if (!removed) { removed = true; li.remove(); } };
    li.addEventListener('transitionend', (e) => { if (e.target === li && e.propertyName === 'height') remove(); });
    setTimeout(remove, 500);
  }

  function renderList({ stagger = false } = {}) {
    clearTimeout(pendingListRender);
    const visible = tasks.filter(matchesFilter);
    const ids = new Set(visible.map((t) => t.id));

    for (const [id, li] of nodes) {
      if (ids.has(id)) continue;
      nodes.delete(id);
      if (id === editingId) editingId = null;
      animateOut(li, stagger);
    }

    let cursor = el.list.firstElementChild;
    let fresh = 0;
    for (const task of visible) {
      let li = nodes.get(task.id);
      if (!li) {
        li = createTaskEl(task);
        nodes.set(task.id, li);
        animateIn(li, stagger ? Math.min(fresh++, 10) * 35 : 0);
      }
      if (task.id !== editingId) updateTaskEl(li, task);

      while (cursor && cursor.classList.contains('leaving')) cursor = cursor.nextElementSibling;
      if (cursor === li) cursor = li.nextElementSibling;
      else el.list.insertBefore(li, cursor);
    }

    const isEmpty = visible.length === 0;
    el.empty.hidden = !isEmpty;
    if (isEmpty) {
      const copy = {
        all: ['All clear', 'Nothing on your plate. Add a task above to get going.'],
        active: tasks.length
          ? ['Everything’s done', 'Every task is checked off. Take a breath — you earned it.']
          : ['All clear', 'No active tasks. Add one above when you’re ready.'],
        completed: ['Nothing done yet', 'Completed tasks will gather here. You’ve got this.'],
      }[filter];
      el.emptyTitle.textContent = copy[0];
      el.emptyText.textContent = copy[1];
    }
  }

  function render(opts) {
    renderChrome();
    renderList(opts);
  }

  /* ------------------------------------------------------------------------
     Actions
     ------------------------------------------------------------------------ */

  function addTask(title, priority, due) {
    tasks.unshift({ id: uid(), title, done: false, priority, due, createdAt: Date.now(), completedAt: null });
    save();
    if (filter === 'completed') setFilter('all');
    else render();
  }

  function toggleTask(id) {
    const task = findTask(id);
    if (!task) return;
    task.done = !task.done;
    task.completedAt = task.done ? Date.now() : null;
    save();

    const li = nodes.get(id);
    if (li) {
      updateTaskEl(li, task);
      li.classList.remove('just-checked');
      if (task.done) { void li.offsetWidth; li.classList.add('just-checked'); }
    }
    renderChrome();

    // Let the check animation play before a task leaves the current view.
    if (matchesFilter(task)) renderList();
    else {
      clearTimeout(pendingListRender);
      pendingListRender = setTimeout(() => renderList(), 520);
    }

    if (task.done && tasks.every((t) => t.done)) celebrate();
  }

  function deleteTasks(predicate, message) {
    const removed = [];
    tasks.forEach((task, index) => { if (predicate(task)) removed.push({ task, index }); });
    if (!removed.length) return;
    tasks = tasks.filter((t) => !predicate(t));
    undoState = removed;
    save();
    render();
    showToast(message(removed.length), true);
  }

  const deleteTask = (id) => deleteTasks((t) => t.id === id, () => 'Task deleted');

  function undo() {
    if (!undoState) return;
    undoState
      .sort((a, b) => a.index - b.index)
      .forEach(({ task, index }) => {
        if (!findTask(task.id)) tasks.splice(Math.min(index, tasks.length), 0, task);
      });
    undoState = null;
    save();
    hideToast();
    render();
  }

  function setFilter(next) {
    if (!FILTERS.includes(next)) return;
    if (editingId) commitEdit();
    filter = next;
    storage.set(KEYS.filter, filter);
    render({ stagger: true });
  }

  function applyVisibleOrder(orderedIds) {
    const set = new Set(orderedIds);
    const byId = new Map(tasks.map((t) => [t.id, t]));
    let k = 0;
    tasks = tasks.map((t) => (set.has(t.id) ? byId.get(orderedIds[k++]) : t));
    save();
  }

  /* Editing */

  function startEdit(id) {
    if (editingId === id) return;
    if (editingId) commitEdit();
    const li = nodes.get(id);
    const task = findTask(id);
    if (!li || !task) return;
    editingId = id;
    li.classList.add('editing');
    const r = li.refs;
    r.editInput.value = task.title;
    setPriority(r.editPrio, task.priority);
    setDate(r.editDate, task.due);
    r.editInput.focus();
    r.editInput.setSelectionRange(task.title.length, task.title.length);
  }

  function commitEdit() {
    const id = editingId;
    if (!id) return;
    editingId = null;
    closeMenu(false);
    const li = nodes.get(id);
    const task = findTask(id);
    if (!li || !task) return;
    li.classList.remove('editing');
    const title = li.refs.editInput.value.trim();
    if (!title) { deleteTask(id); return; }
    task.title = title;
    task.priority = li.refs.editPrio.dataset.value;
    task.due = li.refs.editDate.dataset.value;
    save();
    render();
  }

  function cancelEdit() {
    const id = editingId;
    if (!id) return;
    editingId = null;
    closeMenu(false);
    const li = nodes.get(id);
    if (!li) return;
    li.classList.remove('editing');
    updateTaskEl(li, findTask(id));
    $('.act-edit', li).focus();
  }

  // Clicking anywhere outside the task being edited saves it.
  document.addEventListener('pointerdown', (e) => {
    if (!editingId) return;
    const li = nodes.get(editingId);
    if (li && !li.contains(e.target) && !e.target.closest('#date-menu')) commitEdit();
  });

  /* ------------------------------------------------------------------------
     Toast & celebration
     ------------------------------------------------------------------------ */

  let toastTimer = 0;

  function showToast(message, withUndo) {
    el.toastText.textContent = message;
    el.toastUndo.hidden = !withUndo;
    el.toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(hideToast, 5000);
  }

  function hideToast() {
    el.toast.classList.remove('show');
    clearTimeout(toastTimer);
  }

  el.toastUndo.addEventListener('click', undo);
  el.toast.addEventListener('pointerenter', () => clearTimeout(toastTimer));
  el.toast.addEventListener('pointerleave', () => { toastTimer = setTimeout(hideToast, 2500); });

  function celebrate() {
    el.ring.classList.remove('pop');
    void el.ring.offsetWidth;
    el.ring.classList.add('pop');
    showToast('All done. Nicely played.', false);
    if (reduceMotion.matches || !Element.prototype.animate) return;

    const r = el.ring.getBoundingClientRect();
    const cx = r.left + r.width / 2;
    const cy = r.top + r.height / 2;
    const css = getComputedStyle(root);
    const colors = ['--accent', '--high', '--medium', '--low'].map((v) => css.getPropertyValue(v).trim());
    for (let i = 0; i < 32; i++) {
      const p = document.createElement('span');
      p.className = 'confetti';
      p.style.background = colors[i % colors.length];
      p.style.left = `${cx}px`;
      p.style.top = `${cy}px`;
      document.body.appendChild(p);
      const angle = Math.random() * Math.PI * 2;
      const dist = 50 + Math.random() * 110;
      const dx = Math.cos(angle) * dist;
      const dy = Math.sin(angle) * dist;
      p.animate(
        [
          { transform: 'translate(-50%, -50%) scale(0.4) rotate(0deg)', opacity: 1 },
          { transform: `translate(calc(-50% + ${dx}px), calc(-50% + ${dy}px)) scale(1) rotate(${Math.random() * 360}deg)`, opacity: 1, offset: 0.45 },
          { transform: `translate(calc(-50% + ${dx * 1.15}px), calc(-50% + ${dy + 90}px)) scale(0.8) rotate(${Math.random() * 720}deg)`, opacity: 0 },
        ],
        { duration: 1100 + Math.random() * 600, easing: 'cubic-bezier(.15,.7,.3,1)', fill: 'forwards' }
      ).onfinish = () => p.remove();
    }
  }

  /* ------------------------------------------------------------------------
     Drag to reorder (pointer events: mouse, touch and pen)
     ------------------------------------------------------------------------ */

  function beginDrag(e, li, grip) {
    const items = [...el.list.children].filter((n) => !n.classList.contains('leaving'));
    const from = items.indexOf(li);
    if (items.length < 2 || from < 0) return;
    e.preventDefault();

    const rects = items.map((n) => n.getBoundingClientRect());
    const self = rects[from];
    const minDy = rects[0].top - self.top;
    const maxDy = rects[rects.length - 1].bottom - self.bottom;
    const startY = e.clientY;
    let dy = 0;
    let to = from;

    grip.setPointerCapture(e.pointerId);
    li.classList.add('dragging');
    el.list.classList.add('is-dragging');
    document.body.classList.add('is-grabbing');

    const onMove = (ev) => {
      dy = Math.max(minDy, Math.min(maxDy, ev.clientY - startY));
      li.style.transform = `translateY(${dy}px) scale(1.015)`;
      const center = self.top + self.height / 2 + dy;
      to = 0;
      rects.forEach((rc, i) => { if (i !== from && center > rc.top + rc.height / 2) to++; });
      items.forEach((n, i) => {
        if (i === from) return;
        let shift = 0;
        if (from < to && i > from && i <= to) shift = -self.height;
        else if (to < from && i >= to && i < from) shift = self.height;
        n.style.transform = shift ? `translateY(${shift}px)` : '';
      });
    };

    const onEnd = () => {
      grip.removeEventListener('pointermove', onMove);
      grip.removeEventListener('pointerup', onEnd);
      grip.removeEventListener('pointercancel', onEnd);
      const visualTop = self.top + dy;

      items.forEach((n) => { n.style.transition = 'none'; n.style.transform = ''; });
      li.classList.remove('dragging');
      el.list.classList.remove('is-dragging');
      document.body.classList.remove('is-grabbing');

      if (to !== from) {
        const ids = items.map((n) => n.dataset.id);
        ids.splice(to, 0, ids.splice(from, 1)[0]);
        applyVisibleOrder(ids);
        renderList();
      }

      // Glide the dropped item from where it was released into its slot.
      const delta = visualTop - li.getBoundingClientRect().top;
      li.style.transform = `translateY(${delta}px)`;
      void li.offsetHeight;
      items.forEach((n) => { n.style.transition = ''; });
      if (Math.abs(delta) > 0.5 && !reduceMotion.matches) {
        li.style.transition = 'transform .28s cubic-bezier(.2,.8,.2,1)';
        li.style.transform = '';
        setTimeout(() => { li.style.transition = ''; }, 300);
      } else {
        li.style.transform = '';
      }
    };

    grip.addEventListener('pointermove', onMove);
    grip.addEventListener('pointerup', onEnd);
    grip.addEventListener('pointercancel', onEnd);
  }

  function moveByKeyboard(li, dir) {
    const items = [...el.list.children].filter((n) => !n.classList.contains('leaving'));
    const from = items.indexOf(li);
    const to = from + dir;
    if (from < 0 || to < 0 || to >= items.length) return;
    const ids = items.map((n) => n.dataset.id);
    ids.splice(to, 0, ids.splice(from, 1)[0]);
    applyVisibleOrder(ids);
    renderList();
    $('.grip', li).focus();
  }

  /* ------------------------------------------------------------------------
     Events
     ------------------------------------------------------------------------ */

  el.input.addEventListener('input', () => { el.addBtn.disabled = !el.input.value.trim(); });
  el.input.addEventListener('keydown', (e) => { if (e.key === 'Escape') el.input.blur(); });

  el.composer.addEventListener('submit', (e) => {
    e.preventDefault();
    const title = el.input.value.trim();
    if (!title) {
      el.composer.classList.remove('shake');
      void el.composer.offsetWidth;
      el.composer.classList.add('shake');
      return;
    }
    addTask(title, el.newPriority.dataset.value || 'none', el.newDue.dataset.value || '');
    el.input.value = '';
    el.addBtn.disabled = true;
    setPriority(el.newPriority, 'none');
    setDate(el.newDue, '');
    el.input.focus();
  });
  el.composer.addEventListener('animationend', () => el.composer.classList.remove('shake'));

  el.tabs.addEventListener('click', (e) => {
    const tab = e.target.closest('.tab');
    if (tab && tab.dataset.filter !== filter) setFilter(tab.dataset.filter);
  });
  el.tabs.addEventListener('keydown', (e) => {
    const dir = { ArrowRight: 1, ArrowLeft: -1 }[e.key];
    if (!dir) return;
    const i = FILTERS.indexOf(filter);
    const next = FILTERS[(i + dir + FILTERS.length) % FILTERS.length];
    setFilter(next);
    $(`.tab[data-filter="${next}"]`, el.tabs).focus();
  });

  el.clearDone.addEventListener('click', () =>
    deleteTasks((t) => t.done, (n) => `Cleared ${plural(n, 'completed task')}`)
  );

  el.list.addEventListener('click', (e) => {
    const li = e.target.closest('.task');
    if (!li) return;
    const id = li.dataset.id;
    if (e.target.closest('.check')) toggleTask(id);
    else if (e.target.closest('.act-edit')) startEdit(id);
    else if (e.target.closest('.act-delete')) deleteTask(id);
    else if (e.target.closest('.act-save')) commitEdit();
    else if (e.target.closest('.act-cancel')) cancelEdit();
  });

  el.list.addEventListener('dblclick', (e) => {
    const title = e.target.closest('.task-title');
    if (title) startEdit(title.closest('.task').dataset.id);
  });

  el.list.addEventListener('keydown', (e) => {
    if (e.target.matches('.edit-input')) {
      if (e.key === 'Enter' && !e.isComposing) { e.preventDefault(); commitEdit(); }
      else if (e.key === 'Escape') { e.preventDefault(); cancelEdit(); }
      return;
    }
    if (e.target.matches('.grip') && (e.key === 'ArrowUp' || e.key === 'ArrowDown')) {
      e.preventDefault();
      moveByKeyboard(e.target.closest('.task'), e.key === 'ArrowUp' ? -1 : 1);
    }
  });

  el.list.addEventListener('pointerdown', (e) => {
    const grip = e.target.closest('.grip');
    if (!grip || e.button !== 0 || editingId) return;
    beginDrag(e, grip.closest('.task'), grip);
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && el.menu.classList.contains('open')) {
      e.preventDefault();
      closeMenu(true);
      return;
    }
    const typing = e.target.closest?.('input, textarea, select, [contenteditable="true"]');
    if (!typing && (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'z' && undoState && el.toast.classList.contains('show')) {
      e.preventDefault();
      undo();
      return;
    }
    if (typing || e.metaKey || e.ctrlKey || e.altKey) return;
    if (e.key === 'n' || e.key === 'N' || e.key === '/') {
      e.preventDefault();
      el.input.focus();
    }
  });

  // Keep multiple open tabs in sync.
  window.addEventListener('storage', (e) => {
    if (e.key !== KEYS.tasks) return;
    const next = normalize(storage.get(KEYS.tasks));
    if (!next) return;
    if (editingId) cancelEdit();
    tasks = next;
    render();
  });

  // Keep "Today", greetings and overdue states fresh across midnight.
  setInterval(() => { if (!editingId) render(); }, 60 * 1000);

  if ('ResizeObserver' in window) new ResizeObserver(() => moveIndicator(false)).observe(el.tabs);

  /* ------------------------------------------------------------------------
     Boot
     ------------------------------------------------------------------------ */

  syncThemeUI();
  setDate(el.newDue, '');
  render({ stagger: true });
  moveIndicator(false);
  document.fonts?.ready.then(() => moveIndicator(false));
  save();
})();
