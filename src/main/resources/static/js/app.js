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
        this.render();
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
        const response = await fetch(endpoint, { ...defaultOptions, ...options });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(`API Error: ${response.status} - ${error}`);
        }
        return response.json();
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
                    <a href="#" onclick="app.navigate('home'); return false;">Главная</a>
                    <a href="#" onclick="app.navigate('dashboard'); return false;">Панель управления</a>
                `;
            }
        } else {
            if (userName) userName.textContent = '';
            if (logoutBtn) logoutBtn.style.display = 'none';
            if (mainNav) {
                mainNav.innerHTML = `
                    <a href="#" onclick="app.navigate('home'); return false;">Главная</a>
                `;
            }
        }

        // Render based on current view and role
        if (!this.currentUser) {
            if (this.currentView === 'home' || !this.currentView || this.currentView === '') {
                container.innerHTML = this.renderWelcomePage();
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
            } else {
                container.innerHTML = this.renderWelcomePage();
            }
        } else {
            if (this.currentView === 'home' || this.currentView === '') {
                container.innerHTML = this.renderHomePage();
            } else if (this.currentView === 'dashboard') {
                container.innerHTML = this.renderDashboard();
                // Load default view
                setTimeout(() => {
                    const activeTab = document.querySelector('.tab-btn.active');
                    if (activeTab) {
                        this.loadView(activeTab.dataset.view);
                    }
                }, 100);
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

    renderWelcomePage() {
        return `
            <section class="hero">
                <div class="hero-content">
                    <h1 class="hero-title">Добро пожаловать в Musical Philharmonic</h1>
                    <p class="hero-sub">Онлайн-трансляции и концерты. Подключайтесь к событиям филармонии, бронируйте места и следите за новыми программами.</p>
                    <div class="hero-actions">
                        <button class="btn-primary btn-large" onclick="app.showLogin()">Войти</button>
                        <button class="btn btn-large" onclick="app.showRegister()">Зарегистрироваться</button>
                    </div>
                    <div class="tags" style="margin-top: 24px;">
                        <span class="tag">Классика</span>
                        <span class="tag">Джаз</span>
                        <span class="tag">Live</span>
                        <span class="tag">Абонементы</span>
                    </div>
                </div>
            </section>
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
    }

    renderHomePage() {
        const roleGreetings = {
            'CUSTOMER': 'Добро пожаловать в личный кабинет',
            'CASHIER': 'Панель кассира',
            'ADMIN': 'Панель администратора'
        };
        
        return `
            <section class="hero">
                <div class="hero-content">
                    <h1 class="hero-title">${roleGreetings[this.currentRole] || 'Добро пожаловать'}, ${this.currentUser.name || 'Пользователь'}!</h1>
                    <p class="hero-sub">Выберите раздел в меню для работы с системой</p>
                    <div class="hero-actions" style="margin-top: 24px;">
                        <button class="btn-primary btn-large" onclick="app.navigate('dashboard')">Перейти к панели управления</button>
                    </div>
                </div>
            </section>
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
                await this.handleLogin();
            };
        }

        // Register form
        const registerForm = document.getElementById('register-form');
        if (registerForm) {
            registerForm.onsubmit = async (e) => {
                e.preventDefault();
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
            this.render();
        } catch (err) {
            alert('Ошибка входа: ' + err.message);
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
            this.render();
        } catch (err) {
            alert('Ошибка регистрации: ' + err.message);
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
        this.render();
    }
}

// Initialize app
const app = new App();

