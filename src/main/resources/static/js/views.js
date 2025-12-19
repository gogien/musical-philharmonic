// View Loader and Component Renderers
class ViewLoader {
    constructor(app) {
        this.app = app;
    }

    async load(viewName, container) {
        const role = this.app.currentRole;
        
        // Role-specific view mapping
        let viewFunc = null;
        
        if (role === 'CUSTOMER') {
            if (viewName === 'concerts') viewFunc = () => this.loadCustomerConcerts(container);
            else if (viewName === 'my-tickets') viewFunc = () => this.loadMyTickets(container);
            else if (viewName === 'profile') viewFunc = () => this.loadCustomerProfile(container);
        } else if (role === 'CASHIER') {
            if (viewName === 'sell-ticket') viewFunc = () => this.loadSellTicket(container);
            else if (viewName === 'tickets') viewFunc = () => this.loadCashierTicketsTable(container);
            else if (viewName === 'sales-history') viewFunc = () => this.loadSalesHistory(container);
        } else if (role === 'ADMIN') {
            if (viewName === 'concerts') viewFunc = () => this.loadConcertsTable(container);
            else if (viewName === 'tickets') viewFunc = () => this.loadTicketsTable(container);
            else if (viewName === 'users') viewFunc = () => this.loadUsersTable(container);
            else if (viewName === 'halls') viewFunc = () => this.loadHallsTable(container);
            else if (viewName === 'performers') viewFunc = () => this.loadPerformersTable(container);
        }

        if (viewFunc) {
            await viewFunc();
        } else {
            container.innerHTML = '<p>View not found</p>';
        }
    }

    // Customer Views
    async loadCustomerConcerts(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        try {
            const request = {
                page: 0,
                size: 20,
                sort: 'date,asc'
            };
            const today = new Date().toISOString().split('T')[0];
            const nextYear = new Date();
            nextYear.setFullYear(nextYear.getFullYear() + 1);
            request.startDate = today;
            request.endDate = nextYear.toISOString().split('T')[0];

            const data = await this.app.apiCall('/api/customer/concerts/upcoming', {
                method: 'POST',
                body: JSON.stringify(request)
            });

            container.innerHTML = `
                <div class="concerts-grid">
                    ${data.content.map(concert => `
                        <div class="concert-card">
                            <h3>${concert.title}</h3>
                            <p>${new Date(concert.date).toLocaleDateString('ru-RU')} в ${concert.time}</p>
                            <p>Цена: ${concert.ticketPrice} ₽</p>
                            <div style="display:flex; gap:8px; margin-top:12px;">
                                <button class="btn-primary" onclick="viewLoader.showConcertAvailability(${concert.id})">
                                    Доступность мест
                                </button>
                                <button class="btn" onclick="viewLoader.bookTicket(${concert.id})">
                                    Забронировать
                                </button>
                                <button class="btn" onclick="viewLoader.purchaseTicket(${concert.id})">
                                    Купить
                                </button>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка: ${err.message}</div>`;
            app.showNotification(`Ошибка загрузки концертов: ${err.message}`, 'error');
        }
    }

    async loadMyTickets(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        try {
            const request = { page: 0, size: 20, sort: 'purchaseTimestamp,desc' };
            const data = await this.app.apiCall('/api/customer/tickets/mine', {
                method: 'POST',
                body: JSON.stringify(request)
            });

            container.innerHTML = this.renderTicketsTable(data.content);
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка: ${err.message}</div>`;
            app.showNotification(`Ошибка загрузки концертов: ${err.message}`, 'error');
        }
    }

    // Cashier Views
    async loadSellTicket(container) {
        container.innerHTML = `
            <div class="form-container">
                <h2>Продать билет</h2>
                <form id="sell-ticket-form">
                    <div class="form-group">
                        <label>Концерт ID:</label>
                        <input type="number" id="sell-concert-id" required>
                    </div>
                    <div class="form-group">
                        <label>Номер места:</label>
                        <input type="text" id="sell-seat-number" required>
                    </div>
                    <div class="form-group">
                        <label>Покупатель ID (опционально):</label>
                        <input type="text" id="sell-buyer-id">
                    </div>
                    <div class="form-group">
                        <label>Способ оплаты:</label>
                        <select id="sell-payment-method">
                            <option value="cash">Наличные</option>
                            <option value="card">Карта</option>
                        </select>
                    </div>
                    <button type="submit" class="btn-primary">Продать билет</button>
                </form>
            </div>
        `;

        const sellForm = document.getElementById('sell-ticket-form');
        if (sellForm) {
            sellForm.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'sell-concert-id': [{ required: true, number: true, min: 1 }],
                    'sell-seat-number': [{ required: true, minLength: 1 }]
                };
                
                if (!FormValidator.validateForm(sellForm, rules)) {
                    return;
                }
                
                await this.handleSellTicket();
            };
        }
    }

    async handleSellTicket() {
        try {
            const request = {
                concertId: parseInt(document.getElementById('sell-concert-id').value),
                seatNumber: document.getElementById('sell-seat-number').value,
                buyerId: document.getElementById('sell-buyer-id').value || null,
                paymentMethod: document.getElementById('sell-payment-method').value
            };
            await this.app.apiCall('/api/tickets/sell', {
                method: 'POST',
                body: JSON.stringify(request)
            });
            app.showNotification('Билет успешно продан!', 'success');
            document.getElementById('sell-ticket-form').reset();
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async loadSalesHistory(container) {
        container.innerHTML = `
            <div class="table-container">
                <h2>История продаж</h2>
                <div class="filters">
                    <input type="datetime-local" id="sales-from" placeholder="От">
                    <input type="datetime-local" id="sales-to" placeholder="До">
                    <button class="btn-primary" onclick="viewLoader.searchSalesHistory()">Поиск</button>
                </div>
                <div id="sales-results"></div>
            </div>
        `;
    }

    async searchSalesHistory() {
        const from = document.getElementById('sales-from').value;
        const to = document.getElementById('sales-to').value;
        if (!from || !to) {
            alert('Укажите период');
            return;
        }
        const request = {
            from: new Date(from).toISOString(),
            to: new Date(to).toISOString(),
            page: 0,
            size: 50,
            sort: 'purchaseTimestamp,desc'
        };
        try {
            const data = await this.app.apiCall('/api/tickets/sales', {
                method: 'POST',
                body: JSON.stringify(request)
            });
            document.getElementById('sales-results').innerHTML = this.renderTicketsTable(data.content);
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    // Cashier Views - Tickets (read-only, no create/delete)
    async loadCashierTicketsTable(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        const table = new DataTable(this.app, {
            title: 'Билеты',
            endpoint: '/api/tickets/search',
            columns: [
                { key: 'id', label: 'ID', sortable: true },
                { key: 'concertId', label: 'Концерт ID', sortable: true },
                { key: 'seatNumber', label: 'Место', sortable: true },
                { key: 'status', label: 'Статус', sortable: true },
                { key: 'buyerId', label: 'Покупатель', sortable: true },
                { key: 'paymentMethod', label: 'Оплата', sortable: true }
            ],
            searchFields: ['concertId', 'buyerId', 'status'],
            onEdit: null, // Cashiers can't edit tickets directly
            onDelete: null, // Cashiers can't delete tickets
            onCreate: null, // Cashiers use sell-ticket form instead
            role: 'CASHIER'
        });
        await table.render(container);
    }

    // Admin Views - Data Tables
    async loadConcertsTable(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        const table = new DataTable(this.app, {
            title: 'Концерты',
            endpoint: '/api/concerts/search',
            columns: [
                { key: 'id', label: 'ID', sortable: true },
                { key: 'title', label: 'Название', sortable: true },
                { key: 'date', label: 'Дата', sortable: true, formatter: (v) => new Date(v).toLocaleDateString('ru-RU') },
                { key: 'time', label: 'Время', sortable: true },
                { key: 'ticketPrice', label: 'Цена', sortable: true, formatter: (v) => v + ' ₽' }
            ],
            searchFields: ['title', 'date', 'startDate', 'endDate', 'performerId', 'hallId'],
            onEdit: (item) => this.editConcert(item),
            onDelete: (item) => this.deleteConcert(item.id),
            onCreate: () => this.createConcert(),
            role: 'ADMIN'
        });
        await table.render(container);
    }

    async loadTicketsTable(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        const table = new DataTable(this.app, {
            title: 'Билеты',
            endpoint: '/api/tickets/search',
            columns: [
                { key: 'id', label: 'ID', sortable: true },
                { key: 'concertId', label: 'Концерт ID', sortable: true },
                { key: 'seatNumber', label: 'Место', sortable: true },
                { key: 'status', label: 'Статус', sortable: true },
                { key: 'buyerId', label: 'Покупатель', sortable: true },
                { key: 'paymentMethod', label: 'Оплата', sortable: true }
            ],
            searchFields: ['concertId', 'buyerId', 'status'],
            onEdit: null, // Tickets are managed through sell/return actions
            onDelete: (item) => this.deleteTicket(item.id),
            onCreate: null, // Tickets are created through sell action
            role: 'ADMIN'
        });
        await table.render(container);
    }

    async loadUsersTable(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        const table = new DataTable(this.app, {
            title: 'Пользователи',
            endpoint: '/api/users/search',
            columns: [
                { key: 'id', label: 'ID', sortable: true },
                { key: 'email', label: 'Email', sortable: true },
                { key: 'name', label: 'Имя', sortable: true },
                { key: 'role', label: 'Роль', sortable: true },
                { key: 'phone', label: 'Телефон', sortable: true }
            ],
            searchFields: ['name', 'email', 'role'],
            onEdit: (item) => this.editUser(item),
            onDelete: (item) => this.deleteUser(item.id),
            onCreate: () => this.createUser(),
            role: 'ADMIN'
        });
        await table.render(container);
    }

    async loadHallsTable(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        const table = new DataTable(this.app, {
            title: 'Залы',
            endpoint: '/api/halls/search',
            columns: [
                { key: 'id', label: 'ID', sortable: true },
                { key: 'name', label: 'Название', sortable: true },
                { key: 'capacity', label: 'Вместимость', sortable: true },
                { key: 'location', label: 'Местоположение', sortable: true }
            ],
            searchFields: [],
            onEdit: (item) => this.editHall(item),
            onDelete: (item) => this.deleteHall(item.id),
            onCreate: () => this.createHall(),
            role: 'ADMIN'
        });
        await table.render(container);
    }

    async loadPerformersTable(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        const table = new DataTable(this.app, {
            title: 'Исполнители',
            endpoint: '/api/performers/search',
            columns: [
                { key: 'id', label: 'ID', sortable: true },
                { key: 'name', label: 'Имя', sortable: true }
            ],
            searchFields: ['name'],
            onEdit: (item) => this.editPerformer(item),
            onDelete: (item) => this.deletePerformer(item.id),
            onCreate: () => this.createPerformer(),
            role: 'ADMIN'
        });
        await table.render(container);
    }

    // CRUD Operations
    async createConcert() {
        this.showConcertForm();
    }

    async editConcert(item) {
        this.showConcertForm(item);
    }

    showConcertForm(concert = null) {
        const modal = document.getElementById('modal');
        const modalBody = document.getElementById('modal-body');
        const isEdit = !!concert;
        
        modalBody.innerHTML = `
            <h2>${isEdit ? 'Редактировать' : 'Создать'} концерт</h2>
            <form id="concert-form">
                <div class="form-group">
                    <label>Название:</label>
                    <input type="text" id="form-title" value="${concert?.title || ''}" required>
                </div>
                <div class="form-group">
                    <label>Дата:</label>
                    <input type="date" id="form-date" value="${concert?.date || ''}" required>
                </div>
                <div class="form-group">
                    <label>Время:</label>
                    <input type="time" id="form-time" value="${concert?.time || ''}" required>
                </div>
                <div class="form-group">
                    <label>ID Зала:</label>
                    <input type="number" id="form-hall-id" value="${concert?.hallId || ''}" required>
                </div>
                <div class="form-group">
                    <label>ID Исполнителя:</label>
                    <input type="number" id="form-performer-id" value="${concert?.performerId || ''}" required>
                </div>
                <div class="form-group">
                    <label>Цена билета:</label>
                    <input type="number" step="0.01" id="form-price" value="${concert?.ticketPrice || ''}" required>
                </div>
                <div style="display:flex; gap:12px; margin-top:20px;">
                    <button type="submit" class="btn-primary">Сохранить</button>
                    <button type="button" class="btn" onclick="closeModal()">Отмена</button>
                </div>
            </form>
        `;
        
        modal.style.display = 'block';
        
        const form = document.getElementById('concert-form');
        if (form) {
            form.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'form-title': [{ required: true }],
                    'form-date': [{ required: true, date: true }],
                    'form-time': [{ required: true, time: true }],
                    'form-hall-id': [{ required: true, number: true, min: 1 }],
                    'form-performer-id': [{ required: true, number: true, min: 1 }],
                    'form-price': [{ required: true, number: true, min: 0 }]
                };
                
                if (!FormValidator.validateForm(form, rules)) {
                    return;
                }
                
                await this.saveConcert(concert?.id);
            };
        }
    }

    async saveConcert(id) {
        try {
            const request = {
                title: document.getElementById('form-title').value,
                date: document.getElementById('form-date').value,
                time: document.getElementById('form-time').value,
                hallId: parseInt(document.getElementById('form-hall-id').value),
                performerId: parseInt(document.getElementById('form-performer-id').value),
                ticketPrice: parseFloat(document.getElementById('form-price').value)
            };
            
            const endpoint = id ? `/api/concerts/${id}` : '/api/concerts';
            const method = id ? 'PUT' : 'POST';
            
            await this.app.apiCall(endpoint, {
                method: method,
                body: JSON.stringify(request)
            });
            
            closeModal();
            app.showNotification('Концерт сохранен!', 'success');
            location.reload();
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async deleteConcert(id) {
        if (confirm('Удалить концерт?')) {
            try {
                await this.app.apiCall(`/api/concerts/${id}`, { method: 'DELETE' });
                alert('Концерт удален');
                // Reload current view
                const activeTab = document.querySelector('.tab-btn.active');
                if (activeTab) {
                    await this.load(activeTab.dataset.view, document.getElementById('admin-content'));
                }
            } catch (err) {
                app.showNotification(`Ошибка: ${err.message}`, 'error');
            }
        }
    }

    async createUser() {
        this.showUserForm();
    }

    async editUser(user) {
        this.showUserForm(user);
    }

    showUserForm(user = null) {
        const modal = document.getElementById('modal');
        const modalBody = document.getElementById('modal-body');
        const isEdit = !!user;
        
        modalBody.innerHTML = `
            <h2>${isEdit ? 'Редактировать' : 'Создать'} пользователя</h2>
            <form id="user-form">
                <div class="form-group">
                    <label>Email:</label>
                    <input type="email" id="form-email" value="${user?.email || ''}" ${isEdit ? 'readonly' : 'required'}>
                </div>
                <div class="form-group">
                    <label>Имя:</label>
                    <input type="text" id="form-name" value="${user?.name || ''}" required>
                </div>
                <div class="form-group">
                    <label>Телефон:</label>
                    <input type="text" id="form-phone" value="${user?.phone || ''}">
                </div>
                <div class="form-group">
                    <label>Роль:</label>
                    <select id="form-role" required>
                        <option value="CUSTOMER" ${user?.role === 'CUSTOMER' ? 'selected' : ''}>CUSTOMER</option>
                        <option value="CASHIER" ${user?.role === 'CASHIER' ? 'selected' : ''}>CASHIER</option>
                        <option value="ADMIN" ${user?.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                    </select>
                </div>
                ${!isEdit ? `
                <div class="form-group">
                    <label>Пароль:</label>
                    <input type="password" id="form-password" required minlength="6">
                </div>
                ` : ''}
                <div style="display:flex; gap:12px; margin-top:20px;">
                    <button type="submit" class="btn-primary">Сохранить</button>
                    <button type="button" class="btn" onclick="closeModal()">Отмена</button>
                </div>
            </form>
        `;
        
        modal.style.display = 'block';
        
        const form = document.getElementById('user-form');
        if (form) {
            form.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'form-email': [{ required: true, email: true }],
                    'form-name': [{ required: true, minLength: 2 }],
                    'form-phone': [{ phone: true }],
                    'form-role': [{ required: true }],
                    'form-password': user?.id ? [] : [{ required: true, minLength: 6 }]
                };
                
                if (!FormValidator.validateForm(form, rules)) {
                    return;
                }
                
                await this.saveUser(user?.id);
            };
        }
    }

    async saveUser(id) {
        try {
            const request = {
                email: document.getElementById('form-email').value,
                name: document.getElementById('form-name').value,
                phone: document.getElementById('form-phone').value || null,
                role: document.getElementById('form-role').value
            };
            
            if (!id) {
                request.password = document.getElementById('form-password').value;
            }
            
            const endpoint = id ? `/api/users/${id}` : '/api/users';
            const method = id ? 'PUT' : 'POST';
            
            await this.app.apiCall(endpoint, {
                method: method,
                body: JSON.stringify(request)
            });
            
            closeModal();
            app.showNotification('Пользователь сохранен!', 'success');
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async deleteUser(id) {
        try {
            await this.app.apiCall(`/api/users/${encodeURIComponent(id)}`, { method: 'DELETE' });
            // Success - apiCall handles the response (200 OK with no body)
            app.showNotification('Пользователь удален', 'success');
            // Reload the table
            if (window.dataTable) {
                window.dataTable.loadData();
            } else {
                location.reload();
            }
        } catch (err) {
            // Error already shown by apiCall
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async createHall() {
        this.showHallForm();
    }

    async editHall(hall) {
        this.showHallForm(hall);
    }

    showHallForm(hall = null) {
        const modal = document.getElementById('modal');
        const modalBody = document.getElementById('modal-body');
        const isEdit = !!hall;
        
        modalBody.innerHTML = `
            <h2>${isEdit ? 'Редактировать' : 'Создать'} зал</h2>
            <form id="hall-form">
                <div class="form-group">
                    <label>Название:</label>
                    <input type="text" id="form-hall-name" value="${hall?.name || ''}" required>
                </div>
                <div class="form-group">
                    <label>Вместимость:</label>
                    <input type="number" id="form-capacity" value="${hall?.capacity || ''}" required>
                </div>
                <div class="form-group">
                    <label>Местоположение:</label>
                    <input type="text" id="form-location" value="${hall?.location || ''}">
                </div>
                <div style="display:flex; gap:12px; margin-top:20px;">
                    <button type="submit" class="btn-primary">Сохранить</button>
                    <button type="button" class="btn" onclick="closeModal()">Отмена</button>
                </div>
            </form>
        `;
        
        modal.style.display = 'block';
        
        const form = document.getElementById('hall-form');
        if (form) {
            form.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'form-hall-name': [{ required: true, minLength: 2 }],
                    'form-capacity': [{ required: true, number: true, min: 1 }]
                };
                
                if (!FormValidator.validateForm(form, rules)) {
                    return;
                }
                
                await this.saveHall(hall?.id);
            };
        }
    }

    async saveHall(id) {
        try {
            const request = {
                name: document.getElementById('form-hall-name').value,
                capacity: parseInt(document.getElementById('form-capacity').value),
                location: document.getElementById('form-location').value || null
            };
            
            const endpoint = id ? `/api/halls/${id}` : '/api/halls';
            const method = id ? 'PUT' : 'POST';
            
            await this.app.apiCall(endpoint, {
                method: method,
                body: JSON.stringify(request)
            });
            
            closeModal();
            app.showNotification('Зал сохранен!', 'success');
            location.reload();
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async deleteHall(id) {
        if (confirm('Удалить зал?')) {
            try {
                await this.app.apiCall(`/api/halls/${id}`, { method: 'DELETE' });
                alert('Зал удален');
                location.reload();
            } catch (err) {
                app.showNotification(`Ошибка: ${err.message}`, 'error');
            }
        }
    }

    async createPerformer() {
        this.showPerformerForm();
    }

    async editPerformer(performer) {
        this.showPerformerForm(performer);
    }

    showPerformerForm(performer = null) {
        const modal = document.getElementById('modal');
        const modalBody = document.getElementById('modal-body');
        const isEdit = !!performer;
        
        modalBody.innerHTML = `
            <h2>${isEdit ? 'Редактировать' : 'Создать'} исполнителя</h2>
            <form id="performer-form">
                <div class="form-group">
                    <label>Имя:</label>
                    <input type="text" id="form-performer-name" value="${performer?.name || ''}" required>
                </div>
                <div style="display:flex; gap:12px; margin-top:20px;">
                    <button type="submit" class="btn-primary">Сохранить</button>
                    <button type="button" class="btn" onclick="closeModal()">Отмена</button>
                </div>
            </form>
        `;
        
        modal.style.display = 'block';
        
        const form = document.getElementById('performer-form');
        if (form) {
            form.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'form-performer-name': [{ required: true, minLength: 2 }]
                };
                
                if (!FormValidator.validateForm(form, rules)) {
                    return;
                }
                
                await this.savePerformer(performer?.id);
            };
        }
    }

    async savePerformer(id) {
        try {
            const request = {
                name: document.getElementById('form-performer-name').value
            };
            
            const endpoint = id ? `/api/performers/${id}` : '/api/performers';
            const method = id ? 'PUT' : 'POST';
            
            await this.app.apiCall(endpoint, {
                method: method,
                body: JSON.stringify(request)
            });
            
            closeModal();
            app.showNotification('Исполнитель сохранен!', 'success');
            location.reload();
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async deletePerformer(id) {
        if (confirm('Удалить исполнителя?')) {
            try {
                await this.app.apiCall(`/api/performers/${id}`, { method: 'DELETE' });
                alert('Исполнитель удален');
                location.reload();
            } catch (err) {
                app.showNotification(`Ошибка: ${err.message}`, 'error');
            }
        }
    }

    async deleteTicket(id) {
        if (confirm('Удалить билет?')) {
            try {
                await this.app.apiCall(`/api/tickets/${id}`, { method: 'DELETE' });
                alert('Билет удален');
                location.reload();
            } catch (err) {
                app.showNotification(`Ошибка: ${err.message}`, 'error');
            }
        }
    }

    renderTicketsTable(tickets) {
        if (!tickets || tickets.length === 0) {
            return '<p>Билеты не найдены</p>';
        }
        return `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Концерт</th>
                        <th>Место</th>
                        <th>Статус</th>
                        <th>Покупатель</th>
                        <th>Дата покупки</th>
                    </tr>
                </thead>
                <tbody>
                    ${tickets.map(t => `
                        <tr>
                            <td>${t.id}</td>
                            <td>${t.concertId}</td>
                            <td>${t.seatNumber}</td>
                            <td>${t.status}</td>
                            <td>${t.buyerId || '-'}</td>
                            <td>${t.purchaseTimestamp ? new Date(t.purchaseTimestamp).toLocaleString('ru-RU') : '-'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }

    // Customer-specific methods
    async loadCustomerProfile(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        try {
            const userData = this.app.currentUser;
            container.innerHTML = `
                <div class="form-container">
                    <h2>Мой профиль</h2>
                    <form id="profile-form">
                        <div class="form-group">
                            <label>Email:</label>
                            <input type="email" id="profile-email" value="${userData.email || ''}" readonly>
                        </div>
                        <div class="form-group">
                            <label>Имя:</label>
                            <input type="text" id="profile-name" value="${userData.name || ''}">
                        </div>
                        <div class="form-group">
                            <label>Телефон:</label>
                            <input type="text" id="profile-phone" value="${userData.phone || ''}">
                        </div>
                        <div style="display:flex; gap:12px; margin-top:20px;">
                            <button type="submit" class="btn-primary">Сохранить</button>
                        </div>
                    </form>
                </div>
            `;
            
            document.getElementById('profile-form').onsubmit = async (e) => {
                e.preventDefault();
                await this.saveCustomerProfile();
            };
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка: ${err.message}</div>`;
            app.showNotification(`Ошибка загрузки концертов: ${err.message}`, 'error');
        }
    }

    async saveCustomerProfile() {
        // Note: Customer can only edit their own profile through API
        // This would require a customer profile update endpoint
        alert('Обновление профиля будет доступно через API');
    }

    async showConcertAvailability(concertId) {
        const modal = document.getElementById('modal');
        const modalBody = document.getElementById('modal-body');
        
        try {
            const request = { page: 0, size: 100, sort: 'seatNumber,asc' };
            const data = await this.app.apiCall(`/api/customer/concerts/${concertId}/availability`, {
                method: 'POST',
                body: JSON.stringify(request)
            });

            const available = data.content.filter(t => t.status === 'AVAILABLE').length;
            const reserved = data.content.filter(t => t.status === 'RESERVED').length;
            const sold = data.content.filter(t => t.status === 'SOLD').length;

            modalBody.innerHTML = `
                <h2>Доступность мест</h2>
                <div style="margin-bottom:20px;">
                    <p><strong>Доступно:</strong> ${available}</p>
                    <p><strong>Забронировано:</strong> ${reserved}</p>
                    <p><strong>Продано:</strong> ${sold}</p>
                </div>
                <div style="max-height:400px; overflow-y:auto;">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Место</th>
                                <th>Статус</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.content.map(t => `
                                <tr>
                                    <td>${t.seatNumber}</td>
                                    <td>${t.status}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
                <button class="btn" onclick="closeModal()" style="margin-top:20px;">Закрыть</button>
            `;
            modal.style.display = 'block';
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async bookTicket(concertId) {
        const seatNumber = prompt('Введите номер места:');
        if (!seatNumber) return;
        
        const minutes = prompt('Время резервации в минутах (по умолчанию 30):', '30');
        
        try {
            const request = {
                concertId: concertId,
                seatNumber: seatNumber,
                minutes: minutes ? parseInt(minutes) : 30
            };
            await this.app.apiCall('/api/customer/tickets/book', {
                method: 'POST',
                body: JSON.stringify(request)
            });
            app.showNotification('Билет забронирован!', 'success');
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }

    async purchaseTicket(concertId) {
        const seatNumber = prompt('Введите номер места:');
        if (!seatNumber) return;
        
        const paymentMethod = prompt('Способ оплаты (cash/card):', 'card');
        
        try {
            const request = {
                concertId: concertId,
                seatNumber: seatNumber,
                paymentMethod: paymentMethod || 'card'
            };
            await this.app.apiCall('/api/customer/tickets/purchase', {
                method: 'POST',
                body: JSON.stringify(request)
            });
            app.showNotification('Билет куплен!', 'success');
        } catch (err) {
            app.showNotification(`Ошибка: ${err.message}`, 'error');
        }
    }
}

const viewLoader = new ViewLoader(app);

