// TODO-1: 매직넘버나 스트링을 상수로 분리
// TODO-2: 설정값을 서버에서 주입받아서 사용(베너, 버전, author, license, 등...)
// TODO-3: ✓ vs ✅ 어떤걸 사용? 아니면 마지막만 ✅를 사용할까?
// TODO-4: ✗ vs ❌
// TODO-5: handleKeyPress에서 handleSignupKeyPress 대신 handleSpecialModeKeyPress 사용하도록 변경
// TODO-6: exit 명령어 사용시, 창을 나갈지 물어보는 모달 보여주고, 창 나가기
// TODO-7: help [command]를 사용하면 자세한 설명 출력하기
// TODO-8: vi, vim, emac 등의 명령어 사용하면 터미널 처럼 편집기 모드로 들어가짐 -> 여기서 글을 작성해서 저장하면 글이 저장됨
// TODO-9: ls 명령어를 통해 저장한 글 조회 기능?(50개 까지 보여주기, 페이징 적용)
// TODO-10: 내가 작성한 글 보는 기능?
// TODO-11: 모든 API 요청에 대한 공통 함수 만들어서 사용?(credentials: "include" 적용)
// TODO-12: 테마 추가(라이트모드, 다크모드, 등..)
// TODO-13: JobType 목록을 서버에서 가져오기/캐싱
// TODO-14: 필드에 대한 검증 로직을 클라이언트 사이드에서 다시 정의해서 사용하고 있음
// - 서버사이드에서 규칙을 가져오는 방법은 없을까?(SSOT으로 관리하고 싶음)
// - properties 파일을 하나 만들어서 규칙을 외부에서 주입하는 방식 고려
// TOOD-15: 코드 가독성 리팩토링
// TOOD-16: JS 모듈화

/**
 * TISSUE Terminal System
 * 완전한 명령어 기반 터미널 인터페이스
 */
class TissueTerminal {
  constructor() {
    // 시스템 상태
    this.isInitialized = false;
    this.isDestroyed = false;
    this.bootCompleted = false;

    // DOM 요소들
    this.terminalScreen = null;
    this.terminalHistory = null;
    this.currentPrompt = null;
    this.currentInput = null;
    this.terminalCursor = null;
    this.focusKeeper = null;

    // 입력 상태
    this.currentInputText = "";
    this.commandHistory = [];
    this.historyIndex = -1;

    // 시스템 설정
    this.promptPrefix = "guest@tissue:~$ ";
    this.systemName = "TISSUE Terminal";

    // 회원가입 관련 상태 변수들
    this.signupInProgress = false;
    this.signupStep = 0;
    this.signupData = {};
    this.currentFieldInfo = null;

    // 이메일 인증 관련
    this.emailVerificationStatus = "none";
    this.emailPollingInterval = null;

    // 로그인 관련 상태
    this.isLoggedIn = false;
    this.currentUser = null;
    this.loginInProgress = false;
    this.loginStep = 0;
    this.loginData = {};

    // 프로필 수정 관련 상태
    this.editInProgress = false;
    this.editStep = 0;
    this.editData = {};
    this.editFieldInfo = null;

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
    this.systemInfo = config.systemInfo || {
      version: "1.0.0",
      repository: "github.com/your-username/tissue",
      author: "Your Name",
      email: "your.email@example.com",
      license: "MIT",
      documentation: "tissue.docs.example.com",
    };

    console.log("TISSUE Terminal: Config loaded", this.systemInfo);
  }

  /**
   * 시스템 초기화
   */
  async initialize() {
    try {
      console.log("TISSUE Terminal: Initializing...");

      // DOM이 준비될 때까지 대기
      if (document.readyState === "loading") {
        await new Promise((resolve) => {
          document.addEventListener("DOMContentLoaded", resolve);
        });
      }

      // DOM 요소 설정
      this.setupDOMElements();

      // 이벤트 리스너 설정
      this.setupEventListeners();

      // 부팅 완료
      this.bootCompleted = true;
      this.isInitialized = true;

      // 포커스 설정
      this.maintainFocus();

      // 자동으로 banner 명령어 실행 (초기 환영 메시지)
      await this.executeCommand("banner");

      console.log("TISSUE Terminal: Ready");
    } catch (error) {
      console.error("TISSUE Terminal: Initialization failed", error);
      this.showCriticalError("System initialization failed");
    }
  }

  /**
   * DOM 요소 설정
   */
  setupDOMElements() {
    this.terminalScreen = document.getElementById("terminal-screen");
    this.terminalHistory = document.getElementById("terminal-history");
    this.currentPrompt = document.getElementById("current-prompt");
    this.currentInput = document.getElementById("current-input");
    this.terminalCursor = document.getElementById("terminal-cursor");
    this.focusKeeper = document.getElementById("focus-keeper");

    if (!this.terminalScreen) {
      throw new Error("Terminal screen element not found");
    }

    // 포커스 키퍼 설정
    if (this.focusKeeper) {
      this.focusKeeper.addEventListener("blur", () => {
        setTimeout(() => this.maintainFocus(), 10);
      });
    }
  }

  /**
   * 이벤트 리스너 설정
   */
  setupEventListeners() {
    // 전역 키보드 이벤트
    document.addEventListener("keydown", (e) => this.handleKeyPress(e), true);

    // 전역 클릭 이벤트 - 어디를 클릭해도 터미널에 포커스
    document.addEventListener("click", () => this.maintainFocus());

    // 윈도우 포커스 이벤트
    window.addEventListener("focus", () => this.maintainFocus());

    // 페이지 언로드 시 정리
    window.addEventListener("beforeunload", () => this.cleanup());

    // 복사/붙여넣기 지원
    document.addEventListener("paste", (e) => this.handlePaste(e));

    // 터미널 화면 클릭 시 포커스 유지
    if (this.terminalScreen) {
      this.terminalScreen.addEventListener("click", (e) => {
        e.preventDefault();
        this.maintainFocus();
      });
    }
  }

  /**
   * 키 입력 처리
   */
  handleKeyPress(event) {
    if (!this.bootCompleted) return;

    // 기본 동작 방지
    event.preventDefault();

    // 특별한 모드(회원가입, 로그인, 수정) 중인 경우 별도 처리
    if (this.signupInProgress || this.loginInProgress || this.editInProgress) {
      this.handleSpecialModeKeyPress(event);
      return;
    }

    if (event.key === "Enter") {
      this.processCommand();
    } else if (event.key === "Backspace") {
      this.handleBackspace();
    } else if (event.key === "ArrowUp") {
      this.navigateHistory(-1);
    } else if (event.key === "ArrowDown") {
      this.navigateHistory(1);
    } else if (event.ctrlKey && event.key.toLowerCase() === "l") {
      this.executeCommand("clear");
    } else if (event.ctrlKey && event.key.toLowerCase() === "c") {
      this.handleCancel();
    } else if (event.key === "Tab") {
      this.handleTabCompletion();
    } else if (
      event.key.length === 1 &&
      !event.ctrlKey &&
      !event.altKey &&
      !event.metaKey
    ) {
      this.addCharacterToInput(event.key);
    }
  }

  /**
   * 명령어 처리
   */
  async processCommand() {
    const command = this.currentInputText.trim();

    // 명령어를 히스토리에 추가
    this.addCommandToHistory(command);

    if (command) {
      // 명령어 히스토리에 저장
      this.commandHistory.unshift(command);
      if (this.commandHistory.length > 100) {
        this.commandHistory.pop();
      }
    }

    this.historyIndex = -1;
    this.currentInputText = "";
    this.updateInputDisplay();

    // 명령어 실행
    if (command) {
      await this.executeCommand(command);
    }
  }

  /**
   * 명령어 실행
   */
  async executeCommand(commandText) {
    const [commandName, ...args] = commandText.split(" ");
    const command = this.commands[commandName];

    if (command) {
      try {
        const result = await command.call(this, args);
        if (result) {
          this.addHistoryLine(result, "history-output");
        }
      } catch (error) {
        console.error(`Command execution failed: ${commandName}`, error);
        this.addHistoryLine(
          `Error executing command: ${error.message}`,
          "error-msg"
        );
      }
    } else {
      this.addHistoryLine(`${commandName}: command not found`, "error-msg");
      this.addHistoryLine(
        `Type 'help' to see available commands.`,
        "system-msg"
      );
    }

    this.addHistoryLine("", ""); // 빈 줄 추가
  }

  /**
   * 사용 가능한 명령어들
   */
  commands = {
    // 베너 출력 명령어
    banner: function () {
      this.displayBanner();
      return null;
    },

    // 화면 지우기 명령어
    clear: function () {
      this.clearTerminal();
      return null;
    },

    // 도움말 명령어
    help: function () {
      const commandList = Object.keys(this.commands).sort();
      const helpText = [
        "Available commands:",
        "",
        ...commandList.map(
          (cmd) => `  ${cmd.padEnd(12)} - ${this.getCommandDescription(cmd)}`
        ),
        "",
        "Use Ctrl+L to clear screen, Ctrl+C to cancel input.",
        "Use Tab for command completion, Up/Down arrows for command history.",
      ].join("\n");

      return helpText;
    },

    // 시스템 정보 명령어
    info: function () {
      this.displaySystemInfo();
      return null;
    },

    // 버전 정보 명령어
    version: function () {
      return `TISSUE Terminal v${this.systemInfo.version}`;
    },

    // 현재 시간 명령어
    date: function () {
      return new Date().toLocaleString();
    },

    // 에코 명령어
    echo: function (args) {
      return args.join(" ");
    },

    // 사용자 정보 명령어 (수정: 로그인 상태 반영)
    whoami: function () {
      if (this.isLoggedIn && this.currentUser) {
        return this.currentUser.username;
      }
      return "guest";
    },

    // 종료 명령어
    exit: function () {
      this.addHistoryLine("Goodbye!", "success-msg");
      setTimeout(() => {
        window.location.href = "/";
      }, 1000);
      return null;
    },

    signup: function (args) {
      if (this.signupInProgress) {
        return "Signup process is already in progress. Use Ctrl+C to cancel.";
      }
      this.startSignupProcess();
      return null;
    },

    login: function (args) {
      if (this.loginInProgress) {
        return "Login process is already in progress. Use Ctrl+C to cancel.";
      }
      this.startLoginProcess();
      return null;
    },

    logout: function (args) {
      if (!this.isLoggedIn) {
        return "You are not logged in.";
      }
      this.performLogout();
      return null;
    },

    profile: function (args) {
      if (!this.isLoggedIn) {
        return "Please login first to view your profile.";
      }
      this.displayUserProfile();
      return null;
    },

    edit: function (args) {
      if (!this.isLoggedIn) {
        return "Please login first to edit your profile.";
      }
      if (this.editInProgress) {
        return "Profile editing is already in progress. Use Ctrl+C to cancel.";
      }
      this.startEditProcess(args);
      return null;
    },

    status: function (args) {
      if (this.isLoggedIn) {
        return `Logged in as: ${this.currentUser.username} (${this.currentUser.email})`;
      } else {
        return "Not logged in (guest session)";
      }
    },
  };

  /**
   * 명령어 설명 반환
   */
  getCommandDescription(commandName) {
    const descriptions = {
      banner: "Display system banner and information",
      clear: "Clear the terminal screen",
      help: "Show this help message",
      info: "Display system information",
      version: "Show current version of tissue",
      date: "Display current date and time",
      echo: "Echo the given text",
      whoami: "Display current username",
      exit: "Exit the terminal",
      signup: "Create a new user account",
      login: "Sign in to your account",
      logout: "Sign out from your account",
      profile: "View your profile information",
      edit: "Edit profile information",
      status: "Show current login status",
    };
    return descriptions[commandName] || "No description available";
  }

  /**
   * 베너 출력
   */
  displayBanner() {
    // ASCII 아트 베너
    const bannerAscii = `████████╗███████╗██████╗ ███╗   ███╗██╗███╗   ██╗ █████╗ ██╗
╚══██╔══╝██╔════╝██╔══██╗████╗ ████║██║████╗  ██║██╔══██╗██║
   ██║   █████╗  ██████╔╝██╔████╔██║██║██╔██╗ ██║███████║██║
   ██║   ██╔══╝  ██╔══██╗██║╚██╔╝██║██║██║╚██╗██║██╔══██║██║
   ██║   ███████╗██║  ██║██║ ╚═╝ ██║██║██║ ╚████║██║  ██║███████╗
   ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝
   ██╗███████╗███████╗██╗   ██╗███████╗
   ██║██╔════╝██╔════╝██║   ██║██╔════╝
   ██║███████╗███████╗██║   ██║█████╗
   ██║╚════██║╚════██║██║   ██║██╔══╝
   ██║███████║███████║╚██████╔╝███████╗
   ╚═╝╚══════╝╚══════╝ ╚═════╝ ╚══════╝`;

    // 베너 컨테이너 생성
    const bannerContainer = document.createElement("div");
    bannerContainer.className = "banner-container";

    // ASCII 아트 요소
    const bannerElement = document.createElement("pre");
    bannerElement.className = "ascii-banner";
    bannerElement.textContent = bannerAscii;

    // 버전 정보 요소
    const versionElement = document.createElement("span");
    versionElement.className = "version-info";
    versionElement.textContent = ` ver${this.systemInfo.version}`;

    // 베너 요소들을 컨테이너에 추가
    bannerContainer.appendChild(bannerElement);
    bannerContainer.appendChild(versionElement);

    // 터미널 히스토리에 추가
    this.terminalHistory.appendChild(bannerContainer);

    // 시스템 정보 출력
    this.displaySystemInfo();

    // 텍스트를 여러 부분으로 나누어서 처리
    const helpLine = document.createElement("div");
    helpLine.className = "help-msg";
    helpLine.innerHTML =
      "Type <span class=\"command-highlight\">'help'</span> to see the list of available commands.";
    this.terminalHistory.appendChild(helpLine);

    this.addHistoryLine("", "");
    this.addHistoryLine("\n", "");

    this.scrollToBottom();
  }

  /**
   * 시스템 정보 출력
   */
  displaySystemInfo() {
    const infoContainer = document.createElement("div");
    infoContainer.className = "system-info-container";

    // 제목
    const title = document.createElement("div");
    title.className = "system-info-title";
    title.textContent = "Terminal Issue Management & Collaboration";
    infoContainer.appendChild(title);

    // 빈 줄
    const emptyLine = document.createElement("div");
    emptyLine.className = "system-info-line";
    emptyLine.innerHTML = "&nbsp;";
    infoContainer.appendChild(emptyLine);

    // 시스템 정보 항목들
    const infoItems = [
      { label: "Repository:", value: this.systemInfo.repository },
      {
        label: "Author:",
        value: `${this.systemInfo.author} <${this.systemInfo.email}>`,
      },
      { label: "License:", value: this.systemInfo.license },
      { label: "Documentation:", value: this.systemInfo.documentation },
    ];

    infoItems.forEach((item) => {
      const line = document.createElement("div");
      line.className = "system-info-line";

      const label = document.createElement("span");
      label.className = "info-label";
      label.textContent = item.label;

      const value = document.createElement("span");
      value.className = "info-value";
      value.textContent = item.value;

      line.appendChild(label);
      line.appendChild(value);
      infoContainer.appendChild(line);
    });

    this.terminalHistory.appendChild(infoContainer);
    this.scrollToBottom();
  }

  /**
   * 터미널 화면 지우기
   */
  clearTerminal() {
    if (this.terminalHistory) {
      this.terminalHistory.innerHTML = "";
    }
  }

  /**
   * 명령어를 히스토리에 추가
   */
  addCommandToHistory(command) {
    const line = document.createElement("div");
    line.className = "history-line";

    const prompt = document.createElement("span");
    prompt.className = "history-prompt";
    prompt.textContent = this.promptPrefix;

    const commandSpan = document.createElement("span");
    commandSpan.className = "history-command";
    commandSpan.textContent = command;

    line.appendChild(prompt);
    line.appendChild(commandSpan);

    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
  }

  /**
   * 히스토리 라인 추가
   */
  addHistoryLine(text, className = "history-output") {
    if (!this.terminalHistory) return;

    const line = document.createElement("div");
    line.className = `history-line ${className}`;
    line.textContent = text;

    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
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
   * 명령어 히스토리 탐색
   */
  navigateHistory(direction) {
    if (this.commandHistory.length === 0) return;

    this.historyIndex += direction;

    if (this.historyIndex < -1) {
      this.historyIndex = -1;
    } else if (this.historyIndex >= this.commandHistory.length) {
      this.historyIndex = this.commandHistory.length - 1;
    }

    if (this.historyIndex === -1) {
      this.currentInputText = "";
    } else {
      this.currentInputText = this.commandHistory[this.historyIndex];
    }

    this.updateInputDisplay();
  }

  /**
   * 탭 완성
   */
  handleTabCompletion() {
    const input = this.currentInputText.trim();
    if (!input) return;

    const commands = Object.keys(this.commands);
    const matches = commands.filter((cmd) => cmd.startsWith(input));

    if (matches.length === 1) {
      this.currentInputText = matches[0] + " ";
      this.updateInputDisplay();
    } else if (matches.length > 1) {
      this.addHistoryLine("", "");
      this.addHistoryLine("Available completions:", "info-msg");
      this.addHistoryLine(matches.join("  "), "system-msg");
      this.addHistoryLine("", "");
    }
  }

  /**
   * 취소 처리 (Ctrl+C)
   */
  handleCancel() {
    if (this.currentInputText) {
      this.addCommandToHistory(this.currentInputText + "^C");
      this.currentInputText = "";
      this.updateInputDisplay();
    }
  }

  /**
   * 입력 표시 업데이트 (수정: 모든 특별 모드 마스킹 처리)
   */
  updateInputDisplay() {
    if (!this.currentInput) return;

    // 특별 모드 중이고 민감한 필드인 경우 마스킹 처리
    if (
      (this.signupInProgress || this.loginInProgress || this.editInProgress) &&
      this.currentFieldInfo?.sensitive
    ) {
      this.updateMaskedInputDisplay();
    } else {
      this.currentInput.textContent = this.currentInputText;
      this.refreshCursor();
    }
  }

  /**
   * 패스워드 필드용 마스킹된 입력 표시 업데이트
   */
  updateMaskedInputDisplay() {
    if (!this.currentInput) return;

    // 실제 입력 텍스트 길이만큼 * 표시
    const maskedText = "*".repeat(this.currentInputText.length);
    this.currentInput.textContent = maskedText;
    this.refreshCursor();
  }

  /**
   * 커서 새로고침
   */
  refreshCursor() {
    if (this.terminalCursor) {
      this.terminalCursor.style.animation = "none";
      this.terminalCursor.offsetHeight; // 강제 리플로우
      this.terminalCursor.style.animation = "terminalBlink 1s infinite";
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
        console.warn("Focus maintenance failed:", error);
      }
    }
  }

  /**
   * 붙여넣기 처리
   */
  handlePaste(event) {
    if (!this.bootCompleted) return;

    event.preventDefault();
    const pastedText = event.clipboardData.getData("text/plain");

    // 여러 줄 텍스트는 첫 번째 줄만 사용
    const singleLineText = pastedText.split("\n")[0];

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
    console.log("TISSUE Terminal: Cleaning up...");
    this.isDestroyed = true;

    // 이메일 폴링 정리
    if (this.emailPollingInterval) {
      clearInterval(this.emailPollingInterval);
      this.emailPollingInterval = null;
    }
  }

  // ========== 회원가입 관련 메서드들 ==========

  /**
   * 회원가입 필드 정의
   */
  getSignupFields() {
    return [
      {
        name: "loginId",
        prompt: "Login ID",
        description:
          "4-20 alphanumeric characters (letters, numbers, underscore)",
        required: true,
        validation: this.validateLoginId.bind(this),
      },
      {
        name: "email",
        prompt: "Email Address",
        description: "Valid email address (verification required)",
        required: true,
        validation: this.validateEmail.bind(this),
        needsVerification: true,
      },
      {
        name: "username",
        prompt: "Username",
        description: "Your username (2-30 characters)",
        required: true,
        validation: this.validateUsername.bind(this),
      },
      {
        name: "password",
        prompt: "Password",
        description: "At least 8 characters with letters, numbers, and symbols",
        required: true,
        sensitive: true,
        validation: this.validatePassword.bind(this),
      },
      {
        name: "confirmPassword",
        prompt: "Confirm Password",
        description: "Re-enter your password for confirmation",
        required: true,
        sensitive: true,
        validation: this.validatePasswordConfirm.bind(this),
      },
      {
        name: "name",
        prompt: "Name",
        description: "Your given name (optional)",
        required: false,
        validation: this.validateName.bind(this),
      },
      {
        name: "birthDate",
        prompt: "Birth Date",
        description: "YYYY-MM-DD format (optional)",
        required: false,
        validation: this.validateBirthDate.bind(this),
      },
      {
        name: "jobType",
        prompt: "Job Type",
        description: 'Your profession (optional, type "list" to see options)',
        required: false,
        validation: this.validateJobType.bind(this),
      },
    ];
  }

  /**
   * 회원가입 프로세스 시작
   */
  startSignupProcess() {
    this.signupInProgress = true;
    this.signupStep = 0;
    this.signupData = {};

    this.addHistoryLine("\n", "");
    this.addHistoryLine("=".repeat(60), "info-msg");
    this.addHistoryLine(
      "              TISSUE Registration Wizard",
      "success-msg"
    );
    this.addHistoryLine("=".repeat(60), "info-msg");
    this.addHistoryLine("", "");
    this.addHistoryLine(
      "Welcome! This wizard will guide you through the registration process.",
      "system-msg"
    );
    this.addHistoryLine(
      "You can use Ctrl+C at any time to cancel the registration.",
      "system-msg"
    );
    this.addHistoryLine(
      "Optional fields can be skipped by pressing Enter with empty input.",
      "system-msg"
    );
    this.addHistoryLine("", "");

    setTimeout(() => this.promptNextField(), 1000);
  }

  /**
   * 다음 필드 입력 요청
   */
  promptNextField() {
    const fields = this.getSignupFields();

    if (this.signupStep >= fields.length) {
      this.completeSignupProcess();
      return;
    }

    const field = fields[this.signupStep];
    this.currentFieldInfo = field;

    // 진행률 표시
    const progress = Math.round((this.signupStep / fields.length) * 100);
    const progressBar =
      "▓".repeat(Math.floor(progress / 5)) +
      "░".repeat(20 - Math.floor(progress / 5));

    this.addHistoryLine(`[${progress}%] ${progressBar}`, "info-msg");
    this.addHistoryLine("", "");

    // 필드 정보 표시
    const requiredText = field.required ? " *" : " (optional)";
    this.addHistoryLine(
      `Step ${this.signupStep + 1}/${fields.length}: ${
        field.prompt
      }${requiredText}`,
      "success-msg"
    );
    this.addHistoryLine(`${field.description}`, "system-msg");

    if (!field.required) {
      this.addHistoryLine(
        "Press Enter with empty input to skip this field",
        "system-msg"
      );
    }

    this.addHistoryLine("", "");

    // 특별한 경우 처리 (예: 이미 검증된 이메일)
    if (
      field.name === "email" &&
      this.emailVerificationStatus === "verified" &&
      this.signupData.email
    ) {
      this.addHistoryLine(
        `✓ Email already verified: ${this.signupData.email}`,
        "success-msg"
      );
      this.addHistoryLine("", "");
      this.signupStep++;
      setTimeout(() => this.promptNextField(), 500);
      return;
    }

    // Job Type 필드의 경우 선택 옵션 표시
    if (field.name === "jobType") {
      this.showJobTypeOptions();
    }

    // 프롬프트 업데이트
    this.updatePromptForSignup(field);
  }

  /**
   * 회원가입용 프롬프트 업데이트
   */
  updatePromptForSignup(field) {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = `${field.prompt}: `;
      promptElement.style.color = "#FFD93D";
    }
  }

  /**
   * 회원가입 중 키 입력 처리
   */
  handleSignupKeyPress(event) {
    const field = this.currentFieldInfo;
    if (!field) return;

    if (event.key === "Enter") {
      this.processSignupInput();
    } else if (event.key === "Backspace") {
      this.handleBackspace();
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
    } else if (event.ctrlKey && event.key.toLowerCase() === "c") {
      this.cancelSignupProcess();
    } else if (event.key === "Tab" && field.name === "jobType") {
      this.showJobTypeOptions();
    } else if (
      event.key.length === 1 &&
      !event.ctrlKey &&
      !event.altKey &&
      !event.metaKey
    ) {
      this.addCharacterToInput(event.key);
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
    }
  }

  /**
   * 회원가입 입력 처리
   */
  async processSignupInput() {
    const field = this.currentFieldInfo;
    const value = this.currentInputText.trim();

    // 입력 내용을 히스토리에 표시
    const displayValue = field.sensitive
      ? "*".repeat(this.currentInputText.length)
      : this.currentInputText;
    this.addCommandToSignupHistory(field.prompt + ": " + displayValue);

    // 필수 필드 검증
    if (field.required && !value) {
      this.addHistoryLine("✗ This field is required", "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
      return;
    }

    // 선택 필드이고 빈 값이면 스킵
    if (!field.required && !value) {
      this.addHistoryLine("⊝ Skipped", "warning-msg");
      this.addHistoryLine("", "");
      this.nextSignupStep();
      return;
    }

    // 필드별 검증 실행
    try {
      const isValid = await field.validation(value);
      if (!isValid.valid) {
        this.addHistoryLine(`✗ ${isValid.error}`, "error-msg");
        this.addHistoryLine("", "");
        this.currentInputText = "";
        this.updateInputDisplay();
        return;
      }

      // 값 저장
      this.signupData[field.name] = value;

      // 성공 메시지
      const successValue = field.sensitive ? "[HIDDEN]" : value;
      this.addHistoryLine(`✓ ${field.name}: ${successValue}`, "success-msg");

      // 이메일 필드의 경우 인증 프로세스 시작
      if (field.needsVerification) {
        await this.handleEmailVerificationInSignup(value);
      } else {
        this.addHistoryLine("", "");
        this.nextSignupStep();
      }
    } catch (error) {
      this.addHistoryLine(`✗ Validation error: ${error.message}`, "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
    }
  }

  /**
   * 다음 회원가입 단계로 이동
   */
  nextSignupStep() {
    this.signupStep++;
    this.currentInputText = "";
    this.updateInputDisplay();
    this.resetPromptAfterSignup();
    setTimeout(() => this.promptNextField(), 500);
  }

  /**
   * 회원가입 중 이메일 인증 처리
   */
  async handleEmailVerificationInSignup(email) {
    this.addHistoryLine("", "");
    this.addHistoryLine("📧 Sending verification email...", "info-msg");

    try {
      const response = await fetch(
        "/api/v1/members/email-verification/request",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: email }),
        }
      );

      if (response.ok) {
        this.addHistoryLine(
          "✓ Verification email sent successfully!",
          "success-msg"
        );
        this.addHistoryLine("", "");
        this.addHistoryLine(
          "⏳ Waiting for email verification...",
          "warning-msg"
        );
        this.addHistoryLine(
          "   Check your email and click the verification link",
          "system-msg"
        );
        this.addHistoryLine(
          "   This process will continue automatically",
          "system-msg"
        );
        this.addHistoryLine("", "");

        this.startEmailPollingForSignup(email);
      } else {
        const errorData = await response.json().catch(() => null);
        const errorMessage =
          errorData?.message || "Failed to send verification email";
        this.addHistoryLine(`✗ ${errorMessage}`, "error-msg");
        this.addHistoryLine("", "");
        this.currentInputText = "";
        this.updateInputDisplay();
      }
    } catch (error) {
      console.error("Email verification request failed:", error);
      this.addHistoryLine("✗ Network error occurred", "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
    }
  }

  /**
   * 회원가입 중 이메일 인증 폴링
   */
  startEmailPollingForSignup(email) {
    if (this.emailPollingInterval) {
      clearInterval(this.emailPollingInterval);
    }

    let attempts = 0;
    const maxAttempts = 300; // 5분

    this.emailPollingInterval = setInterval(async () => {
      attempts++;

      if (attempts >= maxAttempts) {
        clearInterval(this.emailPollingInterval);
        this.addHistoryLine("⏰ Email verification timeout", "warning-msg");
        this.addHistoryLine(
          "   Please try again or contact support",
          "system-msg"
        );
        this.addHistoryLine("", "");
        return;
      }

      try {
        const response = await fetch(
          `/api/v1/members/email-verification/status?email=${encodeURIComponent(
            email
          )}`
        );

        if (response.ok) {
          const data = await response.json();

          if (data.data === true) {
            clearInterval(this.emailPollingInterval);
            this.emailVerificationStatus = "verified";

            this.addHistoryLine(
              "✅ Email verified successfully!",
              "success-msg"
            );
            this.addHistoryLine("", "");

            this.nextSignupStep();
          }
        }
      } catch (error) {
        console.error("Email verification polling error:", error);
      }
    }, 1000);
  }

  /**
   * 회원가입 프로세스 완료
   */
  async completeSignupProcess() {
    this.addHistoryLine("", "");
    this.addHistoryLine("🔄 Processing registration...", "info-msg");
    this.addHistoryLine(
      "   Creating your account in the system...",
      "system-msg"
    );
    this.addHistoryLine("", "");

    try {
      const signupData = this.prepareSignupData();

      const response = await fetch("/api/v1/members", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(signupData),
      });

      if (response.ok) {
        const result = await response.json();

        const memberData = {
          username: this.signupData.username,
          email: this.signupData.email,
          loginId: this.signupData.loginId,
        };

        await this.displaySuccessMessage(memberData);
        this.resetSignupState();
      } else {
        const errorData = await response.json().catch(() => null);
        this.handleSignupError(errorData);
      }
    } catch (error) {
      console.error("Signup request failed:", error);
      this.addHistoryLine(
        "✗ Network error occurred during registration",
        "error-msg"
      );
      this.addHistoryLine(
        "   Please check your connection and try again",
        "system-msg"
      );
      this.addHistoryLine("", "");
      this.resetSignupState();
    }
  }

  /**
   * 회원가입 데이터 준비
   */
  prepareSignupData() {
    const { confirmPassword, ...dataToSend } = this.signupData;

    return {
      loginId: dataToSend.loginId,
      email: dataToSend.email,
      username: dataToSend.username,
      password: dataToSend.password,
      name: dataToSend.name || "",
      birthDate: dataToSend.birthDate || null,
      jobType: dataToSend.jobType || "ETC",
    };
  }

  /**
   * 성공 메시지 표시
   */
  async displaySuccessMessage(memberData) {
    await this.typeMessage(
      "Registration completed successfully!",
      "success-msg"
    );
    this.addHistoryLine("", "");
    this.addHistoryLine("🎉 Welcome to TISSUE!", "success-msg");
    this.addHistoryLine(`   Username: ${memberData.username}`, "info-msg");
    this.addHistoryLine(`   Login ID: ${memberData.loginId}`, "info-msg");
    this.addHistoryLine(`   Email: ${memberData.email}`, "info-msg");
    this.addHistoryLine("", "");
    this.addHistoryLine(
      'You can now use "login" command to sign in to your account.',
      "system-msg"
    );
    this.addHistoryLine("", "");
  }

  /**
   * 회원가입 상태 초기화
   */
  resetSignupState() {
    this.signupInProgress = false;
    this.signupStep = 0;
    this.signupData = {};
    this.currentFieldInfo = null;
    this.emailVerificationStatus = "none";

    if (this.emailPollingInterval) {
      clearInterval(this.emailPollingInterval);
      this.emailPollingInterval = null;
    }

    this.resetPromptAfterSignup();
  }

  /**
   * 프롬프트를 기본 상태로 복원
   */
  resetPromptAfterSignup() {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = this.promptPrefix;
      promptElement.style.color = "#00AAFF";
    }

    this.currentInputText = "";
    this.updateInputDisplay();
  }

  /**
   * 회원가입 취소 처리
   */
  cancelSignupProcess() {
    this.addHistoryLine("", "");
    this.addHistoryLine("^C", "system-msg");
    this.addHistoryLine("Registration cancelled by user", "warning-msg");
    this.addHistoryLine("", "");
    this.resetSignupState();
  }

  /**
   * 회원가입 명령어를 히스토리에 추가
   */
  addCommandToSignupHistory(command) {
    const line = document.createElement("div");
    line.className = "history-line";

    const prompt = document.createElement("span");
    prompt.className = "history-prompt";
    prompt.textContent = this.currentFieldInfo.prompt + ": ";
    prompt.style.color = "#FFD93D";

    const commandSpan = document.createElement("span");
    commandSpan.className = "history-command";
    commandSpan.textContent = command.split(": ")[1] || "";

    line.appendChild(prompt);
    line.appendChild(commandSpan);

    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
  }

  /**
   * 타이핑 효과로 메시지 출력
   */
  async typeMessage(text, className = "history-output", speed = 50) {
    const line = document.createElement("div");
    line.className = `history-line ${className}`;
    this.terminalHistory.appendChild(line);

    for (let i = 0; i <= text.length; i++) {
      line.textContent = text.substring(0, i);
      this.scrollToBottom();
      await new Promise((resolve) => setTimeout(resolve, speed));
    }
  }

  /**
   * 회원가입 에러 처리
   */
  handleSignupError(errorData) {
    this.addHistoryLine("✗ Registration failed", "error-msg");

    if (errorData && errorData.message) {
      this.addHistoryLine(`   ${errorData.message}`, "error-msg");
    } else {
      this.addHistoryLine("   An unexpected error occurred", "error-msg");
    }

    this.addHistoryLine("", "");
    this.addHistoryLine(
      'You can try again by using the "signup" command.',
      "system-msg"
    );
    this.addHistoryLine("", "");

    this.resetSignupState();
  }

  // ========== 검증 함수들 ==========

  /**
   * Login ID 검증
   */
  async validateLoginId(value) {
    if (!/^[a-zA-Z0-9_]{4,20}$/.test(value)) {
      return {
        valid: false,
        error:
          "Login ID must be 4-20 characters (letters, numbers, underscore only)",
      };
    }

    try {
      const response = await fetch(
        `/api/v1/members/check-loginid?loginId=${encodeURIComponent(value)}`
      );

      if (response.status === 200) {
        return { valid: true };
      } else if (response.status === 409) {
        const result = await response.json();
        return {
          valid: false,
          error: result.message || "This Login ID is already taken",
        };
      } else {
        console.warn("Unexpected response status:", response.status);
        return {
          valid: false,
          error: "Unable to verify Login ID availability",
        };
      }
    } catch (error) {
      console.warn("Failed to check Login ID availability:", error);
      return {
        valid: false,
        error: "Network error. Please try again later.",
      };
    }
  }

  /**
   * 이메일 검증
   */
  async validateEmail(value) {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      return {
        valid: false,
        error: "Please enter a valid email address",
      };
    }

    try {
      const response = await fetch(
        `/api/v1/members/check-email?email=${encodeURIComponent(value)}`
      );

      if (response.status === 200) {
        return { valid: true };
      } else if (response.status === 409) {
        const result = await response.json();
        return {
          valid: false,
          error: result.message || "This email is already registered",
        };
      } else {
        console.warn("Unexpected response status:", response.status);
        return {
          valid: false,
          error: "Unable to verify email availability",
        };
      }
    } catch (error) {
      console.warn("Failed to check email availability:", error);
      return {
        valid: false,
        error: "Network error. Please try again later.",
      };
    }
  }

  /**
   * 패스워드 검증
   */
  async validatePassword(value) {
    if (value.length < 8) {
      return {
        valid: false,
        error: "Password must be at least 8 characters long",
      };
    }

    const hasLetter = /[a-zA-Z]/.test(value);
    const hasNumber = /\d/.test(value);
    const hasSymbol = /[!@#$%^&*(),.?":{}|<>]/.test(value);

    if (!hasLetter || !hasNumber || !hasSymbol) {
      return {
        valid: false,
        error: "Password must contain letters, numbers, and symbols",
      };
    }

    return { valid: true };
  }

  /**
   * 패스워드 확인 검증
   */
  async validatePasswordConfirm(value) {
    if (value !== this.signupData.password) {
      return {
        valid: false,
        error: "Passwords do not match",
      };
    }
    return { valid: true };
  }

  /**
   * 사용자명 검증
   */
  async validateUsername(value) {
    if (value.length < 4 || value.length > 20) {
      return {
        valid: false,
        error: "Username must be between 4 and 20 characters",
      };
    }

    if (!/^[\p{L}][\p{L}\p{N}]*$/u.test(value)) {
      return {
        valid: false,
        error:
          "Username must start with a letter and contain only letters and numbers",
      };
    }

    try {
      const response = await fetch(
        `/api/v1/members/check-username?username=${encodeURIComponent(value)}`
      );

      if (response.status === 200) {
        return { valid: true };
      } else if (response.status === 409) {
        const result = await response.json();
        return {
          valid: false,
          error: result.message || "This username is already taken",
        };
      } else {
        console.warn("Unexpected response status:", response.status);
        return {
          valid: false,
          error: "Unable to verify username availability",
        };
      }
    } catch (error) {
      console.warn("Failed to check username availability:", error);
      return {
        valid: false,
        error: "Network error. Please try again later.",
      };
    }
  }

  /**
   * 이름 검증
   */
  async validateName(value) {
    if (value.length > 50) {
      return {
        valid: false,
        error: "Name must be 50 characters or less",
      };
    }

    if (!/^[\p{L}]+( [\p{L}]+)*$/u.test(value)) {
      return {
        valid: false,
        error: "Name contains invalid characters",
      };
    }

    return { valid: true };
  }

  /**
   * 생년월일 검증
   */
  async validateBirthDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return {
        valid: false,
        error: "Please use YYYY-MM-DD format",
      };
    }

    const date = new Date(value);
    const now = new Date();

    if (isNaN(date.getTime())) {
      return {
        valid: false,
        error: "Invalid date",
      };
    }

    if (date > now) {
      return {
        valid: false,
        error: "Birth date cannot be in the future",
      };
    }

    const minDate = new Date(
      now.getFullYear() - 150,
      now.getMonth(),
      now.getDate()
    );
    if (date < minDate) {
      return {
        valid: false,
        error: "Please enter a valid birth date",
      };
    }

    return { valid: true };
  }

  /**
   * 직업 유형 검증
   */
  async validateJobType(value) {
    const validJobTypes = [
      "DEVELOPER",
      "DESIGNER",
      "DEVOPS",
      "MANAGER",
      "RESEARCHER",
      "ETC",
    ];

    if (value.toLowerCase() === "list") {
      this.showJobTypeOptions();
      return {
        valid: false,
        error: "Please select from the options above",
      };
    }

    const upperValue = value.toUpperCase();
    if (!validJobTypes.includes(upperValue)) {
      return {
        valid: false,
        error: `Invalid job type. Type "list" to see available options`,
      };
    }

    return { valid: true };
  }

  /**
   * 직업 옵션 표시
   */
  showJobTypeOptions() {
    this.addHistoryLine("Available job types:", "info-msg");
    this.addHistoryLine("  DEVELOPER     - Software Developer", "system-msg");
    this.addHistoryLine("  DEVOPS        - Devops Engineer", "system-msg");
    this.addHistoryLine("  DATA_ANALYST  - Data Analyst", "system-msg");
    this.addHistoryLine("  DESIGNER      - Designer", "system-msg");
    this.addHistoryLine("  MANAGER       - Project Manager", "system-msg");
    this.addHistoryLine("  RESEARCHER    - Researcher", "system-msg");
    this.addHistoryLine("  ETC           - Other", "system-msg");
    this.addHistoryLine("", "");
  }

  // ========== 로그인 관련 메서드들 ==========

  /**
   * 로그인 프로세스 시작
   */
  startLoginProcess() {
    this.loginInProgress = true;
    this.loginStep = 0;
    this.loginData = {};

    this.addHistoryLine("=".repeat(50), "info-msg");
    this.addHistoryLine("             TISSUE Login", "success-msg");
    this.addHistoryLine("=".repeat(50), "info-msg");
    this.addHistoryLine("", "");
    this.addHistoryLine("Please enter your login credentials.", "system-msg");
    this.addHistoryLine("Use Ctrl+C to cancel login process.", "system-msg");
    this.addHistoryLine("", "");

    setTimeout(() => this.promptLoginField(), 500);
  }

  /**
   * 로그인 필드 입력 요청
   */
  promptLoginField() {
    const fields = [
      { name: "identifier", prompt: "Login ID(or Email)", sensitive: false },
      { name: "password", prompt: "Password", sensitive: true },
    ];

    if (this.loginStep >= fields.length) {
      this.processLogin();
      return;
    }

    const field = fields[this.loginStep];
    this.currentFieldInfo = field;

    this.addHistoryLine(`${field.prompt}:`, "info-msg");
    this.updatePromptForLogin(field);
  }

  /**
   * 로그인용 프롬프트 업데이트
   */
  updatePromptForLogin(field) {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = `${field.prompt}: `;
      promptElement.style.color = "#00FF00";
    }
  }

  /**
   * 로그인 중 키 입력 처리
   */
  handleLoginKeyPress(event) {
    const field = this.currentFieldInfo;
    if (!field) return;

    if (event.key === "Enter") {
      this.processLoginInput();
    } else if (event.key === "Backspace") {
      this.handleBackspace();
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
    } else if (event.ctrlKey && event.key.toLowerCase() === "c") {
      this.cancelLoginProcess();
    } else if (
      event.key.length === 1 &&
      !event.ctrlKey &&
      !event.altKey &&
      !event.metaKey
    ) {
      this.addCharacterToInput(event.key);
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
    }
  }

  /**
   * 로그인 입력 처리
   */
  async processLoginInput() {
    const field = this.currentFieldInfo;
    const value = this.currentInputText.trim();

    // 입력 내용을 히스토리에 표시
    const displayValue = field.sensitive
      ? "*".repeat(this.currentInputText.length)
      : value;
    this.addCommandToLoginHistory(field.prompt + ": " + displayValue);

    if (!value) {
      this.addHistoryLine("✗ This field is required", "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
      return;
    }

    // 데이터 저장
    this.loginData[field.name] = value;

    // 다음 단계로
    this.loginStep++;
    this.currentInputText = "";
    this.updateInputDisplay();

    setTimeout(() => this.promptLoginField(), 300);
  }

  /**
   * 로그인 처리
   */
  async processLogin() {
    this.addHistoryLine("", "");
    this.addHistoryLine("🔐 Authenticating...", "info-msg");

    try {
      const response = await fetch("/api/v1/auth/login", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(this.loginData),
      });

      if (response.status === 200) {
        const result = await response.json();

        this.isLoggedIn = true;
        this.currentUser = result.data;
        this.promptPrefix = `${this.currentUser.username}@tissue:~$ `;

        this.addHistoryLine("✓ Login successful!", "success-msg");
        this.addHistoryLine(
          `Welcome back, ${this.currentUser.username}!`,
          "system-msg"
        );
        this.addHistoryLine("", "");

        this.resetLoginState();
      } else if (response.status === 401) {
        this.addHistoryLine("✗ Invalid credentials", "error-msg");
        this.addHistoryLine(
          "Please check your login ID (or email) and password.",
          "system-msg"
        );
        this.addHistoryLine("", "");
        this.resetLoginState();
      } else {
        this.addHistoryLine("✗ Login failed", "error-msg");
        this.addHistoryLine("Please try again later.", "system-msg");
        this.addHistoryLine("", "");
        this.resetLoginState();
      }
    } catch (error) {
      console.error("Login failed:", error);
      this.addHistoryLine("✗ Network error occurred", "error-msg");
      this.addHistoryLine("Please check your connection.", "system-msg");
      this.addHistoryLine("", "");
      this.resetLoginState();
    }
  }

  /**
   * 로그아웃 처리
   */
  async performLogout() {
    this.addHistoryLine("🔓 Logging out...", "info-msg");

    try {
      const response = await fetch("/api/v1/auth/logout", {
        method: "POST",
        credentials: "include",
      });

      this.isLoggedIn = false;
      this.currentUser = null;
      this.promptPrefix = "guest@tissue:~$ ";

      this.addHistoryLine("✓ Logged out successfully", "success-msg");
      this.addHistoryLine("Thank you for using TISSUE!", "system-msg");
      this.addHistoryLine("", "");

      this.resetPromptAfterLogout();
    } catch (error) {
      console.error("Logout error:", error);
      this.isLoggedIn = false;
      this.currentUser = null;
      this.promptPrefix = "guest@tissue:~$ ";
      this.resetPromptAfterLogout();
    }
  }

  /**
   * 사용자 프로필 표시
   */
  async displayUserProfile() {
    this.addHistoryLine("📋 Loading profile information...", "info-msg");

    try {
      const response = await fetch("/api/v1/members", {
        credentials: "include",
      });

      if (response.status === 200) {
        const result = await response.json();
        const profile = result.data;

        this.addHistoryLine("", "");
        this.addHistoryLine("=".repeat(60), "info-msg");
        this.addHistoryLine("                    USER PROFILE", "success-msg");
        this.addHistoryLine("=".repeat(60), "info-msg");
        this.addHistoryLine("", "");

        // 프로필 정보 표시
        const profileInfo = [
          { label: "Login ID:", value: profile.loginId || "N/A" },
          { label: "Username:", value: profile.username || "N/A" },
          { label: "Email:", value: profile.email || "N/A" },
          { label: "Name:", value: profile.name || "Not set" },
          { label: "Birth Date:", value: profile.birthDate || "Not set" },
          { label: "Job Type:", value: profile.jobType || "Not set" },
        ];

        profileInfo.forEach((item) => {
          const line = document.createElement("div");
          line.className = "system-info-line";

          const label = document.createElement("span");
          label.className = "info-label";
          label.textContent = item.label.padEnd(15);

          const value = document.createElement("span");
          value.className = "info-value";
          value.textContent = item.value;

          line.appendChild(label);
          line.appendChild(value);
          this.terminalHistory.appendChild(line);
        });

        this.addHistoryLine("", "");
        this.addHistoryLine(
          'Use "edit [field]" to modify profile information.',
          "system-msg"
        );
        this.addHistoryLine(
          "Available fields: username, email, name, birthDate, jobType, password",
          "system-msg"
        );
        this.addHistoryLine("", "");

        this.scrollToBottom();
      } else if (response.status === 401) {
        this.addHistoryLine("✗ Session expired", "error-msg");
        this.addHistoryLine("Please login again.", "system-msg");
        this.addHistoryLine("", "");
        this.handleSessionExpired();
      } else {
        this.addHistoryLine("✗ Failed to load profile", "error-msg");
        this.addHistoryLine("Please try again later.", "system-msg");
        this.addHistoryLine("", "");
      }
    } catch (error) {
      console.error("Profile loading failed:", error);
      this.addHistoryLine("✗ Network error occurred", "error-msg");
      this.addHistoryLine("", "");
    }
  }

  // ========== 프로필 수정 관련 메서드들 (보안 강화) ==========

  /**
   * 프로필 수정 프로세스 시작 (보안 강화된 버전)
   */
  startEditProcess(args) {
    const field = args[0];

    if (!field) {
      this.addHistoryLine("Usage: edit [field]", "error-msg");
      this.addHistoryLine(
        "Available fields: username, email, name, birthDate, jobType, password",
        "system-msg"
      );
      this.addHistoryLine("", "");
      return;
    }

    const editableFields = {
      username: {
        prompt: "New Username",
        description: "4-20 characters (letters and numbers only)",
        validation: this.validateUsername.bind(this),
        endpoint: "/api/v1/members/username",
        requestKey: "newUsername",
        requiresCurrentPassword: true, // 민감한 필드는 현재 패스워드 필요
      },
      email: {
        prompt: "New Email Address",
        description: "Valid email address (verification required)",
        validation: this.validateEmail.bind(this),
        endpoint: "/api/v1/members/email",
        requestKey: "newEmail",
        requiresCurrentPassword: true, // 민감한 필드는 현재 패스워드 필요
        requiresVerification: true,
      },
      name: {
        prompt: "New Name",
        description: "Your display name (optional field)",
        validation: this.validateName.bind(this),
        endpoint: "/api/v1/members",
        requestKey: "name",
        requiresCurrentPassword: false, // 일반 필드는 패스워드 불필요
      },
      birthDate: {
        prompt: "Birth Date",
        description: "YYYY-MM-DD format (optional field)",
        validation: this.validateBirthDate.bind(this),
        endpoint: "/api/v1/members",
        requestKey: "birthDate",
        requiresCurrentPassword: false, // 일반 필드는 패스워드 불필요
      },
      jobType: {
        prompt: "Job Type",
        description: 'Your profession (type "list" to see options)',
        validation: this.validateJobType.bind(this),
        endpoint: "/api/v1/members",
        requestKey: "jobType",
        requiresCurrentPassword: false, // 일반 필드는 패스워드 불필요
      },
      password: {
        prompt: "New Password",
        description: "At least 8 characters with letters, numbers, and symbols",
        validation: this.validateNewPassword.bind(this),
        endpoint: "/api/v1/members/password",
        requestKey: "newPassword",
        requiresCurrentPassword: true,
        requiresConfirmation: true,
        sensitive: true,
      },
    };

    if (!editableFields[field]) {
      this.addHistoryLine(`✗ Unknown field: ${field}`, "error-msg");
      this.addHistoryLine(
        "Available fields: " + Object.keys(editableFields).join(", "),
        "system-msg"
      );
      this.addHistoryLine("", "");
      return;
    }

    this.startFieldEdit(field, editableFields[field]);
  }

  /**
   * 필드 편집 시작
   */
  startFieldEdit(field, fieldInfo) {
    this.editInProgress = true;
    this.editData = { field: field, fieldInfo: fieldInfo };
    this.editFieldInfo = fieldInfo;

    this.addHistoryLine("✏️  Profile Edit Mode", "info-msg");
    this.addHistoryLine(`Editing: ${field}`, "system-msg");
    this.addHistoryLine(`${fieldInfo.description}`, "system-msg");
    this.addHistoryLine("Use Ctrl+C to cancel editing.", "system-msg");
    this.addHistoryLine("", "");

    if (field === "jobType") {
      this.showJobTypeOptions();
    }

    // 현재 패스워드가 필요한 필드인 경우
    if (fieldInfo.requiresCurrentPassword) {
      this.editData.step = "current_password";
      this.currentFieldInfo = {
        prompt: "Current Password",
        sensitive: true,
      };
      this.addHistoryLine(
        "First, please enter your current password:",
        "info-msg"
      );
    } else {
      // 일반 필드는 바로 입력 시작
      this.editData.step = "field_input";
      this.currentFieldInfo = fieldInfo;
    }

    this.currentInputText = "";
    this.updateInputDisplay();
    this.updatePromptForEdit();
  }

  /**
   * 수정용 프롬프트 업데이트
   */
  updatePromptForEdit() {
    const promptElement = this.currentPrompt?.querySelector(".prompt-prefix");
    if (promptElement && this.currentFieldInfo) {
      promptElement.textContent = `${this.currentFieldInfo.prompt}: `;
      promptElement.style.color = "#FF6B6B"; // 수정 중에는 빨간색
    }
  }

  /**
   * 수정 중 키 입력 처리
   */
  handleEditKeyPress(event) {
    const field = this.currentFieldInfo;
    if (!field) return;

    if (event.key === "Enter") {
      this.processEditInput();
    } else if (event.key === "Backspace") {
      this.handleBackspace();
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
    } else if (event.ctrlKey && event.key.toLowerCase() === "c") {
      this.cancelEditProcess();
    } else if (event.key === "Tab" && this.editData.field === "jobType") {
      this.showJobTypeOptions();
    } else if (
      event.key.length === 1 &&
      !event.ctrlKey &&
      !event.altKey &&
      !event.metaKey
    ) {
      this.addCharacterToInput(event.key);
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
    }
  }

  /**
   * 프로필 수정 처리 (보안 강화된 버전)
   */
  async processEditInput() {
    const value = this.currentInputText.trim();
    const step = this.editData.step;

    if (!value) {
      this.addHistoryLine("✗ Value cannot be empty", "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
      return;
    }

    // 입력 값을 히스토리에 표시
    const displayValue = this.currentFieldInfo?.sensitive
      ? "*".repeat(this.currentInputText.length)
      : value;

    this.addCommandToEditHistory(
      `${this.currentFieldInfo?.prompt || "Input"}: ${displayValue}`
    );

    try {
      if (step === "current_password") {
        await this.handleCurrentPasswordInput(value);
      } else if (step === "new_password") {
        await this.handleNewPasswordInput(value);
      } else if (step === "confirm_password") {
        await this.handleConfirmPasswordInput(value);
      } else if (step === "field_input") {
        await this.handleFieldInput(value);
      } else {
        console.warn(`Unknown edit step: ${step}`);
        this.addHistoryLine("✗ Internal error: unknown edit step", "error-msg");
        this.addHistoryLine("", "");
        this.resetEditState();
      }
    } catch (error) {
      console.error("Edit process failed:", error);
      this.addHistoryLine("✗ Network error occurred", "error-msg");
      this.addHistoryLine("", "");
      this.resetEditState();
    }
  }

  /**
   * 현재 패스워드 입력 처리
   */
  async handleCurrentPasswordInput(currentPassword) {
    // 민감한 필드들은 먼저 권한 획득 필요
    if (this.editData.fieldInfo.requiresCurrentPassword) {
      this.addHistoryLine("", "");
      this.addHistoryLine(
        "🔐 Verifying password and getting permission...",
        "info-msg"
      );

      try {
        // 1단계: 권한 획득
        await this.requestUpdatePermission(currentPassword);

        this.addHistoryLine("✓ Permission granted", "success-msg");
        this.addHistoryLine("", "");

        // 패스워드 변경인 경우: 현재 패스워드를 저장 (이중 검증용)
        if (this.editData.field === "password") {
          this.editData.currentPassword = currentPassword; // 나중에 API 요청 시 필요
          this.editData.step = "new_password";
          this.currentFieldInfo = {
            prompt: "New Password",
            sensitive: true,
          };
          this.addHistoryLine("Now enter your new password:", "info-msg");
        } else {
          // 다른 민감한 필드들은 해당 필드 입력으로
          this.editData.step = "field_input";
          this.currentFieldInfo = this.editData.fieldInfo;
          this.addHistoryLine(
            `Now enter your new ${this.editData.field}:`,
            "info-msg"
          );
        }
      } catch (error) {
        this.addHistoryLine("✗ Incorrect current password", "error-msg");
        this.addHistoryLine("", "");
        this.resetEditState();
        return;
      }
    } else {
      // 일반 필드는 바로 필드 입력으로
      this.editData.step = "field_input";
      this.currentFieldInfo = this.editData.fieldInfo;
      this.addHistoryLine("", "");
      this.addHistoryLine(
        `Now enter your new ${this.editData.field}:`,
        "info-msg"
      );
    }

    this.currentInputText = "";
    this.updateInputDisplay();
    this.updatePromptForEdit();
  }

  /**
   * 권한 요청 (현재 패스워드로 세션에 권한 설정)
   */
  async requestUpdatePermission(currentPassword) {
    const response = await fetch("/api/v1/members/permissions", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password: currentPassword }),
    });

    if (response.status !== 200) {
      throw new Error("Permission request failed");
    }
  }

  /**
   * 현재 패스워드 검증 (서버로 즉시 전송)
   */
  async sendCurrentPasswordVerification(currentPassword) {
    const response = await fetch("/api/v1/members/verify-password", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password: currentPassword }),
    });

    if (response.status !== 200) {
      throw new Error("Password verification failed");
    }

    // 임시 토큰을 받아서 다음 요청에 사용
    const result = await response.json();
    this.editData.verificationToken = result.data?.token;
  }

  /**
   * 새 패스워드 입력 처리
   */
  async handleNewPasswordInput(newPassword) {
    // 검증 실행
    const isValid = await this.editData.fieldInfo.validation(newPassword);
    if (!isValid.valid) {
      this.addHistoryLine(`✗ ${isValid.error}`, "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
      return;
    }

    // 패스워드 확인이 필요한 경우
    if (this.editData.fieldInfo.requiresConfirmation) {
      this.editData.newPassword = newPassword;
      this.editData.step = "confirm_password";
      this.currentFieldInfo = {
        prompt: "Confirm New Password",
        sensitive: true,
      };

      this.addHistoryLine("", "");
      this.addHistoryLine("Please confirm your new password:", "info-msg");

      this.currentInputText = "";
      this.updateInputDisplay();
      this.updatePromptForEdit();
      return;
    }

    // 패스워드 업데이트 요청 (이중 검증: 세션 권한 + 현재 패스워드)
    await this.sendUpdateRequest(
      {
        originalPassword: this.editData.currentPassword, // 실제 현재 패스워드 필요!
        newPassword: newPassword,
      },
      this.editData.fieldInfo.endpoint
    );
  }

  /**
   * 패스워드 재입력 확인 처리
   */
  async handleConfirmPasswordInput(confirmPassword) {
    if (confirmPassword !== this.editData.newPassword) {
      this.addHistoryLine("✗ Passwords do not match", "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
      return;
    }

    // 패스워드 업데이트 요청 (이중 검증: 세션 권한 + 현재 패스워드)
    await this.sendUpdateRequest(
      {
        originalPassword: this.editData.currentPassword, // 실제 현재 패스워드 필요!
        newPassword: this.editData.newPassword,
      },
      this.editData.fieldInfo.endpoint
    );
  }

  /**
   * 일반 필드 입력 처리
   */
  async handleFieldInput(value) {
    const fieldInfo = this.editData.fieldInfo;
    if (!fieldInfo) {
      console.error("fieldInfo is missing in editData");
      this.addHistoryLine(
        "✗ Internal error: field information missing",
        "error-msg"
      );
      this.resetEditState();
      return;
    }

    try {
      // 이메일의 경우 별도 처리
      if (this.editData.field === "email") {
        await this.handleEmailUpdate(value);
        return;
      }

      const isValid = await fieldInfo.validation(value);
      if (!isValid.valid) {
        this.addHistoryLine(`✗ ${isValid.error}`, "error-msg");
        this.addHistoryLine("", "");
        this.currentInputText = "";
        this.updateInputDisplay();
        return;
      }

      // 업데이트 요청 데이터 준비
      const updateData = {};
      updateData[fieldInfo.requestKey] = value;

      // 모든 업데이트는 세션 권한으로 처리 (별도 패스워드 불필요)
      await this.sendUpdateRequest(updateData, fieldInfo.endpoint);
    } catch (error) {
      console.error("Field validation or update failed:", error);
      this.addHistoryLine(`✗ Validation error: ${error.message}`, "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
    }
  }

  /**
   * 이메일 업데이트 처리 (현재 패스워드와 함께)
   */
  async handleEmailUpdate(email) {
    // 기본 이메일 형식 검증
    const isValid = await this.editData.fieldInfo.validation(email);
    if (!isValid.valid) {
      this.addHistoryLine(`✗ ${isValid.error}`, "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
      return;
    }

    // 이메일 인증 요청 (세션 권한으로 처리)
    this.addHistoryLine("", "");
    this.addHistoryLine("📧 Sending verification email...", "info-msg");

    try {
      const response = await fetch(
        "/api/v1/members/email-verification/request",
        {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: email }), // 패스워드 불필요, 세션 권한으로 처리
        }
      );

      if (response.ok) {
        this.addHistoryLine(
          "✓ Verification email sent successfully!",
          "success-msg"
        );
        this.addHistoryLine("", "");
        this.addHistoryLine(
          "⏳ Waiting for email verification...",
          "warning-msg"
        );
        this.addHistoryLine(
          "   Check your email and click the verification link",
          "system-msg"
        );

        // 이메일 인증 대기
        this.editData.pendingEmail = email;
        this.startEmailPollingForEdit(email);
      } else if (response.status === 403) {
        this.addHistoryLine(
          "✗ Permission expired or insufficient",
          "error-msg"
        );
        this.addHistoryLine("Please try the edit command again.", "system-msg");
        this.addHistoryLine("", "");
        this.resetEditState();
      } else {
        const errorData = await response.json().catch(() => null);
        this.addHistoryLine(
          `✗ ${errorData?.message || "Failed to send verification email"}`,
          "error-msg"
        );
        this.addHistoryLine("", "");
        this.currentInputText = "";
        this.updateInputDisplay();
      }
    } catch (error) {
      console.error("Email verification request failed:", error);
      this.addHistoryLine("✗ Network error occurred", "error-msg");
      this.addHistoryLine("", "");
      this.currentInputText = "";
      this.updateInputDisplay();
    }
  }

  /**
   * 편집 중 이메일 인증 폴링
   */
  startEmailPollingForEdit(email) {
    if (this.emailPollingInterval) {
      clearInterval(this.emailPollingInterval);
    }

    let attempts = 0;
    const maxAttempts = 300; // 5분

    this.emailPollingInterval = setInterval(async () => {
      attempts++;

      if (attempts >= maxAttempts) {
        clearInterval(this.emailPollingInterval);
        this.addHistoryLine("⏰ Email verification timeout", "warning-msg");
        this.addHistoryLine("", "");
        this.resetEditState();
        return;
      }

      try {
        const response = await fetch(
          `/api/v1/members/email-verification/status?email=${encodeURIComponent(
            email
          )}`
        );

        if (response.ok) {
          const data = await response.json();

          if (data.data === true) {
            clearInterval(this.emailPollingInterval);
            this.addHistoryLine(
              "✅ Email verified successfully!",
              "success-msg"
            );
            this.addHistoryLine("", "");

            // 이메일 업데이트 요청 (세션 권한으로 처리)
            await this.sendUpdateRequest(
              { newEmail: this.editData.pendingEmail },
              this.editData.fieldInfo.endpoint
            );
          }
        }
      } catch (error) {
        console.error("Email verification polling error:", error);
      }
    }, 1000);
  }

  /**
   * 일반 업데이트 요청 전송
   */
  async sendUpdateRequest(data, endpoint) {
    this.addHistoryLine("🔄 Updating profile...", "info-msg");

    try {
      const response = await fetch(endpoint, {
        method: "PATCH",
        credentials: "include", // 세션 권한 체크
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data), // 패스워드 변경 시 originalPassword 포함
      });

      if (response.status === 200) {
        this.addHistoryLine("✓ Profile updated successfully!", "success-msg");

        // 필드별 성공 메시지
        if (this.editData.field === "password") {
          this.addHistoryLine("Password has been changed", "info-msg");
          this.addHistoryLine(
            "Please use your new password for future logins",
            "system-msg"
          );
        } else {
          const displayValue =
            data[this.editData.fieldInfo.requestKey] || data.newEmail;
          this.addHistoryLine(
            `${this.editData.field}: ${displayValue}`,
            "info-msg"
          );
        }

        this.addHistoryLine("", "");
        this.resetEditState();
      } else if (response.status === 401) {
        this.addHistoryLine("✗ Authentication failed", "error-msg");
        this.addHistoryLine("Please login again.", "system-msg");
        this.addHistoryLine("", "");
        this.handleSessionExpired();
      } else if (response.status === 403) {
        // 세션 권한 또는 패스워드 검증 실패
        const errorData = await response.json().catch(() => null);
        if (errorData?.message?.includes("password")) {
          this.addHistoryLine(
            "✗ Current password verification failed",
            "error-msg"
          );
          this.addHistoryLine(
            "The current password you entered is incorrect.",
            "system-msg"
          );
        } else {
          this.addHistoryLine(
            "✗ Permission expired or insufficient",
            "error-msg"
          );
          this.addHistoryLine(
            "Please try the edit command again.",
            "system-msg"
          );
        }
        this.addHistoryLine("", "");
        this.resetEditState();
      } else if (response.status === 409) {
        const errorData = await response.json().catch(() => null);
        this.addHistoryLine(
          `✗ ${errorData?.message || "Value already in use"}`,
          "error-msg"
        );
        this.addHistoryLine("", "");
        this.currentInputText = "";
        this.updateInputDisplay();
      } else {
        const errorData = await response.json().catch(() => null);
        this.addHistoryLine(
          `✗ Update failed: ${errorData?.message || "Unknown error"}`,
          "error-msg"
        );
        this.addHistoryLine("Please try again later.", "system-msg");
        this.addHistoryLine("", "");
        this.resetEditState();
      }
    } catch (error) {
      console.error("Update request failed:", error);
      this.addHistoryLine("✗ Network error occurred", "error-msg");
      this.addHistoryLine("", "");
      this.resetEditState();
    }
  }

  /**
   * 새 패스워드 검증
   */
  async validateNewPassword(value) {
    if (value.length < 8) {
      return {
        valid: false,
        error: "Password must be at least 8 characters long",
      };
    }

    const hasLetter = /[a-zA-Z]/.test(value);
    const hasNumber = /\d/.test(value);
    const hasSymbol = /[!@#$%^&*(),.?":{}|<>]/.test(value);

    if (!hasLetter || !hasNumber || !hasSymbol) {
      return {
        valid: false,
        error: "Password must contain letters, numbers, and symbols",
      };
    }

    return { valid: true };
  }

  // ========== 특별 모드 키 입력 처리 ==========

  /**
   * 특별 모드 키 입력 처리
   */
  handleSpecialModeKeyPress(event) {
    if (this.signupInProgress) {
      this.handleSignupKeyPress(event);
    } else if (this.loginInProgress) {
      this.handleLoginKeyPress(event);
    } else if (this.editInProgress) {
      this.handleEditKeyPress(event);
    }
  }

  // ========== 상태 초기화 및 정리 메서드들 ==========

  /**
   * 로그인 상태 초기화
   */
  resetLoginState() {
    this.loginInProgress = false;
    this.loginStep = 0;
    this.loginData = {};
    this.currentFieldInfo = null;
    this.resetPromptAfterLogin();
  }

  /**
   * 프로필 수정 상태 초기화
   */
  resetEditState() {
    this.editInProgress = false;
    this.editFieldInfo = null;
    this.currentFieldInfo = null;

    // 메모리에서 민감한 데이터 완전 제거
    if (this.editData.currentPassword) {
      // 메모리에서 완전히 제거
      this.editData.currentPassword = null;
      delete this.editData.currentPassword;
    }
    if (this.editData.newPassword) {
      this.editData.newPassword = null;
      delete this.editData.newPassword;
    }

    this.editData = {};

    if (this.emailPollingInterval) {
      clearInterval(this.emailPollingInterval);
      this.emailPollingInterval = null;
    }

    this.resetPromptAfterEdit();
  }

  /**
   * 프롬프트 복원 메서드들
   */
  resetPromptAfterLogin() {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = this.promptPrefix;
      promptElement.style.color = "#00AAFF";
    }
    this.currentInputText = "";
    this.updateInputDisplay();
  }

  resetPromptAfterEdit() {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = this.promptPrefix;
      promptElement.style.color = "#00AAFF";
    }
    this.currentInputText = "";
    this.updateInputDisplay();
  }

  resetPromptAfterLogout() {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = this.promptPrefix;
      promptElement.style.color = "#00AAFF";
    }
  }

  /**
   * 취소 처리 메서드들
   */
  cancelLoginProcess() {
    this.addHistoryLine("", "");
    this.addHistoryLine("^C", "system-msg");
    this.addHistoryLine("Login cancelled by user", "warning-msg");
    this.addHistoryLine("", "");
    this.resetLoginState();
  }

  cancelEditProcess() {
    this.addHistoryLine("", "");
    this.addHistoryLine("^C", "system-msg");
    this.addHistoryLine("Profile editing cancelled", "warning-msg");
    this.addHistoryLine("", "");
    this.resetEditState();
  }

  /**
   * 히스토리 추가 메서드들
   */
  addCommandToLoginHistory(command) {
    const line = document.createElement("div");
    line.className = "history-line";

    const prompt = document.createElement("span");
    prompt.className = "history-prompt";
    prompt.textContent = this.currentFieldInfo.prompt + ": ";
    prompt.style.color = "#00FF00";

    const commandSpan = document.createElement("span");
    commandSpan.className = "history-command";
    commandSpan.textContent = command.split(": ")[1] || "";

    line.appendChild(prompt);
    line.appendChild(commandSpan);
    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
  }

  addCommandToEditHistory(command) {
    const line = document.createElement("div");
    line.className = "history-line";

    const prompt = document.createElement("span");
    prompt.className = "history-prompt";

    if (this.currentFieldInfo) {
      prompt.textContent = this.currentFieldInfo.prompt + ": ";
    } else {
      prompt.textContent = "Input: ";
    }
    prompt.style.color = "#FF6B6B";

    const commandSpan = document.createElement("span");
    commandSpan.className = "history-command";
    commandSpan.textContent = command.split(": ")[1] || command;

    line.appendChild(prompt);
    line.appendChild(commandSpan);
    this.terminalHistory.appendChild(line);
    this.scrollToBottom();
  }

  /**
   * 세션 만료 처리
   */
  handleSessionExpired() {
    this.isLoggedIn = false;
    this.currentUser = null;
    this.promptPrefix = "guest@tissue:~$ ";
    this.resetPromptAfterLogout();

    // 진행 중인 프로세스들 정리
    if (this.editInProgress) this.resetEditState();
    if (this.loginInProgress) this.resetLoginState();
    if (this.signupInProgress) this.resetSignupState();
  }
}

// ========== 전역 인스턴스 및 초기화 ==========

// 전역 인스턴스
let terminal = null;

/**
 * 시스템 초기화
 */
document.addEventListener("DOMContentLoaded", () => {
  try {
    console.log("TISSUE Terminal: Starting system...");
    terminal = new TissueTerminal();
  } catch (error) {
    console.error("TISSUE Terminal: Critical startup failure", error);

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
window.addEventListener("beforeunload", () => {
  if (terminal && !terminal.isDestroyed) {
    terminal.cleanup();
  }
});

// 개발자 도구용 전역 접근
if (typeof window !== "undefined") {
  window.TISSUE_TERMINAL = terminal;
}
