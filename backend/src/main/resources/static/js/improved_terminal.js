// TODO: 매직넘버나 스트링을 상수로 분리
// TODO: 설정값을 서버에서 주입받아서 사용(베너, 버전, author, license, 등...)

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
    this.signupInProgress = false; // 현재 회원가입 진행 중인지
    this.signupStep = 0; // 현재 회원가입 단계
    this.signupData = {}; // 수집된 회원가입 데이터
    this.currentFieldInfo = null; // 현재 입력받고 있는 필드 정보

    // 이메일 인증 관련
    this.emailVerificationStatus = "none"; // none, pending, verified, failed
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

    // 회원가입이 진행 중인 경우 별도 처리
    if (this.signupInProgress) {
      this.handleSignupKeyPress(event);
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
      return null; // displayBanner가 직접 출력하므로 추가 반환값 없음
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

    // 사용자 정보 명령어
    // TODO: 로그인안하면 guset, 로그인하면 본인 username 반환
    whoami: function () {
      return "guest";
    },

    // 종료 명령어
    // TODO: exit을 실행하면 나갈지 물어보는 모달창을 보여주거나, exit 명령어를 제거(clear 사용)
    exit: function () {
      this.addHistoryLine("Goodbye!", "success-msg");
      setTimeout(() => {
        window.location.href = "/";
      }, 1000);
      return null;
    },

    signup: function (args) {
      // 이미 회원가입이 진행 중인지 확인
      if (this.signupInProgress) {
        return "Signup process is already in progress. Use Ctrl+C to cancel.";
      }

      // 회원가입 프로세스 시작
      this.startSignupProcess();
      return null; // 추가 출력 없음 (startSignupProcess에서 처리)
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
      version: "Show version information",
      date: "Display current date and time",
      echo: "Echo the given text",
      whoami: "Display current user",
      exit: "Exit the terminal",
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

    // 도움말 메시지
    //    this.addHistoryLine("Type 'help' to see the list of commands.", "help-msg");
    //    this.addHistoryLine("", "");
    //    this.addHistoryLine("\n", "");

    // 텍스트를 여러 부분으로 나누어서 처리
    const helpLine = document.createElement("div");
    helpLine.className = "help-msg";
    helpLine.innerHTML =
      "Type <span class=\"command-highlight\">'help'</span> to see the list of available commands.";
    this.terminalHistory.appendChild(helpLine);

    this.addHistoryLine("", ""); // 빈 줄
    this.addHistoryLine("\n", ""); // 빈 줄

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
   * 유틸리티 메서드들
   */

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

  // /**
  //  * 입력 표시 업데이트
  //  */
  // updateInputDisplay() {
  //   if (!this.currentInput) return;
  //   this.currentInput.textContent = this.currentInputText;
  //   this.refreshCursor();
  // }

  /**
   * 입력 표시 업데이트
   */
  updateInputDisplay() {
    if (!this.currentInput) return;

    // 회원가입 중이고 민감한 필드인 경우 마스킹 처리
    if (this.signupInProgress && this.currentFieldInfo?.sensitive) {
      this.updateMaskedInputDisplay();
    } else {
      this.currentInput.textContent = this.currentInputText;
      this.refreshCursor();
    }
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
    // 이벤트 리스너들은 페이지 언로드 시 자동으로 정리됨
  }

  // 회원가입 필드 정의 (기존 SignupFormRequest와 동일한 구조)
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
        prompt: "Display Name",
        description: "Your display name (2-30 characters)",
        required: true,
        validation: this.validateUsername.bind(this),
      },
      {
        name: "password",
        prompt: "Password",
        description: "At least 8 characters with letters, numbers, and symbols",
        required: true,
        sensitive: true, // 입력 시 마스킹 처리
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
        name: "firstName",
        prompt: "First Name",
        description: "Your given name (optional)",
        required: false,
        validation: this.validateName.bind(this),
      },
      {
        name: "lastName",
        prompt: "Last Name",
        description: "Your family name (optional)",
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
      {
        name: "biography",
        prompt: "Biography",
        description: "Brief description about yourself (optional)",
        required: false,
        validation: this.validateBiography.bind(this),
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

    // 환영 메시지 출력
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

    // 첫 번째 필드 입력 시작
    setTimeout(() => this.promptNextField(), 1000);
  }

  /**
   * 다음 필드 입력 요청
   */
  promptNextField() {
    const fields = this.getSignupFields();

    // 모든 필드를 완료했는지 확인
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
      promptElement.style.color = "#FFD93D"; // 회원가입 중에는 노란색으로 표시
    }
  }

  // /**
  //  * 회원가입 중 키 입력 처리
  //  */
  // handleSignupKeyPress(event) {
  //   const field = this.currentFieldInfo;
  //   if (!field) return;

  //   if (event.key === "Enter") {
  //     this.processSignupInput();
  //   } else if (event.key === "Backspace") {
  //     this.handleBackspace();
  //   } else if (event.ctrlKey && event.key.toLowerCase() === "c") {
  //     this.cancelSignupProcess();
  //   } else if (event.key === "Tab" && field.name === "jobType") {
  //     this.showJobTypeOptions();
  //   } else if (
  //     event.key.length === 1 &&
  //     !event.ctrlKey &&
  //     !event.altKey &&
  //     !event.metaKey
  //   ) {
  //     this.addCharacterToInput(event.key);
  //   }
  // }

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
      // 패스워드 필드인 경우 마스킹된 화면 업데이트
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
      // 패스워드 필드인 경우 마스킹된 화면 업데이트
      if (field.sensitive) {
        this.updateMaskedInputDisplay();
      }
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

    // 프롬프트를 기본 상태로 복원
    this.resetPromptAfterSignup();

    setTimeout(() => this.promptNextField(), 500);
  }

  /**
   * Login ID 검증
   */
  async validateLoginId(value) {
    // 기본 형식 검증
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
    // 기본 이메일 형식 검증
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

    // 복잡성 검증: 영문자, 숫자, 특수문자 포함
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
    // 길이 검증 (4-20자)
    if (value.length < 4 || value.length > 20) {
      return {
        valid: false,
        error: "Username must be between 4 and 20 characters",
      };
    }

    // 패턴 검증: 첫 글자는 문자, 나머지는 문자 또는 숫자
    // JavaScript에서 \p{L}과 \p{N}은 u 플래그와 함께 사용
    if (!/^[\p{L}][\p{L}\p{N}]*$/u.test(value)) {
      return {
        valid: false,
        error:
          "Username must start with a letter and contain only letters and numbers",
      };
    }

    // 서버에서 중복 검사
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

  // TODO: birthdate, name, job에 대한 검증도 필요

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

        // 이메일 재입력 요청
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
      // 서버로 회원가입 데이터 전송
      const signupData = this.prepareSignupData();

      const response = await fetch("/api/v1/members", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(signupData),
      });

      if (response.ok) {
        const result = await response.json();

        // 성공 메시지 출력
        const memberData = {
          username: this.signupData.username,
          email: this.signupData.email,
          loginId: this.signupData.loginId,
        };

        await this.displaySuccessMessage(memberData);

        // 상태 초기화
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
    // confirmPassword는 서버로 전송하지 않음
    const { confirmPassword, ...dataToSend } = this.signupData;

    return {
      loginId: dataToSend.loginId,
      email: dataToSend.email,
      username: dataToSend.username,
      password: dataToSend.password,
      firstName: dataToSend.firstName || "",
      lastName: dataToSend.lastName || "",
      birthDate: dataToSend.birthDate || null,
      jobType: dataToSend.jobType || "ETC",
      biography: dataToSend.biography || "",
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

    // 프롬프트 복원
    this.resetPromptAfterSignup();
  }

  /**
   * 프롬프트를 기본 상태로 복원
   */
  resetPromptAfterSignup() {
    const promptElement = this.currentPrompt.querySelector(".prompt-prefix");
    if (promptElement) {
      promptElement.textContent = this.promptPrefix;
      promptElement.style.color = "#00AAFF"; // 기본 파란색으로 복원
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
   * 회원가입 명령어를 히스토리에 추가 (일반 명령어와 구분)
   */
  addCommandToSignupHistory(command) {
    const line = document.createElement("div");
    line.className = "history-line";

    const prompt = document.createElement("span");
    prompt.className = "history-prompt";
    prompt.textContent = this.currentFieldInfo.prompt + ": ";
    prompt.style.color = "#FFD93D"; // 회원가입 프롬프트는 노란색

    const commandSpan = document.createElement("span");
    commandSpan.className = "history-command";
    commandSpan.textContent = command.split(": ")[1] || ""; // 프롬프트 부분 제거하고 값만 표시

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
      'You can try again by typing "signup" command.',
      "system-msg"
    );
    this.addHistoryLine("", "");

    this.resetSignupState();
  }

  /**
   * 진행률 계산 및 표시
   */
  calculateProgress() {
    const fields = this.getSignupFields();
    const completedFields = Object.keys(this.signupData).length;
    return Math.round((completedFields / fields.length) * 100);
  }

  /**
   * 회원가입 진행 상황 요약 표시 (중간에 확인용)
   */
  showSignupSummary() {
    this.addHistoryLine("", "");
    this.addHistoryLine("Registration Progress Summary:", "info-msg");
    this.addHistoryLine("─".repeat(40), "system-msg");

    Object.entries(this.signupData).forEach(([key, value]) => {
      const displayValue =
        key === "password" || key === "confirmPassword" ? "[HIDDEN]" : value;
      this.addHistoryLine(`  ${key}: ${displayValue}`, "system-msg");
    });

    this.addHistoryLine("─".repeat(40), "system-msg");
    this.addHistoryLine("", "");
  }

  /**
   * 이름 검증 (firstName, lastName)
   */
  async validateName(value) {
    if (value.length > 50) {
      return {
        valid: false,
        error: "Name must be 50 characters or less",
      };
    }

    // 특수문자 제한 (선택적)
    if (!/^[\p{L}\s'-]*$/u.test(value)) {
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

    // 150세 제한
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
      "DEV-OPS",
      "MANAGER",
      "RESEARCHER",
      "ETC",
      // "STUDENT"
    ];

    if (value.toLowerCase() === "list") {
      // 옵션 목록 표시
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
   * 자기소개 검증
   */
  async validateBiography(value) {
    if (value.length > 500) {
      return {
        valid: false,
        error: "Biography must be 500 characters or less",
      };
    }

    return { valid: true };
  }

  /**
   * 직업 옵션 표시
   */
  showJobTypeOptions() {
    this.addHistoryLine("Available job types:", "info-msg");
    // this.addHistoryLine("  STUDENT      - Student", "system-msg");
    this.addHistoryLine("  DEVELOPER     - Software Developer", "system-msg");
    this.addHistoryLine("  DEVOPS        - Devops Engineer", "system-msg");
    this.addHistoryLine("  DATA_ANALYST  - Data Analyst", "system-msg");
    this.addHistoryLine("  DESIGNER      - Designer", "system-msg");
    this.addHistoryLine("  MANAGER       - Project Manager", "system-msg");
    this.addHistoryLine("  RESEARCHER    - Researcher", "system-msg");
    this.addHistoryLine("  ETC           - Other", "system-msg");
    this.addHistoryLine("", "");
  }
}

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
