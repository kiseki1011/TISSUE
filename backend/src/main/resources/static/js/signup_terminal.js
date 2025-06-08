/**
 * TISSUE Terminal Signup System
 * 브라우저를 실제 터미널처럼 만드는 완전 몰입형 인터페이스
 */
class TissueTerminal {
    constructor() {
        // 시스템 상태
        this.isInitialized = false;
        this.isDestroyed = false;
        this.bootCompleted = false;

        // DOM 요소들
        this.terminalScreen = null;
        this.bootSequence = null;
        this.terminalInterface = null;
        this.terminalHistory = null;
        this.currentPrompt = null;
        this.currentInput = null;
        this.terminalCursor = null;
        this.focusKeeper = null;

        // 입력 상태
        this.currentInputText = '';
        this.isInputActive = false;
        this.signupStep = 0;
        this.signupData = {};

        // 시스템 설정
        this.promptPrefix = 'guest@tissue:~$ ';
        this.systemName = 'TISSUE Terminal';
        this.version = '1.0.0';

        // 이메일 인증
        this.emailVerificationStatus = 'none'; // none, pending, verified, failed
        this.emailPollingInterval = null;

        // 서버 데이터 로드
        this.loadServerConfig();

        // 초기화 시작
        this.initialize();
    }

    /**
     * 서버 설정 로드
     */
    loadServerConfig() {
        const config = window.TISSUE_CONFIG || {};
        this.emailVerified = config.emailVerified || false;
        this.globalError = config.globalError || null;
        this.validationErrors = config.validationErrors || {};

        console.log('TISSUE Terminal: Config loaded', {
            emailVerified: this.emailVerified,
            hasGlobalError: !!this.globalError
        });
    }

    /**
     * 시스템 초기화
     */
    async initialize() {
        try {
            console.log('TISSUE Terminal: Initializing...');

            // DOM이 준비될 때까지 대기
            if (document.readyState === 'loading') {
                await new Promise(resolve => {
                    document.addEventListener('DOMContentLoaded', resolve);
                });
            }

            // DOM 요소 설정
            this.setupDOMElements();

            // 이벤트 리스너 설정
            this.setupEventListeners();

            // 부팅 시퀀스 시작
            await this.startBootSequence();

            this.isInitialized = true;
            console.log('TISSUE Terminal: Ready');

        } catch (error) {
            console.error('TISSUE Terminal: Initialization failed', error);
            this.showCriticalError('System initialization failed');
        }
    }

    /**
     * DOM 요소 설정
     */
    setupDOMElements() {
        this.terminalScreen = document.getElementById('terminal-screen');
        this.bootSequence = document.getElementById('boot-sequence');
        this.terminalInterface = document.getElementById('terminal-interface');
        this.terminalHistory = document.getElementById('terminal-history');
        this.currentPrompt = document.getElementById('current-prompt');
        this.currentInput = document.getElementById('current-input');
        this.terminalCursor = document.getElementById('terminal-cursor');
        this.focusKeeper = document.getElementById('focus-keeper');

        if (!this.terminalScreen) {
            throw new Error('Terminal screen element not found');
        }

        // 포커스 키퍼 설정
        if (this.focusKeeper) {
            this.focusKeeper.addEventListener('blur', () => {
                // 포커스가 벗어나면 다시 포커스
                setTimeout(() => this.maintainFocus(), 10);
            });
        }
    }

    /**
     * 이벤트 리스너 설정
     */
    setupEventListeners() {
        // 전역 키보드 이벤트
        document.addEventListener('keydown', (e) => this.handleGlobalKeypress(e), true);

        // 전역 클릭 이벤트 - 어디를 클릭해도 터미널에 포커스
        document.addEventListener('click', () => this.maintainFocus());

        // 윈도우 포커스 이벤트
        window.addEventListener('focus', () => this.maintainFocus());

        // 페이지 언로드 시 정리
        window.addEventListener('beforeunload', () => this.cleanup());

        // 복사/붙여넣기 지원
        document.addEventListener('paste', (e) => this.handlePaste(e));

        // 터미널 화면 클릭 시 포커스 유지
        if (this.terminalScreen) {
            this.terminalScreen.addEventListener('click', (e) => {
                e.preventDefault();
                this.maintainFocus();
            });
        }
    }

    /**
     * 부팅 시퀀스 실행
     */
    async startBootSequence() {
        // 베너는 그대로 두고, 그 아래에 터미널 인터페이스 추가
        await this.delay(1000); // 부팅 애니메이션 완료까지 대기

        // 터미널 인터페이스를 베너 아래에 표시
        if (this.terminalInterface) {
            this.terminalInterface.style.display = 'block';
        }

        this.bootCompleted = true;

        // 포커스 설정
        this.maintainFocus();

        // 환영 메시지 출력(deprecated 예정)
        await this.showWelcomeMessage();

        // 환영 메시지 없이 바로 명령어 입력 모드로(커맨드 설정 추가 후 사용)
        // this.startCommandMode();

        // 회원가입 프로세스 시작(커맨드를 사용하는 경우 제거)
        this.startSignupProcess();
    }

    /**
     * 환영 메시지 표시
     */
    async showWelcomeMessage() {
        await this.delay(500);

        // 텍스트를 여러 부분으로 나누어서 처리
        const helpLine = document.createElement('div');
        helpLine.className = 'history-line default-system-text';
        helpLine.innerHTML = 'Type <span class="command-highlight">\'help\'</span> to see the list of available commands.';
        this.terminalHistory.appendChild(helpLine);

        this.addHistoryLine('', ''); // 빈 줄
        this.addHistoryLine('\n', ''); // 빈 줄

        // 서버 에러가 있으면 표시
        if (this.globalError) {
            this.addHistoryLine('⚠ Previous registration attempt failed:', 'error-msg');
            this.addHistoryLine(`  ${this.globalError}`, 'error-msg');
            this.addHistoryLine('', '');
        }

        await this.delay(500);
    }

    /**
     * 회원가입 프로세스 시작
     */
    startSignupProcess() {
        this.addHistoryLine('Starting user registration wizard...', 'info-msg');
        this.addHistoryLine('Type your information when prompted. Use Ctrl+C to cancel.', 'system-msg');
        this.addHistoryLine('', '');

        // 첫 번째 단계 시작
        setTimeout(() => this.nextSignupStep(), 1000);
    }

    /**
     * 다음 회원가입 단계
     */
    nextSignupStep() {
        const steps = [
            { field: 'loginId', prompt: 'Login ID', desc: '4-20 alphanumeric characters', required: true },
            { field: 'email', prompt: 'Email', desc: 'Valid email address (verification required)', required: true },
            { field: 'username', prompt: 'Username', desc: 'Display name (2-30 characters)', required: true },
            { field: 'password', prompt: 'Password', desc: 'Min 8 chars with letters, numbers, symbols', required: true, sensitive: true },
            { field: 'confirmPassword', prompt: 'Confirm Password', desc: 'Re-enter your password', required: true, sensitive: true },
            { field: 'firstName', prompt: 'First Name', desc: 'Given name (optional)', required: false },
            { field: 'lastName', prompt: 'Last Name', desc: 'Family name (optional)', required: false },
            { field: 'birthDate', prompt: 'Birth Date', desc: 'YYYY-MM-DD format (optional)', required: false },
            { field: 'jobType', prompt: 'Job Type', desc: 'Your profession (optional)', required: false },
            { field: 'biography', prompt: 'Biography', desc: 'Brief description (optional)', required: false }
        ];

        if (this.signupStep >= steps.length) {
            this.completeSignup();
            return;
        }

        const step = steps[this.signupStep];
        const progress = Math.round((this.signupStep / steps.length) * 100);

        // 진행률 표시
        this.addHistoryLine(`[${progress}%] ${'▓'.repeat(Math.floor(progress/5))}${'░'.repeat(20-Math.floor(progress/5))}`, 'progress-line');
        this.addHistoryLine('', '');

        // 단계 정보 표시
        const requiredText = step.required ? ' *' : ' (optional)';
        this.addHistoryLine(`Step ${this.signupStep + 1}/${steps.length}: ${step.prompt}${requiredText}`, 'info-msg');
        this.addHistoryLine(`${step.desc}`, 'field-description');

        if (!step.required) {
            this.addHistoryLine('Press Enter with empty input to skip', 'field-description');
        }

        this.addHistoryLine('', '');

        // 이메일 단계 특별 처리
        if (step.field === 'email' && this.emailVerified && this.signupData.email) {
            this.addHistoryLine(`✓ Email already verified: ${this.signupData.email}`, 'success-msg');
            this.addHistoryLine('', '');
            this.signupStep++;
            setTimeout(() => this.nextSignupStep(), 1000);
            return;
        }

        // 입력 프롬프트 활성화
        this.activateInput(step.prompt + ': ', step);
    }

    /**
     * 입력 활성화
     */
    activateInput(promptText, stepInfo) {
        this.isInputActive = true;
        this.currentStepInfo = stepInfo;
        this.currentInputText = '';

        // 프롬프트 텍스트 업데이트
        if (this.currentPrompt) {
            this.currentPrompt.querySelector('.prompt-prefix').textContent = promptText;
        }

        this.updateInputDisplay();
        this.maintainFocus();
        this.scrollToBottom();
    }

    /**
     * 전역 키 입력 처리
     */
    handleGlobalKeypress(event) {
        // 부팅이 완료되지 않았으면 무시
        if (!this.bootCompleted) {
            return;
        }

        // 입력이 활성화되지 않았으면 일부 키만 처리
        if (!this.isInputActive) {
            if (event.ctrlKey && event.key.toLowerCase() === 'c') {
                event.preventDefault();
                this.showExitPrompt();
            }
            return;
        }

        const step = this.currentStepInfo;
        if (!step) return;

        // 기본 동작 방지
        event.preventDefault();

        if (event.key === 'Enter') {
            this.processCurrentInput();
        } else if (event.key === 'Backspace') {
            this.handleBackspace();
        } else if (event.ctrlKey && event.key.toLowerCase() === 'c') {
            this.handleCancel();
        } else if (event.ctrlKey && event.key.toLowerCase() === 'l') {
            this.clearScreen();
        } else if (event.key === 'Tab') {
            this.showFieldHint();
        } else if (event.key.length === 1 && !event.ctrlKey && !event.altKey && !event.metaKey) {
            // 일반 문자 입력
            this.addCharacterToInput(event.key);
        }
    }

    /**
     * 입력에 문자 추가
     */
    addCharacterToInput(char) {
        this.currentInputText += char;
        this.updateInputDisplay();
    }

    /**
     * 백스페이스 처리
     */
    handleBackspace() {
        if (this.currentInputText.length > 0) {
            this.currentInputText = this.currentInputText.slice(0, -1);
            this.updateInputDisplay();
        }
    }

    /**
     * 입력 표시 업데이트
     */
    updateInputDisplay() {
        if (!this.currentInput) return;

        const step = this.currentStepInfo;
        if (step && step.sensitive) {
            // 패스워드 필드는 마스킹
            this.currentInput.innerHTML = `<span class="password-mask">${'*'.repeat(this.currentInputText.length)}</span>`;
        } else {
            this.currentInput.textContent = this.currentInputText;
        }

        // 커서 위치 조정
        this.refreshCursor();
    }

    /**
     * 커서 새로고침
     */
    refreshCursor() {
        if (this.terminalCursor) {
            // 커서 애니메이션 재시작
            this.terminalCursor.style.animation = 'none';
            this.terminalCursor.offsetHeight; // 강제 리플로우
            this.terminalCursor.style.animation = 'terminalBlink 1s infinite';
        }
    }

    /**
     * 현재 입력 처리
     */
    async processCurrentInput() {
        const step = this.currentStepInfo;
        const value = this.currentInputText.trim();

        // 입력 비활성화
        this.isInputActive = false;

        // 입력 내용을 히스토리에 추가
        const promptText = this.currentPrompt.querySelector('.prompt-prefix').textContent;
        const displayValue = step.sensitive ? '*'.repeat(this.currentInputText.length) : this.currentInputText;
        this.addHistoryLine(promptText + displayValue, 'history-line');

        // 필수 필드 검증
        if (step.required && value === '') {
            this.addHistoryLine('✗ This field is required', 'error-msg');
            this.addHistoryLine('', '');
            setTimeout(() => this.activateInput(promptText, step), 500);
            return;
        }

        // 선택 필드이고 빈 값이면 스킵
        if (!step.required && value === '') {
            this.addHistoryLine('⊝ Skipped', 'warning-msg');
            this.addHistoryLine('', '');
            this.signupStep++;
            setTimeout(() => this.nextSignupStep(), 500);
            return;
        }

        // 유효성 검사
        const validation = await this.validateField(step.field, value);
        if (!validation.valid) {
            this.addHistoryLine(`✗ ${validation.error}`, 'error-msg');
            this.addHistoryLine('', '');
            setTimeout(() => this.activateInput(promptText, step), 500);
            return;
        }

        // 값 저장
        this.signupData[step.field] = value;

        // 성공 메시지
        this.addHistoryLine(`✓ ${step.field}: ${step.sensitive ? '[HIDDEN]' : value}`, 'success-msg');

        // 이메일 인증 처리
        if (step.field === 'email') {
            await this.handleEmailVerification(value);
        } else {
            this.addHistoryLine('', '');
            this.signupStep++;
            setTimeout(() => this.nextSignupStep(), 500);
        }
    }

    /**
     * 필드 유효성 검사
     */
    async validateField(fieldName, value) {
        switch (fieldName) {
            case 'loginId':
                if (!/^[a-zA-Z0-9_]{4,20}$/.test(value)) {
                    return { valid: false, error: 'Login ID must be 4-20 characters (letters, numbers, underscore only)' };
                }
                break;

            case 'email':
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                    return { valid: false, error: 'Please enter a valid email address' };
                }
                break;

            case 'username':
                if (value.length < 2 || value.length > 30) {
                    return { valid: false, error: 'Username must be 2-30 characters long' };
                }
                break;

            case 'password':
                if (value.length < 8) {
                    return { valid: false, error: 'Password must be at least 8 characters long' };
                }
                if (!/(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>])/.test(value)) {
                    return { valid: false, error: 'Password must contain letters, numbers, and symbols' };
                }
                break;

            case 'confirmPassword':
                if (value !== this.signupData.password) {
                    return { valid: false, error: 'Passwords do not match' };
                }
                break;

            case 'birthDate':
                if (value && !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
                    return { valid: false, error: 'Please use YYYY-MM-DD format' };
                }
                if (value && new Date(value) >= new Date()) {
                    return { valid: false, error: 'Birth date must be in the past' };
                }
                break;
        }

        return { valid: true };
    }

    /**
     * 이메일 인증 처리
     */
    async handleEmailVerification(email) {
        this.addHistoryLine('', '');
        this.addHistoryLine('📧 Sending verification email...', 'info-msg');

        try {
            const headers = { 'Content-Type': 'application/json' };

            const response = await fetch('/api/v1/members/email-verification/request', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ email: email })
            });

            if (response.ok) {
                this.addHistoryLine('✓ Verification email sent successfully!', 'success-msg');
                this.addHistoryLine('', '');
                this.addHistoryLine('⏳ Waiting for email verification...', 'warning-msg');
                this.addHistoryLine('   Check your email and click the verification link', 'system-msg');
                this.addHistoryLine('   This process will continue automatically', 'system-msg');
                this.addHistoryLine('', '');

                this.startEmailPolling(email);
            } else {
                const errorData = await response.json().catch(() => null);
                const errorMessage = errorData?.message || 'Failed to send verification email';
                this.addHistoryLine(`✗ ${errorMessage}`, 'error-msg');
                this.addHistoryLine('', '');

                // 다시 입력받기
                const promptText = 'Email: ';
                setTimeout(() => this.activateInput(promptText, this.currentStepInfo), 1000);
            }
        } catch (error) {
            console.error('Email verification request failed:', error);
            this.addHistoryLine('✗ Network error occurred', 'error-msg');
            this.addHistoryLine('', '');

            const promptText = 'Email: ';
            setTimeout(() => this.activateInput(promptText, this.currentStepInfo), 1000);
        }
    }

    /**
     * 이메일 인증 폴링 시작
     */
    startEmailPolling(email) {
        if (this.emailPollingInterval) {
            clearInterval(this.emailPollingInterval);
        }

        let attempts = 0;
        const maxAttempts = 300; // 5분

        this.emailPollingInterval = setInterval(async () => {
            attempts++;

            if (attempts >= maxAttempts) {
                clearInterval(this.emailPollingInterval);
                this.addHistoryLine('⏰ Email verification timeout', 'warning-msg');
                this.addHistoryLine('   Please try again or contact support', 'system-msg');
                this.addHistoryLine('', '');
                return;
            }

            try {
                const response = await fetch(`/api/v1/members/email-verification/status?email=${encodeURIComponent(email)}`);

                if (response.ok) {
                    const data = await response.json();

                    if (data.data === true) {
                        clearInterval(this.emailPollingInterval);
                        this.emailVerificationStatus = 'verified';

                        this.addHistoryLine('✅ Email verified successfully!', 'success-msg');
                        this.addHistoryLine('', '');

                        this.signupStep++;
                        setTimeout(() => this.nextSignupStep(), 1000);
                    }
                }
            } catch (error) {
                console.error('Email verification polling error:', error);
            }
        }, 1000);
    }

    /**
     * 회원가입 완료
     */
    async completeSignup() {
        this.addHistoryLine('', '');
        this.addHistoryLine('🔄 Processing registration...', 'info-msg');
        this.addHistoryLine('   Creating your account in the system...', 'system-msg');

        // 타이핑 효과로 완료 메시지 출력
        await this.delay(1000);
        await this.typeMessage('Registration completed successfully!', 'success-msg');

        this.addHistoryLine('', '');
        this.addHistoryLine('🎉 Welcome to TISSUE!', 'success-msg');
        this.addHistoryLine('   Submitting registration data...', 'info-msg');

        // 폼 데이터 설정 및 제출
        this.populateHiddenForm();

        setTimeout(() => {
            const form = document.getElementById('hidden-signup-form');
            if (form) {
                form.submit();
            } else {
                this.addHistoryLine('✗ Form submission failed', 'error-msg');
            }
        }, 2000);
    }

    /**
     * 숨겨진 폼에 데이터 채우기
     */
    populateHiddenForm() {
        Object.keys(this.signupData).forEach(key => {
            if (key === 'confirmPassword') return; // 서버로 전송하지 않음

            const field = document.getElementById(`form-${key}`);
            if (field) {
                field.value = this.signupData[key] || '';
            }
        });
    }

    /**
     * 유틸리티 메서드들
     */

    /**
     * 히스토리 라인 추가
     */
    addHistoryLine(text, className = 'history-output') {
        if (!this.terminalHistory) return;

        const line = document.createElement('div');
        line.className = `history-line ${className}`;
        line.textContent = text;

        this.terminalHistory.appendChild(line);
        this.scrollToBottom();
    }

    /**
     * 타이핑 효과로 메시지 출력
     */
    async typeMessage(text, className = 'history-output', speed = 50) {
        const line = document.createElement('div');
        line.className = `history-line ${className}`;
        this.terminalHistory.appendChild(line);

        for (let i = 0; i <= text.length; i++) {
            line.textContent = text.substring(0, i);
            this.scrollToBottom();
            await this.delay(speed);
        }
    }

    /**
     * 화면 스크롤을 아래로
     */
    scrollToBottom() {
        if (this.terminalScreen) {
            this.terminalScreen.scrollTop = this.terminalScreen.scrollHeight;
        }
    }

    /**
     * 포커스 유지
     */
    maintainFocus() {
        if (this.focusKeeper && document.activeElement !== this.focusKeeper) {
            try {
                this.focusKeeper.focus();
            } catch (error) {
                console.warn('Focus maintenance failed:', error);
            }
        }
    }

    /**
     * 지연 함수
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * 필드 힌트 표시
     */
    showFieldHint() {
        const step = this.currentStepInfo;
        if (step) {
            this.addHistoryLine('', '');
            this.addHistoryLine(`💡 ${step.desc}`, 'info-msg');
            if (!step.required) {
                this.addHistoryLine('   This field is optional - press Enter to skip', 'system-msg');
            }
            this.addHistoryLine('', '');
        }
    }

    /**
     * 취소 처리
     */
    handleCancel() {
        this.addHistoryLine('', '');
        this.addHistoryLine('^C', 'system-msg');
        this.addHistoryLine('Registration cancelled by user', 'warning-msg');
        this.addHistoryLine('', '');
        this.showExitPrompt();
    }

    /**
     * 종료 프롬프트 표시
     */
    showExitPrompt() {
        this.isInputActive = false;
        this.addHistoryLine('Do you want to exit? (y/N)', 'warning-msg');

        const handleExitResponse = (event) => {
            event.preventDefault();
            const key = event.key.toLowerCase();

            if (key === 'y') {
                this.addHistoryLine('y', 'history-output');
                this.addHistoryLine('', '');
                this.addHistoryLine('Goodbye!', 'system-msg');
                setTimeout(() => {
                    window.location.href = '/';
                }, 1000);
            } else if (key === 'n' || key === 'enter') {
                this.addHistoryLine('n', 'history-output');
                this.addHistoryLine('', '');
                this.addHistoryLine('Registration resumed', 'info-msg');
                this.addHistoryLine('', '');
                document.removeEventListener('keydown', handleExitResponse, true);

                // 현재 단계로 돌아가기
                setTimeout(() => {
                    const step = this.currentStepInfo;
                    if (step) {
                        this.activateInput(step.prompt + ': ', step);
                    } else {
                        this.nextSignupStep();
                    }
                }, 500);
            }
        };

        document.addEventListener('keydown', handleExitResponse, true);
    }

    /**
     * 화면 지우기
     */
    clearScreen() {
        if (this.terminalHistory) {
            this.terminalHistory.innerHTML = '';
        }
        this.addHistoryLine('Screen cleared', 'system-msg');
        this.addHistoryLine('', '');
    }

    /**
     * 붙여넣기 처리
     */
    handlePaste(event) {
        if (!this.isInputActive) return;

        event.preventDefault();
        const pastedText = event.clipboardData.getData('text/plain');

        // 여러 줄 텍스트는 첫 번째 줄만 사용
        const singleLineText = pastedText.split('\n')[0];

        this.currentInputText += singleLineText;
        this.updateInputDisplay();
    }

    /**
     * 치명적 에러 표시
     */
    showCriticalError(message) {
        document.body.innerHTML = `
            <div style="background: #000; color: #ff0000; font-family: monospace; padding: 20px; height: 100vh;">
                <h1>TISSUE Terminal - Critical Error</h1>
                <p>${message}</p>
                <p>Please refresh the page or contact support.</p>
            </div>
        `;
    }

    /**
     * 리소스 정리
     */
    cleanup() {
        console.log('TISSUE Terminal: Cleaning up...');

        this.isDestroyed = true;

        if (this.emailPollingInterval) {
            clearInterval(this.emailPollingInterval);
        }

        // 이벤트 리스너들은 페이지 언로드 시 자동으로 정리됨
    }
}

// 전역 인스턴스
let terminal = null;

/**
 * 시스템 초기화
 */
document.addEventListener('DOMContentLoaded', () => {
    try {
        console.log('TISSUE Terminal: Starting system...');
        terminal = new TissueTerminal();
    } catch (error) {
        console.error('TISSUE Terminal: Critical startup failure', error);

        // 폴백 에러 화면
        document.body.innerHTML = `
            <div style="background: #000; color: #ff0000; font-family: monospace; padding: 20px; height: 100vh;">
                <h1>TISSUE Terminal - Startup Failed</h1>
                <p>The terminal system could not be initialized.</p>
                <p>Error: ${error.message}</p>
                <p><a href="/" style="color: #00ff00;">Return to homepage</a></p>
            </div>
        `;
    }
});

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
    if (terminal && !terminal.isDestroyed) {
        terminal.cleanup();
    }
});

// 개발자 도구용 전역 접근
if (typeof window !== 'undefined') {
    window.TISSUE_TERMINAL = terminal;
}