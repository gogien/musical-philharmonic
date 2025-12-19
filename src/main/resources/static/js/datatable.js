// Reusable Data Table Component
class DataTable {
    constructor(app, config) {
        this.app = app;
        this.config = config;
        this.currentPage = 0;
        this.pageSize = 20;
        this.currentSort = config.columns.find(c => c.sortable)?.key + ',asc' || 'id,asc';
        this.filters = {};
    }

    async render(container) {
        // Store instance globally so onclick handlers can access it
        window.dataTable = this;
        
        // Check if user has permission to create (only ADMIN for most entities)
        const canCreate = this.config.onCreate && 
                         (this.app.currentRole === 'ADMIN' || 
                          (this.app.currentRole === 'CASHIER' && this.config.title === 'Билеты'));
        
        container.innerHTML = `
            <div class="table-wrapper">
                <div class="table-header">
                    <h2>${this.config.title}</h2>
                    ${canCreate ? '<button class="btn-primary" onclick="dataTable.create()">Создать</button>' : ''}
                </div>
                <div class="table-filters">
                    ${this.renderFilters()}
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                ${this.config.columns.map(col => `
                                    <th>
                                        ${col.label}
                                        ${col.sortable ? `<span class="sort-icon" data-sort="${col.key}">⇅</span>` : ''}
                                    </th>
                                `).join('')}
                                <th>Действия</th>
                            </tr>
                        </thead>
                        <tbody id="table-body">
                            <tr><td colspan="${this.config.columns.length + 1}">Загрузка...</td></tr>
                        </tbody>
                    </table>
                </div>
                <div class="table-pagination">
                    <button class="btn" onclick="dataTable.prevPage()" id="prev-btn">Назад</button>
                    <span id="page-info">Страница 1</span>
                    <button class="btn" onclick="dataTable.nextPage()" id="next-btn">Вперед</button>
                </div>
            </div>
        `;

        // Store reference for callbacks
        window.dataTable = this;

        await this.loadData();
        this.attachSortListeners();
        this.attachActionListeners();
    }

    attachActionListeners() {
        // Edit buttons
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.onclick = () => {
                const id = btn.getAttribute('data-id');
                if (id && id !== 'undefined' && id !== 'null') {
                    this.editItem(id);
                } else {
                    console.error('Invalid ID for edit:', id);
                    alert('Ошибка: не удалось определить ID записи');
                }
            };
        });

        // Delete buttons
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.onclick = () => {
                const id = btn.getAttribute('data-id');
                if (id && id !== 'undefined' && id !== 'null') {
                    this.deleteItem(id);
                } else {
                    console.error('Invalid ID for delete:', id);
                    alert('Ошибка: не удалось определить ID записи');
                }
            };
        });

        // Return buttons (for cashiers)
        document.querySelectorAll('.return-btn').forEach(btn => {
            btn.onclick = () => {
                const id = btn.getAttribute('data-id');
                if (id && id !== 'undefined' && id !== 'null') {
                    this.returnTicket(id);
                } else {
                    console.error('Invalid ID for return:', id);
                    alert('Ошибка: не удалось определить ID записи');
                }
            };
        });
    }

    renderFilters() {
        if (!this.config.searchFields || this.config.searchFields.length === 0) {
            return '<input type="text" id="table-search" placeholder="Поиск..." onkeyup="dataTable.handleSearch()">';
        }
        return `
            <div class="filter-group">
                ${this.config.searchFields.map(field => {
                    if (field === 'date' || field === 'startDate' || field === 'endDate') {
                        return `<input type="date" id="filter-${field}" placeholder="${field}" onchange="dataTable.handleFilter()">`;
                    }
                    if (field === 'role' || field === 'status') {
                        return `<select id="filter-${field}" onchange="dataTable.handleFilter()">
                            <option value="">Все</option>
                            <option value="ADMIN">ADMIN</option>
                            <option value="CASHIER">CASHIER</option>
                            <option value="CUSTOMER">CUSTOMER</option>
                        </select>`;
                    }
                    return `<input type="text" id="filter-${field}" placeholder="${field}" onkeyup="dataTable.handleFilter()">`;
                }).join('')}
            </div>
        `;
    }

    attachSortListeners() {
        document.querySelectorAll('.sort-icon').forEach(icon => {
            icon.onclick = () => {
                const key = icon.dataset.sort;
                const [currentKey, currentDir] = this.currentSort.split(',');
                const newDir = (currentKey === key && currentDir === 'asc') ? 'desc' : 'asc';
                this.currentSort = `${key},${newDir}`;
                this.loadData();
            };
        });
    }

    handleSearch() {
        const search = document.getElementById('table-search')?.value || '';
        this.filters.search = search;
        this.currentPage = 0;
        this.loadData();
    }

    handleFilter() {
        this.config.searchFields.forEach(field => {
            const input = document.getElementById(`filter-${field}`);
            if (input) {
                const value = input.value;
                if (value) {
                    this.filters[field] = value;
                } else {
                    delete this.filters[field];
                }
            }
        });
        this.currentPage = 0;
        this.loadData();
    }

    async loadData() {
        const request = {
            page: this.currentPage,
            size: this.pageSize,
            sort: this.currentSort,
            ...this.filters
        };

        try {
            const data = await this.app.apiCall(this.config.endpoint, {
                method: 'POST',
                body: JSON.stringify(request)
            });

            this.renderTableBody(data.content);
            this.updatePagination(data);
            this.attachActionListeners();
        } catch (err) {
            document.getElementById('table-body').innerHTML = 
                `<tr><td colspan="${this.config.columns.length + 1}">Ошибка: ${err.message}</td></tr>`;
        }
    }

    renderTableBody(items) {
        const tbody = document.getElementById('table-body');
        if (!items || items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="${this.config.columns.length + 1}">Нет данных</td></tr>`;
            return;
        }

        tbody.innerHTML = items.map(item => {
            // Ensure we have a valid ID - try different possible ID fields
            const itemId = item.id || item.userId || item.ticketId || item.concertId || item.hallId || item.performerId;
            if (!itemId) {
                console.error('Item missing ID:', item);
                console.error('Available keys:', Object.keys(item));
            }
            // Convert UUID to string if it's an object
            const idString = itemId ? String(itemId) : null;
            return `
            <tr>
                ${this.config.columns.map(col => {
                    let value = item[col.key];
                    if (col.formatter) {
                        value = col.formatter(value, item);
                    }
                    return `<td>${value || '-'}</td>`;
                }).join('')}
                <td class="actions">
                    ${this.config.onEdit && this.app.currentRole === 'ADMIN' && idString ? `<button class="btn-small edit-btn" data-id="${idString}">Изменить</button>` : ''}
                    ${this.config.onDelete && this.app.currentRole === 'ADMIN' && idString ? `<button class="btn-small btn-danger delete-btn" data-id="${idString}">Удалить</button>` : ''}
                    ${this.app.currentRole === 'CASHIER' && this.config.title === 'Билеты' && idString ? `<button class="btn-small return-btn" data-id="${idString}">Вернуть</button>` : ''}
                </td>
            </tr>
        `;
        }).join('');
    }

    updatePagination(data) {
        document.getElementById('page-info').textContent = 
            `Страница ${data.number + 1} из ${data.totalPages} (Всего: ${data.totalElements})`;
        document.getElementById('prev-btn').disabled = !data.first;
        document.getElementById('next-btn').disabled = !data.last;
    }

    prevPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.loadData();
        }
    }

    nextPage() {
        this.currentPage++;
        this.loadData();
    }

    create() {
        if (this.config.onCreate) {
            this.config.onCreate();
        }
    }

    async editItem(id) {
        if (!this.config.onEdit) return;
        
        // Try to fetch full item data
        try {
            // Determine endpoint from config
            const endpoint = this.config.endpoint.replace('/search', '');
            const item = await this.app.apiCall(`${endpoint}/${id}`, { method: 'GET' });
            this.config.onEdit(item);
        } catch (err) {
            // Fallback: extract from table
            const tbody = document.getElementById('table-body');
            const row = Array.from(tbody.querySelectorAll('tr')).find(r => {
                const firstCell = r.querySelector('td');
                return firstCell && firstCell.textContent.trim() === String(id);
            });
            if (!row) {
                alert('Не удалось загрузить данные');
                return;
            }
            
            const cells = row.querySelectorAll('td');
            const item = { id: id };
            this.config.columns.forEach((col, idx) => {
                item[col.key] = cells[idx].textContent.trim();
            });
            this.config.onEdit(item);
        }
    }

    async deleteItem(id) {
        // Only ADMIN can delete
        if (this.app.currentRole !== 'ADMIN') {
            alert('Недостаточно прав для удаления');
            return;
        }
        
        if (!id || id === 'undefined' || id === 'null') {
            alert('Ошибка: не удалось определить ID записи');
            console.error('Invalid ID for deletion:', id);
            return;
        }
        
        if (this.config.onDelete && confirm('Удалить запись?')) {
            try {
                // Pass as object to match callback signature: (item) => this.deleteUser(item.id)
                await this.config.onDelete({ id: id });
                this.loadData();
            } catch (err) {
                alert('Ошибка удаления: ' + err.message);
            }
        }
    }

    async returnTicket(id) {
        if (this.config.role !== 'CASHIER') return;
        
        const reason = prompt('Причина возврата:', 'customer request');
        if (!reason) return;
        
        try {
            await this.app.apiCall(`/api/tickets/${id}/return`, {
                method: 'POST',
                body: JSON.stringify({ reason: reason })
            });
            alert('Билет возвращен');
            this.loadData();
        } catch (err) {
            alert('Ошибка: ' + err.message);
        }
    }
}

