// Form Validation Utilities
class FormValidator {
    static validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    static validatePhone(phone) {
        if (!phone) return true; // Optional
        const re = /^[\d\s\-\+\(\)]+$/;
        return re.test(phone) && phone.replace(/\D/g, '').length >= 10;
    }

    static validateRequired(value) {
        return value != null && value.toString().trim().length > 0;
    }

    static validateNumber(value, min = null, max = null) {
        const num = parseFloat(value);
        if (isNaN(num)) return false;
        if (min != null && num < min) return false;
        if (max != null && num > max) return false;
        return true;
    }

    static validateDate(date) {
        if (!date) return false;
        const d = new Date(date);
        return d instanceof Date && !isNaN(d);
    }

    static validateTime(time) {
        if (!time) return false;
        const re = /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/;
        return re.test(time);
    }

    static showFieldError(field, message) {
        field.classList.add('error-field');
        let errorDiv = field.parentElement.querySelector('.field-error');
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.className = 'field-error';
            field.parentElement.appendChild(errorDiv);
        }
        errorDiv.textContent = message;
    }

    static clearFieldError(field) {
        field.classList.remove('error-field');
        const errorDiv = field.parentElement.querySelector('.field-error');
        if (errorDiv) {
            errorDiv.remove();
        }
    }

    static validateForm(form, rules) {
        let isValid = true;
        
        for (const [fieldName, fieldRules] of Object.entries(rules)) {
            const field = form.querySelector(`[name="${fieldName}"], #${fieldName}`);
            if (!field) continue;
            
            this.clearFieldError(field);
            
            for (const rule of fieldRules) {
                const value = field.value;
                
                if (rule.required && !this.validateRequired(value)) {
                    this.showFieldError(field, rule.message || 'Это поле обязательно для заполнения');
                    isValid = false;
                    break;
                }
                
                if (rule.email && value && !this.validateEmail(value)) {
                    this.showFieldError(field, rule.message || 'Введите корректный email адрес');
                    isValid = false;
                    break;
                }
                
                if (rule.phone && value && !this.validatePhone(value)) {
                    this.showFieldError(field, rule.message || 'Введите корректный номер телефона');
                    isValid = false;
                    break;
                }
                
                if (rule.minLength && value && value.length < rule.minLength) {
                    this.showFieldError(field, rule.message || `Минимальная длина: ${rule.minLength} символов`);
                    isValid = false;
                    break;
                }
                
                if (rule.number && value && !this.validateNumber(value, rule.min, rule.max)) {
                    this.showFieldError(field, rule.message || 'Введите корректное число');
                    isValid = false;
                    break;
                }
                
                if (rule.date && value && !this.validateDate(value)) {
                    this.showFieldError(field, rule.message || 'Введите корректную дату');
                    isValid = false;
                    break;
                }
                
                if (rule.time && value && !this.validateTime(value)) {
                    this.showFieldError(field, rule.message || 'Введите корректное время (HH:MM)');
                    isValid = false;
                    break;
                }
            }
        }
        
        return isValid;
    }
}

