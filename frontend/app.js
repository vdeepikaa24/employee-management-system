(() => {
      const API_BASE = 'http://localhost:8080';
      const state = { token: '', role: '', email: '', page: 0, size: 10, sort: 'asc', name: '', departmentId: '', totalPages: 0, departments: [], searchTimer: null };
      const byId = (id) => document.getElementById(id);
      const loginView = byId('loginView');
      const appView = byId('appView');
      const sessionBar = byId('sessionBar');
      const modalBackdrop = byId('modalBackdrop');
      byId('apiAddress').textContent = API_BASE;

      function notice(target, message, kind = 'info') {
        target.textContent = message;
        target.className = `notice ${kind}`;
      }

      function clearNotice(target) {
        target.textContent = '';
        target.className = 'notice';
      }

      function escapeHtml(value = '') {
        return String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
      }

      async function api(path, options = {}) {
        const headers = { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...options.headers };
        let response;
        try {
          response = await fetch(`${API_BASE}${path}`, { ...options, headers });
        } catch (error) {
          throw new Error(`Cannot reach ${API_BASE}. Start the Spring Boot API and check the local frontend server.`);
        }
        const raw = response.status === 204 ? '' : await response.text();
        let payload = null;
        if (raw) {
          try { payload = JSON.parse(raw); } catch { payload = raw; }
        }
        if (!response.ok) {
          if (response.status === 401 && state.token) signOut();
          const message = payload?.message || payload?.detail || payload?.error || `Request failed (${response.status})`;
          throw new Error(message);
        }
        return payload;
      }

      async function login(email, password) {
        const result = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
        state.token = result.accessToken;
        state.role = result.role;
        state.email = email;
        state.page = 0;
        sessionStorage.setItem('peopleDeskToken', state.token);
        sessionStorage.setItem('peopleDeskRole', state.role);
        sessionStorage.setItem('peopleDeskEmail', state.email);
        showDashboard();
      }

      function signOut() {
        state.token = '';
        state.role = '';
        state.email = '';
        sessionStorage.removeItem('peopleDeskToken');
        sessionStorage.removeItem('peopleDeskRole');
        sessionStorage.removeItem('peopleDeskEmail');
        loginView.classList.remove('hidden');
        appView.classList.add('hidden');
        sessionBar.classList.add('hidden');
        byId('passwordInput').value = '';
      }

      async function showDashboard() {
        loginView.classList.add('hidden');
        appView.classList.remove('hidden');
        sessionBar.classList.remove('hidden');
        byId('sessionEmail').textContent = state.email;
        byId('sessionRole').textContent = state.role;
        document.querySelectorAll('.admin-only').forEach((element) => {
          element.classList.toggle('hidden', state.role !== 'ADMIN');
        });
        await Promise.all([loadEmployees(), loadDepartments()]);
      }

      async function loadEmployees() {
        clearNotice(byId('appNotice'));
        const params = new URLSearchParams({ name: state.name, page: String(state.page), size: String(state.size), sort: `name,${state.sort}` });
        if (state.departmentId) params.set('departmentId', state.departmentId);
        try {
          const page = await api(`/api/employees/search?${params}`);
          state.totalPages = page.totalPages;
          byId('resultSummary').textContent = `${page.totalElements} employee${page.totalElements === 1 ? '' : 's'} found`;
          byId('pageSummary').textContent = `Page ${page.number + 1} of ${Math.max(page.totalPages, 1)} · ${page.size} per page`;
          byId('previousButton').disabled = page.first;
          byId('nextButton').disabled = page.last;
          const rows = byId('employeeRows');
          rows.innerHTML = page.content.map((employee) => `
            <tr>
              <td><div class="employee-name">${escapeHtml(employee.name)}</div><div class="employee-email">${escapeHtml(employee.email)}</div></td>
              <td><span class="dept-chip">${escapeHtml(employee.departmentName)}</span></td>
              <td class="muted">${escapeHtml(employee.createdBy || '—')}</td>
              ${state.role === 'ADMIN' ? `<td><div class="row-actions"><button class="icon-button edit-employee" data-id="${employee.id}" type="button" title="Edit" aria-label="Edit ${escapeHtml(employee.name)}">✎</button><button class="button button-danger delete-employee" data-id="${employee.id}" type="button">Delete</button></div></td>` : ''}
            </tr>`).join('');
          byId('emptyState').classList.toggle('hidden', page.content.length > 0);
          rows.classList.toggle('hidden', page.content.length === 0);
          rows.querySelectorAll('.edit-employee').forEach((button) => button.addEventListener('click', () => {
            const employee = page.content.find((item) => String(item.id) === button.dataset.id);
            if (employee) openEmployeeModal(employee);
          }));
          rows.querySelectorAll('.delete-employee').forEach((button) => button.addEventListener('click', () => deleteEmployee(button.dataset.id)));
        } catch (error) {
          notice(byId('appNotice'), error.message, 'error');
        }
      }

      async function loadDepartments() {
        try {
          state.departments = await api('/api/departments');
          const departmentFilter = byId('departmentFilter');
          const selectedDepartmentId = state.departmentId;
          departmentFilter.innerHTML = '<option value="">All departments</option>' + state.departments
            .map((department) => `<option value="${department.id}">${escapeHtml(department.name)}</option>`).join('');
          departmentFilter.value = selectedDepartmentId;
          byId('departmentSummary').textContent = `${state.departments.length} department${state.departments.length === 1 ? '' : 's'} in the organization`;
          byId('departmentList').innerHTML = state.departments.length
            ? state.departments.map((department) => `
                <li>
                  <span class="department-label"><span class="dept-dot"></span>${escapeHtml(department.name)}</span>
                  ${state.role === 'ADMIN' ? `<span class="department-actions"><button class="icon-button edit-department" data-id="${department.id}" type="button" title="Rename ${escapeHtml(department.name)}" aria-label="Rename ${escapeHtml(department.name)}">✎</button><button class="button button-danger delete-department" data-id="${department.id}" type="button">Delete</button></span>` : ''}
                </li>`).join('')
            : '<li class="muted">No departments yet</li>';
          byId('departmentList').querySelectorAll('.edit-department').forEach((button) => button.addEventListener('click', () => {
            const department = state.departments.find((item) => String(item.id) === button.dataset.id);
            if (department) openDepartmentModal(department);
          }));
          byId('departmentList').querySelectorAll('.delete-department').forEach((button) => button.addEventListener('click', () => {
            const department = state.departments.find((item) => String(item.id) === button.dataset.id);
            if (department) deleteDepartment(department);
          }));
        } catch (error) {
          byId('departmentSummary').textContent = error.message;
        }
      }

      function openEmployeeModal(employee = null) {
        if (!state.departments.length) {
          notice(byId('appNotice'), 'Create a department before adding an employee.', 'info');
          return;
        }
        byId('departmentForm').classList.add('hidden');
        byId('employeeForm').classList.remove('hidden');
        byId('modalTitle').textContent = employee ? 'Edit employee' : 'Add employee';
        byId('employeeId').value = employee?.id || '';
        byId('employeeName').value = employee?.name || '';
        byId('employeeEmail').value = employee?.email || '';
        byId('employeeDepartment').innerHTML = state.departments.map((department) => `<option value="${department.id}">${escapeHtml(department.name)}</option>`).join('');
        if (employee) byId('employeeDepartment').value = employee.departmentId;
        modalBackdrop.classList.add('open');
        byId('employeeName').focus();
      }

      function openDepartmentModal(department = null) {
        byId('employeeForm').classList.add('hidden');
        byId('departmentForm').classList.remove('hidden');
        byId('modalTitle').textContent = department ? 'Rename department' : 'Add department';
        byId('departmentId').value = department?.id || '';
        byId('departmentName').value = department?.name || '';
        byId('saveDepartmentButton').textContent = department ? 'Save changes' : 'Create department';
        modalBackdrop.classList.add('open');
        byId('departmentName').focus();
      }

      function closeModal() { modalBackdrop.classList.remove('open'); }

      async function deleteEmployee(id) {
        if (!window.confirm('Delete this employee record? This cannot be undone.')) return;
        try {
          await api(`/api/employees/${id}`, { method: 'DELETE' });
          notice(byId('appNotice'), 'Employee deleted.', 'success');
          if (byId('employeeRows').children.length === 1 && state.page > 0) state.page -= 1;
          await loadEmployees();
        } catch (error) { notice(byId('appNotice'), error.message, 'error'); }
      }

      async function deleteDepartment(department) {
        if (!window.confirm(`Delete the department "${department.name}"? This cannot be undone.`)) return;
        try {
          await api(`/api/departments/${department.id}`, { method: 'DELETE' });
          if (state.departmentId === String(department.id)) {
            state.departmentId = '';
            byId('departmentFilter').value = '';
          }
          state.page = 0;
          notice(byId('appNotice'), `Department "${department.name}" deleted.`, 'success');
          await Promise.all([loadDepartments(), loadEmployees()]);
        } catch (error) {
          notice(byId('appNotice'), error.message, 'error');
        }
      }

      function csvCell(value) {
        let text = String(value ?? '');
        if (/^[\s\uFEFF]*[=+@\-]/.test(text)) text = `'${text}`;
        return `"${text.replace(/"/g, '""')}"`;
      }

      async function exportEmployeesCsv() {
        const button = byId('exportCsvButton');
        button.disabled = true;
        button.textContent = 'Preparing CSV…';
        try {
          const name = byId('searchInput').value.trim();
          state.name = name;
          const params = new URLSearchParams({
            name,
            page: '0',
            size: '100',
            sort: `name,${state.sort}`
          });
          if (state.departmentId) params.set('departmentId', state.departmentId);

          const firstPage = await api(`/api/employees/search?${params}`);
          const employees = [...firstPage.content];
          for (let pageNumber = 1; pageNumber < firstPage.totalPages; pageNumber += 1) {
            params.set('page', String(pageNumber));
            const page = await api(`/api/employees/search?${params}`);
            employees.push(...page.content);
          }

          if (!employees.length) {
            notice(byId('appNotice'), 'There are no employees matching these filters to export.', 'info');
            return;
          }

          const columns = [
            ['Name', 'name'],
            ['Email', 'email'],
            ['Department', 'departmentName'],
            ['Created by', 'createdBy'],
            ['Created date', 'createdDate'],
            ['Last modified by', 'lastModifiedBy'],
            ['Last modified date', 'lastModifiedDate']
          ];
          const csv = [
            columns.map(([heading]) => csvCell(heading)).join(','),
            ...employees.map((employee) => columns
              .map(([, key]) => csvCell(employee[key]))
              .join(','))
          ].join('\r\n');
          const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
          const downloadUrl = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = downloadUrl;
          link.download = `employees-${new Date().toISOString().slice(0, 10)}.csv`;
          document.body.appendChild(link);
          link.click();
          link.remove();
          window.setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000);
          notice(byId('appNotice'), `Exported ${employees.length} employee${employees.length === 1 ? '' : 's'} to CSV.`, 'success');
        } catch (error) {
          notice(byId('appNotice'), error.message, 'error');
        } finally {
          button.disabled = false;
          button.textContent = '↓ Export CSV';
        }
      }

      byId('loginForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        clearNotice(byId('loginNotice'));
        const button = event.currentTarget.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = 'Signing in…';
        try { await login(byId('emailInput').value.trim(), byId('passwordInput').value); }
        catch (error) { notice(byId('loginNotice'), error.message, 'error'); }
        finally { button.disabled = false; button.textContent = 'Sign in to dashboard'; }
      });

      byId('logoutButton').addEventListener('click', signOut);
      byId('addEmployeeButton').addEventListener('click', () => openEmployeeModal());
      byId('addDepartmentButton').addEventListener('click', openDepartmentModal);
      byId('exportCsvButton').addEventListener('click', exportEmployeesCsv);
      byId('refreshButton').addEventListener('click', () => { loadEmployees(); loadDepartments(); });
      byId('previousButton').addEventListener('click', () => { if (state.page > 0) { state.page -= 1; loadEmployees(); } });
      byId('nextButton').addEventListener('click', () => { if (state.page + 1 < state.totalPages) { state.page += 1; loadEmployees(); } });
      byId('searchInput').addEventListener('input', (event) => {
        clearTimeout(state.searchTimer);
        state.searchTimer = setTimeout(() => { state.name = event.target.value.trim(); state.page = 0; loadEmployees(); }, 250);
      });
      byId('sortSelect').addEventListener('change', (event) => { state.sort = event.target.value; state.page = 0; loadEmployees(); });
      byId('sizeSelect').addEventListener('change', (event) => { state.size = Number(event.target.value); state.page = 0; loadEmployees(); });
      byId('departmentFilter').addEventListener('change', (event) => {
        state.departmentId = event.target.value;
        state.page = 0;
        loadEmployees();
      });
      byId('closeModalButton').addEventListener('click', closeModal);
      document.querySelectorAll('.cancel-modal').forEach((button) => button.addEventListener('click', closeModal));
      modalBackdrop.addEventListener('click', (event) => { if (event.target === modalBackdrop) closeModal(); });
      document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeModal(); });

      byId('employeeForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const id = byId('employeeId').value;
        const body = {
          name: byId('employeeName').value.trim(),
          email: byId('employeeEmail').value.trim(),
          departmentId: Number(byId('employeeDepartment').value)
        };
        try {
          await api(id ? `/api/employees/${id}` : '/api/employees', {
            method: id ? 'PUT' : 'POST', body: JSON.stringify(body)
          });
          closeModal();
          state.page = 0;
          notice(byId('appNotice'), id ? 'Employee updated.' : 'Employee created.', 'success');
          await loadEmployees();
        } catch (error) { notice(byId('appNotice'), error.message, 'error'); }
      });

      byId('departmentForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const id = byId('departmentId').value;
        try {
          await api(id ? `/api/departments/${id}` : '/api/departments', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify({ name: byId('departmentName').value.trim() })
          });
          closeModal();
          notice(byId('appNotice'), id ? 'Department renamed.' : 'Department created.', 'success');
          await Promise.all([loadDepartments(), loadEmployees()]);
        } catch (error) { notice(byId('appNotice'), error.message, 'error'); }
      });

      state.token = sessionStorage.getItem('peopleDeskToken') || '';
      state.role = sessionStorage.getItem('peopleDeskRole') || '';
      state.email = sessionStorage.getItem('peopleDeskEmail') || '';
      if (state.token && state.role && state.email) showDashboard();
    })();
