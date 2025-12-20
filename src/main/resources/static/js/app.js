// Main Application State and API Client
class App {
    constructor() {
        this.currentUser = null;
        this.currentRole = null;
        this.currentView = 'home';
        // Initialize after DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }

    async init() {
        await this.checkAuth();
        this.setupNavigation();
        this.setupBrandClick();
        this.render();
    }

    setupBrandClick() {
        // Make brand block clickable on all pages - navigate to welcome page
        const brand = document.querySelector('.brand');
        if (brand) {
            brand.style.cursor = 'pointer';
            brand.onclick = () => {
                this.currentView = 'welcome';
                this.render();
            };
        }
    }

    async checkAuth() {
        try {
            const res = await fetch('/api/auth/me', {
                method: 'GET',
                credentials: 'include'
            });
            if (res.ok) {
                const data = await res.json();
                this.currentUser = data;
                this.currentRole = data.role;
            }
        } catch (err) {
            console.log('Not authenticated');
        }
    }

    setupNavigation() {
        // Navigation will be handled by view rendering
    }

    async apiCall(endpoint, options = {}) {
        const defaultOptions = {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        };
        try {
            const response = await fetch(endpoint, { ...defaultOptions, ...options });
            if (!response.ok) {
                // Read response as text first, then try to parse as JSON
                const text = await response.text();
                let errorData;
                try {
                    errorData = JSON.parse(text);
                } catch (e) {
                    // If not valid JSON, use text as message
                    errorData = { message: text || `HTTP ${response.status}` };
                }
                this.showError(errorData, response.status);
                throw new Error(errorData.message || `API Error: ${response.status}`);
            }
            // Handle responses with no body (e.g., DELETE 200 OK)
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                // No JSON body, return success indicator
                return { success: true };
            }
            // Try to parse JSON, but handle empty responses gracefully
            const text = await response.text();
            if (!text || text.trim() === '') {
                return { success: true };
            }
            return JSON.parse(text);
        } catch (err) {
            if (err.message && !err.message.startsWith('API Error')) {
                this.showError({ message: err.message }, 500);
            }
            throw err;
        }
    }

    showError(errorData, status) {
        const message = errorData.message || errorData.error || `Ошибка ${status}`;
        const details = errorData.fieldErrors ? 
            Object.entries(errorData.fieldErrors).map(([field, msg]) => `${field}: ${msg}`).join('\n') : 
            '';
        
        const fullMessage = details ? `${message}\n\nДетали:\n${details}` : message;
        
        // Show error notification
        this.showNotification(fullMessage, 'error');
    }

    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span class="notification-message">${message}</span>
                <button class="notification-close" onclick="this.parentElement.parentElement.remove()">&times;</button>
            </div>
        `;
        
        const container = document.body;
        container.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 5000);
    }

    navigate(view) {
        this.currentView = view;
        if (view === 'dashboard' && this.currentUser) {
            // Reset to show dashboard instead of home
            this.currentView = 'dashboard';
        }
        this.render();
    }

    render() {
        const container = document.getElementById('app-content');
        if (!container) return;

        // Update user menu
        const userName = document.getElementById('user-name');
        const logoutBtn = document.getElementById('logout-btn');
        const mainNav = document.getElementById('main-nav');
        if (this.currentUser) {
            if (userName) userName.textContent = this.currentUser.name || 'Пользователь';
            if (logoutBtn) logoutBtn.style.display = 'block';
            if (mainNav) {
                mainNav.innerHTML = `
                    <a href="#" onclick="app.currentView='welcome'; app.render(); return false;">Главная</a>
                    <a href="#" onclick="app.currentView='profile'; app.render(); return false;">Профиль</a>
                    ${this.currentRole === 'ADMIN' || this.currentRole === 'CASHIER' ? 
                        '<a href="#" onclick="app.navigate(\'statistics\'); return false;">Статистика</a>' : ''}
                    <a href="#" onclick="app.navigate('about'); return false;">О проекте</a>
                `;
            }
        } else {
            if (userName) userName.textContent = '';
            if (logoutBtn) logoutBtn.style.display = 'none';
            if (mainNav) {
                mainNav.innerHTML = `
                    <a href="#" onclick="app.currentView='welcome'; app.render(); return false;">Главная</a>
                    <a href="#" onclick="app.navigate('about'); return false;">О проекте</a>
                `;
            }
        }

        // Render based on current view and role
        if (!this.currentUser) {
            if (this.currentView === 'welcome' || this.currentView === 'home' || !this.currentView || this.currentView === '') {
                this.loadWelcomePage(container);
            } else if (this.currentView === 'about') {
                this.loadAboutPage(container);
            } else if (this.currentView === 'login') {
                container.innerHTML = this.renderLogin();
                // Show login form
                const registerCard = document.getElementById('register-card');
                if (registerCard) registerCard.style.display = 'none';
            } else if (this.currentView === 'register') {
                container.innerHTML = this.renderLogin();
                // Show register form
                const registerCard = document.getElementById('register-card');
                const loginCard = document.querySelector('.auth-card:first-of-type');
                if (registerCard) registerCard.style.display = 'block';
                if (loginCard) loginCard.style.display = 'none';
            } else if (this.currentView.startsWith('concert-')) {
                // Concert detail page (public access)
                const concertId = parseInt(this.currentView.replace('concert-', ''));
                this.loadConcertDetail(container, concertId);
            } else {
                this.loadWelcomePage(container);
            }
        } else {
            if (this.currentView === 'welcome' || this.currentView === 'home' || this.currentView === '') {
                // Show welcome page even when logged in
                this.loadWelcomePage(container);
            } else if (this.currentView === 'profile') {
                // Profile page based on role
                container.innerHTML = this.renderProfilePage();
                setTimeout(() => {
                    const activeTab = document.querySelector('.tab-btn.active');
                    if (activeTab) {
                        this.loadView(activeTab.dataset.view);
                    }
                }, 100);
            } else if (this.currentView === 'about') {
                this.loadAboutPage(container);
            } else if (this.currentView === 'statistics') {
                this.loadStatisticsPage(container);
            } else if (this.currentView === 'dashboard') {
                container.innerHTML = this.renderDashboard();
                // Load default view
                setTimeout(() => {
                    const activeTab = document.querySelector('.tab-btn.active');
                    if (activeTab) {
                        this.loadView(activeTab.dataset.view);
                    }
                }, 100);
            } else if (this.currentView.startsWith('concert-')) {
                // Concert detail page
                const concertId = parseInt(this.currentView.replace('concert-', ''));
                this.loadConcertDetail(container, concertId);
            } else {
                container.innerHTML = this.renderDashboard();
                // Load default view
                setTimeout(() => {
                    const activeTab = document.querySelector('.tab-btn.active');
                    if (activeTab) {
                        this.loadView(activeTab.dataset.view);
                    }
                }, 100);
            }
        }
        
        // Re-attach event listeners
        this.attachEventListeners();
    }

    async loadWelcomePage(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        
        try {
            // Load upcoming concerts
            const concertsRequest = {
                page: 0,
                size: 6,
                sort: 'date,asc'
            };
            const concertsData = await this.apiCall('/api/concerts/public/upcoming', {
                method: 'POST',
                body: JSON.stringify(concertsRequest)
            });

            // Load performers
            const performersRequest = {
                page: 0,
                size: 6,
                sort: 'name,asc',
                name: null
            };
            const performersData = await this.apiCall('/api/performers/public/list', {
                method: 'POST',
                body: JSON.stringify(performersRequest)
            });

            const authButtons = this.currentUser ? '' : `
                <div class="hero-actions">
                    <button class="btn-primary btn-large" onclick="app.showLogin()">Войти</button>
                    <button class="btn btn-large" onclick="app.showRegister()">Зарегистрироваться</button>
                </div>
            `;

            container.innerHTML = `
                <section class="hero">
                    <div class="hero-content">
                        <h1 class="hero-title">Добро пожаловать в 52harmonic</h1>
                        ${authButtons}
                    </div>
                </section>
                <section class="features">
                    <div class="shell">
                        <h2 class="section-title">О филармонии</h2>
                        <div class="features-grid">
                            <div class="feature-card">
                                <h3>🎵 Концерты</h3>
                                <p>Просматривайте предстоящие концерты и бронируйте билеты онлайн</p>
                            </div>
                            <div class="feature-card">
                                <h3>🎫 Билеты</h3>
                                <p>Управляйте своими билетами, просматривайте историю покупок</p>
                            </div>
                            <div class="feature-card">
                                <h3>👤 Профиль</h3>
                                <p>Управляйте личной информацией и настройками аккаунта</p>
                            </div>
                        </div>
                    </div>
                </section>
                <section class="concerts-section" style="padding: 60px 0; background: #fff;">
                    <div class="shell">
                        <h2 class="section-title">Предстоящие концерты</h2>
                        <div class="concerts-grid">
                            ${concertsData.content && concertsData.content.length > 0 ? 
                                concertsData.content.map(concert => `
                                    <div class="concert-card" style="cursor: pointer;" onclick="app.currentView='concert-${concert.id}'; app.render();">
                                        <h3>${concert.title}</h3>
                                        <p>${new Date(concert.date).toLocaleDateString('ru-RU')} в ${concert.time}</p>
                                        <p>Цена: ${concert.ticketPrice} ₽</p>
                                    </div>
                                `).join('') : 
                                '<p>Концерты не найдены</p>'
                            }
                        </div>
                    </div>
                </section>
                <section class="performers-section" style="padding: 60px 0; background: #f9fafb;">
                    <div class="shell">
                        <h2 class="section-title">Исполнители</h2>
                        <div class="concerts-grid">
                            ${performersData.content && performersData.content.length > 0 ? 
                                performersData.content.map(performer => `
                                    <div class="concert-card">
                                        <h3>${performer.name}</h3>
                                    </div>
                                `).join('') : 
                                '<p>Исполнители не найдены</p>'
                            }
                        </div>
                    </div>
                </section>
            `;
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка загрузки: ${err.message}</div>`;
            console.error('Error loading welcome page:', err);
        }
    }

    renderHomePage() {
        const roleGreetings = {
            'CUSTOMER': 'Добро пожаловать в личный кабинет',
            'CASHIER': 'Добро пожаловать в панель кассира',
            'ADMIN': 'Добро пожаловать в панель администратора'
        };
        
        // Get tabs based on role (no tabs for CUSTOMER)
        let tabs = '';
        let contentId = '';
        let dashboardSection = '';
        
        if (this.currentRole === 'CUSTOMER') {
            // No tabs for customers
            dashboardSection = '';
        } else if (this.currentRole === 'CASHIER') {
            tabs = `
                <button class="tab-btn active" data-view="sell-ticket">Продать билет</button>
                <button class="tab-btn" data-view="tickets">Билеты</button>
                <button class="tab-btn" data-view="sales-history">История продаж</button>
            `;
            contentId = 'cashier-content';
            dashboardSection = `
                <div class="dashboard">
                    <div class="dashboard-tabs">
                        ${tabs}
                    </div>
                    <div id="${contentId}"></div>
                </div>
            `;
        } else if (this.currentRole === 'ADMIN') {
            tabs = `
                <button class="tab-btn active" data-view="concerts">Концерты</button>
                <button class="tab-btn" data-view="tickets">Билеты</button>
                <button class="tab-btn" data-view="users">Пользователи</button>
                <button class="tab-btn" data-view="halls">Залы</button>
                <button class="tab-btn" data-view="performers">Исполнители</button>
            `;
            contentId = 'admin-content';
            dashboardSection = `
                <div class="dashboard">
                    <div class="dashboard-tabs">
                        ${tabs}
                    </div>
                    <div id="${contentId}"></div>
                </div>
            `;
        }
        
        // Don't show features section for ADMIN
        const featuresSection = this.currentRole === 'ADMIN' ? '' : `
            <section class="features">
                <div class="shell">
                    <h2 class="section-title">Возможности</h2>
                    <div class="features-grid">
                        <div class="feature-card">
                            <h3>🎵 Концерты</h3>
                            <p>Просматривайте предстоящие концерты и бронируйте билеты онлайн</p>
                        </div>
                        <div class="feature-card">
                            <h3>🎫 Билеты</h3>
                            <p>Управляйте своими билетами, просматривайте историю покупок</p>
                        </div>
                        <div class="feature-card">
                            <h3>👤 Профиль</h3>
                            <p>Управляйте личной информацией и настройками аккаунта</p>
                        </div>
                    </div>
                </div>
            </section>
        `;
        
        return `
            <section class="hero">
                <div class="hero-content">
                    <h1 class="hero-title">${roleGreetings[this.currentRole] || 'Добро пожаловать'}, ${this.currentUser.name || 'Пользователь'}!</h1>
                    <p class="hero-sub">Выберите раздел для работы с системой</p>
                </div>
            </section>
            ${featuresSection}
            ${dashboardSection}
        `;
    }

    showLogin() {
        this.currentView = 'login';
        this.render();
    }

    showRegister() {
        this.currentView = 'register';
        this.render();
    }

    renderLogin() {
        return `
            <div class="auth-container">
                <div class="auth-card">
                    <h2>Вход в систему</h2>
                    <form id="login-form">
                        <input type="email" id="login-email" placeholder="Email" required>
                        <input type="password" id="login-password" placeholder="Пароль" required>
                        <button type="submit" class="btn-primary">Войти</button>
                    </form>
                    <p>Нет аккаунта? <a href="#" id="show-register">Зарегистрироваться</a></p>
                    <p><a href="#" onclick="app.navigate('home'); return false;">← Вернуться на главную</a></p>
                </div>
                <div class="auth-card" id="register-card" style="display:none;">
                    <h2>Регистрация</h2>
                    <form id="register-form">
                        <input type="email" id="register-email" placeholder="Email" required>
                        <input type="password" id="register-password" placeholder="Пароль" required minlength="6">
                        <input type="text" id="register-name" placeholder="Имя" required>
                        <input type="text" id="register-phone" placeholder="Телефон (опционально)">
                        <button type="submit" class="btn-primary">Зарегистрироваться</button>
                    </form>
                    <p>Уже есть аккаунт? <a href="#" id="show-login">Войти</a></p>
                    <p><a href="#" onclick="app.navigate('home'); return false;">← Вернуться на главную</a></p>
                </div>
            </div>
        `;
    }

    renderDashboard() {
        const roleViews = {
            'CUSTOMER': () => this.renderCustomerDashboard(),
            'CASHIER': () => this.renderCashierDashboard(),
            'ADMIN': () => this.renderAdminDashboard()
        };
        return roleViews[this.currentRole]?.() || '<p>Неизвестная роль</p>';
    }

    renderCustomerDashboard() {
        return `
            <div class="dashboard">
                <h1>Личный кабинет</h1>
                <div class="dashboard-tabs">
                    <button class="tab-btn active" data-view="concerts">Предстоящие концерты</button>
                    <button class="tab-btn" data-view="my-tickets">Мои билеты</button>
                    <button class="tab-btn" data-view="profile">Профиль</button>
                </div>
                <div id="customer-content"></div>
            </div>
        `;
    }

    renderCashierDashboard() {
        return `
            <div class="dashboard">
                <h1>Панель кассира</h1>
                <div class="dashboard-tabs">
                    <button class="tab-btn active" data-view="sell-ticket">Продать билет</button>
                    <button class="tab-btn" data-view="tickets">Билеты</button>
                    <button class="tab-btn" data-view="sales-history">История продаж</button>
                </div>
                <div id="cashier-content"></div>
            </div>
        `;
    }

    renderAdminDashboard() {
        return `
            <div class="dashboard">
                <h1>Панель администратора</h1>
                <div class="dashboard-tabs">
                    <button class="tab-btn active" data-view="concerts">Концерты</button>
                    <button class="tab-btn" data-view="tickets">Билеты</button>
                    <button class="tab-btn" data-view="users">Пользователи</button>
                    <button class="tab-btn" data-view="halls">Залы</button>
                    <button class="tab-btn" data-view="performers">Исполнители</button>
                </div>
                <div id="admin-content"></div>
            </div>
        `;
    }

    attachEventListeners() {
        // Login form
        const loginForm = document.getElementById('login-form');
        if (loginForm) {
            loginForm.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'login-email': [{ required: true, email: true }],
                    'login-password': [{ required: true, minLength: 6 }]
                };
                if (!FormValidator.validateForm(loginForm, rules)) {
                    return;
                }
                await this.handleLogin();
            };
        }

        // Register form
        const registerForm = document.getElementById('register-form');
        if (registerForm) {
            registerForm.onsubmit = async (e) => {
                e.preventDefault();
                const rules = {
                    'register-email': [{ required: true, email: true }],
                    'register-password': [{ required: true, minLength: 6 }],
                    'register-name': [{ required: true, minLength: 2 }],
                    'register-phone': [{ phone: true }]
                };
                if (!FormValidator.validateForm(registerForm, rules)) {
                    return;
                }
                await this.handleRegister();
            };
        }

        // Show register/login
        const showRegister = document.getElementById('show-register');
        if (showRegister) {
            showRegister.onclick = (e) => {
                e.preventDefault();
                document.getElementById('register-card').style.display = 'block';
                document.querySelector('.auth-card:first-of-type').style.display = 'none';
            };
        }

        const showLogin = document.getElementById('show-login');
        if (showLogin) {
            showLogin.onclick = (e) => {
                e.preventDefault();
                document.getElementById('register-card').style.display = 'none';
                document.querySelector('.auth-card:first-of-type').style.display = 'block';
            };
        }

        // Tab buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.onclick = () => {
                document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                const view = btn.dataset.view;
                this.loadView(view);
            };
        });
    }

    async handleLogin() {
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        try {
            const data = await this.apiCall('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });
            await this.checkAuth();
            // Redirect to profile page after login
            this.currentView = 'profile';
            this.render();
        } catch (err) {
            // Error already shown by apiCall
        }
    }

    async handleRegister() {
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        const name = document.getElementById('register-name').value;
        const phone = document.getElementById('register-phone').value;
        try {
            const data = await this.apiCall('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ email, password, name, phone })
            });
            await this.checkAuth();
            // Redirect to profile page after registration
            this.currentView = 'profile';
            this.render();
        } catch (err) {
            // Error already shown by apiCall
        }
    }

    async loadView(viewName) {
        let contentDiv = null;
        if (this.currentRole === 'CUSTOMER') {
            contentDiv = document.getElementById('customer-content');
        } else if (this.currentRole === 'CASHIER') {
            contentDiv = document.getElementById('cashier-content');
        } else if (this.currentRole === 'ADMIN') {
            contentDiv = document.getElementById('admin-content');
        }
        
        if (!contentDiv) {
            console.error('Content div not found');
            return;
        }

        const viewLoader = new ViewLoader(this);
        await viewLoader.load(viewName, contentDiv);
    }

    logout() {
        // Clear JWT cookie by setting it to expire
        document.cookie = 'JWT=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        this.currentUser = null;
        this.currentRole = null;
        this.currentView = 'welcome';
        this.render();
    }

    async loadConcertDetail(container, concertId) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        try {
            const concert = await this.apiCall(`/api/concerts/public/${concertId}`, { method: 'GET' });
            const hall = await this.apiCall(`/api/halls/public/${concert.hallId}`, { method: 'GET' });
            
            const bookButton = this.currentUser && this.currentRole === 'CUSTOMER' ? `
                <button class="btn-primary btn-large" onclick="app.showBookingForm(${concert.id}, ${hall.capacity})">
                    Забронировать билет
                </button>
            ` : this.currentUser ? '' : `
                <p style="color: #6b7280;">Войдите в систему, чтобы забронировать билет</p>
            `;

            container.innerHTML = `
                <section class="hero" style="padding: 40px 0;">
                    <div class="shell">
                        <p><a href="#" onclick="app.currentView='welcome'; app.render(); return false;">← Вернуться на главную</a></p>
                        <div style="max-width: 800px; margin: 40px auto; background: #fff; padding: 40px; border-radius: var(--radius); box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);">
                            <h1 style="margin: 0 0 20px;">${concert.title}</h1>
                            <div style="margin: 20px 0; color: #6b7280;">
                                <p><strong>Дата:</strong> ${new Date(concert.date).toLocaleDateString('ru-RU')}</p>
                                <p><strong>Время:</strong> ${concert.time}</p>
                                <p><strong>Цена билета:</strong> ${concert.ticketPrice} ₽</p>
                            </div>
                            <div style="margin: 30px 0;">
                                ${bookButton}
                            </div>
                            <div id="book-form-container" style="display: none; margin-top: 30px; padding: 20px; background: #f9fafb; border-radius: 8px;">
                                <h3>Бронирование билетов</h3>
                                <p id="book-availability-info" style="margin-bottom: 20px; color: #6b7280;">Вместимость зала: ${hall.capacity} мест. Загрузка доступных билетов...</p>
                                <form id="book-ticket-form">
                                    <div class="form-group">
                                        <label>Количество билетов:</label>
                                        <input type="number" id="book-quantity" min="1" max="${hall.capacity}" value="1" required>
                                    </div>
                                    <button type="submit" class="btn-primary">Забронировать билеты</button>
                                    <button type="button" class="btn" onclick="document.getElementById('book-form-container').style.display='none';">Отмена</button>
                                </form>
                            </div>
                        </div>
                    </div>
                </section>
            `;

            // Attach form handler
            const form = document.getElementById('book-ticket-form');
            if (form) {
                form.onsubmit = async (e) => {
                    e.preventDefault();
                    try {
                        const quantity = parseInt(document.getElementById('book-quantity').value) || 1;
                        if (quantity < 1 || quantity > hall.capacity) {
                            this.showNotification(`Количество должно быть от 1 до ${hall.capacity}`, 'error');
                            return;
                        }
                        const viewLoader = new ViewLoader(this);
                        await viewLoader.bookTicket(concertId, quantity);
                        document.getElementById('book-form-container').style.display = 'none';
                        // Update availability after booking
                        if (window.app) {
                            await app.updateBookingAvailability(concertId, hall.capacity);
                        }
                    } catch (err) {
                        this.showNotification(`Ошибка: ${err.message}`, 'error');
                    }
                };
            }
            
            // Store concert ID and hall capacity for availability updates
            if (typeof window !== 'undefined') {
                window.currentConcertId = concertId;
                window.currentHallCapacity = hall.capacity;
            }
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка загрузки концерта: ${err.message}</div>`;
        }
    }

    renderProfilePage() {
        let tabs = '';
        let contentId = '';
        
        if (this.currentRole === 'CUSTOMER') {
            tabs = `
                <button class="tab-btn active" data-view="my-tickets">Мои билеты</button>
                <button class="tab-btn" data-view="profile">Личная информация</button>
            `;
            contentId = 'customer-content';
        } else if (this.currentRole === 'CASHIER') {
            tabs = `
                <button class="tab-btn active" data-view="sell-ticket">Продать билет</button>
                <button class="tab-btn" data-view="tickets">Билеты</button>
                <button class="tab-btn" data-view="sales-history">История продаж</button>
            `;
            contentId = 'cashier-content';
        } else if (this.currentRole === 'ADMIN') {
            tabs = `
                <button class="tab-btn active" data-view="concerts">Концерты</button>
                <button class="tab-btn" data-view="tickets">Билеты</button>
                <button class="tab-btn" data-view="users">Пользователи</button>
                <button class="tab-btn" data-view="halls">Залы</button>
                <button class="tab-btn" data-view="performers">Исполнители</button>
            `;
            contentId = 'admin-content';
        }
        
        return `
            <section class="hero" style="padding: 40px 0;">
                <div class="hero-content">
                    <h1 class="hero-title">Профиль: ${this.currentUser.name || 'Пользователь'}</h1>
                    <p class="hero-sub">${this.currentRole === 'CUSTOMER' ? 'Управление билетами и личной информацией' : 
                        this.currentRole === 'CASHIER' ? 'Панель управления билетами' : 
                        'Панель администратора'}</p>
                </div>
            </section>
            <div class="dashboard">
                <div class="dashboard-tabs">
                    ${tabs}
                </div>
                <div id="${contentId}"></div>
            </div>
        `;
    }

    async loadAboutPage(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        try {
            const data = await this.apiCall('/api/about', { method: 'GET' });
            container.innerHTML = `
                <section class="about-section">
                    <div class="about-container">
                        <h1>О проекте</h1>
                        <div class="about-card">
                            <h2>Автор проекта</h2>
                            <div class="about-info">
                                <p><strong>ФИО:</strong> ${data.authorName}</p>
                                <p><strong>Группа/Учебное заведение:</strong> ${data.group}</p>
                                <p><strong>Email:</strong> <a href="mailto:${data.contactEmail}">${data.contactEmail}</a></p>
                                <p><strong>Телефон:</strong> ${data.contactPhone}</p>
                            </div>
                        </div>
                        <div class="about-card">
                            <h2>Опыт работы с технологиями</h2>
                            <ul class="tech-list">
                                ${data.technologies.map(tech => `<li>${tech}</li>`).join('')}
                            </ul>
                        </div>
                        <div class="about-card">
                            <h2>Сроки проекта</h2>
                            <div class="about-info">
                                <p><strong>Дата начала:</strong> ${new Date(data.projectStartDate).toLocaleDateString('ru-RU')}</p>
                                <p><strong>Дата окончания:</strong> ${new Date(data.projectEndDate).toLocaleDateString('ru-RU')}</p>
                            </div>
                        </div>
                    </div>
                </section>
            `;
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка загрузки информации: ${err.message}</div>`;
        }
    }

    async loadStatisticsPage(container) {
        container.innerHTML = '<div class="loading">Загрузка...</div>';
        try {
            const data = await this.apiCall('/api/statistics', { method: 'GET' });
            container.innerHTML = `
                <div class="statistics-page">
                    <h1>Статистика системы</h1>
                    <div class="stats-grid">
                        <div class="stat-card">
                            <h3>Всего пользователей</h3>
                            <div class="stat-value">${data.totalUsers}</div>
                        </div>
                        <div class="stat-card">
                            <h3>Среднее время ожидания</h3>
                            <div class="stat-value">${Math.round(data.averageWaitTimeMinutes)} мин</div>
                            <div class="stat-sub">(${data.averageWaitTimeHours.toFixed(2)} часов)</div>
                        </div>
                        <div class="stat-card">
                            <h3>Всего билетов</h3>
                            <div class="stat-value">${data.totalTickets}</div>
                        </div>
                        <div class="stat-card">
                            <h3>Продано билетов</h3>
                            <div class="stat-value">${data.soldTickets}</div>
                        </div>
                    </div>
                    <div class="charts-container">
                        <div class="chart-card">
                            <h3>Распределение пользователей по ролям</h3>
                            <div class="chart" id="users-chart"></div>
                        </div>
                        <div class="chart-card">
                            <h3>Статусы билетов</h3>
                            <div class="chart" id="tickets-chart"></div>
                        </div>
                    </div>
                </div>
            `;
            
            // Render charts
            this.renderUsersChart(data.usersByRole);
            this.renderTicketsChart(data.ticketsByStatus);
        } catch (err) {
            container.innerHTML = `<div class="error">Ошибка загрузки статистики: ${err.message}</div>`;
        }
    }

    renderUsersChart(usersByRole) {
        const chartDiv = document.getElementById('users-chart');
        if (!chartDiv) return;
        
        const data = Object.entries(usersByRole);
        const maxValue = Math.max(...data.map(([_, val]) => val), 1);
        
        chartDiv.innerHTML = `
            <div class="bar-chart">
                ${data.map(([role, count]) => `
                    <div class="bar-item">
                        <div class="bar-label">${role}</div>
                        <div class="bar-container">
                            <div class="bar" style="width: ${(count / maxValue) * 100}%">
                                <span class="bar-value">${count}</span>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    renderTicketsChart(ticketsByStatus) {
        const chartDiv = document.getElementById('tickets-chart');
        if (!chartDiv) return;
        
        const data = Object.entries(ticketsByStatus);
        const maxValue = Math.max(...data.map(([_, val]) => val), 1);
        
        chartDiv.innerHTML = `
            <div class="bar-chart">
                ${data.map(([status, count]) => `
                    <div class="bar-item">
                        <div class="bar-label">${status}</div>
                        <div class="bar-container">
                            <div class="bar" style="width: ${(count / maxValue) * 100}%">
                                <span class="bar-value">${count}</span>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    async showBookingForm(concertId, hallCapacity) {
        const container = document.getElementById('book-form-container');
        if (container) {
            container.style.display = 'block';
            // Update availability info
            await this.updateBookingAvailability(concertId, hallCapacity);
            // Setup form handler
            const form = document.getElementById('book-ticket-form');
            if (form && !form.hasAttribute('data-handler-attached')) {
                form.setAttribute('data-handler-attached', 'true');
                form.onsubmit = async (e) => {
                    e.preventDefault();
                    try {
                        const quantity = parseInt(document.getElementById('book-quantity').value) || 1;
                        if (quantity < 1 || quantity > hallCapacity) {
                            this.showNotification(`Количество должно быть от 1 до ${hallCapacity}`, 'error');
                            return;
                        }
                        const viewLoader = new ViewLoader(this);
                        await viewLoader.bookTicket(concertId, quantity);
                        document.getElementById('book-form-container').style.display = 'none';
                        // Update availability after booking
                        await this.updateBookingAvailability(concertId, hallCapacity);
                    } catch (err) {
                        this.showNotification(`Ошибка: ${err.message}`, 'error');
                        // Update availability even on error (in case it changed)
                        await this.updateBookingAvailability(concertId, hallCapacity);
                    }
                };
            }
        }
    }

    async updateBookingAvailability(concertId, hallCapacity) {
        try {
            const availabilityInfo = document.getElementById('book-availability-info');
            if (!availabilityInfo) return;
            
            const data = await this.apiCall(`/api/concerts/public/${concertId}/available-tickets`, {
                method: 'GET'
            });
            const available = data.availableTickets || 0;
            availabilityInfo.textContent = `Вместимость зала: ${hallCapacity} мест. Доступно билетов: ${available}.`;
            
            // Update max value of quantity input
            const quantityInput = document.getElementById('book-quantity');
            if (quantityInput) {
                quantityInput.max = Math.min(hallCapacity, available);
                if (parseInt(quantityInput.value) > available) {
                    quantityInput.value = Math.max(1, available);
                }
            }
        } catch (err) {
            const availabilityInfo = document.getElementById('book-availability-info');
            if (availabilityInfo) {
                availabilityInfo.textContent = `Вместимость зала: ${hallCapacity} мест. Не удалось загрузить доступные билеты.`;
            }
            console.error('Error fetching availability:', err);
        }
    }
}

// Initialize app
const app = new App();

