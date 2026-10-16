(() => {
      const API_BASE = 'http://localhost:8080';
      const state = { token: '', role: '', email: '', authMode: 'login', page: 0, size: 10, sort: 'asc', name: '', departmentId: '', totalPages: 0, departments: [], searchTimer: null };
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

      function startSession(result, email) {
        state.token = result.accessToken;
        state.role = result.role;
        state.email = email;
        state.page = 0;
        sessionStorage.setItem('peopleDeskToken', state.token);
        sessionStorage.setItem('peopleDeskRole', state.role);
        sessionStorage.setItem('peopleDeskEmail', state.email);
        showDashboard();
      }

      async function login(email, password) {
        const result = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
        startSession(result, email);
      }

      async function register(email, password) {
        const result = await api('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) });
        startSession(result, email);
      }

      function setAuthMode(mode) {
        state.authMode = mode;
        const isRegistering = mode === 'register';
        byId('authEyebrow').textContent = isRegistering ? 'Join your team' : 'Welcome back';
        byId('authTitle').textContent = isRegistering ? 'Create account' : 'Sign in';
        byId('authDescription').textContent = isRegistering
          ? 'Create a read-only account to browse the employee directory. Passwords must be at least 12 characters.'
          : 'Sign in with your Employee Management System account to view your team directory.';
        byId('passwordInput').autocomplete = isRegistering ? 'new-password' : 'current-password';
        byId('passwordInput').minLength = isRegistering ? 12 : 1;
        byId('passwordInput').maxLength = isRegistering ? 72 : 256;
        byId('authSubmitButton').textContent = isRegistering ? 'Create read-only account' : 'Sign in to dashboard';
        byId('authSwitchPrompt').textContent = isRegistering ? 'Already registered?' : 'New here?';
        byId('authSwitchButton').textContent = isRegistering ? 'Sign in' : 'Create an account';
        clearNotice(byId('loginNotice'));
      }

      function signOut() {
        state.token = '';
        state.role = '';
        state.email = '';
        setAuthMode('login');
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
        await Promise.all([loadDepartments(), loadArchive()]);
        await Promise.all([loadEmployees(), loadOverview()]);
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
              <td><button class="employee-name employee-profile-link" data-id="${employee.id}" type="button">${escapeHtml(employee.name)}</button><div class="employee-email">${escapeHtml(employee.email)}</div></td>
              <td><span class="dept-chip">${escapeHtml(employee.departmentName)}</span></td>
              <td class="muted">${escapeHtml(employee.createdBy || '—')}</td>
              ${state.role === 'ADMIN' ? `<td><div class="row-actions"><button class="icon-button edit-employee" data-id="${employee.id}" type="button" title="Edit" aria-label="Edit ${escapeHtml(employee.name)}">✎</button><button class="button button-secondary archive-employee" data-id="${employee.id}" type="button" title="Move this employee to the archive">Archive</button><button class="button button-danger delete-employee" data-id="${employee.id}" type="button">Delete</button></div></td>` : ''}
            </tr>`).join('');
          byId('emptyState').classList.toggle('hidden', page.content.length > 0);
          rows.classList.toggle('hidden', page.content.length === 0);
          rows.querySelectorAll('.edit-employee').forEach((button) => button.addEventListener('click', () => {
            const employee = page.content.find((item) => String(item.id) === button.dataset.id);
            if (employee) openEmployeeModal(employee);
          }));
          rows.querySelectorAll('.delete-employee').forEach((button) => button.addEventListener('click', () => deleteEmployee(button.dataset.id)));
          rows.querySelectorAll('.archive-employee').forEach((button) => button.addEventListener('click', () => {
            const employee = page.content.find((item) => String(item.id) === button.dataset.id);
            if (employee) archiveEmployee(employee);
          }));
          rows.querySelectorAll('.employee-profile-link').forEach((button) => button.addEventListener('click', () => {
            openEmployeeProfile(button.dataset.id);
          }));
        } catch (error) {
          notice(byId('appNotice'), error.message, 'error');
        }
      }

      async function loadOverview() {
        byId('overviewDepartmentCount').textContent = String(state.departments.length);
        try {
          const recentParams = new URLSearchParams({ page: '0', size: '5', sort: 'createdDate,desc' });
          const allParams = new URLSearchParams({ page: '0', size: '100', sort: 'name,asc' });
          const [recentPage, firstEmployeePage] = await Promise.all([
            api(`/api/employees/search?${recentParams}`),
            api(`/api/employees/search?${allParams}`)
          ]);
          const employees = [...firstEmployeePage.content];
          for (let pageNumber = 1; pageNumber < firstEmployeePage.totalPages; pageNumber += 1) {
            allParams.set('page', String(pageNumber));
            const page = await api(`/api/employees/search?${allParams}`);
            employees.push(...page.content);
          }
          byId('overviewEmployeeCount').textContent = String(firstEmployeePage.totalElements);
          renderDepartmentBreakdown(employees);
          const recentEmployees = recentPage.content;
          byId('recentEmployeesList').innerHTML = recentEmployees.length
            ? recentEmployees.map((employee) => `<li><button class="recent-employee-link" data-id="${employee.id}" type="button">${escapeHtml(employee.name)}</button><span class="recent-employee-meta">${escapeHtml(employee.departmentName)} · ${escapeHtml(formatDateTime(employee.createdDate))}</span></li>`).join('')
            : '<li class="muted">No employees have been added yet.</li>';
          byId('recentEmployeesList').querySelectorAll('.recent-employee-link').forEach((button) => button.addEventListener('click', () => {
            openEmployeeProfile(button.dataset.id);
          }));
        } catch (error) {
          byId('overviewEmployeeCount').textContent = '—';
          byId('departmentBreakdownList').innerHTML = `<li class="muted">${escapeHtml(error.message)}</li>`;
          byId('recentEmployeesList').innerHTML = `<li class="muted">${escapeHtml(error.message)}</li>`;
        }
      }

      function renderDepartmentBreakdown(employees) {
        const counts = new Map(state.departments.map((department) => [String(department.id), {
          id: department.id, name: department.name, count: 0
        }]));
        employees.forEach((employee) => {
          const department = counts.get(String(employee.departmentId));
          if (department) department.count += 1;
        });
        const departments = [...counts.values()].sort((left, right) =>
          right.count - left.count || left.name.localeCompare(right.name));
        const maxCount = Math.max(1, ...departments.map((department) => department.count));
        byId('departmentBreakdownList').innerHTML = departments.length
          ? departments.map((department) => `
              <li>
                <button class="department-breakdown-button" data-id="${department.id}" type="button" aria-label="Show ${escapeHtml(department.count)} employees in ${escapeHtml(department.name)}">
                  <span class="department-breakdown-heading"><span>${escapeHtml(department.name)}</span><strong>${department.count}</strong></span>
                  <span class="department-bar-track" aria-hidden="true"><span class="department-bar" style="width:${department.count / maxCount * 100}%"></span></span>
                </button>
              </li>`).join('')
          : '<li class="muted">No departments yet.</li>';
        byId('departmentBreakdownList').querySelectorAll('.department-breakdown-button').forEach((button) => {
          button.addEventListener('click', () => {
            state.departmentId = button.dataset.id;
            state.page = 0;
            byId('departmentFilter').value = state.departmentId;
            loadEmployees();
            byId('employeeRows').closest('.card').scrollIntoView({ behavior: 'smooth', block: 'start' });
          });
        });
      }

      async function loadArchive() {
        if (state.role !== 'ADMIN') return;
        try {
          const archivedEmployees = await api('/api/archive/employees');
          const normalizedEmployees = archivedEmployees.map((employee) => Object.fromEntries(
            Object.entries(employee).map(([key, value]) => [key.toLowerCase(), value])
          ));
          byId('archiveSummary').textContent = `${archivedEmployees.length} employee${archivedEmployees.length === 1 ? '' : 's'} archived`;
          byId('archiveRows').innerHTML = normalizedEmployees.map((employee) => {
            const archivedAt = employee.archivedat || employee.archived_at;
            const formattedDate = archivedAt ? new Date(archivedAt).toLocaleString() : '—';
            return `<tr><td><div class="employee-name">${escapeHtml(employee.name)}</div><div class="employee-email">${escapeHtml(employee.email)}</div></td><td>${escapeHtml(employee.departmentname || employee.department_name)}</td><td class="muted">${escapeHtml(formattedDate)}</td><td><button class="button button-secondary restore-employee" data-id="${employee.id}" type="button">Restore</button></td></tr>`;
          }).join('');
          byId('archiveRows').querySelectorAll('.restore-employee').forEach((button) => button.addEventListener('click', () => {
            const employee = normalizedEmployees.find((item) => String(item.id) === button.dataset.id);
            if (employee) restoreEmployee(employee);
          }));
          byId('archiveEmptyState').classList.toggle('hidden', archivedEmployees.length > 0);
          byId('archiveRows').classList.toggle('hidden', archivedEmployees.length === 0);
        } catch (error) {
          byId('archiveSummary').textContent = error.message;
        }
      }

      async function archiveEmployee(employee) {
        if (!window.confirm(`Move ${employee.name} to the archive? They will leave the active directory and can be restored later.`)) return;
        try {
          await api(`/api/archive/employees/${employee.id}`, { method: 'POST' });
          if (byId('employeeRows').children.length === 1 && state.page > 0) state.page -= 1;
          await loadArchive();
          await loadEmployees();
          await loadOverview();
          notice(byId('appNotice'), `${employee.name} moved to the archive.`, 'success');
        } catch (error) {
          notice(byId('appNotice'), error.message, 'error');
        }
      }

      async function restoreEmployee(employee) {
        if (!window.confirm(`Restore ${employee.name} to the active employee directory?`)) return;
        try {
          await api(`/api/archive/employees/${employee.id}/restore`, { method: 'POST' });
          state.page = 0;
          await loadArchive();
          await loadEmployees();
          await loadOverview();
          notice(byId('appNotice'), `${employee.name} restored to the active directory.`, 'success');
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

      function formatDateTime(value) {
        if (!value) return '—';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
      }

      async function openEmployeeProfile(id) {
        byId('employeeForm').classList.add('hidden');
        byId('departmentForm').classList.add('hidden');
        byId('employeeProfile').classList.remove('hidden');
        byId('modalTitle').textContent = 'Employee profile';
        clearNotice(byId('profileNotice'));
        ['profileName', 'profileEmail', 'profileJobTitle', 'profilePhoneNumber', 'profileHireDate', 'profileDepartment', 'profileCreatedBy',
          'profileCreatedDate', 'profileModifiedBy', 'profileModifiedDate']
          .forEach((fieldId) => { byId(fieldId).textContent = 'Loading…'; });
        byId('employeeHistoryList').innerHTML = '<li class="muted">Loading history…</li>';
        modalBackdrop.classList.add('open');
        byId('closeModalButton').focus();

        try {
          const [employee, history] = await Promise.all([
            api(`/api/employees/${id}`),
            api(`/api/employees/${id}/history`)
          ]);
          byId('profileName').textContent = employee.name || '—';
          byId('profileEmail').textContent = employee.email || '—';
          byId('profileJobTitle').textContent = employee.jobTitle || '—';
          byId('profilePhoneNumber').textContent = employee.phoneNumber || '—';
          byId('profileHireDate').textContent = employee.hireDate
            ? new Date(`${employee.hireDate}T00:00:00`).toLocaleDateString()
            : '—';
          byId('profileDepartment').textContent = employee.departmentName || '—';
          byId('profileCreatedBy').textContent = employee.createdBy || '—';
          byId('profileCreatedDate').textContent = formatDateTime(employee.createdDate);
          byId('profileModifiedBy').textContent = employee.lastModifiedBy || '—';
          byId('profileModifiedDate').textContent = formatDateTime(employee.lastModifiedDate);
          renderEmployeeHistory(history);
        } catch (error) {
          notice(byId('profileNotice'), error.message, 'error');
          byId('employeeHistoryList').innerHTML = `<li class="muted">${escapeHtml(error.message)}</li>`;
        }
      }

      function renderEmployeeHistory(events) {
        const actionLabels = {
          BASELINE: 'Tracking started', CREATED: 'Created', UPDATED: 'Updated', ARCHIVED: 'Archived',
          RESTORED: 'Restored', DELETED: 'Deleted'
        };
        byId('employeeHistoryList').innerHTML = events.length
          ? events.map((event) => `
              <li class="employee-history-event">
                <div class="employee-history-heading"><strong>${escapeHtml(actionLabels[event.action] || event.action)}</strong><time>${escapeHtml(formatDateTime(event.changedAt))}</time></div>
                <p>${escapeHtml(event.summary)}</p>
                <div class="employee-history-actor">By ${escapeHtml(event.changedBy)}</div>
              </li>`).join('')
          : '<li class="muted">No change history has been recorded for this employee yet.</li>';
      }

      function openEmployeeModal(employee = null) {
        if (!state.departments.length) {
          notice(byId('appNotice'), 'Create a department before adding an employee.', 'info');
          return;
        }
        byId('departmentForm').classList.add('hidden');
        byId('employeeProfile').classList.add('hidden');
        byId('employeeForm').classList.remove('hidden');
        byId('modalTitle').textContent = employee ? 'Edit employee' : 'Add employee';
        byId('employeeId').value = employee?.id || '';
        byId('employeeName').value = employee?.name || '';
        byId('employeeEmail').value = employee?.email || '';
        byId('employeeJobTitle').value = employee?.jobTitle || '';
        byId('employeePhoneNumber').value = employee?.phoneNumber || '';
        byId('employeeHireDate').value = employee?.hireDate || '';
        byId('employeeDepartment').innerHTML = state.departments.map((department) => `<option value="${department.id}">${escapeHtml(department.name)}</option>`).join('');
        if (employee) byId('employeeDepartment').value = employee.departmentId;
        updateJobTitleSuggestions();
        modalBackdrop.classList.add('open');
        byId('employeeName').focus();
      }

      function updateJobTitleSuggestions() {
        const department = state.departments.find((item) => String(item.id) === byId('employeeDepartment').value);
        if (!department) {
          byId('jobTitleSuggestions').replaceChildren();
          return;
        }
        const name = department.name.trim();
        const normalizedName = name.toLowerCase();
        let suggestions;
        if (/quality assurance|\bqa\b/.test(normalizedName)) {
          suggestions = ['QA Engineer', 'QA Analyst', 'Test Engineer', 'QA Lead', 'QA Manager'];
        } else if (/engineering|\bece\b/.test(normalizedName)) {
          suggestions = ['Software Engineer', 'Electrical Engineer', 'Systems Engineer', 'Engineering Lead', 'Engineering Manager'];
        } else {
          suggestions = [`${name} Specialist`, `${name} Analyst`, `${name} Lead`, `${name} Manager`];
        }
        byId('jobTitleSuggestions').replaceChildren(...suggestions.map((title) => {
          const option = document.createElement('option');
          option.value = title;
          return option;
        }));
      }

      function openDepartmentModal(department = null) {
        byId('employeeForm').classList.add('hidden');
        byId('employeeProfile').classList.add('hidden');
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
          await loadOverview();
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
          await loadDepartments();
          await Promise.all([loadEmployees(), loadOverview()]);
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
            ['Job Title', 'jobTitle'],
            ['Phone Number', 'phoneNumber'],
            ['Hire Date', 'hireDate'],
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

      function parseCsv(text) {
        const rows = [];
        let row = [];
        let field = '';
        let inQuotes = false;
        let afterQuote = false;
        const source = text.replace(/^\uFEFF/, '');

        for (let index = 0; index < source.length; index += 1) {
          const char = source[index];
          if (inQuotes) {
            if (char === '"') {
              if (source[index + 1] === '"') {
                field += '"';
                index += 1;
              } else {
                inQuotes = false;
                afterQuote = true;
              }
            } else {
              field += char;
            }
            continue;
          }

          if (afterQuote && char !== ',' && char !== '\r' && char !== '\n' && char !== ' ') {
            throw new Error('The CSV has unexpected text after a quoted field.');
          }
          if (char === '"' && field.length === 0 && !afterQuote) {
            inQuotes = true;
          } else if (char === ',') {
            row.push(field);
            field = '';
            afterQuote = false;
          } else if (char === '\r' || char === '\n') {
            row.push(field);
            rows.push(row);
            row = [];
            field = '';
            afterQuote = false;
            if (char === '\r' && source[index + 1] === '\n') index += 1;
          } else if (afterQuote && char === ' ') {
            // Ignore spaces between a closing quote and its delimiter.
          } else {
            field += char;
            afterQuote = false;
          }
        }

        if (inQuotes) throw new Error('The CSV contains an unclosed quoted field.');
        if (field.length || row.length || afterQuote) {
          row.push(field);
          rows.push(row);
        }
        return rows;
      }

      function parseEmployeeCsv(text) {
        const rows = parseCsv(text);
        if (!rows.length) throw new Error('The CSV file is empty.');
        const headers = rows[0].map((header) => header.trim().toLowerCase());
        const nameColumn = headers.indexOf('name');
        const emailColumn = headers.indexOf('email');
        const jobTitleColumn = headers.indexOf('job title');
        const phoneNumberColumn = headers.indexOf('phone number');
        const hireDateColumn = headers.indexOf('hire date');
        const departmentColumn = headers.indexOf('department');
        const departmentIdColumn = headers.indexOf('departmentid');
        if (nameColumn < 0 || emailColumn < 0 || (departmentColumn < 0 && departmentIdColumn < 0)) {
          throw new Error('CSV needs Name, Email, and Department columns (or DepartmentId).');
        }

        const departmentsByName = new Map(state.departments.map((department) => [
          department.name.trim().toLowerCase(), department.id
        ]));
        const departmentsById = new Map(state.departments.map((department) => [
          String(department.id), department.id
        ]));
        const imported = [];
        const errors = [];
        const seenEmails = new Set();

        rows.slice(1).forEach((values, index) => {
          const lineNumber = index + 2;
          if (values.every((value) => !value.trim())) return;
          const name = (values[nameColumn] || '').trim();
          const email = (values[emailColumn] || '').trim();
          const jobTitle = jobTitleColumn < 0 ? '' : (values[jobTitleColumn] || '').trim();
          const phoneNumber = phoneNumberColumn < 0 ? '' : (values[phoneNumberColumn] || '').trim();
          const hireDate = hireDateColumn < 0 ? '' : (values[hireDateColumn] || '').trim();
          const departmentValue = (values[departmentColumn >= 0 ? departmentColumn : departmentIdColumn] || '').trim();
          const problems = [];

          if (!name) problems.push('name is required');
          else if (name.length > 100) problems.push('name must be at most 100 characters');
          if (!email) problems.push('email is required');
          else if (email.length > 255 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) problems.push('email is invalid');
          if (jobTitle.length > 120) problems.push('job title must be at most 120 characters');
          if (phoneNumber && (phoneNumber.length > 30 || !/^[+0-9().\-\s]{7,30}$/.test(phoneNumber))) problems.push('phone number is invalid');
          if (hireDate && (!/^\d{4}-\d{2}-\d{2}$/.test(hireDate)
              || Number.isNaN(new Date(`${hireDate}T00:00:00`).getTime())
              || new Date(`${hireDate}T00:00:00`).toISOString().slice(0, 10) !== hireDate)) {
            problems.push('hire date must use YYYY-MM-DD');
          }

          let departmentId;
          if (!departmentValue) {
            problems.push('department is required');
          } else if (departmentColumn >= 0) {
            departmentId = departmentsByName.get(departmentValue.toLowerCase());
            if (!departmentId) problems.push(`department "${departmentValue}" was not found`);
          } else {
            departmentId = departmentsById.get(departmentValue);
            if (!departmentId) problems.push(`department id "${departmentValue}" was not found`);
          }

          const normalizedEmail = email.toLowerCase();
          if (email && seenEmails.has(normalizedEmail)) problems.push('email appears more than once in the file');
          seenEmails.add(normalizedEmail);

          if (problems.length) errors.push(`Line ${lineNumber}: ${problems.join(', ')}`);
          else imported.push({ name, email, departmentId, jobTitle: jobTitle || null, phoneNumber: phoneNumber || null, hireDate: hireDate || null });
        });

        if (!imported.length && !errors.length) throw new Error('The CSV has a header but no employee rows.');
        if (errors.length) {
          const displayedErrors = errors.slice(0, 8).join('\n');
          const remaining = errors.length > 8 ? `\nAnd ${errors.length - 8} more error(s).` : '';
          throw new Error(`${errors.length} row(s) need correction:\n${displayedErrors}${remaining}`);
        }
        if (imported.length > 1000) throw new Error('Import is limited to 1,000 employees per file.');
        return imported;
      }

      async function importEmployeesCsv(file) {
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) {
          notice(byId('appNotice'), 'CSV file must be 5 MB or smaller.', 'error');
          return;
        }

        try {
          const employees = parseEmployeeCsv(await file.text());
          const departmentNames = new Map(state.departments.map((department) => [
            department.id, department.name
          ]));
          const preview = employees.slice(0, 5)
            .map((employee) => `• ${employee.name} — ${departmentNames.get(employee.departmentId)}`)
            .join('\n');
          const more = employees.length > 5 ? `\n…and ${employees.length - 5} more` : '';
          if (!window.confirm(`Import ${employees.length} employee(s)?\n\n${preview}${more}`)) return;

          const result = await api('/api/employees/batch', {
            method: 'POST',
            body: JSON.stringify(employees)
          });
          state.page = 0;
          await Promise.all([loadEmployees(), loadOverview()]);
          notice(byId('appNotice'), `Imported ${result.inserted} employee${result.inserted === 1 ? '' : 's'}.`, 'success');
        } catch (error) {
          notice(byId('appNotice'), error.message, 'error');
        }
      }

      byId('loginForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        clearNotice(byId('loginNotice'));
        const button = event.currentTarget.querySelector('button[type="submit"]');
        button.disabled = true;
        const isRegistering = state.authMode === 'register';
        button.textContent = isRegistering ? 'Creating account…' : 'Signing in…';
        try {
          const email = byId('emailInput').value.trim();
          const password = byId('passwordInput').value;
          if (isRegistering) await register(email, password);
          else await login(email, password);
        }
        catch (error) { notice(byId('loginNotice'), error.message, 'error'); }
        finally {
          button.disabled = false;
          button.textContent = state.authMode === 'register' ? 'Create read-only account' : 'Sign in to dashboard';
        }
      });

      byId('authSwitchButton').addEventListener('click', () => {
        setAuthMode(state.authMode === 'login' ? 'register' : 'login');
      });
      byId('logoutButton').addEventListener('click', signOut);
      byId('addEmployeeButton').addEventListener('click', () => openEmployeeModal());
      byId('addDepartmentButton').addEventListener('click', openDepartmentModal);
      byId('exportCsvButton').addEventListener('click', exportEmployeesCsv);
      byId('importCsvButton').addEventListener('click', () => byId('employeeCsvInput').click());
      byId('employeeCsvInput').addEventListener('change', async (event) => {
        try { await importEmployeesCsv(event.target.files[0]); }
        finally { event.target.value = ''; }
      });
      byId('refreshButton').addEventListener('click', async () => {
        await Promise.all([loadDepartments(), loadArchive()]);
        await Promise.all([loadEmployees(), loadOverview()]);
      });
      byId('refreshArchiveButton').addEventListener('click', loadArchive);
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
      byId('employeeDepartment').addEventListener('change', updateJobTitleSuggestions);
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
          departmentId: Number(byId('employeeDepartment').value),
          jobTitle: byId('employeeJobTitle').value.trim() || null,
          phoneNumber: byId('employeePhoneNumber').value.trim() || null,
          hireDate: byId('employeeHireDate').value || null
        };
        try {
          await api(id ? `/api/employees/${id}` : '/api/employees', {
            method: id ? 'PUT' : 'POST', body: JSON.stringify(body)
          });
          closeModal();
          state.page = 0;
          notice(byId('appNotice'), id ? 'Employee updated.' : 'Employee created.', 'success');
          await Promise.all([loadEmployees(), loadOverview()]);
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
          await loadDepartments();
          await Promise.all([loadEmployees(), loadOverview()]);
        } catch (error) { notice(byId('appNotice'), error.message, 'error'); }
      });

      state.token = sessionStorage.getItem('peopleDeskToken') || '';
      state.role = sessionStorage.getItem('peopleDeskRole') || '';
      state.email = sessionStorage.getItem('peopleDeskEmail') || '';
      if (state.token && state.role && state.email) showDashboard();
    })();
